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
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import org.jppf.execute.ExecutorStatus;
import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.*;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 */
public class AsyncJobScheduler extends AbstractAsyncJobScheduler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncJobScheduler.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this task queue checker with the specified node server.
   * @param server the node server.
   * @param queue the reference queue to use.
   * @param stats reference to the statistics.
   * @param bundlerFactory the load-balancer factory.
   */
  AsyncJobScheduler(final AsyncNodeNioServer server, final JPPFPriorityQueue queue, final JPPFStatistics stats, final JPPFBundlerFactory bundlerFactory) {
    super(server, queue, stats, bundlerFactory);
  }

  /**
   * Perform the assignment of tasks.
   */
  @Override
  public void run() {
    if (debugEnabled) log.debug("starting {}", getClass().getSimpleName());
    reservationHandler = server.getNodeReservationHandler();
    try {
      while (!isStopped()) {
        if (!dispatch()) goToSleep(1000L);
      }
    } catch (final Throwable t) {
      log.error("error in driver dispatch loop", t);
    }
  }

  /**
   * Perform the assignment of jobs to nodes.
   * @return true if a job was dispatched, false otherwise.
   */
  private boolean dispatch() {
    try {
      queue.getBroadcastManager().processPendingBroadcasts();
      if (queue.isEmpty()) return false;
      BaseNodeContext channel = null;
      ServerTaskBundleNode nodeBundle = null;
      synchronized(idleChannels) {
        if (idleChannels.isEmpty()) return false;
        final List<ServerJob> allJobs = queue.getAllJobsFromPriorityMap();
        if (debugEnabled) log.debug("there are {} idle channels and {} jobs in the queue", idleChannels.size(), allJobs.size());
        try {
          final Iterator<ServerJob> jobIterator = allJobs.iterator();
          while ((channel == null) && jobIterator.hasNext() && !idleChannels.isEmpty()) {
            final ServerJob job = jobIterator.next();
            if (debugEnabled) log.debug("checking {}", job);
            if (!performJobChecks(job)) continue;
            channel = findIdleChannelIndex(job);
            if (channel == null) continue;
            synchronized(channel.getMonitor()) {
              if (job.getSLA().getDesiredNodeConfiguration() != null) {
                final String readyJobUUID = reservationHandler.getReadyJobUUID(channel);
                final String pendingJobUUID = reservationHandler.getPendingJobUUID(channel);
                if ((pendingJobUUID == null) && (readyJobUUID == null)) {
                  if (debugEnabled) log.debug("reserving {} with {}", job, channel);
                  reservationHandler.doReservation(job, channel);
                  channel = null;
                  continue;
                }
              }
              if (channel.getCurrentNbJobs() >= channel.getMaxJobs()) removeIdleChannel(channel);
              if (!channel.isEnabled()) {
                if (debugEnabled) log.debug("channel is disabled [}", channel);
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
   * Prepare the specified job for the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to prepare dispatch the job to.
   * @param selectedJob the job to dispatch.
   * @return the task bundle to dispatch to the specified node.
   */
  private ServerTaskBundleNode prepareJobDispatch(final BaseNodeContext channel, final ServerJob selectedJob) {
    if (debugEnabled) log.debug("dispatching jobUuid=" + selectedJob.getUuid() + " to node " + channel + ", nodeUuid=" + channel.getConnectionUuid());
    int size = 1;
    try {
      updateBundler(selectedJob.getJob(), channel);
      size = channel.getBundler().getBundleSize();
      if (selectedJob.getSLA().getMaxDispatchSize() < size) size = selectedJob.getSLA().getMaxDispatchSize();
    } catch (final Exception e) {
      log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1", e);
      size = bundlerFactory.getFallbackBundler().getBundleSize();
    }
    return selectedJob.isCancelled() ? null : queue.nextBundle(selectedJob, size, channel);
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to dispatch the job to.
   * @param nodeBundle the job to dispatch.
   * @throws Exception if any error occurs.
   */
  private static void dispatchJobToChannel(final BaseNodeContext channel, final ServerTaskBundleNode nodeBundle) throws Exception {
    if (debugEnabled) log.debug("dispatching {} tasks of job '{}' to node {}", nodeBundle.getTaskCount(), nodeBundle.getJob().getName(), channel.getUuid());
    if (log.isTraceEnabled()) {
      final Set<Long> set = new TreeSet<>();
      for (final ServerTask task: nodeBundle.getTaskList()) {
        final long id = task.getBundle().getId();
        if (!set.contains(id)) set.add(id);
      }
      StringBuilder sb = new StringBuilder();
      int count = 0;
      for (final long id: set) {
        if (count > 0) sb.append(", ");
        sb.append("ServerTaskBundleClient[id=").append(id).append(']');
        count++;
      }
      log.trace("client bundles in dispatch: {}", sb);
      sb = new StringBuilder();
      count = 0;
      for (final ServerTask task: nodeBundle.getTaskList()) {
        if (count > 0) sb.append(", ");
        sb.append(task.getPosition());
        count++;
      }
      log.trace("tasks positions in dispatch: {}", sb);
    }
    synchronized(channel.getMonitor()) {
      final Future<?> future = channel.submit(nodeBundle);
      nodeBundle.jobDispatched(channel, future);
    }
    if (debugEnabled) log.debug("dispatched {} tasks of job '{}' to node {}", nodeBundle.getTaskCount(), nodeBundle.getJob().getName(), channel.getUuid());
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param job the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private BaseNodeContext findIdleChannelIndex(final ServerJob job) {
    if (debugEnabled) log.debug("checking {}", job);
    final JobSLA sla = job.getSLA();
    final JPPFNodeConfigSpec spec =  sla.getDesiredNodeConfiguration();
    final TypedProperties desiredConfiguration = (spec == null) ? null : spec.getConfiguration();
    List<BaseNodeContext> acceptableChannels = new ArrayList<>(idleChannels.size());
    final List<BaseNodeContext> toRemove = new LinkedList<>();
    Iterator<BaseNodeContext> nodeIterator = null;
    if (sla.getPreferencePolicy() != null) {
      final Set<BaseNodeContext> preferedChannels = filterPreferredNodes(job);
      if (preferedChannels.isEmpty()) return null;
      nodeIterator = preferedChannels.iterator();
    }
    else nodeIterator = idleChannels.iterator();
    while (nodeIterator.hasNext()) {
      final AsyncNodeContext channel = (AsyncNodeContext) nodeIterator.next();
      synchronized(channel.getMonitor()) {
        if ((channel.getExecutionStatus() != ExecutorStatus.ACTIVE) || channel.isClosed() || !channel.isEnabled()) {
          if (debugEnabled) log.debug("channel is not opened: {}", channel);
          toRemove.add(channel);
          continue;
        }
        if (!channel.isActive() || !channel.isAcceptingNewJobs()) {
          if (debugEnabled) log.debug("node not accepting jobs: {}", channel);
          continue;
        }
        if (channel.isPeer() && (server.nodeConnectionHandler.getConnectedRealNodes() >= peerLoadBalanceThreshold)) {
          if (debugEnabled) log.debug("this driver has {} nodes and the threshold is {}", server.nodeConnectionHandler.getConnectedNodes(), peerLoadBalanceThreshold);
          continue;
        }
        if (channel.getCurrentNbJobs() >= channel.getMaxJobs()) {
          if (debugEnabled) log.debug("[currentNbJobs = {}] >= maxJobs = {}] for {}", channel.getCurrentNbJobs(), channel.getMaxJobs(), channel);
          continue;
        }
        if (!checkJobAgainstChannel(channel, job)) continue;
        if (job.getBroadcastUUID() != null && !job.getBroadcastUUID().equals(channel.getUuid())) continue;
        final JPPFSystemInformation info = channel.getSystemInformation();
        if (channel.isPeer() && !disptachtoPeersWithoutNode) {
          if ((info != null) && (info.getJppf().getInt(PeerAttributesHandler.PEER_TOTAL_NODES, 0) <= 0)) {
            if (debugEnabled) log.debug("peer has no attached node: {}", channel.getUuid());
            continue;
          }
        }
        if (!checkExecutionPolicy(channel, job, sla.getExecutionPolicy(), info, job.getNbChannels())) continue;
        if (!checkMaxNodeGroups(channel, job)) continue;
        final Collection<String> readyNodes = (spec == null) ? null : reservationHandler.getReadyNodes(job.getUuid());
        if (debugEnabled) log.debug("jobUuid={}, readyNodes={}", job.getUuid(), readyNodes);
        if (!checkDesiredConfiguration(desiredConfiguration, channel, job, readyNodes, reservationHandler.getNbReservedNodes(job.getUuid()))) continue;
        if (channel.isLocal() && localNodeBiasEnabled) { // add a bias toward local node
          if (desiredConfiguration != null) continue;
          else return channel;
        }
        acceptableChannels.add(channel);
      }
    }
    if (!toRemove.isEmpty()) {
      for (final BaseNodeContext c: toRemove) removeIdleChannel(c);
    }
    if (!checkJobNotCancelled(job)) return null;
    if (!acceptableChannels.isEmpty() && (desiredConfiguration != null)) acceptableChannels = filterLowestDistances(job, acceptableChannels);
    return selectChannel(acceptableChannels);
  }

  /**
   * Select a single channle from a list of channels eligible  for a job.
   * @param acceptableChannels the list of channels to select from.
   * @return an instance of {@link BaseNodeContext}, or {@code null} if the list is empty or no channel passes the selection criteria.
   */
  private BaseNodeContext selectChannel(final List<BaseNodeContext> acceptableChannels) {
    if (acceptableChannels.isEmpty()) return null;
    final int size = acceptableChannels.size();
    if (debugEnabled) log.debug("found {} acceptable channels", size);
    final BaseNodeContext channel = (size > 0) ? acceptableChannels.get(size > 1 ? random.nextInt(size) : 0) : null;
    return channel;
  }

  /**
   * 
   * @param job the job to check.
   * @return {@code true} if the job can be scheduiled, {@code false} otherwise.
   */
  private boolean performJobChecks(final ServerJob job) {
    final JobDependencySpec dependencySpec = job.getSLA().getDependencySpec();
    //if (debugEnabled) log.debug("job graph node: {}", dependencyHandler.getGraph().getNode(dependencySpec.getId()));
    if ((dependencySpec.getId() != null) && !job.isJobGraphAlreadyHandled() && dependencySpec.hasDependency() && dependencyHandler.hasPendingDependencyOrCancelled(dependencySpec.getId())) {
      if (debugEnabled) log.debug("job dependency check false for {}", job);
      return false;
    }
    final JPPFNodeConfigSpec spec =  job.getSLA().getDesiredNodeConfiguration();
    if (spec != null) {
      if ((reservationHandler.getNbReservedNodes(job.getUuid()) >= job.getSLA().getMaxNodes()) && !reservationHandler.hasReadyNode(job.getUuid())) {
        if (debugEnabled) log.debug("node config check false for {}", job);
        return false;
      }
    }
    if ((job.getTaskGraph() != null) && !job.hasAvailableGraphNode()) {
      if (debugEnabled) log.debug("tasks graph check false for {}", job);
      return false;
    }
    if (!checkGridPolicy(job)) return false;
    return checkJobState(job);
  }

  /**
   * Check that the job did not already get through the specified channel (collision avoidance) and that the SLA's max driver depth is not reached.
   * @param channel the node to check.
   * @param job the job to check against.
   * @return {@code true} if the check succeeds, {@code false} otherwise.
   */
  private boolean checkJobAgainstChannel(final AsyncNodeContext channel, final ServerJob job) {
    final List<String> uuidPath = job.getJob().getUuidPath().getList();
    if (debugEnabled) log.debug("uuid path={}, node uuid={}", uuidPath, channel.getUuid());
    final String driverUuid = server.getDriver().getUuid();
    final int index = uuidPath.indexOf(driverUuid);
    if ((index >= 0) && (index != uuidPath.size() - 1)) log.warn("uuid path contains this driver's uuid {}: uuidPath={}", driverUuid, uuidPath);
    if (uuidPath.contains(channel.getUuid())) {
      if (debugEnabled) log.debug("bundle uuid path already contains node {} : uuidPath={}, nodeUuid={}", channel, uuidPath, channel.getUuid());
      return false;
    }
    if (channel.isPeer() && (uuidPath.size() - 1 >= job.getSLA().getMaxDriverDepth())) {
      if (debugEnabled) log.debug("job [name={}, uuid={}, uuidPath={}] reached max driver depth of {}", job.getName(), job.getUuid(), uuidPath, job.getSLA().getMaxDriverDepth());
      return false;
    }
    if (!job.getSLA().isAllowMultipleDispatchesToSameChannel()) {
      final int nbDispatches = channel.getNbBundlesForJob(job.getUuid());
      if (nbDispatches > 0) return false;
    }
    return true;
  }

  /**
   * Check if the node is ready for the job's configuration.
   * @param config the desired node configuration
   * @param channel the node to check.
   * @param job the job to check against.
   * @param readyNodes the set of nodes ready for the desried configuration..
   * @param nbReservedNodes number of nodes reserved for the job.
   * @return {@code true} if the check succeeds, {@code false} otherwise.
   */
  private boolean checkDesiredConfiguration(final TypedProperties config, final BaseNodeContext channel, final ServerJob job, final Collection<String> readyNodes, final int nbReservedNodes) {
    if (config != null) {
      if (reservationHandler.getPendingJobUUID(channel) != null) return false;
      final String readyJobUuid = reservationHandler.getReadyJobUUID(channel);
      boolean b = true;
      if (readyNodes != null) b = readyNodes.contains(channel.getUuid());
      if (debugEnabled) log.debug("nodeUuid={}, readyJobUuid={}, jobUuid={}, b={}", channel.getUuid(), readyJobUuid, job.getUuid(), b);
      if (!b && (nbReservedNodes >= job.getSLA().getMaxNodes())) return false;
    }
    return true;
  }

  /**
   * Check whether the job's execution policy, if any, matches the node.
   * @param channel the node on which to matach the policy.
   * @param job the job to check.
   * @param policy the execution policy to evaluate.
   * @param info the information against which the policy is evaluated.
   * @param nbJobChannels the number of nodes to which the job is already dispatched, passed on to the policy context.
   * @return {@code true} if the policy is {@code null} or if the node matches, {@code false} otherwise.
   */
  private boolean checkExecutionPolicy(final BaseNodeContext channel, final ServerJob job, final ExecutionPolicy policy, final JPPFSystemInformation info, final int nbJobChannels) {
    if (policy == null) return true;
    if (debugEnabled) log.debug("job has an execution policy: {}\n{}", job, policy.toString().trim());
      boolean b = false;
      try {
        preparePolicy(policy, job, stats, nbJobChannels);
        b = policy.evaluate(info);
      } catch(final Exception ex) {
        log.error("An error occurred while running the execution policy to determine node participation.", ex);
      }
      if (debugEnabled) log.debug("rule execution is *{}* for job [name={}, uuid={}] on channel {}", b, job.getName(), job.getUuid(), channel);
      return b;
  }

  /**
   * Check if the job state allows it to be dispatched on another node.
   * There are two cases when this method will return false: when the job is suspended and
   * when the job is already executing on its maximum allowed number of nodes.
   * @param job encapsulates the job information.
   * @return true if the job can be dispatched to at least one more node, false otherwise.
   */
  private static boolean checkJobState(final ServerJob job) {
    if (job.isCancelled()) {
      if (debugEnabled) log.debug("job is cancelled: {}", job);
      return false;
    }
    final JobSLA sla = job.getSLA();
    if (debugEnabled) log.debug("job '{}', suspended={}, pending={}, expired={}", new Object[] {job.getName(), sla.isSuspended(), job.isPending(), job.isJobExpired()});
    if (sla.isSuspended() || job.isPending() || job.isJobExpired()) return false;
    if (debugEnabled) log.debug("current nodes = " + job.getNbChannels() + ", maxNodes = " + sla.getMaxNodes());
    return job.getNbChannels() < sla.getMaxNodes();
  }

  /**
   * Check that the job is not cancelled.
   * @param job encapsulates the job information.
   * @return {@code true} if the job is NOT cancelled, {@code false} otherwise.
   */
  private static boolean checkJobNotCancelled(final ServerJob job) {
    final Lock lock = job.getLock();
    lock.lock();
    try {
      return !job.isCancelled();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Check if the job state allows it to be dispatched to a specific master/slaves group of nodes.
   * @param currentNode the node currently being evaluated.
   * @param job the bundle from which to get the job information.
   * @return true if the job can be dispatched to at least one more node, false otherwise.
   */
  private static boolean checkMaxNodeGroups(final BaseNodeContext currentNode, final ServerJob job) {
    final JPPFManagementInfo currentInfo = currentNode.getManagementInfo();
    if (currentInfo == null) return true;
    final String currentMasterUuid = getMasterUuid(currentInfo);
    if (currentMasterUuid == null) return true;
    final int maxNodeGroups = job.getSLA().getMaxNodeProvisioningGroupss();
    if ((maxNodeGroups == Integer.MAX_VALUE) || (maxNodeGroups <= 0)) return true;
    final Set<ServerTaskBundleNode> nodes = job.getDispatchSet();
    final Set<String> masterUuids = new HashSet<>();
    masterUuids.add(currentMasterUuid);
    for (final ServerTaskBundleNode node: nodes) {
      final AsyncNodeContext ctx = (AsyncNodeContext) node.getChannel();
      final JPPFManagementInfo info = ctx.getManagementInfo();
      final String uuid = getMasterUuid(info);
      if (uuid != null) {
        if (!masterUuids.contains(uuid)) masterUuids.add(uuid);
        if (masterUuids.size() > maxNodeGroups) {
          if (log.isTraceEnabled()) log.trace("[masterUuids.size() = {}] > [maxNodeGroups = {}] for {}", masterUuids.size(), maxNodeGroups, currentNode);
          return false;
        }
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
  private void updateBundler(final TaskBundle taskBundle, final BaseNodeContext context) {
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
    boolean result = true;
    if (policy != null) {
      preparePolicy(policy, job, stats, job.getNbChannels());
      result = policy.evaluate(this.driverInfo);
      if (!result && debugEnabled) log.debug("grid policy check false for {}", job);
    }
    return result;
  }

  /**
   * Keep only the nodes whose configuration have the lowest distance when compared to the desired configuration.
   * @param job the job that specifies the desired node configuration.
   * @param channels the list of eligible channels.
   * @return one or more channels with the minimum computed score.
   */
  private List<BaseNodeContext> filterLowestDistances(final ServerJob job, final List<BaseNodeContext> channels) {
    final JPPFNodeConfigSpec spec =  job.getSLA().getDesiredNodeConfiguration();
    final TypedProperties desiredConfiguration = (spec == null) ? null : spec.getConfiguration();
    final CollectionSortedMap<Integer, BaseNodeContext> scoreMap = new SetSortedMap<>();
    if (debugEnabled) log.debug("computing scores for job '{}', uuid={}", job.getName(), job.getUuid());
    for (final BaseNodeContext channel: channels) {
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
      for (Map.Entry<Integer, Collection<BaseNodeContext>> entry: scoreMap.entrySet()) {
        for (final BaseNodeContext c: entry.getValue()) map.putValue(entry.getKey(), c.getUuid());
      }
      log.debug("computed scores: {}", map);
    }
    final int n = scoreMap.firstKey();
    return (scoreMap.isEmpty()) ? Collections.<BaseNodeContext>emptyList() : new ArrayList<>(scoreMap.getValues(n));
  }
}
