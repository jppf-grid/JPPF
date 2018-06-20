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
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
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
  private final CollectionSortedMap<Integer, ChannelWrapper> idleChannels = new SetSortedMap<>(new AbstractJPPFClient.DescendingIntegerComparator());
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
   * Initialize this task queue checker with the specified node server.
   * @param queue the reference queue to use.
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
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (channel.getExecutionStatus() != ExecutorStatus.ACTIVE) throw new IllegalStateException("channel is not active: " + channel);
    channelsExecutor.execute(new Runnable() {
      @Override
      public void run() {
        if (traceEnabled) log.trace("Adding idle channel " + channel);
        synchronized (idleChannels) {
          idleChannels.putValue(channel.getPriority(), channel);
        }
        wakeUp();
      }
    });
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
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   * @return a reference to the removed channel.
   */
  public ChannelWrapper removeIdleChannel(final ChannelWrapper channel) {
    channelsExecutor.execute(new Runnable() {
      @Override
      public void run() {
        if (traceEnabled) log.trace("Removing idle channel " + channel);
        synchronized (idleChannels) {
          idleChannels.removeValue(channel.getPriority(), channel);
        }
      }
    });
    return channel;
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
          int size = idleChannels.size();
          if (size == 1) log.debug("1 channel idle: {}", idleChannels.getValues(idleChannels.firstKey()));
          else log.debug("{} channels idle", size);
        }
        ChannelWrapper channel = null;
        ClientJob selectedBundle = null;
        queueLock.lock();
        try {
          Iterator<ClientJob> it = queue.iterator();
          while ((channel == null) && it.hasNext() && !idleChannels.isEmpty()) {
            ClientJob bundleWrapper = it.next();
            if (bundleWrapper.isCancelled() || bundleWrapper.isCancelling()) continue;
            channel = findIdleChannelIndex(bundleWrapper);
            if (channel != null) selectedBundle = bundleWrapper;
          }
          if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle" : "channel found for bundle: " + channel);
          if (channel != null) {
            dispatchJobToChannel(channel, selectedBundle);
            dispatched = true;
          }
        } catch (Exception ex) {
          log.error("An error occurred while attempting to dispatch task bundles. This is most likely due to an error in the load balancer implementation.", ex);
        } finally {
          queueLock.unlock();
        }
      }
    } catch (Exception ex) {
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
    int idleChannelsSize = idleChannels.size();
    List<ChannelWrapper> acceptableChannels = new ArrayList<>(idleChannelsSize);
    int highestPriority = getHighestPriority();
    Collection<ChannelWrapper> channels = idleChannels.getValues(highestPriority);
    if (channels == null) return null;
    Iterator<ChannelWrapper> iterator = channels.iterator();
    Queue<ChannelWrapper> channelsToRemove = new LinkedBlockingQueue<>();
    while (iterator.hasNext()) {
      ChannelWrapper ch = iterator.next();
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
    int size = acceptableChannels.size();
    if (debugEnabled) log.debug("found " + size + " acceptable channels");
    return (size > 0) ? acceptableChannels.get(size > 1 ? random.nextInt(size) : 0) : null;
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the driver channel to dispatch the job to.
   * @param selectedBundle the job to dispatch.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  private void dispatchJobToChannel(final ChannelWrapper channel, final ClientJob selectedBundle)  throws Exception {
    if (debugEnabled) log.debug("dispatching jobUuid={} to channel {}, connectionUuid=", new Object[] {selectedBundle.getJob().getUuid(), channel, channel.getConnectionUuid()});
    synchronized (channel.getMonitor()) {
      int size = 1;
      try {
        updateBundler(selectedBundle.getJob(), channel);
        size = channel.getBundler().getBundleSize();
      } catch (Exception e) {
        log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1: {}", ExceptionUtils.getStackTrace(e));
        size = bundlerFactory.getFallbackBundler().getBundleSize();
      }
      ClientTaskBundle bundleWrapper = queue.nextBundle(selectedBundle, size);
      selectedBundle.addChannel(channel);
      channel.submit(bundleWrapper);
    }
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param taskBundle the job.
   * @param context    the current node context.
   */
  @SuppressWarnings("deprecation")
  private void updateBundler(final JPPFJob taskBundle, final ChannelWrapper context) {
    context.checkBundler(bundlerFactory, jppfContext);
    if (context.getBundler() instanceof JobAwareness) {
      ((JobAwareness) context.getBundler()).setJobMetadata(taskBundle.getMetadata());
    } else if (context.getBundler() instanceof JobAwarenessEx) {
      ((JobAwarenessEx) context.getBundler()).setJob(taskBundle);
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
