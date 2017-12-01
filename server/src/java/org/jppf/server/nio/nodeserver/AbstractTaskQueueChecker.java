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

package org.jppf.server.nio.nodeserver;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import org.jppf.execute.ExecutorStatus;
import org.jppf.load.balancer.JPPFContext;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 * @param <C> type of the <code>ExecutorChannel</code>.
 */
abstract class AbstractTaskQueueChecker<C extends AbstractNodeContext> extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractTaskQueueChecker.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Whether to allow dispatching to peer drivers without any node attached, defaults to {@code false}.
   */
  static final boolean disptachtoPeersWithoutNode = JPPFConfiguration.get(JPPFProperties.PEER_ALLOW_ORPHANS);
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
   * Lock on the job queue.
   */
  final Lock queueLock;
  /**
   * The list of idle node channels.
   */
  final Set<C> idleChannels = new LinkedHashSet<>();
  /**
   * Holds information about the execution context.
   */
  final JPPFContext jppfContext;
  /**
   * Used to add channels asynchronously to avoid deadlocks.
   * @see <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-344">JPPF-344 Server deadlock with many slave nodes</a>
   */
  private final ExecutorService channelsExecutor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("ChannelsExecutor"));
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
  NodeNioServer server;
  /**
   * Handles reservations of nodes to jobs.
   */
  NodeReservationHandler reservationHandler;

  /**
   * Initialize this task queue checker with the specified node server.
   * @param server the node server.
   * @param queue the reference queue to use.
   * @param stats reference to the statistics.
   * @param bundlerFactory the load-balancer factory.
   */
  AbstractTaskQueueChecker(final NodeNioServer server, final JPPFPriorityQueue queue, final JPPFStatistics stats, final JPPFBundlerFactory bundlerFactory) {
    this.server = server;
    this.queue = queue;
    this.jppfContext = new JPPFContextDriver(queue);
    this.stats = stats;
    this.bundlerFactory = bundlerFactory;
    this.queueLock = queue.getLock();
    this.driverInfo = JPPFDriver.getInstance().getSystemInformation();
  }

  /**
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  JPPFContext getJPPFContext() {
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
  void addIdleChannel(final C channel) {
    if (channel == null) {
      String message  = "channel is null";
      log.error(message);
      throw new IllegalArgumentException(message);
    }
    if (channel.getExecutionStatus() != ExecutorStatus.ACTIVE) { 
      String message  = "channel is not active: " + channel;
      log.error(message);
      throw new IllegalStateException(message);
    }
    if (debugEnabled) log.debug("request to add idle channel {}", channel);
    channelsExecutor.execute(new Runnable() {
      @Override
      public void run() {
        if (debugEnabled) log.debug("adding idle channel {}", channel);
        if (channel.getChannel().isOpen()) {
          synchronized(idleChannels) {
            if (!reservationHandler.transitionReservation(channel)) reservationHandler.removeReservation(channel);
            if (idleChannels.add(channel)) {
              channel.idle.set(true);
              JPPFSystemInformation info = channel.getSystemInformation();
              if (info != null) info.getJppf().set(JPPFProperties.NODE_IDLE, true);
              stats.addValue(JPPFStatisticsHelper.IDLE_NODES, 1);
            }
          }
          wakeUp();
        }
        else channel.handleException(channel.getChannel(), null);
      }
    });
  }

  /**
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   * @return a reference to the removed channel.
   */
  C removeIdleChannel(final C channel) {
    if (debugEnabled) log.debug("removing idle channel {}", channel);
    synchronized(idleChannels) {
      if (idleChannels.remove(channel)) {
        channel.idle.set(false);
        JPPFSystemInformation info = channel.getSystemInformation();
        if (info != null) info.getJppf().set(JPPFProperties.NODE_IDLE, false);
        stats.addValue(JPPFStatisticsHelper.IDLE_NODES, -1);
      } // else log.warn("could not remove idle channel {}, call stack:\n{}", channel, ExceptionUtils.getCallStack());
    }
    return channel;
  }

  /**
   * Asynchronously remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   * @return a futrue on the removal request task.
   */
  Future<?> removeIdleChannelAsync(final C channel) {
    if (debugEnabled) log.debug("request to remove idle channel {}", channel);
    return channelsExecutor.submit(new Runnable() {
      @Override
      public void run() {
        removeIdleChannel(channel);
      }
    });
  }

  /**
   * Get the list of idle channels.
   * @return a new copy of the underlying list of idle channels.
   */
  List<C> getIdleChannels() {
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
}
