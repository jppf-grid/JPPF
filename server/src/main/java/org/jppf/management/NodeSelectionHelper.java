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

import org.jppf.management.forwarding.NodeSelectionProvider;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.nio.nodeserver.async.AsyncJobScheduler;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class NodeSelectionHelper implements NodeSelectionProvider {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeSelectionHelper.class);
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Reference to the JPPF driver.
   */
  private final JPPFDriver driver;

  /**
   * 
   * @param driver reference to the JPPF driver.
   */
  public NodeSelectionHelper(final JPPFDriver driver) {
    this.driver = driver;
  }

  /**
   * Determine whether the specified selector accepts the specified node.
   * @param node the node to check.
   * @param selector the node selector used as a filter.
   * @return a set of {@link BaseNodeContext} instances.
   * @exclude
   */
  public boolean isNodeAccepted(final BaseNodeContext node, final NodeSelector selector) {
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (selector instanceof AllNodesSelector) return true;
    if (node.isPeer()) return false;
    if (selector instanceof ExecutionPolicySelector) {
      final ExecutionPolicy policy = ((ExecutionPolicySelector) selector).getPolicy();
      AsyncJobScheduler.preparePolicy(policy, null, driver.getStatistics(), 0);
      return policy.evaluate(node.getSystemInformation());
    }
    return selector.accepts(node.getManagementInfo());
  }

  @Override
  public boolean isNodeAccepted(final String nodeUuid, final NodeSelector selector) {
    if (nodeUuid == null) throw new IllegalArgumentException("node uuid cannot be null");
    final BaseNodeContext node = driver.getAsyncNodeNioServer().getConnection(nodeUuid);
    if (node == null) throw new IllegalArgumentException("unknown selector type: " + selector.getClass().getName());
    return isNodeAccepted(node, selector);
  }

  /**
   * Get a set of channels based on a NodeSelector.
   * @param selector the node selector used as a filter.
   * @return a set of {@link BaseNodeContext} instances.
   */
  public Set<BaseNodeContext> getChannels(final NodeSelector selector) {
    return getChannels(selector, false, false);
  }

  /**
   * Get a set of channels based on a NodeSelector.
   * @param selector the node selector used as a filter.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @param forForwarding whether this is for a node forwarding request, in which case only nodes with a working jmx connection are selected.
   * @return a set of {@link BaseNodeContext} instances.
   */
  public Set<BaseNodeContext> getChannels(final NodeSelector selector, final boolean includePeers, final boolean forForwarding) {
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (selector instanceof ExecutionPolicySelector) return getChannels((ExecutionPolicySelector) selector, includePeers, forForwarding);
    final Set<BaseNodeContext> fullSet = driver.getAsyncNodeNioServer().getAllChannelsAsSet();
    final Set<BaseNodeContext> result = new HashSet<>();
    for (final BaseNodeContext ctx : fullSet) {
      if (nodeAccepted(selector, ctx, includePeers, forForwarding)) result.add(ctx);
    }
    //if (traceEnabled) log.trace("got {} results", result.size());
    return result;
  }

  /**
   * Get the available channels for the specified nodes.
   * @param selector an execution policy selector to match against the nodes.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @param forForwarding whether this is for a node forwarding request, in which case only nodes with a working jmx connection are selected.
   * @return a {@link Set} of {@link BaseNodeContext} instances.
   */
  private Set<BaseNodeContext> getChannels(final ExecutionPolicySelector selector, final boolean includePeers, final boolean forForwarding) {
    final ExecutionPolicy policy = selector.getPolicy();
    if (policy.getContext() == null) AsyncJobScheduler.preparePolicy(policy, null, driver.getStatistics(), 0);
    final Set<BaseNodeContext> result = new HashSet<>();
    final List<BaseNodeContext> allChannels = driver.getAsyncNodeNioServer().getAllChannels();
    AsyncJobScheduler.preparePolicy(policy, null, driver.getStatistics(), 0);
    for (final BaseNodeContext context : allChannels) {
      if (nodeAccepted(selector, context, includePeers, forForwarding)) result.add(context);
    }
    //if (traceEnabled) log.trace("got {} results", result.size());
    return result;
  }
  
  /**
   * Get the management info for a given node or create one if needed.
   * @param context the node for which to get the management info.
   * @return a {@link JPPFManagementInfo} instyance, or {@code null}.
   */
  private static JPPFManagementInfo getManagementInfo(final BaseNodeContext context) {
    JPPFManagementInfo info = context.getManagementInfo();
    if (info == null) {
      final JPPFSystemInformation sysInfo = context.getSystemInformation();
      if (sysInfo != null) {
        info = new JPPFManagementInfo("", "", -1, context.getUuid(), -1, false);
        info.setSystemInfo(sysInfo);
      }
    }
    return info;
  }

  /**
   * Get the number of channels matching a NodeSelector.
   * @param selector the node selector used as a filter.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @param forForwarding whether this is for a node forwarding request, in which case only nodes with a working jmx connection are selected.
   * @return a set of {@link BaseNodeContext} instances.
   */
  public int getNbChannels(final NodeSelector selector, final boolean includePeers, final boolean forForwarding) {
    if (selector == null) throw new IllegalArgumentException("selector cannot be null");
    if (selector instanceof ExecutionPolicySelector) return getNbChannels((ExecutionPolicySelector) selector, includePeers, forForwarding);
    final Set<BaseNodeContext> fullSet = driver.getAsyncNodeNioServer().getAllChannelsAsSet();
    //if (selector instanceof AllNodesSelector) return fullSet.size();
    int result = 0;
    for (final BaseNodeContext ctx : fullSet) {
      if (nodeAccepted(selector, ctx, includePeers, forForwarding)) result++;
    }
    return result;
  }

  /**
   * Get the number of available channels that match the specified execution policy.
   * @param selector an execution policy selector to match against the nodes.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @param forForwarding whether this is for a node forwarding request, in which case only nodes with a working jmx connection are selected.
   * @return a {@link Set} of {@link BaseNodeContext} instances.
   */
  private int getNbChannels(final ExecutionPolicySelector selector, final boolean includePeers, final boolean forForwarding) {
    final ExecutionPolicy policy = selector.getPolicy();
    if (policy.getContext() == null) AsyncJobScheduler.preparePolicy(policy, null, driver.getStatistics(), 0);
    int result = 0;
    final List<BaseNodeContext> allChannels = driver.getAsyncNodeNioServer().getAllChannels();
    AsyncJobScheduler.preparePolicy(policy, null, driver.getStatistics(), 0);
    for (final BaseNodeContext context : allChannels) {
      if (nodeAccepted(selector, context, includePeers, forForwarding)) result++;
    }
    return result;
  }

  /**
   * Determine whether the specified node has a working JMX ocnnection.
   * @param ctx the context associated witht he node.
   * @return {@code true} if node has a working JMX connection, {@code false} otherwise.
   */
  private static boolean hasWorkingJmxConnection(final BaseNodeContext ctx) {
    if (ctx.isPeer()) return true;
    final JMXNodeConnectionWrapper jmx = ctx.getJmxConnection();
    return (jmx != null) && jmx.isConnected();
  }

  /**
   * Determine whether a node is accepted for inclusion in a query result.
   * @param selector a node selector to apply.
   * @param context represent the node.
   * @param includePeers whether peer drivers should be counted as nodes and included.
   * @param forForwarding whether this is for a node forwarding request, in which case only nodes with a working jmx connection are selected.
   * @return {@code true} if the node is accepted, {@code false} otherwise.
   */
  private static boolean nodeAccepted(final NodeSelector selector, final BaseNodeContext context, final boolean includePeers, final boolean forForwarding) {
    if (!includePeers && context.isPeer()) return false;
    final boolean hasJmx = hasWorkingJmxConnection(context);
    final boolean offline = context.isOffline();
    if (traceEnabled) log.trace("includePeers={}, forForwarding={}, hasJmx={}, offline={} for {}", includePeers, forForwarding, hasJmx, offline, context);
    if (forForwarding && !hasJmx) return false;
    if (!forForwarding && !hasJmx && !offline) return false;
    final JPPFManagementInfo info = getManagementInfo(context);
    //if (traceEnabled) log.trace("node '{}', info={}", context.getUuid(), info);
    if (info == null) return false;
    return selector.accepts(info);
  }
}
