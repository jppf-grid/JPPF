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

package org.jppf.example.wordcount;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;

/**
 * This class reads Wikipedia articles from a file and builds JPPF tasks and jobs from these articles.
 * @author Laurent Cohen
 */
public class JobProvider {
  /**
   * The object that reads articles from a file.
   */
  private final DataReader reader;
  /**
   * Processes and merges the results.
   */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  /**
   * The configuration parameters extracted from the configuration file.
   */
  private final Parameters params;
  /**
   * Count of submitted jobs.
   */
  private int jobCount = 0;
  /**
   * Count of tasks sent for execution.
   */
  private int totalTasksSent = 0;
  /**
   * Count of tasks whose results have been processed.
   */
  private final AtomicInteger totalTasksProcessed = new AtomicInteger(0);
  /**
   * Count of articles read from the file.
   */
  private int totalArticles = 0;
  /**
   * Count of articles read from the file.
   */
  private final AtomicInteger totalRedirects = new AtomicInteger(0);
  /**
   * Number formatter.
   */
  private final NumberFormat nf = WordCountRunner.createFormatter();
  /**
   * Holds the merged results.
   */
  private final Map<String, Long> mergedResults = new HashMap<>();

  /**
   * Initialize this job provider with the specified application parameters.
   * @param params the parameters to use.
   */
  public JobProvider(final Parameters params) {
    this.params = params;
    reader = new DataReader(params.dataFile);
  }

  /**
   * Read the data and return the next job from its tasks.
   * @return a <code>JPPFJob</code>.
   * @throws Exception if any error occurs.
   */
  public JPPFJob nextJob() throws Exception {
    int taskCount  = 0;
    int totalJobArticles = 0;
    JPPFJob job = new JPPFJob();
    job.setName("job-" + (jobCount + 1));
    // job can be sent over this number of channels in parallel
    job.getClientSLA().setMaxChannels(params.nbChannels);
    job.setBlocking(false);
    while (!reader.isClosed() && (taskCount < params.nbTasks)) {
      int articleCount = 0;
      String article = null;
      List<String> list = new ArrayList<>(params.nbArticles);
      while (!reader.isClosed() && (articleCount < params.nbArticles)) {
        article = reader.nextArticle();
        if (article == null) break;
        list.add(article);
        articleCount++;
      }
      if (articleCount > 0) {
        job.add(new WordCountTask(list));
        totalJobArticles += articleCount;
        taskCount++;
      }
    }
    if (taskCount > 0) {
      totalArticles += totalJobArticles;
      jobCount++;
      totalTasksSent += taskCount;
      job.addJobListener(new MyResultCollector());
      System.out.println("submitting job " + nf.format(jobCount) + " with " + nf.format(taskCount) + " tasks and " + nf.format(totalJobArticles) + " articles");
      return job;
    }
    return null;
  }

  /**
   * Get the total number of submitted jobs.
   * @return an int value.
   */
  public int getJobCount() {
    return jobCount;
  }

  /**
   * Get the total number of tasks to the server.
   * @return the number of tasks as an int value.
   */
  public int getTotalTasksSent() {
    return totalTasksSent;
  }

  /**
   * Get the total number of executed tasks whose results have been received.
   * @return an int value.
   */
  public int getTotalTasksProcessed() {
    return totalTasksProcessed.get();
  }

  /**
   * Get the total number of articles read form the data file.
   * @return an int value.
   */
  public int getTotalArticles() {
    return totalArticles;
  }

  /**
   * Get the number of redirect found in the data file.
   * @return an int value.
   */
  public int getTotalRedirects() {
    return totalRedirects.get();
  }

  /**
   * Get the merged results from all executed tasks.
   * @return  a mapping of words to their respective count.
   */
  public Map<String, Long> getMergedResults() {
    return mergedResults;
  }

  /**
   * Close this job provider and release the system resources it uses.
   */
  public void close() {
    if (!reader.isClosed()) reader.close();;
    executor.shutdown();
    try {
      while (!executor.awaitTermination(1L, TimeUnit.MILLISECONDS));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * This result collector submits a work item to merge the results of the received tasks.
   * With this mechanism, results are processed almost as soon as they are received from the
   * nodes, thus allowing us to emulate a stream of incoming results.
   */
  private class MyResultCollector extends JobListenerAdapter {
    @Override
    public void jobReturned(final JobEvent event) {
      if (event.getJobTasks() != null) executor.submit(new MergerTask(event.getJobTasks()));
    }
  }

  /**
   * This class merges the results received from a set set of tasks into the aggregated map of word counts.
   */
  private class MergerTask implements Runnable {
    /**
     * The tasks whose results are to be merged.
     */
    private final List<Task<?>> tasks;

    /**
     * Initialize with the specified set of tasks.
     * @param tasks the tasks to process.
     */
    public MergerTask(final List<Task<?>> tasks) {
      this.tasks = tasks;
    }

    @Override
    public void run() {
      for (Task<?> task: tasks) {
        int nbRedirects = ((WordCountTask) task).getNbRedirects();
        if (nbRedirects > 0) totalRedirects.addAndGet(nbRedirects);
        @SuppressWarnings("unchecked")
        Map<String, Long> map = (Map<String, Long>) task.getResult();
        if (map == null) continue;
        task.setResult(null); // to free some memory asap
        for (Map.Entry<String, Long> entry: map.entrySet()) {
          Long count = mergedResults.get(entry.getKey());
          long n = entry.getValue() + (count == null ?  0L : count);
          mergedResults.put(entry.getKey(), n);
        }
      }
      totalTasksProcessed.addAndGet(tasks.size());
    }
  }
}
