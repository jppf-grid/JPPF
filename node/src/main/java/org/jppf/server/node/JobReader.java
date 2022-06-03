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

package org.jppf.server.node;

import java.util.concurrent.*;

import org.jppf.node.protocol.BundleWithTasks;
import org.jppf.utils.concurrent.ThreadSynchronization;

/**
 * Read the jobs from the network connection and make them available in a queue.
 * @exclude
 */
class JobReader extends ThreadSynchronization implements Runnable {
  /**
   * Bundle set in the JobReader or JobWriter queue when an exception occurs.
   */
  private static final BundleWithTasks EXCEPTIONAL_BUNDLE = new BundleWithTasks(null, null);
  /**
   * The node which receives the messages.
   */
  private final JPPFNode node;
  /**
   * The queue of received jobs.
   */
  private BlockingQueue<BundleWithTasks> queue = new LinkedBlockingQueue<>();
  /**
   * Captures the last exception caught suring an I/O operation.
   */
  private Exception lastException; 

  /**
   * 
   * @param node the node which sends the messages.
   */
  JobReader(final JPPFNode node) {
    this.node = node;
  }

  @Override
  public void run() {
    while (!isStopped() && !node.isStopped() && !node.hasPendingAction()) {
      try {
        queue.offer(node.getNodeIO().readJob());
      } catch (final Exception e) {
        lastException = e;
        setStopped(true);
        // to avoid being stuck in queue.take() when calling the nextJob() method
        queue.offer(EXCEPTIONAL_BUNDLE);
        break;
      }
    }
  }

  /**
   * Get the next job from the queue, blocking if the queue is empty.
   * @return a pairing of a job header and its tasks.
   * @throws Exception if any error occurs.
   */
  BundleWithTasks nextJob() throws Exception {
    BundleWithTasks result = null;
    if (lastException == null) result = queue.take();
    if (lastException != null) {
      queue.clear();
      final Exception e = lastException;
      lastException = null;
      throw e;
    }
    return result;
  }

  /**
   * Close this job reader.
   */
  void close() {
    setStopped(true);
    queue.clear();
  }
}
