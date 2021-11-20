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

package org.jppf.server.nio.nodeserver;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.management.*;

import org.jppf.load.balancer.*;
import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class PeerAttributesHandler implements NotificationListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(PeerAttributesHandler.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Property for total nodes attached to a peer driver.
   */
  public static final String PEER_TOTAL_NODES = "jppf.peer.total.nodes";
  /**
   * Property for total threads for the nodes attached to a peer driver.
   */
  public static final String PEER_TOTAL_THREADS = JPPFProperties.PEER_PROCESSING_THREADS.getName();
  /**
   * Executes tasks that fetch the number of nodes and total threads for a single peer driver.
   */
  private final ExecutorService executor;
  /**
   * Total number of fully connected nodes.
   */
  private final AtomicInteger totalNodes = new AtomicInteger(0);
  /**
   * Total number of processing threads for all fully connected nodes.
   */
  private final AtomicInteger totalThreads = new AtomicInteger(0);
  /**
   * Notification sequence number.
   */
  private final AtomicLong notifCount = new AtomicLong(0L);
  /**
   * The JPPF driver.
   */
  private final JPPFDriver driver;

  /**
   * Initialize this handler.
   * @param driver reference to the JPPF driver.
   * @param nbThreads the number of threads in the thread pool.
   */
  public PeerAttributesHandler(final JPPFDriver driver, final int nbThreads) {
    this.driver = driver;
    executor = Executors.newFixedThreadPool(nbThreads, new JPPFThreadFactory("PeerHandler"));
  }

  /**
   * Get the total number of fully connected nodes.
   * @return the total number of nodes.
   */
  public int getTotalNodes() {
    return totalNodes.get();
  }

  /**
   * Get the total number of processing threads for all fully connected nodes.
   * @return the total number of threads.
   */
  public int getTotalThreads() {
    return totalThreads.get();
  }

  /**
   * Update the specified peer.
   * @param peer the peer to update.
   * @param props the updated attributes.
   */
  private static void updatePeer(final BaseNodeContext peer, final TypedProperties props) {
    final int newNodes = props.getInt(PEER_TOTAL_NODES, 0);
    final int newThreads = props.getInt(PEER_TOTAL_THREADS, 0);
    final JPPFSystemInformation info = peer.getSystemInformation();
    if (debugEnabled) log.debug("updating peer with newNodes={}, newThreads={}, systemInfo={}, peer={}", newNodes, newThreads, info, peer);
    if (info != null) {
      final TypedProperties jppf = info.getJppf();
      final int nodes = jppf.getInt(PEER_TOTAL_NODES);
      final int threads = jppf.getInt(PEER_TOTAL_THREADS);
      if ((nodes != newNodes) || (threads != newThreads)) {
        if (debugEnabled) log.debug("newNodes={}, newThreads={} for " + peer, newNodes, newThreads);
        jppf.setInt(PEER_TOTAL_NODES, newNodes).setInt(PEER_TOTAL_THREADS, newThreads);
        final Bundler<?> bundler = peer.getBundler();
        if (bundler instanceof ChannelAwareness) ((ChannelAwareness) bundler).setChannelConfiguration(info);
      }
    }
  }

  /**
   * Called when a node gets closed.
   * @param context the node context.
   */
  public void onCloseNode(final BaseNodeContext context) {
    if (!context.isPeer()) {
      totalNodes.decrementAndGet();
      final JPPFSystemInformation sys = context.getSystemInformation();
      if (sys != null) {
        final int nbThreads = sys.getJppf().getInt(JPPFProperties.PROCESSING_THREADS.getName(), 1);
        totalThreads.addAndGet(-nbThreads);
      }
      sendNotification();
      if (debugEnabled) log.debug("totalNodes={}, totalThreads={}", totalNodes, totalThreads);
    } else {
      final JMXDriverConnectionWrapper jmx = context.getPeerJmxConnection();
      if (jmx != null) {
        try {
          jmx.removeNotificationListener(PeerDriverMBean.MBEAN_NAME, this, null, context);
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
    }
  }

  /**
   * Called when a node gets connected to the server.
   * @param context the node context.
   */
  public void onNodeConnected(final BaseNodeContext context) {
    if (!context.isPeer()) {
      totalNodes.incrementAndGet();
      final JPPFSystemInformation sys = context.getSystemInformation();
      if (sys != null) {
        final int nbThreads = sys.getJppf().getInt(JPPFProperties.PROCESSING_THREADS.getName(), 1);
        totalThreads.addAndGet(nbThreads);
      }
      sendNotification();
      if (debugEnabled) log.debug("totalNodes={}, totalThreads={}", totalNodes, totalThreads);
    } else {
      final JMXDriverConnectionWrapper jmx = context.getPeerJmxConnection();
      if (jmx != null) {
        try {
          if (debugEnabled) log.debug("registered peer attributes handler notification listener on node {}", context);
          executor.execute(() -> {
            try {
              updatePeer(context, (TypedProperties) jmx.getAttribute(PeerDriverMBean.MBEAN_NAME, "PeerProperties"));
            } catch (final Exception e) {
              if (debugEnabled) log.debug("getting peer config for {}", context, e);
            }
          });
          jmx.addNotificationListener(PeerDriverMBean.MBEAN_NAME, this, null, context);
        } catch (final Exception e) {
          if (debugEnabled) log.debug("error registering notification listener", e);
        }
      }
    }
  }

  /**
   * Send the current attributes values as a JMX notification.
   */
  private void sendNotification() {
    final Notification notif = new Notification("peer.attribute", PeerDriverMBean.MBEAN_NAME, notifCount.incrementAndGet(), System.currentTimeMillis());
    final TypedProperties props = new TypedProperties();
    props.setInt(PEER_TOTAL_NODES, totalNodes.get());
    props.setInt(PEER_TOTAL_THREADS, totalThreads.get());
    notif.setUserData(props);
    driver.getPeerDriver().sendNotification(notif);
  }

  /**
   * Close and cleanup this handler.
   */
  void close() {
    executor.shutdownNow();
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    if (debugEnabled) log.debug("received notification {} for {}", notification.getUserData(), handback);
    executor.execute(() -> {
      final TypedProperties props = (TypedProperties) notification.getUserData();
      updatePeer((BaseNodeContext) handback, props);
    });
  }
}
