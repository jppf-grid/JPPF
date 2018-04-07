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

import java.lang.management.ThreadInfo;
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
  public void addIdleChannelAsync(final ChannelWrapper channel) {
    if ((channelsExecutor == null) || channelsExecutor.isShutdown() || isStopped()) return;
    if (channel == null) throw new IllegalArgumentException("channel is null");
    final ExecutorStatus status = channel.getExecutionStatus();
    if (status != ExecutorStatus.ACTIVE) throw new IllegalStateException("channel is not active ("+ status + "): " + channel);
    final CountDownLatch countDown = new CountDownLatch(1);
    channelsExecutor.execute(new Runnable() {
      @Override
      public void run() {
        addIdleChannel(channel);
        countDown.countDown();
      }
    });
    try {
      countDown.await();
    } catch (final InterruptedException e) {
      log.error(e.getMessage(), e);
    }
  }
   */

  /**
   * Add a channel to the list of idle channels.
   * @param channel the channel to add to the list.
   */
  public void addIdleChannel(final ChannelWrapper channel) {
    if (debugEnabled) log.debug("adding chhanel {}", channel);
    if ((channelsExecutor == null) || channelsExecutor.isShutdown() || isStopped()) return;
    if (channel == null) throw new IllegalArgumentException("channel is null");
    final ExecutorStatus status = channel.getExecutionStatus();
    if (status != ExecutorStatus.ACTIVE) throw new IllegalStateException("channel is not active ("+ status + "): " + channel);
    if (traceEnabled) {
      final String idleChannelsName = SystemUtils.getSystemIdentityName(idleChannels);
      log.trace("Adding idle channel {} to {}", channel, idleChannelsName);
      final ThreadInfo info = DeadlockDetector.getMonitorOwner(idleChannels);
      if (info != null) log.trace("information on owner of idleChannels {}:\n{}", idleChannelsName, DeadlockDetector.printThreadInfo(info));
    }
    synchronized (idleChannels) {
      if (debugEnabled) log.debug("Adding idle channel from synchronized block: {}", channel);
      idleChannels.putValue(channel.getPriority(), channel);
    }
    wakeUp();
  }

  /**
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
  public void removeIdleChannelAsync(final ChannelWrapper channel) {
    if ((channelsExecutor == null) || channelsExecutor.isShutdown() || isStopped()) return;
    final CountDownLatch countDown = new CountDownLatch(1);
    channelsExecutor.execute(new Runnable() {
      @Override
      public void run() {
        removeIdleChannel(channel);
        countDown.countDown();
      }
    });
    try {
      countDown.await();
    } catch (final InterruptedException e) {
      log.error(e.getMessage(), e);
    }
  }
   */

  /**
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   */
  public void removeIdleChannel(final ChannelWrapper channel) {
    if (debugEnabled) log.debug("removing chhanel {}", channel);
    if ((channelsExecutor == null) || channelsExecutor.isShutdown() || isStopped()) return;
    if (traceEnabled) {
      final String idleChannelsName = SystemUtils.getSystemIdentityName(idleChannels);
      log.trace("Removing idle channel {} from {}", channel, idleChannelsName);
      final ThreadInfo info = DeadlockDetector.getMonitorOwner(idleChannels);
      if (info != null) log.trace("information on owner of idleChannels {}:\n{}", idleChannelsName, DeadlockDetector.printThreadInfo(info));
    }
    synchronized (idleChannels) {
      if (debugEnabled) log.debug("Removing idle channel from synchronized block: {}", channel);
      idleChannels.removeValue(channel.getPriority(), channel);
    }
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
   * Perform the assignment of tasks.
   * @return true if a job was dispatched, false otherwise.
   * @see Runnable#run()
   */
  public boolean dispatch() {
    boolean dispatched = false;
    try {
      queue.processPendingBroadcasts();
      synchronized (idleChannels) {
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
            final ClientJob bundleWrapper = it.next();
            channel = findIdleChannelIndex(bundleWrapper);
            if (channel != null) selectedBundle = bundleWrapper;
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
   * @param bundle the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private ChannelWrapper findIdleChannelIndex(final ClientJob bundle) {
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
      if (!bundle.acceptsChannel(ch)) continue;
      if(bundle.getBroadcastUUID() != null && !bundle.getBroadcastUUID().equals(ch.getUuid())) continue;
      acceptableChannels.add(ch);
    }
    if (!channelsToRemove.isEmpty()){
      ChannelWrapper ch = null;
      while ((ch = channelsToRemove.poll()) != null) idleChannels.removeValue(ch.getPriority(), ch);
    }
    final int size = acceptableChannels.size();
    if (debugEnabled) log.debug("found " + size + " acceptable channels");
    return (size > 0) ? acceptableChannels.get(size > 1 ? random.nextInt(size) : 0) : null;
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the driver channel to dispatch the job to.
   * @param selectedBundle the job to dispatch.
   * @throws Exception if any error occurs.
   */
  private void dispatchJobToChannel(final ChannelWrapper channel, final ClientJob selectedBundle)  throws Exception {
    if (debugEnabled) log.debug("dispatching jobUuid={} to channel {}, connectionUuid=", new Object[] {selectedBundle.getJob().getUuid(), channel, channel.getConnectionUuid()});
    synchronized (channel.getMonitor()) {
      int size = 1;
      try {
        updateBundler(selectedBundle.getJob(), channel);
        size = channel.getBundler().getBundleSize();
      } catch (final Exception e) {
        log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1: {}", ExceptionUtils.getStackTrace(e));
        size = bundlerFactory.getFallbackBundler().getBundleSize();
      }
      final ClientTaskBundle bundleWrapper = queue.nextBundle(selectedBundle, size);
      selectedBundle.addChannel(channel);
      channel.submit(bundleWrapper);
    }
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param taskBundle the job.
   * @param channel    the current node context.
   */
  private void updateBundler(final JPPFJob taskBundle, final ChannelWrapper channel) {
    channel.checkBundler(bundlerFactory, jppfContext);
    if (channel.getBundler() instanceof JobAwareness) {
      ((JobAwareness) channel.getBundler()).setJob(taskBundle);
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
}
