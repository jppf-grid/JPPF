/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
import java.util.concurrent.atomic.*;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.utils.*;
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
   * Property for total nodes attached to a peer driver.
   */
  public static final String PEER_TOTAL_THREADS = "jppf.peer.processing.threads";
  /**
   * Property for how often the timer task runs.
   */
  private static final long PEERIOD = JPPFConfiguration.getProperties().getLong("jppf.peer.handler.period", 1000L);
  /**
   * The peers to manage.
   */
  private final List<AbstractNodeContext> peers = new ArrayList<>();
  /**
   * Executes tasks that fetch the number of nodes and total threads for a single peer driver.
   */
  private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new JPPFThreadFactory("PeerHandler"));
  /**
   * Completion service used to fetch the results of the tasks submitted tot he executor.
   */
  private final CompletionService<Void> completer = new ExecutorCompletionService<>(executor);
  /**
   * Whether the timer task is currently running.
   */
  private final AtomicBoolean updating = new AtomicBoolean(false);
  /**
   * Executes a task periodically to update the number of nodes and total threads of all the peer drivers.
   */
  private final Timer timer;
  /**
   * The task which updates the number of nodes and total threads of all the peer drivers.
   */
  private final TimerTask timerTask;
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
   * Polling vs. notifications flag.
   */
  private final boolean pollingMode = false;

  /**
   * Initialize this handler.
   */
  public PeerAttributesHandler() {
    if (pollingMode) {
      timer = new Timer("PeerHandlerTimer", true);
      this.timerTask = new TimerTask() {
        @Override
        public void run() {
          updatePeers();
        }
      };
      timer.schedule(timerTask, 1000L, PEERIOD);
    } else {
      timer = null;
      timerTask = null;
    }
  }

  /**
   * Add the specified peer to the list.
   * @param peer the peer to add.
   */
  void addPeer(final AbstractNodeContext peer) {
    synchronized(peers) {
      peers.add(peer);
    }
  }

  /**
   * Remove the specified peer from the list.
   * @param peer the peer to remove.
   */
  void removePeer(final AbstractNodeContext peer) {
    synchronized(peers) {
      peers.remove(peer);
    }
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
   * Update all the registered peers.
   */
  private void updatePeers() {
    if (updating.compareAndSet(false, true)) {
      try {
        List<AbstractNodeContext> temp;
        synchronized(peers) {
          if (peers.isEmpty()) return;
          temp = new ArrayList<>(peers);
        }
        int count = temp.size();
        for (final AbstractNodeContext peer: temp) {
          completer.submit(new Runnable() {
            @Override
            public void run() {
              updatePeer(peer);
            }
          }, null);
        }
        try {
          while (count > 0) {
            completer.take();
            count--;
          }
        } catch(Exception e) {
          if (debugEnabled) log.debug("error updating peers", e);
        }
      } finally {
        updating.set(false);
      }
    }
  }

  /**
   * Update the specified peer.
   * @param peer the peer to update.
   */
  private void updatePeer(final AbstractNodeContext peer) {
    JMXDriverConnectionWrapper jmx = peer.getPeerJmxConnection();
    if ((jmx != null) && jmx.isConnected()) {
      try {
        TypedProperties props = (TypedProperties) jmx.getAttribute(PeerDriverMBean.MBEAN_NAME, "PeerProperties");
        updatePeer(peer, props);
      } catch(Exception e) {
        if (debugEnabled) log.debug("error getting attributes of " + peer, e);
      }
    }
  }

  /**
   * Update the specified peer.
   * @param peer the peer to update.
   * @param props the updated attributes.
   */
  private void updatePeer(final AbstractNodeContext peer, final TypedProperties props) {
    int newNodes = props.getInt(PEER_TOTAL_NODES, 0);
    int newThreads = props.getInt(PEER_TOTAL_THREADS, 0);
    JPPFSystemInformation info = peer.getSystemInformation();
    if (info != null) {
      int nodes = info.getJppf().getInt(PEER_TOTAL_NODES);
      int threads = info.getJppf().getInt(PEER_TOTAL_THREADS);
      if ((nodes != newNodes) || (threads != newThreads)) {
        if (debugEnabled) log.debug("newNodes={}, newThreads={} for " + peer, newNodes, newThreads);
        info.getJppf().setInt(PEER_TOTAL_NODES, newNodes);
        info.getJppf().setInt(PEER_TOTAL_THREADS, newThreads);
      }
    }
  }

  /**
   * Called when a node gets closed.
   * @param context the node context.
   */
  void onCloseNode(final AbstractNodeContext context) {
    if (!context.isPeer()) {
      totalNodes.decrementAndGet();
      JPPFSystemInformation sys = context.getSystemInformation();
      if (sys != null) {
        int nbThreads = sys.getJppf().getInt("jppf.processing.threads", 1);
        totalThreads.addAndGet(-nbThreads);
      }
      sendNotification();
      if (debugEnabled) log.debug("totalNodes={}, totalThreads={}", totalNodes, totalThreads);
    } else {
      removePeer(context);
      JMXDriverConnectionWrapper jmx = context.getPeerJmxConnection();
      if (jmx != null) {
        try {
          jmx.removeNotificationListener(PeerDriverMBean.MBEAN_NAME, this, null, context);
        } catch (Exception ignore) {
        }
      }
    }
  }

  /**
   * Called when a node gets connected to the server.
   * @param context the node context.
   */
  void onNodeConnected(final AbstractNodeContext context) {
    if (!context.isPeer()) {
      totalNodes.incrementAndGet();
      JPPFSystemInformation sys = context.getSystemInformation();
      if (sys != null) {
        int nbThreads = sys.getJppf().getInt("jppf.processing.threads", 1);
        totalThreads.addAndGet(nbThreads);
      }
      sendNotification();
      if (debugEnabled) log.debug("totalNodes={}, totalThreads={}", totalNodes, totalThreads);
    } else {
      addPeer(context);
      JMXDriverConnectionWrapper jmx = context.getPeerJmxConnection();
      if (jmx != null) {
        try {
          jmx.addNotificationListener(PeerDriverMBean.MBEAN_NAME, this, null, context);
        } catch (Exception ignore) {
        }
      }
    }
  }

  /**
   * Send the current attributes values as a JMX notification.
   */
  private void sendNotification() {
    Notification notif = new Notification("peer.attribute", PeerDriverMBean.MBEAN_NAME, notifCount.incrementAndGet(), System.currentTimeMillis());
    TypedProperties props = new TypedProperties();
    props.setInt(PEER_TOTAL_NODES, totalNodes.get());
    props.setInt(PEER_TOTAL_THREADS, totalThreads.get());
    notif.setUserData(props);
    PeerDriver.getInstance().sendNotification(notif);
  }

  /**
   * Close and cleanup this handler.
   */
  void close() {
    synchronized(peers) {
      peers.clear();
    }
    executor.shutdownNow();
    timer.cancel();
    timerTask.cancel();
    timer.purge();
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        TypedProperties props = (TypedProperties) notification.getUserData();
        updatePeer((AbstractNodeContext) handback, props);
      }
    });
  }
}
