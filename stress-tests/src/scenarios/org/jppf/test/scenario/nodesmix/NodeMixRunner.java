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

package org.jppf.test.scenario.nodesmix;

import java.io.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeMixRunner extends AbstractScenarioRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NodeMixRunner.class);
  /**
   * Numbher of jobs to submit.
   */
  private int nbJobs;
  /**
   * Number of tasks per job.
   */
  private int nbTasks;
  /**
   * Duration of each task in ms.
   */
  private long duration;
  /**
   * Not used.
   */
  private int nbChannels;
  /**
   * Max number of expirations for each job dispatch.
   */
  private int maxDispatches;
  /**
   * Expiration timeout for each job dispatch.
   */
  private long dispatchTimeout;
  /**
   * Max number of jobs that can be concurrently submitted via the executor.
   */
  private int concurrentJobs;
  /**
   * Limits the number of jobs that can be submitted concurrently from the executor.
   */
  private Semaphore semaphore;
  /**
   * Executes job submissions in parallel by the same client.
   */
  private ExecutorService executor;
  /**
   * Executes job submissions in parallel by the same client.
   */
  private CompletionService<JPPFJob> completionService;
  /**
   * The JPPF client.
   */
  private JPPFClient client;
  /**
   * Used to format task numbers.
   */
  private DecimalFormat nf;
  /**
   * Used to format job numbers.
   */
  private DecimalFormat nfJob;
  /**
   * Max number of digits in a task number.
   */
  private int digits;

  @Override
  public void run() {
    client = getSetup().getClient();
    TypedProperties config = getConfiguration().getProperties();
    try {
      StreamUtils.waitKeyPressed("Start the admin console and press [Enter] ...");
      nbJobs = config.getInt("nbJobs", 1);
      nbTasks = config.getInt("nbTasks", 1);
      duration = config.getLong("task.duration", 1L);
      nbChannels = config.getInt("jobChannels", 1);
      maxDispatches = config.getInt("max.dispatches", 0);
      dispatchTimeout = config.getLong("dispatch.expiration", 10000L);
      concurrentJobs = config.getInt("max.concurrent.jobs", 1);
      digits = (int) Math.floor(Math.log10(nbTasks)) + 1;
      nf = new DecimalFormat(StringUtils.padRight("", '0', digits));
      int digitsJob = (int) Math.floor(Math.log10(nbJobs)) + 1;
      nfJob = new DecimalFormat(StringUtils.padRight("", '0', digitsJob));

      semaphore = new Semaphore(concurrentJobs);
      executor = new ThreadPoolExecutor(concurrentJobs, concurrentJobs, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new JPPFThreadFactory("NodeMixRunner")) {
        @Override
        protected void afterExecute(final Runnable r, final Throwable t) {
          super.afterExecute(r, t);
          semaphore.release();
        }

        @Override
        public <T> Future<T> submit(final Callable<T> task) {
          try {
            semaphore.acquire();
          } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
          }
          return super.submit(task);
        }
      };
      completionService = new ExecutorCompletionService<>(executor);

      new Thread(new JobSubmitter(), "JobSubmitter").start();
      for (int i=1; i<=nbJobs; i++) {
        Future<JPPFJob> future = completionService.take();
        JPPFJob job = future.get();
        JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
        List<Task<?>> result = collector.awaitResults();
        int nbNoExec = 0;
        int nbErrors = 0;
        int nbOk = 0;
        for (Task<?> task: result) {
          Throwable t = task.getThrowable();
          if (t != null) {
            output(task.getId() + " has an error: " + ExceptionUtils.getStackTrace(t));
            nbErrors++;
          } else if (task.getResult() == null) nbNoExec++;
          else nbOk++;
        }
        output("got results for " + job.getName() + ": tasks in error = " + StringUtils.padLeft("" + nbErrors, ' ', digits) +
            ", not executed = " + StringUtils.padLeft("" + nbNoExec, ' ', digits) + ", good = " + StringUtils.padLeft("" + nbOk, ' ', digits));
      }
      /*
      JMXDriverConnectionWrapper jmx = getSetup().getDriverManagementProxy();
      String debug = (String) jmx.invoke("org.jppf:name=debug,type=driver", "all");
      output("debug info:\n" + debug);
      output(jmx.statistics().toString());
      */
      for (int i=1; i<=getConfiguration().getNbDrivers(); i++) {
        File file = new File(String.format("%s/logs/driver-%d.log", getConfiguration().getConfigDir().getPath(), i));
        searchTextInFile(file, "expiring");
      }
      StreamUtils.waitKeyPressed();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (executor != null) executor.shutdownNow();
    }
  }

  /**
   * Creates a submits a job to the JPPF client.
   */
  private class JobSubmitter implements Runnable {

    @Override
    public void run() {
      for (int i=1; i<= nbJobs; i++) {
        completionService.submit(new JobSubmissionTask(i));
      }
    }
  }

  /**
   * Instances of this class create and submit a job via the JPPF client.
   */
  private class JobSubmissionTask implements Callable<JPPFJob> {
    /**
     * The job to create and submit.
     */
    private JPPFJob job;
    /**
     * The index of the job to submit.
     */
    private final int index;

    /**
     * Initialize this task with the specified index.
     * @param index the index of the job to submit.
     */
    public JobSubmissionTask(final int index) {
      this.index = index;
    }

    @Override
    public JPPFJob call() throws Exception {
      job = new JPPFJob();
      job.setName("job-" + nfJob.format(index));
      for (int i=1; i<=nbTasks;i++) job.add(new NodeMixTask(duration)).setId("task-" + nf.format(i));
      job.getSLA().setMaxDispatchExpirations(maxDispatches);
      job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(dispatchTimeout));
      job.getClientSLA().setMaxChannels(nbChannels);
      client.submitJob(job);
      return job;
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  private void output(final String message) {
    System.out.println(message);
    log.info(message);
  }

  /**
   * Search for an occurrence of the given text in the specified file.
   * @param file the file to search.
   * @param text the text to search for.
   * @return the line of the file that contains the text, along with the line number, or <code>null</code> if the text couldn't be found.
   * @throws Exception if any error occurs.
   */
  private Pair<Integer, String> searchTextInFile(final File file, final String text) throws Exception {
    Pair<Integer, String> result = null;
    Pattern pattern = Pattern.compile(".*" + text + ".*");
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String s;
      int count = 0;
      while (((s = reader.readLine()) != null) && (result == null)) {
        count++;
        if (pattern.matcher(s).matches()) result = new Pair<>(count, s);
      }
    }
    if (result != null) output("'" + text + "' found in '" + file + "'" + result.first() + " :\n" + result.second());
    else output("'" + text + "' was not found in '" + file +  "'");
    return result;
  }
}
