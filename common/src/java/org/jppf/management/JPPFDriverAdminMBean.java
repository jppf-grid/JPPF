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

package org.jppf.management;

import java.util.*;

import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.doc.*;
import org.jppf.utils.stats.JPPFStatistics;

/**
 * MBean interface for the management of a JPPF driver.
 * @author Laurent Cohen
 */
@MBeanDescription("administration of the JPPF server")
public interface JPPFDriverAdminMBean extends JPPFAdminMBean {
  /**
   * Name of the driver's admin MBean.
   */
  String MBEAN_NAME = "org.jppf:name=admin,type=driver";

  /**
   * Get the latest statistics snapshot from the JPPF driver.
   * @return a <code>JPPFStatistics</code> instance.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the server statistics")
  JPPFStatistics statistics() throws Exception;

  /**
   * Get the number of nodes attached to the driver.
   * Note that this method is equivalent to calling {@link #nbNodes(NodeSelector) nbNodes(null)}.
   * @return the number of nodes, or -1 if information on the nodes could not be retrieved. The returned number does not include peer drivers.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the number of nodes attached to the driver")
  Integer nbNodes() throws Exception;

  /**
   * Get the number of nodes attached to the driver that satisfy the specified selector.
   * @param selector specifies which nodes shouyld be counted. If null, then {@link NodeSelector#ALL_NODES} will be used.
   * @return the number of nodes, or -1 if information on the nodes could not be retrieved. The returned number does not include peer drivers.
   * @throws Exception if any error occurs.
   * @since 5.0
   */
  @MBeanDescription("get the number of selected nodes attached to the driver")
  Integer nbNodes(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Request the JMX connection information for all the nodes attached to the server.
   * Note that this method is equivalent to calling {@link #nodesInformation(NodeSelector, boolean) nodesInformation(null, false)}.
   * @return a collection of {@link JPPFManagementInfo} instances, or {@code null} if information on the nodes could not be retrieved.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the JMX connection information for all the nodes attached to the server")
  Collection<JPPFManagementInfo> nodesInformation() throws Exception;

  /**
   * Request the JMX connection information for all the nodes attached to the server which satisfy the specified selector.
   * Note that this method is equivalent to calling {@link #nodesInformation(NodeSelector, boolean) nodesInformation(selector, false)}.
   * @param selector specifies which nodes shouyld be counted. If {@code null}, then {@link NodeSelector#ALL_NODES} will be used.
   * @return a collection of {@link JPPFManagementInfo} instances, or {@code null} if information on the nodes could not be retrieved.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the JMX connection information for the selected nodes attached to the server")
  Collection<JPPFManagementInfo> nodesInformation(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Request the JMX connection information for all the nodes attached to the server which satisfy the specified selector.
   * @param selector specifies which nodes shouyld be counted. If {@code null}, then {@link NodeSelector#ALL_NODES} will be used.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @return a collection of {@link JPPFManagementInfo} instances, or {@code null} if information on the nodes could not be retrieved.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the JMX connection information for the selected nodes attached to the server, possibly including other peer drivers")
  Collection<JPPFManagementInfo> nodesInformation(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("icludePeers") boolean includePeers) throws Exception;

  /**
   * Perform a shutdown or restart of the server.
   * @param shutdownDelay the delay before shutting down the server, once the command is received.
   * @param restartDelay the delay before restarting, once the server is shutdown. If it is < 0, no restart occurs.
   * @return an acknowledgement message.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("perform a shutdown, possibly followed by a restart, of the server, with a specified delay before each")
  String restartShutdown(@MBeanParamName("shutDownDelay") Long shutdownDelay, @MBeanParamName("restartDelay") Long restartDelay) throws Exception;

  /**
   * Change the bundle size tuning settings.
   * @param algorithm the name opf the load-balancing algorithm to set.
   * @param parameters the algorithm's parameters.
   * @return an acknowledgement or error message.
   * @throws Exception if an error occurred while updating the settings.
   */
  @MBeanDescription("change the load balancer settings")
  String changeLoadBalancerSettings(@MBeanParamName("algorithm") String algorithm, @MBeanParamName("parameters") Map<Object, Object> parameters) throws Exception;

  /**
   * Obtain the current load-balancing settings.
   * @return an instance of <code>LoadBalancingInformation</code>.
   * @throws Exception if an error occurred while fetching the settings.
   */
  @MBeanDescription("get the load balancer settings")
  LoadBalancingInformation loadBalancerInformation() throws Exception;

  /**
   * Reset this server's statistics.
   * This method triggers a <code>reset()</code> event via the <code>JPPFDriverStatsManager</code> instance.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("reste the server statistics")
  void resetStatistics() throws Exception;

  /**
   * Get the number of nodes currently idle.
   * Note that this method is equivalent to calling {@link #nbIdleNodes(NodeSelector) nbIdleNodes(null)}.
   * @return the number of idle nodes, or -1 if information on the nodes could not be retrieved. The returned number does not include peer drivers.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the number of attached nodes currently idle")
  Integer nbIdleNodes() throws Exception;

  /**
   * Get the number of idle nodes attached to the driver that satisfy the specified selector.
   * @param selector specifies which nodes should be counted. If {@code null}, then {@link NodeSelector#ALL_NODES} will be used.
   * @return the number of idle nodes, or -1 if information on the nodes could not be retrieved. The returned number does not include peer drivers.
   * @throws Exception if any error occurs.
   * @since 5.0
   */
  @MBeanDescription("get the number of selected attached nodes currently idle")
  Integer nbIdleNodes(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Get the number of idle nodes attached to the driver that satisfy the specified selector.
   * @param selector specifies which nodes should be counted. If {@code null}, then {@link NodeSelector#ALL_NODES} will be used.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @return the number of idle nodes, or -1 if information on the nodes could not be retrieved. The returned number does not include peer drivers.
   * @throws Exception if any error occurs.
   * @since 5.0
   */
  @MBeanDescription("get the number of selected attached nodes currently idle, possibly including other peer drivers")
  Integer nbIdleNodes(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("includePeers") boolean includePeers) throws Exception;

  /**
   * Request the JMX connection information for all the idle nodes attached to the server.
   * Note that this method is equivalent to calling {@link #idleNodesInformation(NodeSelector) idleNodesInformation(null)}.
   * @return a collection of {@link JPPFManagementInfo} instances, or {@code null} if information on the nodes could not be retrieved.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the JMX connection information for all the idle nodes attached to the server")
  Collection<JPPFManagementInfo> idleNodesInformation() throws Exception;

  /**
   * Request the JMX connection information for all the idle nodes attached to the server which satisfy the specified selector.
   * @param selector specifies which nodes shouyld be counted. If {@code null}, then {@link NodeSelector#ALL_NODES} will be used.
   * @return a collection of {@link JPPFManagementInfo} instances, or {@code null} if information on the nodes could not be retrieved.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the JMX connection information for all the selected idle nodes attached to the server")
  Collection<JPPFManagementInfo> idleNodesInformation(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Toggle the activate state of the specified nodes. Nodes in 'active' state will be deactivated, nodes in 'inactive' state will be activated.
   * @param selector determines which nodes will be activated or deactivated.  If {@code null}, then {@link NodeSelector#ALL_NODES} will be used.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("toggle the activate state of the selected nodes")
  void toggleActiveState(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Get the active states of the nodes specified vith a {@link NodeSelector}.
   * @param selector specifies for which nodes to retrieve the active states. If {@code null}, then {@link NodeSelector#ALL_NODES} will be used.
   * @return a mmaping of node uuids to their active state.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get the activate state of the selected nodes")
  Map<String, Boolean> getActiveState(@MBeanParamName("nodeSelector") NodeSelector selector) throws Exception;

  /**
   * Set the active state of the specified nodes.
   * @param selector determines which nodes will be activated or deactivated. If {@code null}, then {@link NodeSelector#ALL_NODES} will be used.
   * @param active specifies the activer state to set on the selected nodes.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("set the active state of the selected nodes")
  void setActiveState(@MBeanParamName("nodeSelector") NodeSelector selector, @MBeanParamName("active") boolean active) throws Exception;

  /**
   * Activate or deactivate the broadcasting of the driver's connection information.
   * if the broadcast is already in the desired state, this method has no effect.
   * @param broadcasting {@code true} to activate the broadcast, {@code false} to deactivate it.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("whether the driver's connection information is broadcast via UDP multicast")
  void setBroadcasting(boolean broadcasting) throws Exception;

  /**
   * Determine whether the driver is broadcasting or not.
   * @return {@code true} if the broadcasting service is on, {@code false} if it is off.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("whether the driver's connection information is broadcast via UDP multicast")
  boolean getBroadcasting() throws Exception;
}
