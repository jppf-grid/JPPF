/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
 * @param <C> type of the <code>ExecutorChannel</code>.
 */
public class TaskQueueChecker<C extends AbstractNodeContext> extends AbstractTaskQueueChecker<C> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TaskQueueChecker.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this task queue checker with the specified node server.
   * @param server the node server.
   * @param queue the reference queue to use.
   * @param stats reference to the statistics.
   * @param bundlerFactory the load-balancer factory.
   */
  TaskQueueChecker(final NodeNioServer server, final JPPFPriorityQueue queue, final JPPFStatistics stats, final JPPFBundlerFactory bundlerFactory) {
    super(server, queue, stats, bundlerFactory);
  }

  /**
   * Perform the assignment of tasks.
   */
  @Override
  public void run() {
    reservationHandler = server.getNodeReservationHandler();
    while (!isStopped()) {
      if (!dispatch()) goToSleep(1000L);
    }
  }

  /**
   * Perform the assignment of tasks.
   * @return true if a job was dispatched, false otherwise.
   */
  private boolean dispatch() {
    boolean dispatched = false;
    try {
      queue.getBroadcastManager().processPendingBroadcasts();
      C channel = null;
      ServerTaskBundleNode nodeBundle = null;
      synchronized(idleChannels) {
        if (idleChannels.isEmpty() || queue.isEmpty()) return false;
        if (debugEnabled) log.debug(Integer.toString(idleChannels.size()) + " channels idle");
        queueLock.lock();
        try {
          Iterator<ServerJob> it = queue.iterator();
          while ((channel == null) && it.hasNext() && !idleChannels.isEmpty()) {
            ServerJob job = it.next();
            JPPFNodeConfigSpec spec =  job.getSLA().getDesiredNodeConfiguration();
            if (spec != null) {
              if ((reservationHandler.getNbReservedNodes(job.getUuid()) >= job.getSLA().getMaxNodes()) &&
                !reservationHandler.hasReadyNode(job.getUuid())) continue;
            }
            if (!checkGridPolicy(job)) continue;
            channel = retrieveChannel(job);
            if (channel != null) {
              synchronized(channel.getMonitor()) {
                if (spec != null) {
                  String readyJobUUID = reservationHandler.getReadyJobUUID(channel);
                  String pendingJobUUID = reservationHandler.getPendingJobUUID(channel);
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
                  } catch (Exception e) {
                    log.error(String.format("%s%nchannel=%s%njob=%s%nstack trace: %s", ExceptionUtils.getMessage(e), channel, nodeBundle, ExceptionUtils.getStackTrace(e)));
                    channel.unclose();
                    channel.handleException(channel.getChannel(), e);
                  }
                }
              }
            }
          }
          if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle " : "channel found for bundle " + channel);
        } catch(Exception e) {
          log.error("An error occurred while attempting to dispatch task bundles. This is most likely due to an error in the load balancer implementation.", e);
        } finally {
          queueLock.unlock();
        }
      }
    } catch (Exception e) {
      log.error("An error occurred while preparing for bundle creation and dispatching.", e);
    }
    return dispatched;
  }

  /**
   * Retrieve a suitable channel for the specified job.
   * @param bundleWrapper the job to execute.
   * @return a channel for a node on which to execute the job.
   * @throws Exception if any error occurs.
   */
  private C retrieveChannel(final ServerJob bundleWrapper) throws Exception {
    return checkJobState(bundleWrapper) ? findIdleChannelIndex(bundleWrapper) : null;
  }

  /**
   * Prepare the specified job for the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to prepare dispatch the job to.
   * @param selectedBundle the job to dispatch.
   * @return the task bundle to dispatch to the specified node.
   */
  private ServerTaskBundleNode prepareJobDispatch(final C channel, final ServerJob selectedBundle) {
    if (debugEnabled) log.debug("dispatching jobUuid=" + selectedBundle.getUuid() + " to node " + channel + ", nodeUuid=" + channel.getConnectionUuid());
    int size = 1;
    try {
      updateBundler(selectedBundle.getJob(), channel);
      size = channel.getBundler().getBundleSize();
    } catch (Exception e) {
      log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1", e);
      size = bundlerFactory.getFallbackBundler().getBundleSize();
    }
    return queue.nextBundle(selectedBundle, size);
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to dispatch the job to.
   * @param nodeBundle the job to dispatch.
   * @throws Exception if any error occurs.
   */
  private void dispatchJobToChannel(final C channel, final ServerTaskBundleNode nodeBundle) throws Exception {
    if (debugEnabled) log.debug(String.format("dispatching %d tasks of job '%s' to node %s", nodeBundle.getTaskCount(), nodeBundle.getJob().getName(), channel.getUuid()));
    synchronized(channel.getMonitor()) {
      Future<?> future = channel.submit(nodeBundle);
      nodeBundle.jobDispatched(channel, future);
    }
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param job the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private C findIdleChannelIndex(final ServerJob job) {
    JobSLA sla = job.getJob().getSLA();
    ExecutionPolicy policy = sla.getExecutionPolicy();
    JPPFNodeConfigSpec spec =  sla.getDesiredNodeConfiguration();
    TypedProperties desiredConfiguration = (spec == null) ? null : spec.getConfiguration();
    if (debugEnabled && (policy != null)) log.debug("Bundle " + job + " has an execution policy:\n" + policy);
    List<C> acceptableChannels = new ArrayList<>(idleChannels.size());
    List<C> toRemove = new LinkedList<>();
    List<String> uuidPath = job.getJob().getUuidPath().getList();
    Iterator<C> iterator = idleChannels.iterator();
    int nbJobChannels = job.getNbChannels();
    int nbReservedNodes = reservationHandler.getNbReservedNodes(job.getUuid());
    Collection<String> readyNodes = (spec == null) ? null : reservationHandler.getReadyNodes(job.getUuid());
    if (debugEnabled) log.debug(String.format("jobUuid=%s, readyNodes=%s", job.getUuid(), readyNodes));
    while (iterator.hasNext()) {
      C channel = iterator.next();
      synchronized(channel.getMonitor()) {
        if ((channel.getExecutionStatus() != ExecutorStatus.ACTIVE) || !channel.getChannel().isOpen() || channel.isClosed() || !channel.isEnabled()) {
          if (debugEnabled) log.debug("channel is not opened: " + channel);
          toRemove.add(channel);
          continue;
        }
        if (!channel.isActive()) continue;
        if (debugEnabled) log.debug("uuid path=" + uuidPath + ", node uuid=" + channel.getUuid());
        if (uuidPath.contains(channel.getUuid())) {
          if (debugEnabled) log.debug("bundle uuid path already contains node " + channel + " : uuidPath=" + uuidPath + ", nodeUuid=" + channel.getUuid());
          continue;
        }
        if (job.getBroadcastUUID() != null && !job.getBroadcastUUID().equals(channel.getUuid())) continue;
        JPPFSystemInformation info = channel.getSystemInformation();
        if (channel.isPeer() && !disptachtoPeersWithoutNode) {
          if ((info != null) && (info.getJppf().getInt(PeerAttributesHandler.PEER_TOTAL_NODES, 0) <= 0)) continue;
        }
        if (policy != null) {
          boolean b = false;
          try {
            preparePolicy(policy, job, stats, nbJobChannels);
            b = policy.accepts(info);
          } catch(Exception ex) {
            log.error("An error occurred while running the execution policy to determine node participation.", ex);
          }
          if (debugEnabled) log.debug("rule execution is *" + b + "* for jobUuid=" + job.getUuid() + " on local channel=" + channel);
          if (!b) continue;
        }
        if (!checkMaxNodeGroups(channel, job)) continue;
        if (desiredConfiguration != null) {
          if (reservationHandler.getPendingJobUUID(channel) != null) continue;
          String readyJobUuid = reservationHandler.getReadyJobUUID(channel);
          boolean b = true;
          if (readyNodes != null) {
            b = readyNodes.contains(channel.getUuid());
          }
          if (debugEnabled) log.debug(String.format("nodeUuid=%s, readyJobUuid=%s, jobUuid=%s, b=%b", channel.getUuid(), readyJobUuid, job.getUuid(), b));
          if (!b && (nbReservedNodes >= sla.getMaxNodes())) continue;
        }
        if (channel.isLocal()) { // add a bias toward local node
          if (desiredConfiguration != null) continue;
          else return channel;
        }
        acceptableChannels.add(channel);
      }
    }
    if (!toRemove.isEmpty()) {
      for (C c: toRemove) removeIdleChannel(c);
    }
    //if ((desiredConfiguration != null) && !reservationHandler.hasPendingNode(job.getUuid())) acceptableChannels = filterLowestDistances(job, acceptableChannels);
    if (!acceptableChannels.isEmpty() && (desiredConfiguration != null)) acceptableChannels = filterLowestDistances(job, acceptableChannels);
    int size = acceptableChannels.size();
    if (debugEnabled) log.debug("found " + size + " acceptable channels");
    C channel = (size > 0) ? acceptableChannels.get(size > 1 ? random.nextInt(size) : 0) : null;
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
  private boolean checkJobState(final ServerJob job) {
    if (job.isCancelled()) return false;
    JobSLA sla = job.getJob().getSLA();
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
  private boolean checkMaxNodeGroups(final C currentNode, final ServerJob job) {
    JPPFManagementInfo currentInfo = currentNode.getManagementInfo();
    if (currentInfo == null) return true;
    String currentMasterUuid = getMasterUuid(currentInfo);
    if (currentMasterUuid == null) return true;
    int maxNodeGroups = job.getJob().getSLA().getMaxNodeProvisioningGroupss();
    if ((maxNodeGroups == Integer.MAX_VALUE) || (maxNodeGroups <= 0)) return true;
    Set<ServerTaskBundleNode> nodes = job.getDispatchSet();
    Set<String> masterUuids = new HashSet<>();
    masterUuids.add(currentMasterUuid);
    for (ServerTaskBundleNode node: nodes) {
      AbstractNodeContext ctx = (AbstractNodeContext) node.getChannel();
      JPPFManagementInfo info = ctx.getManagementInfo();
      String uuid = getMasterUuid(info);
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
  private String getMasterUuid(final JPPFManagementInfo info) {
    if (info.isMasterNode()) return info.getUuid();
    else if (info.isSlaveNode()) {
      JPPFSystemInformation systemInfo = info.getSystemInfo();
      if (systemInfo != null) return systemInfo.getJppf().get(JPPFProperties.PROVISIONING_MASTER_UUID);
    }
    return null;
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param taskBundle the job.
   * @param context the current node context.
   */
  @SuppressWarnings("deprecation")
  private void updateBundler(final TaskBundle taskBundle, final C context) {
    context.checkBundler(bundlerFactory, jppfContext);
    Bundler<?> ctxBundler = context.getBundler();
    if (ctxBundler instanceof JobAwareness) ((JobAwareness) ctxBundler).setJobMetadata(taskBundle.getMetadata());
    else if (ctxBundler instanceof JobAwarenessEx) ((JobAwarenessEx) ctxBundler).setJob(taskBundle);
  }

  /**
   * Check whether the grid state and job's grid policy match.
   * If no grid policy is defined for the job, then it is considered matching.
   * @param job the job to check.
   * @return {@code true} if the job has no grid policy or the policy matches with the current grid state, {@code false} otherwise.
   */
  private boolean checkGridPolicy(final ServerJob job) {
    ExecutionPolicy policy = job.getSLA().getGridExecutionPolicy();
    if (policy != null) {
      preparePolicy(policy, job, stats, job.getNbChannels());
      return policy.accepts(this.driverInfo);
    }
    return true;
  }

  /**
   * Keep only the nodes whose configuration has the lowest distance when compared to the desired configuration.
   * @param job the job that specifies the desired node configuration.
   * @param channels the list of eligible channels.
   * @return one or more channels with the minimum computed score.
   */
  private List<C> filterLowestDistances(final ServerJob job, final List<C> channels) {
    JPPFNodeConfigSpec spec =  job.getSLA().getDesiredNodeConfiguration();
    TypedProperties desiredConfiguration = (spec == null) ? null : spec.getConfiguration();
    CollectionSortedMap<Integer, C> scoreMap = new SetSortedMap<>();
    TypedPropertiesSimilarityEvaluator ev = new TypedPropertiesSimilarityEvaluator();
    if (debugEnabled) log.debug(String.format("computing scores for job '%s', uuid=%s", job.getName(), job.getUuid()));
    for (C channel: channels) {
      if (!channel.isLocal() && !channel.isOffline() && !channel.isPeer()) {
        String reservedJobUuid = server.getNodeReservationHandler().getPendingJobUUID(channel);
        if ((reservedJobUuid != null) && reservedJobUuid.equals(job.getUuid())) continue;
        else {
          TypedProperties props = channel.getSystemInformation().getJppf();
          int score = ev.computeDistance(desiredConfiguration, props);
          channel.reservationScore = score;
          scoreMap.putValue(score, channel);
        }
      }
    }
    if (debugEnabled) {
      CollectionMap<Integer, String> map = new SetSortedMap<>();
      for (Map.Entry<Integer, Collection<C>> entry: scoreMap.entrySet()) {
        for (C c: entry.getValue()) map.putValue(entry.getKey(), c.getUuid());
      }
      log.debug("computed scores: {}", map);
    }
    int n = scoreMap.firstKey();
    return (scoreMap.isEmpty()) ? Collections.<C>emptyList() : new ArrayList<>(scoreMap.getValues(n));
  }
}
