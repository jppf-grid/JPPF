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

import java.util.List;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
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
   * 
   */
  private int nbJobs;
  /**
   * 
   */
  private int nbTasks;
  /**
   * 
   */
  private long duration;
  /**
   * 
   */
  private int nbChannels;
  /**
   * 
   */
  private int maxDispatches;
  /**
   * 
   */
  private long dispatchTimeout;
  /**
   * 
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
   * 
   */
  private CompletionService<JPPFJob> completionService;
  /**
   * 
   */
  private JPPFClient client;

  @Override
  public void run() {
    client = getSetup().getClient();
    TypedProperties config = getConfiguration().getProperties();
    try {
      nbJobs = config.getInt("nbJobs", 1);
      nbTasks = config.getInt("nbTasks", 1);
      duration = config.getLong("task.duration", 1L);
      nbChannels = config.getInt("jobChannels", 1);
      maxDispatches = config.getInt("max.dispatches", 0);
      dispatchTimeout = config.getLong("dispatch.expiration", 10000L);
      concurrentJobs = config.getInt("max.concurrent.jobs", 1);
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
        System.out.println("got results for " + job.getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (executor != null) executor.shutdownNow();
    }
  }

  /**
   * 
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
      job.setName("job-" + index);
      for (int i=1; i<=nbTasks;i++) job.add(new NodeMixTask(duration)).setId("task-" + i);
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
  private static void output(final String message) {
    System.out.println(message);
    log.info(message);
  }
}
