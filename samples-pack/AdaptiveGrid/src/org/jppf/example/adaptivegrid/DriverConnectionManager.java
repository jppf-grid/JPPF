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
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.TypedProperties;

/**
 * This class encapsulates the functionality for managing a JPPF driver
 * and its attached nodes via the JMX-based management APIs.
 * @author Laurent Cohen
 */
public class DriverConnectionManager implements AutoCloseable {
  /**
   * The signature of the provisioning method in the nodes provisioning MBeans.
   */
  private static final String[] PROVISIONING_SIGNATURE = {int.class.getName(), TypedProperties.class.getName()};
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
   * Initialize this manager with the specified JPPF client and connection pool.
   * @param client the client to use.
   * @param poolName the name of the connection pool to use.
   * @throws Exception if any error occurs.
   */
  public DriverConnectionManager(final JPPFClient client, final String poolName) throws Exception {
    JPPFConnectionPool tmpPool;
    // wait until the connection pool is created by the client
    while ((tmpPool = client.findConnectionPool(poolName)) == null) Thread.sleep(20L);
    this.connectionPool = tmpPool;
    // wait until the connection pool has at least one active connection to the driver
    while (this.connectionPool.connectionCount(JPPFClientConnectionStatus.ACTIVE) <= 0) Thread.sleep(20L);
    // wait until the at least one JMX connection wrapper is created
    JMXDriverConnectionWrapper jmx = null;
    while ((jmx = connectionPool.getJmxConnection()) == null) Thread.sleep(20L);
    // wait until the JMX connection is established
    while (!jmx.isConnected()) Thread.sleep(20L);
    this.forwarder = jmx.getNodeForwarder();
    // create a node selector that only selects master nodes
    ExecutionPolicy masterPolicy = new Equal("jppf.node.provisioning.master", true);
    this.masterSelector = new ExecutionPolicySelector(masterPolicy);
  }

  /**
   * Update the number of running slave nodes.
   * @param nbSlaves the number of slave nodes to reach.
   * @param configOverrides optional overrides to the slave nodes' configuration.
   * @throws Exception if any error occurs.
   */
  public void updateSlaveNodes(final int nbSlaves, final TypedProperties configOverrides) throws Exception {
    String mbeanName = JPPFNodeProvisioningMBean.MBEAN_NAME;
    Object[] params = { nbSlaves, configOverrides };
    // request that <nbSlaves> slave nodes be provisioned
    forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", params, PROVISIONING_SIGNATURE);
  }

  /**
   * Update the number of running slaves nodes, without configuration oveerides.
   * @param nbSlaves the number of slave nodes to reach.
   * @throws Exception if any error occurs.
   */
  public void updateSlaveNodes(final int nbSlaves) throws Exception {
    updateSlaveNodes(nbSlaves, null);
  }

  /**
   * Get the number of nodes in the grid.
   * @return the number of nodes currently attached to the driver.
   * @throws Exception if any error occurs.
   */
  public int getNbNodes() throws Exception {
    return connectionPool.getJmxConnection().nbNodes();
  }

  /**
   * The client connection pool holding the connections to the driver.
   * @return a {@link JPPFConnectionPool} instance.
   */
  public JPPFConnectionPool getConnectionPool() {
    return connectionPool;
  }

  @Override
  public void close() {
  }
}
