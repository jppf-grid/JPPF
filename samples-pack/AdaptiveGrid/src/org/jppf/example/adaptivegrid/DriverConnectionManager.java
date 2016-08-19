/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.*;
import org.jppf.utils.TypedProperties;

/**
 * This class encapsulates the functionality for managing a JPPF driver
 * and its attached nodes via the JMX-based management APIs.
 * @author Laurent Cohen
 */
public class DriverConnectionManager {
  /**
   * A proxy to the driver MBean which forwards management requests to the nodes.
   */
  private JPPFNodeForwardingMBean forwarder;
  /**
   * The client connection pool holding the connections to the driver.
   */
  private final JPPFConnectionPool connectionPool;
  /**
   * A node selector that only selects master nodes.
   */
  private final NodeSelector masterSelector;
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
   * Initialize this manager with the specified JPPF client.
   * @param client the client to use.
   * @param maxAllowedNodes the maximum number of nodes that can be running on the host.
   * @param maxAllowedPoolSize the maximum allowed number of connections in the pool.
   * @throws Exception if any error occurs.
   */
  public DriverConnectionManager(final JPPFClient client, final int maxAllowedNodes, final int maxAllowedPoolSize) throws Exception {
    // wait until there is a connection pool with the at least one active connection to the driver
    connectionPool = client.awaitActiveConnectionPool();
    // wait until at least one JMX connection wrapper is established
    JMXDriverConnectionWrapper jmx = connectionPool.awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
    this.forwarder = jmx.getNodeForwarder();
    // create a node selector that only selects master nodes
    ExecutionPolicy masterPolicy = new Equal("jppf.node.provisioning.master", true);
    this.masterSelector = new ExecutionPolicySelector(masterPolicy);
    this.maxAllowedNodes = maxAllowedNodes;
    this.maxAllowedPoolSize = maxAllowedPoolSize;
    this.currentNodes = 1;
  }

  /**
   * Update the connection pool and number of slave nodes based
   * on the specified number of jobs to submit concurrently.
   * @param nbJobs the number of jobs to submit.
   */
  public void updateGridSetup(final int nbJobs) {
    // Adjust the connection pool size
    int newPoolSize = computePoolSize(nbJobs);
    if (newPoolSize > maxAllowedPoolSize) newPoolSize = maxAllowedPoolSize;
    int currentPoolSize = connectionPool.connectionCount();
    if (newPoolSize != currentPoolSize) {
      AdaptiveGridDemo.print("%screasing the number of server connections to %d", (newPoolSize > currentPoolSize) ? "in" : "de", newPoolSize);
      connectionPool.setSize(newPoolSize);
      // wait until all requested connections are established
      connectionPool.awaitWorkingConnections(Operator.EQUAL, newPoolSize);
    }

    // Adjust the number of nodes
    int newNbNodes = computeNbNodes(nbJobs);
    if (newNbNodes != currentNodes) {
      AdaptiveGridDemo.print("%screasing the number of nodes to %d", (newNbNodes > currentNodes) ? "in" : "de", newNbNodes);
      try {
        // -1 because the master node is counted as a an execution node
        updateSlaveNodes(newNbNodes - 1);
        currentNodes = newNbNodes;
      } catch(Exception e) {
        e.printStackTrace();
        // We don't know how many nodes were actually started,
        // so we have to ask the server
        try {
          currentNodes = connectionPool.getJmxConnection().nbNodes();
        } catch(Exception e2) {
          e2.printStackTrace();
        }
      }
    }
  }

  /**
   * Update the number of running slave nodes.
   * @param nbSlaves the number of slave nodes to reach.
   * @param configOverrides optional overrides to the slave nodes' configuration.
   * @throws Exception if any error occurs.
   */
  private void updateSlaveNodes(final int nbSlaves, final TypedProperties configOverrides) throws Exception {
    // request that <nbSlaves> slave nodes be provisioned
    forwarder.provisionSlaveNodes(masterSelector, nbSlaves, configOverrides);
  }

  /**
   * Update the number of running slaves nodes, without configuration oveerides.
   * @param nbSlaves the number of slave nodes to reach.
   * @throws Exception if any error occurs.
   */
  private void updateSlaveNodes(final int nbSlaves) throws Exception {
    updateSlaveNodes(nbSlaves, null);
  }

  /**
   * Compute the desired connection pool size for the specified number of jobs.
   * @param nbJobs the number of jobs to submit concurrently.
   * @return the new size of the connection pool, in the range [1, maxAllowedPoolSize].
   */
  private int computePoolSize(final int nbJobs) {
    // We apply a simple rule that makes the connection pool as large as the
    // number of jobs, up to the maximum allowed pool size.
    return Math.max(1, Math.min(nbJobs, maxAllowedPoolSize));
  }

  /**
   * Compute the desired number of nodes for the specified number of jobs.
   * @param nbJobs the number of jobs to submit concurrently.
   * @return the new desired number of nodes, always in the range [1, maxAllowedNodes].
   */
  private int computeNbNodes(final int nbJobs) {
    // Since nodes take a lot of system resourcess, we can't have too many.
    // Here we apply a rule that there should be 1 node for every 5 jobs in
    // the queue, up to the allowed maximum number of nodes.
    return Math.min(1 + nbJobs / 5, maxAllowedNodes);
  }
}
