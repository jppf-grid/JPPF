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

package org.jppf.server.nio.nodeserver;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import org.jppf.execute.ExecutorStatus;
import org.jppf.load.balancer.*;
import org.jppf.load.balancer.impl.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFContextDriver;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 * @param <C> type of the <code>ExecutorChannel</code>.
 */
public class TaskQueueChecker<C extends AbstractNodeContext> extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TaskQueueChecker.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * Random number generator used to randomize the choice of idle channel.
   */
  private final Random random = new Random(System.nanoTime());
  /**
   * Reference to the job queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * Reference to the statistics.
   */
  private final JPPFStatistics stats;
  /**
   * Lock on the job queue.
   */
  private final Lock queueLock;
  /**
   * The list of idle node channels.
   */
  private final Set<C> idleChannels = new LinkedHashSet<>();
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  private Bundler bundler;
  /**
   * Holds information about the execution context.
   */
  private final JPPFContext jppfContext;
  /**
   * Used to add channels asynchronously to avoid dedlocks.
   * @see <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-344">JPPF-344 Server deadlock with many slave nodes</a>
   */
  private final ExecutorService channelsExecutor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("ChannelsExecutor"));

  /**
   * Initialize this task queue checker with the specified node server.
   * @param queue the reference queue to use.
   * @param stats reference to the statistics.
   */
  public TaskQueueChecker(final JPPFPriorityQueue queue, final JPPFStatistics stats) {
    this.queue = queue;
    this.jppfContext = new JPPFContextDriver(queue);
    this.stats = stats;
    this.queueLock = queue.getLock();
    this.bundler = createDefault();
  }

  /**
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  public JPPFContext getJPPFContext() {
    return jppfContext;
  }

  /**
   * Create new instance of default bundler.
   * @return a new {@link Bundler} instance.
   */
  protected Bundler createDefault() {
    FixedSizeProfile profile = new FixedSizeProfile();
    profile.setSize(1);
    return new FixedSizeBundler(profile);
  }

  /**
   * Get the bundler used to schedule tasks for the corresponding node.
   * @return a {@link Bundler} instance.
   */
  public Bundler getBundler() {
    return bundler;
  }

  /**
   * Set the bundler used to schedule tasks for the corresponding node.
   * @param bundler a {@link Bundler} instance.
   */
  public void setBundler(final Bundler bundler) {
    this.bundler = (bundler == null) ? createDefault() : bundler;
  }

  /**
   * Get the number of idle channels.
   * @return the size of the underlying list of idle channels.
   */
  public int getNbIdleChannels() {
    synchronized (idleChannels) {
      return idleChannels.size();
    }
  }

  /**
   * Add a channel to the list of idle channels.
   * @param channel the channel to add to the list.
   */
  public void addIdleChannel(final C channel) {
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (channel.getExecutionStatus() != ExecutorStatus.ACTIVE) throw new IllegalStateException("channel is not active: " + channel);
    if (traceEnabled) log.trace("Adding idle channel " + channel);
    channelsExecutor.execute(new Runnable() {
      @Override
      public void run() {
        synchronized(idleChannels) {
          idleChannels.add(channel);
        }
        wakeUp();
        stats.addValue(JPPFStatisticsHelper.IDLE_NODES, 1);
      }
    });
  }

  /**
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   * @return a reference to the removed channel.
   */
  public C removeIdleChannel(final C channel) {
    if (traceEnabled) log.trace("Removing idle channel " + channel);
    boolean removed;
    synchronized(idleChannels) {
      removed = idleChannels.remove(channel);
    }
    if (removed) stats.addValue(JPPFStatisticsHelper.IDLE_NODES, -1);
    return channel;
  }

  /**
   * Get the list of idle channels.
   * @return a new copy of the underlying list of idle channels.
   */
  public List<C> getIdleChannels() {
    synchronized (idleChannels) {
      return new ArrayList<>(idleChannels);
    }
  }

  /**
   * Return whether any idle channel is available.
   * @return <code>true</code> when there are no idle channels.
   */
  public boolean hasIdleChannel() {
    synchronized (idleChannels) {
      return !idleChannels.isEmpty();
    }
  }

  /**
   * Return whether the specified channel is in the set of idle channels.
   * @param channel the channel to check.
   * @return <code>true</code> if the channel is in the set of idle channels, <code>false</code> otherwise.
   */
  public boolean hasIdleChannel(final C channel) {
    synchronized (idleChannels) {
      return idleChannels.contains(channel);
    }
  }

  /**
   * Clear the list of idle channels.
   */
  public void clearIdleChannels() {
    synchronized (idleChannels) {
      idleChannels.clear();
    }
  }

  /**
   * Perform the assignment of tasks.
   * @see Runnable#run()
   */
  @Override
  public void run() {
    while (!isStopped()) {
      if (!dispatch()) goToSleep(1L);
    }
  }

  /**
   * Perform the assignment of tasks.
   * @return true if a job was dispatched, false otherwise.
   */
  public boolean dispatch() {
    boolean dispatched = false;
    try {
      queue.processPendingBroadcasts();
      C channel = null;
      ServerTaskBundleNode nodeBundle = null;
      synchronized(idleChannels) {
        if (idleChannels.isEmpty() || queue.isEmpty()) return false;
        if (debugEnabled) log.debug(Integer.toString(idleChannels.size()) + " channels idle");
        queueLock.lock();
        try {
          Iterator<ServerJob> it = queue.iterator();
          while ((channel == null) && it.hasNext() && !idleChannels.isEmpty()) {
            ServerJob serverJob = it.next();
            channel = retrieveChannel(serverJob);
            if (channel != null) {
              nodeBundle = prepareJobDispatch(channel, serverJob);
              removeIdleChannel(channel);
            }
            if (channel != null && nodeBundle != null) {
              try {
                dispatchJobToChannel(channel, nodeBundle);
                dispatched = true;
                return true;
              } catch (Exception e) {
                log.error(e.getMessage(), e);
                channel.handleException(channel.getChannel(), e);
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
    if (debugEnabled) log.debug("dispatching jobUuid=" + selectedBundle.getJob().getUuid() + " to node " + channel + ", nodeUuid=" + channel.getConnectionUuid());
    int size = 1;
    try {
      updateBundler(getBundler(), selectedBundle.getJob(), channel);
      size = channel.getBundler().getBundleSize();
    } catch (Exception e) {
      log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1", e);
      FixedSizeProfile profile = new FixedSizeProfile();
      profile.setSize(1);
      setBundler(new FixedSizeBundler(profile));
    }
    return queue.nextBundle(selectedBundle, size);
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to dispatch the job to.
   * @param nodeBundle the job to dispatch.
   */
  @SuppressWarnings("unchecked")
  private void dispatchJobToChannel(final C channel, final ServerTaskBundleNode nodeBundle) {
    synchronized(channel.getMonitor()) {
      Future<?> future = channel.submit(nodeBundle);
      nodeBundle.jobDispatched(channel, future);
    }
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param bundle the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private C findIdleChannelIndex(final ServerJob bundle) {
    ExecutionPolicy policy = bundle.getJob().getSLA().getExecutionPolicy();
    if (debugEnabled && (policy != null)) log.debug("Bundle " + bundle + " has an execution policy:\n" + policy);
    List<C> acceptableChannels = new ArrayList<>(idleChannels.size());
    List<String> uuidPath = bundle.getJob().getUuidPath().getList();
    Iterator<C> iterator = idleChannels.iterator();
    int nbJobChannels = bundle.getNbChannels();
    while (iterator.hasNext()) {
      C ch = iterator.next();
      if (ch.getExecutionStatus() != ExecutorStatus.ACTIVE) {
        if (debugEnabled) log.debug("channel is not opened: " + ch);
        iterator.remove();
        continue;
      }
      if (!ch.isActive()) continue;
      if (debugEnabled) log.debug("uuid path=" + uuidPath + ", node uuid=" + ch.getUuid());
      if (uuidPath.contains(ch.getUuid())) {
        if (debugEnabled) log.debug("bundle uuid path already contains node " + ch + " : uuidPath=" + uuidPath + ", nodeUuid=" + ch.getUuid());
        continue;
      }
      if(bundle.getBroadcastUUID() != null && !bundle.getBroadcastUUID().equals(ch.getUuid())) continue;
      if (policy != null) {
        JPPFSystemInformation info = ch.getSystemInformation();
        boolean b = false;
        try {
          preparePolicy(policy, bundle, stats, nbJobChannels);
          b = policy.accepts(info);
        } catch(Exception ex) {
          log.error("An error occurred while running the execution policy to determine node participation.", ex);
        }
        if (debugEnabled) log.debug("rule execution is *" + b + "* for jobUuid=" + bundle.getUuid() + " on local channel=" + ch);
        if (!b) continue;
      }
      // add a bias toward local node
      if (ch.isLocal()) return ch;
      acceptableChannels.add(ch);
    }
    int size = acceptableChannels.size();
    if (debugEnabled) log.debug("found " + size + " acceptable channels");
    return (size > 0) ? acceptableChannels.get(size > 1 ? random.nextInt(size) : 0) : null;
  }

  /**
   * Set the parameters needed as bounded variables fro scripted execution policies.
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
   * @param bundle the bundle from which to get the job information.
   * @return true if the job can be dispatched to at least one more node, false otherwise.
   */
  private static boolean checkJobState(final ServerJob bundle) {
    if (bundle.isCancelled()) return false;
    JobSLA sla = bundle.getJob().getSLA();
    if (debugEnabled) log.debug("job '{}', suspended={}, pending={}, expired={}", new Object[] {bundle.getName(), sla.isSuspended(), bundle.isPending(), bundle.isJobExpired()});
    if (sla.isSuspended() || bundle.isPending() || bundle.isJobExpired()) return false;
    if (debugEnabled) log.debug("current nodes = " + bundle.getNbChannels() + ", maxNodes = " + sla.getMaxNodes());
    return bundle.getNbChannels() < sla.getMaxNodes();
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param bundler the bundler to check and update.
   * @param taskBundle the job.
   * @param context the current node context.
   */
  private void updateBundler(final Bundler bundler, final TaskBundle taskBundle, final C context) {
    context.checkBundler(bundler, jppfContext);
    Bundler ctxBundler = context.getBundler();
    if (ctxBundler instanceof JobAwareness) ((JobAwareness) ctxBundler).setJobMetadata(taskBundle.getMetadata());
  }
}
