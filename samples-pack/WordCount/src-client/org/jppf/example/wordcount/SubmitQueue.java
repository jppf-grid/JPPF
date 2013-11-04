/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.utils.*;

/**
 * This class provides a bounded queue for submitting JPPF jobs and process their results.
 * The queue capacity is specified as the value of a configuration property, for example:<br>
 * <code>wordcount.job.capacity = 3</code><br>
 * Here this means that at most 3 jobs at a will be processed by the server at any given time, while
 * a 4th job will wait until a slot is available.
 * <p>This parameter is used to put a cap on the memory footprint of the client application, since it
 * continues reading data from the wikipedia file during that time, generating a stream of JPPF jobs. 
 * @author Laurent Cohen
 */
public class SubmitQueue extends ThreadSynchronization
{
  /**
   * The JPPF client ot which jobs are submitted.
   */
  private final JPPFClient client;
  /**
   * Total number of submitted jobs.
   */
  private final AtomicInteger resultCount = new AtomicInteger(0);
  /**
   * Executes the jobs and waits for their results.
   */
  private ExecutorService executor;
  /**
   * Maximum number of concurrent jobs.
   */
  private int capacity = 1;
  /**
   * The current number of jobs being executed.
   */
  private int currentJobCount = 0;

  /**
   * Initiialize this submit queue with the specified JPPF client.
   * @param capacity represents the maximum number of jobs that can be executed concurrently by the JPPF server.
   */
  public SubmitQueue(final int capacity) {
    this.client = new JPPFClient();
    this.capacity = capacity;
  }

  /**
   * Submit a job. This method may block if the job capacity has been reached.
   * It will then wait until a job slot becomes available.
   * @param job the job to submit.
   */
  public void submit(final JPPFJob job) {
    // submitting thread waits until a slot is free for submitting a job
    synchronized(SubmitQueue.this) {
      while (currentJobCount >= capacity) goToSleep();
      currentJobCount++;
    }
    executor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          client.submitJob(job);
          JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
          collector.awaitResults();
          resultCount.incrementAndGet();
          // notify submitting threads that a slot is free for submitting a job
          synchronized(SubmitQueue.this) {
            currentJobCount--;
            wakeUp();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Get the count of job results received.
   * @return the count as an int.
   */
  public int getResultCount() {
    return resultCount.get();
  }

  /**
   * Start this submit queue.
   */
  public void start() {
    BlockingQueue<Runnable> queue = new ArrayBlockingQueue(capacity);
    executor = new ThreadPoolExecutor(capacity, capacity, 1L, TimeUnit.MINUTES, queue, new JPPFThreadFactory("SubmitQueue"));
  }

  /**
   * Stop this submit queue.
   */
  public void stop() {
    executor.shutdownNow();
    client.close();
  }
}
