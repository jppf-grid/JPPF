/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;

/**
 * 
 * @author Laurent Cohen
 */
public class SubmitQueue extends ThreadSynchronization implements Runnable {
  /**
   * 
   */
  private BlockingQueue<JPPFJob> queue = null;
  /**
   * 
   */
  @SuppressWarnings("unused")
  private List<Task<?>> results = null;
  /**
   * 
   */
  private final JPPFClient client;
  /**
   * 
   */
  private final AtomicInteger resultCount = new AtomicInteger(0);

  /**
   * 
   * @param client the JPPF clmient to submit to.
   */
  public SubmitQueue(final JPPFClient client) {
    this.client = client;
    final int capacity = JPPFConfiguration.getProperties().getInt("largedata.job.cache.size", 1);
    queue = new ArrayBlockingQueue<>(capacity);
  }

  @Override
  public void run() {
    while (!isStopped()) {
      try {
        final JPPFJob job = queue.poll(1L, TimeUnit.MILLISECONDS);
        if (job != null) {
          results = client.submit(job);
          resultCount.incrementAndGet();
        }
      } catch (final Exception e) {
        e.printStackTrace();
        setStopped(true);
      }
    }
  }

  /**
   * Submit a job.
   * @param job the job to submit.
   */
  public void submit(final JPPFJob job) {
    try {
      queue.put(job);
    } catch (final InterruptedException e) {
      setStopped(true);
      e.printStackTrace();
    }
  }

  /**
   * Get the count of job resultls received.
   * @return the count as an int.
   */
  public int getResultCount() {
    return resultCount.get();
  }
}
