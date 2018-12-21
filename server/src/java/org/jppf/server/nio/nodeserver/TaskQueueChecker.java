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

package org.jppf.server.nio.nodeserver;

import java.util.*;
import java.util.concurrent.Future;

import org.jppf.execute.ExecutorStatus;
import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.*;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 */
public class TaskQueueChecker extends AbstractTaskQueueChecker {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TaskQueueChecker.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Whether bias towards local node is enabled.
   */
  private final boolean localNodeBiasEnabled;

  /**
   * Initialize this task queue checker with the specified node server.
   * @param server the node server.
   * @param queue the reference queue to use.
   * @param stats reference to the statistics.
   * @param bundlerFactory the load-balancer factory.
   */
  TaskQueueChecker(final NodeNioServer server, final JPPFPriorityQueue queue, final JPPFStatistics stats, final JPPFBundlerFactory bundlerFactory) {
    super(server, queue, stats, bundlerFactory);
    this.localNodeBiasEnabled = server.getDriver().getConfiguration().get(JPPFProperties.LOCAL_NODE_BIAS);
  }

  /**
   * Perform the assignment of tasks.
   */
  @Override
  public void run() {
    try {
      reservationHandler = server.getNodeReservationHandler();
      while (!isStopped()) {
        if (!dispatch()) goToSleep(1000L);
      }
    } catch (final Throwable t) {
      log.error(t.getMessage(), t);
    }
  }

  /**
   * Perform the assignment of tasks.
   * @return true if a job was dispatched, false otherwise.
   */
  private boolean dispatch() {
    try {
      queue.getBroadcastManager().processPendingBroadcasts();
      if (queue.isEmpty()) return false;
      AbstractBaseNodeContext<?> channel = null;
      ServerTaskBundleNode nodeBundle = null;
      synchronized(idleChannels) {
        if (idleChannels.isEmpty()) return false;
        final List<ServerJob> allJobs = queue.getAllJobsFromPriorityMap();
        if (debugEnabled) log.debug("there are {} idle channels and {} jobs in the queue", idleChannels.size(), allJobs.size());
        try {
          final Iterator<ServerJob> it = allJobs.iterator();
          while ((channel == null) && it.hasNext() && !idleChannels.isEmpty()) {
            final ServerJob job = it.next();
            final JPPFNodeConfigSpec spec =  job.getSLA().getDesiredNodeConfiguration();
            if (spec != null) {
              if ((reservationHandler.getNbReservedNodes(job.getUuid()) >= job.getSLA().getMaxNodes()) &&
                !reservationHandler.hasReadyNode(job.getUuid())) continue;
            }
            if (!checkGridPolicy(job)) continue;
            channel = retrieveChannel(job);
            if (channel != null) {
              synchronized(channel.getMonitor()) {
                if (spec != null) {
                  final String readyJobUUID = reservationHandler.getReadyJobUUID(channel);
                  final String pendingJobUUID = reservationHandler.getPendingJobUUID(channel);
                  if ((pendingJobUUID == null) && (readyJobUUID == null)) {
                    reservationHandler.doReservation(job, channel);
                    channel = null;
                    continue;
                  }
                }
                removeIdleChannel(channel);
                if (!channel.isEnabled()) {
                  channel = null;
                  continue;
                }
                nodeBundle = prepareJobDispatch(channel, job);
                if (debugEnabled) log.debug("prepareJobDispatch() returned {}", nodeBundle);
                if (nodeBundle != null) {
                  try {
                    dispatchJobToChannel(channel, nodeBundle);
                    return true;
                  } catch (final Exception e) {
                    log.error("{}\nchannel={}\njob={}\nstack trace: {}", ExceptionUtils.getMessage(e), channel, nodeBundle, ExceptionUtils.getStackTrace(e));
                    channel.setClosed(false);
                    channel.handleException(e);
                  }
                }
              }
            }
          }
          if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle " : "channel found for bundle " + channel);
        } catch(final Exception e) {
          log.error("An error occurred while attempting to dispatch task bundles. This is most likely due to an error in the load balancer implementation.", e);
        }
      }
    } catch (final Exception e) {
      log.error("An error occurred while preparing for bundle creation and dispatching.", e);
    }
    return false;
  }

  /**
   * Retrieve a suitable channel for the specified job.
   * @param job the job to execute.
   * @return a channel for a node on which to execute the job.
   * @throws Exception if any error occurs.
   */
  private AbstractBaseNodeContext<?> retrieveChannel(final ServerJob job) throws Exception {
    return checkJobState(job) ? findIdleChannelIndex(job) : null;
  }

  /**
   * Prepare the specified job for the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to prepare dispatch the job to.
   * @param selectedJob the job to dispatch.
   * @return the task bundle to dispatch to the specified node.
   */
  private ServerTaskBundleNode prepareJobDispatch(final AbstractBaseNodeContext<?> channel, final ServerJob selectedJob) {
    if (debugEnabled) log.debug("dispatching jobUuid=" + selectedJob.getUuid() + " to node " + channel + ", nodeUuid=" + channel.getConnectionUuid());
    int size = 1;
    try {
      updateBundler(selectedJob.getJob(), channel);
      size = channel.getBundler().getBundleSize();
    } catch (final Exception e) {
      log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1", e);
      size = bundlerFactory.getFallbackBundler().getBundleSize();
    }
    return queue.nextBundle(selectedJob, size);
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to dispatch the job to.
   * @param nodeBundle the job to dispatch.
   * @throws Exception if any error occurs.
   */
  private static void dispatchJobToChannel(final AbstractBaseNodeContext<?> channel, final ServerTaskBundleNode nodeBundle) throws Exception {
    if (debugEnabled) log.debug("dispatching {} tasks of job '{}' to node {}", nodeBundle.getTaskCount(), nodeBundle.getJob().getName(), channel.getUuid());
    synchronized(channel.getMonitor()) {
      final Future<?> future = channel.submit(nodeBundle);
      nodeBundle.jobDispatched(channel, future);
    }
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param job the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private AbstractBaseNodeContext<?> findIdleChannelIndex(final ServerJob job) {
    final JobSLA sla = job.getJob().getSLA();
    final ExecutionPolicy policy = sla.getExecutionPolicy();
    final JPPFNodeConfigSpec spec =  sla.getDesiredNodeConfiguration();
    final TypedProperties desiredConfiguration = (spec == null) ? null : spec.getConfiguration();
    if (debugEnabled && (policy != null)) log.debug("Bundle " + job + " has an execution policy:\n" + policy);
    List<AbstractBaseNodeContext<?>> acceptableChannels = new ArrayList<>(idleChannels.size());
    final List<AbstractBaseNodeContext<?>> toRemove = new LinkedList<>();
    final List<String> uuidPath = job.getJob().getUuidPath().getList();
    final Iterator<AbstractBaseNodeContext<?>> iterator = idleChannels.iterator();
    final int nbJobChannels = job.getNbChannels();
    final int nbReservedNodes = reservationHandler.getNbReservedNodes(job.getUuid());
    final Collection<String> readyNodes = (spec == null) ? null : reservationHandler.getReadyNodes(job.getUuid());
    if (debugEnabled) log.debug("jobUuid={}, readyNodes={}", job.getUuid(), readyNodes);
    while (iterator.hasNext()) {
      final AbstractBaseNodeContext<?> channel = iterator.next();
      synchronized(channel.getMonitor()) {
        if ((channel.getExecutionStatus() != ExecutorStatus.ACTIVE) || !channel.getChannel().isOpen() || channel.isClosed() || !channel.isEnabled()) {
          if (debugEnabled) log.debug("channel is not opened: " + channel);
          toRemove.add(channel);
          continue;
        }
        if (!channel.isActive()) continue;
        if (channel.isPeer() && (server.nodeConnectionHandler.getConnectedRealNodes() >= peerLoadBalanceThreshold)) {
          if (debugEnabled) log.debug("this driver has {} nodes and the threshold is {}", server.nodeConnectionHandler.getConnectedNodes(), peerLoadBalanceThreshold);
          continue;
        }
        if (debugEnabled) log.debug("uuid path=" + uuidPath + ", node uuid=" + channel.getUuid());
        if (uuidPath.contains(channel.getUuid())) {
          if (debugEnabled) log.debug("bundle uuid path already contains node " + channel + " : uuidPath=" + uuidPath + ", nodeUuid=" + channel.getUuid());
          continue;
        }
        if (job.getBroadcastUUID() != null && !job.getBroadcastUUID().equals(channel.getUuid())) continue;
        final JPPFSystemInformation info = channel.getSystemInformation();
        if (channel.isPeer() && !disptachtoPeersWithoutNode) {
          if ((info != null) && (info.getJppf().getInt(PeerAttributesHandler.PEER_TOTAL_NODES, 0) <= 0)) continue;
        }
        if (policy != null) {
          boolean b = false;
          try {
            preparePolicy(policy, job, stats, nbJobChannels);
            b = policy.evaluate(info);
          } catch(final Exception ex) {
            log.error("An error occurred while running the execution policy to determine node participation.", ex);
          }
          if (debugEnabled) log.debug("rule execution is *" + b + "* for jobUuid=" + job.getUuid() + " on local channel=" + channel);
          if (!b) continue;
        }
        if (!checkMaxNodeGroups(channel, job)) continue;
        if (desiredConfiguration != null) {
          if (reservationHandler.getPendingJobUUID(channel) != null) continue;
          final String readyJobUuid = reservationHandler.getReadyJobUUID(channel);
          boolean b = true;
          if (readyNodes != null) {
            b = readyNodes.contains(channel.getUuid());
          }
          if (debugEnabled) log.debug("nodeUuid={}, readyJobUuid={}, jobUuid={}, b={}", channel.getUuid(), readyJobUuid, job.getUuid(), b);
          if (!b && (nbReservedNodes >= sla.getMaxNodes())) continue;
        }
        if (channel.isLocal() && localNodeBiasEnabled) { // add a bias toward local node
          if (desiredConfiguration != null) continue;
          else return channel;
        }
        acceptableChannels.add(channel);
      }
    }
    if (!toRemove.isEmpty()) {
      for (AbstractBaseNodeContext<?> c: toRemove) removeIdleChannelAsync(c);
    }
    //if ((desiredConfiguration != null) && !reservationHandler.hasPendingNode(job.getUuid())) acceptableChannels = filterLowestDistances(job, acceptableChannels);
    if (!acceptableChannels.isEmpty() && (desiredConfiguration != null)) acceptableChannels = filterLowestDistances(job, acceptableChannels);
    final int size = acceptableChannels.size();
    if (debugEnabled) log.debug("found " + size + " acceptable channels");
    final AbstractBaseNodeContext<?> channel = (size > 0) ? acceptableChannels.get(size > 1 ? random.nextInt(size) : 0) : null;
    return channel;
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
   * Check if the job state allows it to be dispatched on another node.
   * There are two cases when this method will return false: when the job is suspended and
   * when the job is already executing on its maximum allowed number of nodes.
   * @param job encapsulates the job information.
   * @return true if the job can be dispatched to at least one more node, false otherwise.
   */
  private static boolean checkJobState(final ServerJob job) {
    if (job.isCancelled()) return false;
    final JobSLA sla = job.getJob().getSLA();
    if (debugEnabled) log.debug("job '{}', suspended={}, pending={}, expired={}", new Object[] {job.getName(), sla.isSuspended(), job.isPending(), job.isJobExpired()});
    if (sla.isSuspended() || job.isPending() || job.isJobExpired()) return false;
    if (debugEnabled) log.debug("current nodes = " + job.getNbChannels() + ", maxNodes = " + sla.getMaxNodes());
    return job.getNbChannels() < sla.getMaxNodes();
  }

  /**
   * Check if the job state allows it to be dispatched to a specific master/slaves group of nodes.
   * @param currentNode the node currently being evaluated.
   * @param job the bundle from which to get the job information.
   * @return true if the job can be dispatched to at least one more node, false otherwise.
   */
  private static boolean checkMaxNodeGroups(final AbstractBaseNodeContext<?> currentNode, final ServerJob job) {
    final JPPFManagementInfo currentInfo = currentNode.getManagementInfo();
    if (currentInfo == null) return true;
    final String currentMasterUuid = getMasterUuid(currentInfo);
    if (currentMasterUuid == null) return true;
    final int maxNodeGroups = job.getJob().getSLA().getMaxNodeProvisioningGroupss();
    if ((maxNodeGroups == Integer.MAX_VALUE) || (maxNodeGroups <= 0)) return true;
    final Set<ServerTaskBundleNode> nodes = job.getDispatchSet();
    final Set<String> masterUuids = new HashSet<>();
    masterUuids.add(currentMasterUuid);
    for (final ServerTaskBundleNode node: nodes) {
      final AbstractNodeContext ctx = (AbstractNodeContext) node.getChannel();
      final JPPFManagementInfo info = ctx.getManagementInfo();
      final String uuid = getMasterUuid(info);
      if (uuid != null) {
        if (!masterUuids.contains(uuid)) masterUuids.add(uuid);
        if (masterUuids.size() > maxNodeGroups) return false;
      }
    }
    return true;
  }

  /**
   * Get the master node uuid for a node that is either a master or a slave.
   * @param info represents the node information.
   * @return the corresponding master uuid.
   */
  private static String getMasterUuid(final JPPFManagementInfo info) {
    if (info.isMasterNode()) return info.getUuid();
    else if (info.isSlaveNode()) {
      final JPPFSystemInformation systemInfo = info.getSystemInfo();
      if (systemInfo != null) return systemInfo.getJppf().get(JPPFProperties.PROVISIONING_MASTER_UUID);
    }
    return null;
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param taskBundle the job.
   * @param context the current node context.
   */
  private void updateBundler(final TaskBundle taskBundle, final AbstractBaseNodeContext<?> context) {
    context.checkBundler(bundlerFactory, jppfContext);
    final Bundler<?> ctxBundler = context.getBundler();
    if (ctxBundler instanceof JobAwareness) ((JobAwareness) ctxBundler).setJob(taskBundle);
  }

  /**
   * Check whether the grid state and job's grid policy match.
   * If no grid policy is defined for the job, then it is considered matching.
   * @param job the job to check.
   * @return {@code true} if the job has no grid policy or the policy matches with the current grid state, {@code false} otherwise.
   */
  private boolean checkGridPolicy(final ServerJob job) {
    final ExecutionPolicy policy = job.getSLA().getGridExecutionPolicy();
    if (policy != null) {
      preparePolicy(policy, job, stats, job.getNbChannels());
      return policy.evaluate(this.driverInfo);
    }
    return true;
  }

  /**
   * Keep only the nodes whose configuration has the lowest distance when compared to the desired configuration.
   * @param job the job that specifies the desired node configuration.
   * @param channels the list of eligible channels.
   * @return one or more channels with the minimum computed score.
   */
  private List<AbstractBaseNodeContext<?>> filterLowestDistances(final ServerJob job, final List<AbstractBaseNodeContext<?>> channels) {
    final JPPFNodeConfigSpec spec =  job.getSLA().getDesiredNodeConfiguration();
    final TypedProperties desiredConfiguration = (spec == null) ? null : spec.getConfiguration();
    final CollectionSortedMap<Integer, AbstractBaseNodeContext<?>> scoreMap = new SetSortedMap<>();
    if (debugEnabled) log.debug("computing scores for job '{}', uuid={}", job.getName(), job.getUuid());
    for (final AbstractBaseNodeContext<?> channel: channels) {
      if (!channel.isLocal() && !channel.isOffline() && !channel.isPeer()) {
        final String reservedJobUuid = server.getNodeReservationHandler().getPendingJobUUID(channel);
        if ((reservedJobUuid != null) && reservedJobUuid.equals(job.getUuid())) continue;
        else {
          final TypedProperties props = channel.getSystemInformation().getJppf();
          final int score = TypedPropertiesSimilarityEvaluator.computeDistance(desiredConfiguration, props);
          channel.setReservationScore(score);
          scoreMap.putValue(score, channel);
        }
      }
    }
    if (debugEnabled) {
      final CollectionMap<Integer, String> map = new SetSortedMap<>();
      for (Map.Entry<Integer, Collection<AbstractBaseNodeContext<?>>> entry: scoreMap.entrySet()) {
        for (final AbstractBaseNodeContext<?> c: entry.getValue()) map.putValue(entry.getKey(), c.getUuid());
      }
      log.debug("computed scores: {}", map);
    }
    final int n = scoreMap.firstKey();
    return (scoreMap.isEmpty()) ? Collections.<AbstractBaseNodeContext<?>>emptyList() : new ArrayList<>(scoreMap.getValues(n));
  }
}
