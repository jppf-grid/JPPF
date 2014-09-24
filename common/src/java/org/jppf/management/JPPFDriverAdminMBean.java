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

package org.jppf.management;

import java.util.*;

import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.utils.stats.JPPFStatistics;

/**
 * MBean interface for the management of a JPPF driver.
 * @author Laurent Cohen
 */
public interface JPPFDriverAdminMBean extends JPPFAdminMBean
{
  /**
   * Name of the driver's admin MBean.
   */
  String MBEAN_NAME = "org.jppf:name=admin,type=driver";

  /**
   * Get the latest statistics snapshot from the JPPF driver.
   * @return a <code>JPPFStatistics</code> instance.
   * @throws Exception if any error occurs.
   */
  JPPFStatistics statistics() throws Exception;

  /**
   * Get the number of nodes attached to the driver.
   * @return the number of nodes.
   * @throws Exception if any error occurs.
   */
  Integer nbNodes() throws Exception;

  /**
   * Request the JMX connection information for all the nodes attached to the server.
   * @return a collection of <code>NodeManagementInfo</code> instances.
   * @throws Exception if any error occurs.
   */
  Collection<JPPFManagementInfo> nodesInformation() throws Exception;

  /**
   * Perform a shutdown or restart of the server.
   * @param shutdownDelay the delay before shutting down the server, once the command is received.
   * @param restartDelay the delay before restarting, once the server is shutdown. If it is < 0, no restart occurs.
   * @return an acknowledgement message.
   * @throws Exception if any error occurs.
   */
  String restartShutdown(Long shutdownDelay, Long restartDelay) throws Exception;

  /**
   * Change the bundle size tuning settings.
   * @param algorithm the name opf the load-balancing algorithm to set.
   * @param parameters the algorithm's parameters.
   * @return an acknowledgement or error message.
   * @throws Exception if an error occurred while updating the settings.
   */
  String changeLoadBalancerSettings(String algorithm, Map<Object, Object> parameters) throws Exception;

  /**
   * Obtain the current load-balancing settings.
   * @return an instance of <code>LoadBalancingInformation</code>.
   * @throws Exception if an error occurred while fetching the settings.
   */
  LoadBalancingInformation loadBalancerInformation() throws Exception;

  /**
   * Reset this server's statistics.
   * This method triggers a <code>reset()</code> event via the <code>JPPFDriverStatsManager</code> instance.
   * @throws Exception if any error occurs.
   */
  void resetStatistics() throws Exception;

  /**
   * Compute the number of nodes that would match the specified execution policy.
   * @param policy the execution policy to check against the nodes.
   * @return the number of nodes that match the execution policy, or 0 if no node matches it.
   * @throws Exception if any error occurs.
   */
  Integer matchingNodes(ExecutionPolicy policy) throws Exception;

  /**
   * Get the number of nodes currently idle.
   * @return the number of idle nodes.
   * @throws Exception if any error occurs.
   */
  Integer nbIdleNodes() throws Exception;

  /**
   * Request the JMX connection information for all the idle nodes attached to the server.
   * @return a collection of <code>NodeManagementInfo</code> instances.
   * @throws Exception if any error occurs.
   */
  Collection<JPPFManagementInfo> idleNodesInformation() throws Exception;

  /**
   * Toggle the activate state of the specified nodes. Nodes in 'active' state will be deactivated, nodes in 'inactive' state will be activated.
   * @param selector determines which nodes will be activated or deactivated.
   * @throws Exception if any error occurs.
   */
  void toggleActiveState(NodeSelector selector) throws Exception;

  /**
   * Activate or deactivate the broadcasting of the driver's connection information.
   * if the broadcast is already in the desired state, this method has no effect.
   * @param broadcasting {@code true} to activate the broadcast, {@code false} to deactivate it.
   * @throws Exception if any error occurs.
   */
  void setBroadcasting(boolean broadcasting) throws Exception;

  /**
   * Determine whether the driver is broadcasting or not.
   * @return {@code true} if the broadcasting service is on, {@code false} if it is off.
   * @throws Exception if any error occurs.
   */
  boolean getBroadcasting() throws Exception;
}
