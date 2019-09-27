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

package org.jppf.server.nio.nodeserver.async;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.execute.ExecutorStatus;
import org.jppf.load.balancer.JPPFContext;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.server.JPPFContextDriver;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.protocol.ServerJob;
import org.jppf.server.queue.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 */
abstract class AbstractAsyncJobScheduler extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractAsyncJobScheduler.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Whether to allow dispatching to peer drivers without any node attached, defaults to {@code false}.
   */
  final boolean disptachtoPeersWithoutNode;
  /**
   * Random number generator used to randomize the choice of idle channel.
   */
  final Random random = new Random(System.nanoTime());
  /**
   * Reference to the statistics.
   */
  final JPPFStatistics stats;
  /**
   * Reference to the job queue.
   */
  final JPPFPriorityQueue queue;
  /**
   * The list of idle node channels.
   */
  final Set<BaseNodeContext> idleChannels = new LinkedHashSet<>();
  /**
   * Holds information about the execution context.
   */
  final JPPFContext jppfContext;
  /**
   * Used to add channels asynchronously to avoid deadlocks.
   * @see <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-344">JPPF-344 Server deadlock with many slave nodes</a>
   */
  private final ExecutorService channelsExecutor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("NodeChannels"));
  /**
   * The driver's system information
   */
  final JPPFSystemInformation driverInfo;
  /**
   * The load-balancer factory.
   */
  final JPPFBundlerFactory bundlerFactory;
  /**
   * The node server.
   */
  AsyncNodeNioServer server;
  /**
   * Handles reservations of nodes to jobs.
   */
  NodeReservationHandler reservationHandler;
  /**
   * The number of connected nodes below which the driver load-balances to other peer drivers.
   */
  final int peerLoadBalanceThreshold;
  /**
   * Whether bias towards local node is enabled.
   */
  final boolean localNodeBiasEnabled;
  /**
   * Selects the node according to a node selector.
   */
  final NodeSelectionHelper selectionHelper;
  /**
   * 
   */
  final JobDependenciesHandler dependencyHandler; 

  /**
   * Initialize this task queue checker with the specified node server.
   * @param server the node server.
   * @param queue the reference queue to use.
   * @param stats reference to the statistics.
   * @param bundlerFactory the load-balancer factory.
   */
  AbstractAsyncJobScheduler(final AsyncNodeNioServer server, final JPPFPriorityQueue queue, final JPPFStatistics stats, final JPPFBundlerFactory bundlerFactory) {
    this.server = server;
    this.queue = queue;
    this.dependencyHandler = queue.getDependenciesHandler();
    this.disptachtoPeersWithoutNode = server.getDriver().getConfiguration().get(JPPFProperties.PEER_ALLOW_ORPHANS);
    this.jppfContext = new JPPFContextDriver(queue);
    this.stats = stats;
    this.bundlerFactory = bundlerFactory;
    this.driverInfo = server.getDriver().getSystemInformation();
    this.peerLoadBalanceThreshold = server.getDriver().getInitializer().getPeerConnectionPoolHandler().getLoadBalanceThreshold();
    this.localNodeBiasEnabled = server.getDriver().getConfiguration().get(JPPFProperties.LOCAL_NODE_BIAS);
    this.selectionHelper = new NodeSelectionHelper(server.getDriver());
  }

  /**
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  public JPPFContext getJPPFContext() {
    return jppfContext;
  }

  /**
   * Get the number of idle channels.
   * @return the size of the underlying list of idle channels.
   */
  int getNbIdleChannels() {
    synchronized (idleChannels) {
      return idleChannels.size();
    }
  }

  /**
   * Add a channel to the list of idle channels.
   * @param channel the channel to add to the list.
   */
  public void addIdleChannel(final BaseNodeContext channel) {
    if (debugEnabled) log.debug("request to add idle channel {}", channel);
    if (channel == null) {
      final String message  = "channel is null";
      log.error(message);
      throw new IllegalArgumentException(message);
    }
    if (channel.getExecutionStatus() != ExecutorStatus.ACTIVE) { 
      final String message  = "channel is not active: " + channel;
      log.error(message);
      throw new IllegalStateException(message);
    }
    channelsExecutor.execute(() -> {
      if (debugEnabled) log.debug("adding idle channel {}", channel);
      if (!channel.isClosed()) {
        if (!reservationHandler.transitionReservation(channel)) reservationHandler.removeReservation(channel);
        final boolean added;
        synchronized(idleChannels) {
          added = idleChannels.add(channel);
        }
        channel.getIdle().set(true);
        if (added) {
          final JPPFSystemInformation info = channel.getSystemInformation();
          if (info != null) info.getJppf().set(JPPFProperties.NODE_IDLE, true);
          stats.addValue(JPPFStatisticsHelper.IDLE_NODES, 1);
        }
        wakeUp();
      }
      else channel.handleException(null);
    });
  }

  /**
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   * @return a reference to the removed channel.
   */
  BaseNodeContext removeIdleChannel(final BaseNodeContext channel) {
    if (debugEnabled) log.debug("removing idle channel {}", channel);
    final boolean removed;
    synchronized(idleChannels) {
      removed = idleChannels.remove(channel);
    }
    channel.getIdle().set(false);
    if (removed) {
      final JPPFSystemInformation info = channel.getSystemInformation();
      if (info != null) info.getJppf().set(JPPFProperties.NODE_IDLE, false);
      stats.addValue(JPPFStatisticsHelper.IDLE_NODES, -1);
    }
    return channel;
  }

  /**
   * Asynchronously remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   */
  public void removeIdleChannelAsync(final BaseNodeContext channel) {
    if (debugEnabled) log.debug("request to remove idle channel {}", channel);
    channelsExecutor.execute(() -> removeIdleChannel(channel));
  }

  /**
   * Get the list of idle channels.
   * @return a new copy of the underlying list of idle channels.
   */
  public List<BaseNodeContext> getIdleChannels() {
    synchronized (idleChannels) {
      return new ArrayList<>(idleChannels);
    }
  }

  /**
   * Clear the list of idle channels.
   */
  void clearIdleChannels() {
    synchronized (idleChannels) {
      idleChannels.clear();
    }
  }

  /**
   * Set the parameters needed as bounded variables for scripted execution policies.
   * @param policy the root policy to explore.
   * @param job the job containing the sla and metadata.
   * @param stats the server statistics.
   * @param nbJobNodes the number of nodes the job is already dispatched to.
   */
  public static void preparePolicy(final ExecutionPolicy policy, final ServerJob job, final JPPFStatistics stats, final int nbJobNodes) {
    if (policy == null) return;
    if (job == null) policy.setContext(null, null, null, nbJobNodes, stats);
    else policy.setContext(job.getSLA(), null, job.getMetadata(), nbJobNodes, stats);
  }

  /**
   * Filter the idle nodes according to the specified job's preference policy.
   * @param job the job whose preference policy to evaluate.
   * @return a set of nodes that matched the highest possibly child policy of the preference. Possibly empty but never null;
   */
  Set<BaseNodeContext> filterPreferredNodes(final ServerJob job) {
    final Preference preferencePolicy = job.getSLA().getPreferencePolicy();
    final Set<BaseNodeContext> result = new HashSet<>();
    for (final ExecutionPolicy policy: preferencePolicy.getChildren()) {
      preparePolicy(policy, job, stats, job.getNbChannels());
      for (final BaseNodeContext node: idleChannels) {
        if (policy.evaluate(node.getSystemInformation())) result.add(node);
      }
      if (!result.isEmpty()) return result;
    }
    return Collections.emptySet();
  }
}
