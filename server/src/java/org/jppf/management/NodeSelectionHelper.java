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
public class NodeSelectionHelper implements NodeSelectionProvider
{
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
  public boolean isNodeAccepted(final AbstractNodeContext node, final NodeSelector selector)
  {
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (selector instanceof AllNodesSelector) return true;
    if (node.isPeer()) return false;
    else if (selector instanceof UuidSelector)
      return ((UuidSelector) selector).getUuids().contains(node.getUuid());
    else if (selector instanceof ExecutionPolicySelector) {
      ExecutionPolicy policy = ((ExecutionPolicySelector) selector).getPolicy();
      TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
      return policy.accepts(node.getSystemInformation());
    }
    throw new IllegalArgumentException("unknown selector type: " + selector.getClass().getName());
  }

  @Override
  public boolean isNodeAccepted(final String nodeUuid, final NodeSelector selector)
  {
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
    if (selector instanceof AllNodesSelector) {
      Set<AbstractNodeContext> fullSet = getNodeNioServer().getAllChannelsAsSet();
      Set<AbstractNodeContext> result = new HashSet<>();
      for (AbstractNodeContext ctx: fullSet) {
        if (hasWorkingJmxConnection(ctx) || (ctx.isPeer() && includePeers)) result.add(ctx);
      }
      return result;
    }
    else if (selector instanceof UuidSelector)
      return getChannels(new HashSet<>(((UuidSelector) selector).getUuids()), includePeers);
    else if (selector instanceof ExecutionPolicySelector)
      return getChannels(((ExecutionPolicySelector) selector).getPolicy(), includePeers);
    throw new IllegalArgumentException("unknown selector type: " + selector.getClass().getName());
  }

  /**
   * Get the available channels with the specified node uuids.
   * @param uuids the node uuids for which we want a channel reference.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @return a {@link Set} of {@link AbstractNodeContext} instances.
   */
  private Set<AbstractNodeContext> getChannels(final Set<String> uuids, final boolean includePeers) {
    Set<AbstractNodeContext> result = new HashSet<>();
    List<AbstractNodeContext> allChannels = getNodeNioServer().getAllChannels();
    for (AbstractNodeContext context: allChannels) {
      if (!hasWorkingJmxConnection(context) && !(context.isPeer() && includePeers)) continue;
      if (uuids.contains(context.getUuid())) result.add(context);
    }
    return result;
  }

  /**
   * Get the available channels for the specified nodes.
   * @param policy an execution to match against the nodes.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @return a {@link Set} of {@link AbstractNodeContext} instances.
   */
  private Set<AbstractNodeContext> getChannels(final ExecutionPolicy policy, final boolean includePeers) {
    if (policy.getContext() == null) TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
    Set<AbstractNodeContext> result = new HashSet<>();
    List<AbstractNodeContext> allChannels = getNodeNioServer().getAllChannels();
    TaskQueueChecker.preparePolicy(policy, null, driver.getStatistics(), 0);
    for (AbstractNodeContext context: allChannels) {
      if (!hasWorkingJmxConnection(context) && !(context.isPeer() && includePeers)) continue;
      JPPFSystemInformation info = context.getSystemInformation();
      if (info == null) continue;
      if (policy.accepts(info)) result.add(context);
    }
    return result;
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  private NodeNioServer getNodeNioServer()
  {
    return driver.getNodeNioServer();
  }

  /**
   * Determine whether the specified node has a working JMX ocnnection.
   * @param ctx the context associated witht he node.
   * @return true if node has a working JMX connection, false otherwise.
   */
  private boolean hasWorkingJmxConnection(final AbstractNodeContext ctx)
  {
    if (ctx.isPeer()) return false;
    JMXNodeConnectionWrapper jmx = ctx.getJmxConnection();
    if ((jmx == null) || !jmx.isConnected()) return false;
    return true;
  }
}
