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

package org.jppf.example.adaptivegrid;

import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.event.*;

/**
 * This queue listener adjusts the grid topology and the number of connections to the server
 * based on the number of jobs currently in the submission queue.
 * @author Laurent Cohen
 */
public class MyQueueListener implements ClientQueueListener {
  /**
   * Manages the driver and its attached nodes.
   */
  private final DriverConnectionManager manager;
  /**
   * The maximum number of nodes that can be running on the host.
   */
  private final int maxAllowedNodes;
  /**
   * The maximum allowed number of connections in the pool.
   */
  private final int maxAllowedPoolSize;
  /**
   * The current number of nodes. We attempt to store and maintain it locally because
   * obtaining it from the server is a costly operation that requires a remote method call.
   */
  private int currentNodes;

  /**
   * Initialize this client queue listener.
   * @param manager manages the driver and its attached nodes.
   * @param maxAllowedNodes the maximum number of nodes that can be running on the host.
   * @param maxAllowedPoolSize the maximum allowed number of connections in the pool.
   */
  public MyQueueListener(final DriverConnectionManager manager, final int maxAllowedNodes, final int maxAllowedPoolSize) {
    this.manager = manager;
    this.maxAllowedNodes = maxAllowedNodes;
    this.maxAllowedPoolSize = maxAllowedPoolSize;
    this.currentNodes = 1;
    try {
      int nbNodes = manager.getNbNodes();
      // We want only 1 node at the start, so we terminate any slave
      // nodes that might be active, and just keep the master node
      if (nbNodes > 1) this.manager.updateSlaveNodes(0);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public synchronized void jobAdded(final ClientQueueEvent event) {
    updateGridSetup(event.getQueueSize());
  }

  @Override
  public synchronized void jobRemoved(final ClientQueueEvent event) {
    updateGridSetup(event.getQueueSize());
  }

  /**
   * Update the connection pool and number of slave nodes based
   * on the specified job queue size.
   * @param queueSize the current job queue size.
   */
  private void updateGridSetup(final int queueSize) {
    JPPFConnectionPool pool = manager.getConnectionPool();

    // Adjust the connection pool size
    int newPoolSize = computePoolSize(queueSize);
    if ((newPoolSize > pool.getCoreSize()) && (newPoolSize < maxAllowedPoolSize)) {
      int currentPoolSize = manager.getConnectionPool().connectionCount();
      if (newPoolSize != currentPoolSize) {
        System.out.printf("%screasing the number of server connections to %d\n", (newPoolSize > currentPoolSize) ? "in" : "de", newPoolSize);
        pool.setMaxSize(newPoolSize);
      }
    }

    // Adjust the number of nodes
    int newNbNodes = computeNbNodes(queueSize);
    if (newNbNodes != currentNodes) {
      System.out.printf("%screasing the number of nodes to %d\n", (newNbNodes > currentNodes) ? "in" : "de", newNbNodes);
      try {
        // -1 because the master node is counted as a an execution node
        manager.updateSlaveNodes(newNbNodes - 1);
        currentNodes = newNbNodes;
      } catch(Exception e) {
        e.printStackTrace();
        // We don't know how many nodes were actually started,
        // so we have to ask the server
        try {
          currentNodes = manager.getNbNodes();
        } catch(Exception e2) {
          e2.printStackTrace();
        }
      }
    }
  }

  /**
   * Compute the desired connection pool size for the specified queue size.
   * @param queueSize the current queue size.
   * @return the new size of the connection pool, in the range [0, maxAllowedPoolSize].
   */
  private int computePoolSize(final int queueSize) {
    // We apply a simple rule that makes the connection pool as large as the
    // number of jobs in the queue, up to the maximum allowed pool size.
    return Math.min(queueSize, maxAllowedPoolSize);
  }

  /**
   * Compute the desired number of nodes for the specified queue size.
   * @param queueSize the current queue size.
   * @return the new desired number of nodes, always in the range [1, maxAllowedNodes].
   */
  private int computeNbNodes(final int queueSize) {
    // Since nodes take a lot of system resourcess, we can't have too many.
    // Here we apply a rule that there should be 1 node for every 5 jobs in
    // the queue, up to the allowed maximum number of nodes.
    return Math.min(1 + queueSize / 5, maxAllowedNodes);
  }
}
