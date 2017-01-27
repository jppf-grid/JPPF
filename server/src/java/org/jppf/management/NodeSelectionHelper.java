/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.management.forwarding.NodeSelectionProvider;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.*;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class NodeSelectionHelper implements NodeSelectionProvider {
  /**
   * Reference to the JPPF driver.
   */
  private final JPPFDriver driver = JPPFDriver.getInstance();

  /**
   * Determine whether the specified selector accepts the specified node.
   * @param node the node to check.
   * @param selector the node selector used as a filter.
   * @return a set of {@link AbstractNodeContext} instances.
   * @exclude
   */
  public boolean isNodeAccepted(final AbstractNodeContext node, final NodeSelector selector) {
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (selector instanceof AllNodesSelector) return true;
    if (node.isPeer()) return false;
    if (selector instanceof ExecutionPolicySelector) {
      ExecutionPolicy policy = ((ExecutionPolicySelector) selector).getPolicy();
      TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
      return policy.accepts(node.getSystemInformation());
    }
    return selector.accepts(node.getManagementInfo());
  }

  @Override
  public boolean isNodeAccepted(final String nodeUuid, final NodeSelector selector) {
    if (nodeUuid == null) throw new IllegalArgumentException("node uuid cannot be null");
    AbstractNodeContext node = getNodeNioServer().getConnection(nodeUuid);
    if (node == null) throw new IllegalArgumentException("unknown selector type: " + selector.getClass().getName());
    return isNodeAccepted(node, selector);
  }

  /**
   * Get a set of channels based on a NodeSelector.
   * @param selector the node selector used as a filter.
   * @return a set of {@link AbstractNodeContext} instances.
   */
  public Set<AbstractNodeContext> getChannels(final NodeSelector selector) {
    return getChannels(selector, false);
  }

  /**
   * Get a set of channels based on a NodeSelector.
   * @param selector the node selector used as a filter.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @return a set of {@link AbstractNodeContext} instances.
   */
  public Set<AbstractNodeContext> getChannels(final NodeSelector selector, final boolean includePeers) {
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (selector instanceof ExecutionPolicySelector) return getChannels((ExecutionPolicySelector) selector, includePeers);
    Set<AbstractNodeContext> fullSet = getNodeNioServer().getAllChannelsAsSet();
    Set<AbstractNodeContext> result = new HashSet<>();
    for (AbstractNodeContext ctx : fullSet) {
      if ((hasWorkingJmxConnection(ctx) || (ctx.isPeer() && includePeers)) && selector.accepts(ctx.getManagementInfo())) result.add(ctx);
    }
    return result;
  }

  /**
   * Get the available channels for the specified nodes.
   * @param selector an execution policy selector to match against the nodes.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @return a {@link Set} of {@link AbstractNodeContext} instances.
   */
  private Set<AbstractNodeContext> getChannels(final ExecutionPolicySelector selector, final boolean includePeers) {
    ExecutionPolicy policy = selector.getPolicy();
    if (policy.getContext() == null) TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
    Set<AbstractNodeContext> result = new HashSet<>();
    List<AbstractNodeContext> allChannels = getNodeNioServer().getAllChannels();
    TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
    for (AbstractNodeContext context : allChannels) {
      if (!hasWorkingJmxConnection(context) && !(context.isPeer() && includePeers)) continue;
      JPPFManagementInfo info = context.getManagementInfo();
      if (info == null) continue;
      if (selector.accepts(info)) result.add(context);
    }
    return result;
  }
  
  /**
   * Get the number of channels matching a NodeSelector.
   * @param selector the node selector used as a filter.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @return a set of {@link AbstractNodeContext} instances.
   */
  public int getNbChannels(final NodeSelector selector, final boolean includePeers) {
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (selector instanceof ExecutionPolicySelector) return getNbChannels((ExecutionPolicySelector) selector, includePeers);
    Set<AbstractNodeContext> fullSet = getNodeNioServer().getAllChannelsAsSet();
    int result = 0;
    for (AbstractNodeContext ctx : fullSet) {
      if ((hasWorkingJmxConnection(ctx) || (ctx.isPeer() && includePeers)) && selector.accepts(ctx.getManagementInfo())) result++;
    }
    return result;
  }

  /**
   * Get the number of available channels that match the specified execution policy.
   * @param selector an execution policy selector to match against the nodes.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @return a {@link Set} of {@link AbstractNodeContext} instances.
   */
  private int getNbChannels(final ExecutionPolicySelector selector, final boolean includePeers) {
    ExecutionPolicy policy = selector.getPolicy();
    if (policy.getContext() == null) TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
    int result = 0;
    List<AbstractNodeContext> allChannels = getNodeNioServer().getAllChannels();
    TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
    for (AbstractNodeContext context : allChannels) {
      if (!hasWorkingJmxConnection(context) && !(context.isPeer() && includePeers)) continue;
      JPPFManagementInfo info = context.getManagementInfo();
      if (info == null) continue;
      if (selector.accepts(info)) result++;
    }
    return result;
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  private NodeNioServer getNodeNioServer() {
    return driver.getNodeNioServer();
  }

  /**
   * Determine whether the specified node has a working JMX ocnnection.
   * @param ctx the context associated witht he node.
   * @return true if node has a working JMX connection, false otherwise.
   */
  private boolean hasWorkingJmxConnection(final AbstractNodeContext ctx) {
    if (ctx.isPeer()) return false;
    JMXNodeConnectionWrapper jmx = ctx.getJmxConnection();
    return (jmx != null) && jmx.isConnected();
  }
}
