/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import java.util.List;
import java.util.concurrent.*;

import org.jppf.node.protocol.*;
import org.jppf.utils.Pair;
import org.jppf.utils.concurrent.ThreadSynchronization;

/**
 * Get job results from a queue and send them back to the driver.
 * @exclude
 */
public class JobWriter extends ThreadSynchronization implements Runnable {
  /**
   * The node which sends the messages.
   */
  private final JPPFNode node;
  /**
   * The queue of received jobs.
   */
  private BlockingQueue<Pair<TaskBundle, List<Task<?>>>> queue = new LinkedBlockingQueue<>();
  /**
   * Captures the last exception caught suring an I/O operation.
   */
  private Exception lastException;

  /**
   * 
   * @param node the node which sends the messages.
   */
  JobWriter(final JPPFNode node) {
    this.node = node;
  }

  @Override
  public void run() {
    while (!isStopped() && !node.isStopped()) {
      try {
        final Pair<TaskBundle, List<Task<?>>> pair = queue.take();
        node.processResults(pair.first(), pair.second());
      } catch (final Exception e) {
        lastException = e;
        setStopped(true);
        break;
      }
    }
  }

  /**
   * Put the next job results in the send queue.
   * @param bundle the bundle that contains the tasks and header information.
   * @param taskList the tasks results.
   * @throws Exception if any error occurs.
   */
  public void putJob(final TaskBundle bundle, final List<Task<?>> taskList) throws Exception {
    if (lastException != null) {
      final Exception e = lastException;
      lastException = null;
      throw e;
    }
    queue.offer(new Pair<>(bundle, taskList));
  }

  /**
   * Close this job writer.
   */
  void close() {
    setStopped(true);
    queue.clear();
  }
}
