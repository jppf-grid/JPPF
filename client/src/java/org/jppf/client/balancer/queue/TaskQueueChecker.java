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

package org.jppf.client.balancer.queue;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import org.jppf.client.*;
import org.jppf.client.balancer.*;
import org.jppf.execute.ExecutorStatus;
import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks from the job queue.
 */
public class TaskQueueChecker extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TaskQueueChecker.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Random number generator used to randomize the choice of idle channel.
   */
  private final Random random = new Random(System.nanoTime());
  /**
   * Reference to the job queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * Lock on the job queue.
   */
  private final Lock queueLock;
  /**
   * The list of idle node channels.
   */
  private final CollectionSortedMap<Integer, ChannelWrapper> idleChannels = new SetSortedMap<>(new DescendingIntegerComparator());
  /**
   * Holds information about the execution context.
   */
  private final JPPFContext jppfContext;
  /**
   * Used to add channels asynchronously to avoid deadlocks.
   * @see <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-398">JPPF-398 Deadlock in the client</a>
   */
  private final ExecutorService channelsExecutor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("ChannelsExecutor"));
  /**
   * The load-balancer factory.
   */
  private final JPPFBundlerFactory bundlerFactory;
  /**
   * The highest priority for which there is a working connection.
   */
  private int highestPriority = Integer.MIN_VALUE;
  /**
   * Used to synchronize on the highestPriority.
   */
  private final Object priorityLock = new Object();
  /**
   * Queue of actions to remove or add a channel from/to the set of idle channels.
   */
  private final BlockingQueue<Runnable> pendingActions = new LinkedBlockingQueue<>();

  /**
   * Initialize this task queue checker with the specified queue.
   * @param queue the job queue to use.
   * @param bundlerFactory the load-balancer factory.
   */
  public TaskQueueChecker(final JPPFPriorityQueue queue, final JPPFBundlerFactory bundlerFactory) {
    this.queue = queue;
    this.bundlerFactory = bundlerFactory;
    this.jppfContext = new JPPFContextClient(queue);
    this.queueLock = queue.getLock();
  }

  /**
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  public JPPFContext getJPPFContext() {
    return jppfContext;
  }

  /**
   * Set the highest priority.
   * @param priority the new highest priority.
   */
  public void setHighestPriority(final int priority) {
    synchronized(priorityLock) {
      this.highestPriority = priority;
    }
  }

  /**
   * Set the highest priority.
   * @return the highest priority.
   */
  public int getHighestPriority() {
    synchronized(priorityLock) {
      return highestPriority;
    }
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
  public void addIdleChannel(final ChannelWrapper channel) {
    if (debugEnabled) log.debug("adding channel {}", channel);
    if ((channelsExecutor == null) || channelsExecutor.isShutdown() || isStopped()) return;
    if (channel == null) {
      log.warn("channel is null\n{}", ExceptionUtils.getCallStack());
      return;
    }
    final ExecutorStatus status = channel.getExecutionStatus();
    if (status != ExecutorStatus.ACTIVE) {
      log.warn("channel is not active ({})\n{}", channel, ExceptionUtils.getCallStack());
      return;
    }
    pendingActions.offer(new Runnable() {
      @Override
      public void run() {
        if (debugEnabled) log.debug("Adding idle channel from synchronized block: {}", channel);
        idleChannels.putValue(channel.getPriority(), channel);
      }
    });
    wakeUp();
  }

  /**
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   */
  public void removeIdleChannel(final ChannelWrapper channel) {
    if (debugEnabled) log.debug("removing chhanel {}", channel);
    if ((channelsExecutor == null) || channelsExecutor.isShutdown() || isStopped()) return;
    pendingActions.offer(new Runnable() {
      @Override
      public void run() {
        if (debugEnabled) log.debug("Removing idle channel from synchronized block: {}", channel);
        idleChannels.removeValue(channel.getPriority(), channel);
      }
    });
    wakeUp();
  }

  /**
   * Get the list of idle channels.
   * @return a new copy of the underlying list of idle channels.
   */
  public List<ChannelWrapper> getIdleChannels() {
    synchronized (idleChannels) {
      return idleChannels.allValues();
    }
  }

  /**
   * Return whether any idle channel is available.
   * @return true when there are no idle channels.
   */
  public boolean hasIdleChannel() {
    synchronized (idleChannels) {
      return !idleChannels.isEmpty();
    }
  }

  /**
   * Clear all channels from this task queue checker.
   */
  public void clearChannels() {
    synchronized (idleChannels) {
      idleChannels.clear();
    }
  }

  /**
   * Perform the assignment of tasks.
   */
  @Override
  public void run() {
    while (!isStopped()) {
      if (!dispatch()) goToSleep(10L, 10000);
    }
    if (channelsExecutor != null) channelsExecutor.shutdownNow();
    clearChannels();
  }

  /**
   * Process the pending channels to add to or remove from the idle channels.
   */
  private void processPendingActions() {
    Runnable r;
    while ((r = pendingActions.poll()) != null) r.run();
  }

  /**
   * Perform the assignment of tasks.
   * @return true if a job was dispatched, false otherwise.
   */
  public boolean dispatch() {
    boolean dispatched = false;
    try {
      queue.processPendingBroadcasts();
      synchronized (idleChannels) {
        processPendingActions();
        if (idleChannels.isEmpty() || queue.isEmpty()) return false;
        if (debugEnabled) {
          final int size = idleChannels.size();
          if (size == 1) log.debug("1 channel idle: {}", idleChannels.getValues(idleChannels.firstKey()));
          else log.debug("{} channels idle", size);
        }
        ChannelWrapper channel = null;
        ClientJob selectedBundle = null;
        queueLock.lock();
        try {
          final Iterator<ClientJob> it = queue.iterator();
          while ((channel == null) && it.hasNext() && !idleChannels.isEmpty()) {
            final ClientJob job = it.next();
            channel = findIdleChannel(job);
            if (channel != null) selectedBundle = job;
          }
          if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle" : "channel found for bundle: " + channel);
          if (channel != null) {
            dispatchJobToChannel(channel, selectedBundle);
            dispatched = true;
          }
        } catch (final Exception ex) {
          log.error("An error occurred while attempting to dispatch task bundles. This is most likely due to an error in the load balancer implementation.", ex);
        } finally {
          queueLock.unlock();
        }
      }
    } catch (final Exception ex) {
      log.error("An error occurred while preparing for bundle creation and dispatching.", ex);
    }
    return dispatched;
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param job the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private ChannelWrapper findIdleChannel(final ClientJob job) {
    final int idleChannelsSize = idleChannels.size();
    final List<ChannelWrapper> acceptableChannels = new ArrayList<>(idleChannelsSize);
    final int highestPriority = getHighestPriority();
    final Collection<ChannelWrapper> channels = idleChannels.getValues(highestPriority);
    if (channels == null) return null;
    final Iterator<ChannelWrapper> iterator = channels.iterator();
    final Queue<ChannelWrapper> channelsToRemove = new LinkedBlockingQueue<>();
    while (iterator.hasNext()) {
      final ChannelWrapper ch = iterator.next();
      if (ch.getExecutionStatus() != ExecutorStatus.ACTIVE) {
        if (debugEnabled) log.debug("channel is not opened: " + ch);
        channelsToRemove.offer(ch);
        continue;
      }
      if (!job.acceptsChannel(ch)) continue;
      if(job.getBroadcastUUID() != null && !job.getBroadcastUUID().equals(ch.getUuid())) continue;
      acceptableChannels.add(ch);
    }
    processPendingActions();
    if (!channelsToRemove.isEmpty()) {
      ChannelWrapper ch;
      while ((ch = channelsToRemove.poll()) != null) idleChannels.removeValue(ch.getPriority(), ch);
    }
    final int size = acceptableChannels.size();
    if (debugEnabled) log.debug("found " + size + " acceptable channels");
    return (size > 0) ? acceptableChannels.get(size > 1 ? random.nextInt(size) : 0) : null;
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the driver channel to dispatch the job to.
   * @param job the job to dispatch.
   * @throws Exception if any error occurs.
   */
  private void dispatchJobToChannel(final ChannelWrapper channel, final ClientJob job)  throws Exception {
    if (debugEnabled) log.debug("dispatching jobUuid={} to channel {}, connectionUuid=", new Object[] {job.getJob().getUuid(), channel, channel.getConnectionUuid()});
    synchronized (channel.getMonitor()) {
      int size = 1;
      try {
        updateBundler(job.getJob(), channel);
        size = channel.getBundler().getBundleSize();
      } catch (final Exception e) {
        log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1: {}", ExceptionUtils.getStackTrace(e));
        size = bundlerFactory.getFallbackBundler().getBundleSize();
      }
      final ClientTaskBundle jobDispatch = queue.nextBundle(job, size);
      job.addChannel(channel);
      channel.submit(jobDispatch);
    }
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param job the job.
   * @param channel the channel to which the job is submitted.
   */
  private void updateBundler(final JPPFJob job, final ChannelWrapper channel) {
    channel.checkBundler(bundlerFactory, jppfContext);
    if (channel.getBundler() instanceof JobAwareness) {
      ((JobAwareness) channel.getBundler()).setJob(job);
    }
  }
}
