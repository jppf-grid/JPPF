/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.test.largedata;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class LargeDataRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(LargeDataRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * Tjhe object that reads articles from a file.
   */
  private static DataReader reader = null;
  /**
   * Path to the data file.
   */
  private static String dataFile = null;
  /**
   * Maximum number of articles per task.
   */
  private static int nbArticles = 0;
  /**
   * Maximum number of tasks per job.
   */
  private static int nbTasks = 0;
  /**
   * Number of client channels.
   */
  private static int nbChannels = 0;
  /**
   * Count of submitted jobs.
   */
  private static int jobCount = 0;
  /**
   * Count of tasks sent for execution.
   */
  private static int totalTasksSent = 0;
  /**
   * Count of tasks whose results have been processed.
   */
  private static AtomicInteger totalTasksProcessed = new AtomicInteger(0);
  /**
   * Count of articles read from the file.
   */
  private static int totalArticles = 0;
  /**
   * Number formatter.
   */
  private static NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
  /**
   * Processes and merges the results.
   */
  private static ExecutorService executor = Executors.newSingleThreadExecutor();
  /**
   * Holds the merged results.
   */
  private static Map<String, Long> mergedResults = new TreeMap<>();

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    SubmitQueue queue = null;
    try
    {
      nf.setGroupingUsed(true);
      TypedProperties config = JPPFConfiguration.getProperties();
      dataFile = config.getString("largedata.file");
      nbArticles = config.getInt("largedata.articles.per.task");
      nbTasks = config.getInt("largedata.tasks.per.job");
      nbChannels = config.getInt("largedata.channels");
      if (nbChannels < 1) nbChannels = 1;
      config.setProperty("jppf.pool.size", String.valueOf(nbChannels));
      jppfClient = new JPPFClient();
      queue = new SubmitQueue(jppfClient);
      Thread t = new Thread(queue, "SubmitQueue");
      t.setDaemon(true);
      t.start();
      System.out.println("Processing '" + dataFile + "' with " + nf.format(nbArticles) + " articles per task, " + nf.format(nbTasks) + " tasks per job, nb channels = " + nbChannels);
      reader = new DataReader(dataFile);
      boolean end = false;
      long start = System.nanoTime();
      JPPFJob job;
      while ((job = nextJob()) != null)
      {
        queue.submit(job);
        //List<JPPFTask> results = queue.fetchResults();
      }
      Object lock = new Object();
      //while (queue.getResultCount() < jobCount)
      while (totalTasksProcessed.get() < totalTasksSent)
      {
        synchronized(lock)
        {
          lock.wait(1L);
        }
      }
      executor.shutdown();
      while (!executor.awaitTermination(1L, TimeUnit.MILLISECONDS));
      long elapsed = (System.nanoTime() - start) / 1000000L;
      System.out.println("processed " + jobCount + " jobs for " + nf.format(totalArticles) +
          " articles in " + StringUtils.toStringDuration(elapsed) + " (" + nf.format(elapsed) + " ms)");
      BufferedWriter writer = new BufferedWriter(new FileWriter("CountResults.txt"));
      System.out.println("writing results ..."); 
      for (Map.Entry<String, Long> entry: mergedResults.entrySet())
      {
        writer.write(StringUtils.padRight(entry.getKey(), ' ', 26, false));
        writer.write(": ");
        writer.write(StringUtils.padLeft(nf.format(entry.getValue()), ' ', 12));
        writer.write('\n');
      }
      writer.flush();
      writer.close();
      System.out.println("done"); 
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (queue != null) queue.setStopped(true);
      if (jppfClient != null) jppfClient.close();
      if (reader != null) reader.close();
    }
  }

  /**
   * Read the data and return the next job from its tasks.
   * @return a <code>JPPFJob</code>.
   * @throws Exception if any error occurs.
   */
  private static JPPFJob nextJob() throws Exception
  {
    int taskCount  = 0;
    int totalJobArticles = 0;
    JPPFJob job = new JPPFJob();
    job.setName("job-" + (jobCount + 1));
    job.getClientSLA().setMaxChannels(nbChannels);
    while (!reader.isClosed() && (taskCount < nbTasks))
    {
      int articleCount = 0;
      String article = null;
      List<String> list = new ArrayList<>(nbArticles);
      while (!reader.isClosed() && (articleCount < nbArticles))
      {
        article = reader.nextArticle();
        if (article == null) break;
        list.add(article);
        articleCount++;
      }
      if (articleCount > 0)
      {
        job.add(new LargeDataTask(list));
        totalJobArticles += articleCount;
        taskCount++;
      }
    }
    if (taskCount > 0)
    {
      totalArticles += totalJobArticles;
      jobCount++;
      totalTasksSent += taskCount;
      job.addJobListener(new MyJobListener());
      System.out.println("submitting job " + nf.format(jobCount) + " with " + nf.format(taskCount) + " tasks and " + nf.format(totalJobArticles) + " articles");
      return job;
    }
    return null;
  }

  /**
   * 
   */
  private static class SubmitTask implements Callable<List<Task<?>>>
  {
    /**
     * The job to submit.
     */
    private final JPPFJob job;

    /**
     * Initialize with the specified job.
     * @param job the job to submit.
     */
    public SubmitTask(final JPPFJob job)
    {
      this.job = job;
    }

    @Override
    public List<Task<?>> call() throws Exception
    {
      return jppfClient.submitJob(job);
    }
  }

  /**
   * 
   */
  private static class MyJobListener extends JobListenerAdapter
  {
    @Override
    public synchronized void jobReturned(final JobEvent event)
    {
      //System.out.println("received " + event.getTaskList().size() + " task results");
      executor.submit(new MergerTask(event.getJobTasks()));
    }
  }

  /**
   * 
   */
  private static class MergerTask implements Runnable
  {
    /**
     * 
     */
    private final List<Task<?>> tasks;

    /**
     * 
     * @param tasks the tasks to process.
     */
    public MergerTask(final List<Task<?>> tasks)
    {
      this.tasks = tasks;
    }

    @Override
    public void run()
    {
      for (Task task: tasks)
      {
        @SuppressWarnings("unchecked")
        Map<String, Long> map = (Map<String, Long>) task.getResult();
        if (map == null) continue;
        task.setResult(null);
        for (Map.Entry<String, Long> entry: map.entrySet())
        {
          Long n = mergedResults.get(entry.getKey());
          if (n == null) n = entry.getValue();
          else n += entry.getValue();
          mergedResults.put(entry.getKey(), n);
        }
      }
      int n = totalTasksProcessed.addAndGet(tasks.size());
      //System.out.println("processed " + n + " tasks");
    }
  }
}
