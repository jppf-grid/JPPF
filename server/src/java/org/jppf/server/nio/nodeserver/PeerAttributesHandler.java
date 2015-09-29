/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.management.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class PeerAttributesHandler {
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
  public static final String PEER_TOTAL_THREADS = "peer.processing.threads";
  /**
   * Property for how often the timer task runs.
   */
  private static final long PEERIOD = JPPFConfiguration.getProperties().getLong("jppf.peer.handler.period", 1000L);
  /**
   * The peers to manage.
   */
  private final List<AbstractNodeContext> peers = new ArrayList<AbstractNodeContext>();
  /**
   * Executes tasks that fetch the number of nodes and total threads for a single peer driver.
   */
  private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new JPPFThreadFactory("PeerHandler"));
  /**
   * Completion service used to fetch the results of the tasks submitted tot he executor.
   */
  private final CompletionService<Void> completer = new ExecutorCompletionService<Void>(executor);
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
   * Initialize this handler.
   */
  public PeerAttributesHandler() {
    timer = new Timer("PeerHandlerTimer", true);
    this.timerTask = new TimerTask() {
      @Override
      public void run() {
        updatePeers();
      }
    };
    timer.schedule(timerTask, 1000L, PEERIOD);
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
   * Update all the registered peers.
   */
  private void updatePeers() {
    if (updating.compareAndSet(false, true)) {
      try {
        List<AbstractNodeContext> temp;
        synchronized(peers) {
          if (peers.isEmpty()) return;
          temp = new ArrayList<AbstractNodeContext>(peers);
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
          if (debugEnabled) log.debug("error updaing peers", e);
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
        int newNodes = props.getInt(PEER_TOTAL_NODES, 0);
        int newThreads = props.getInt(PEER_TOTAL_THREADS, 0);
        JPPFSystemInformation info = peer.getSystemInformation();
        if (info != null) {
          int nodes = info.getJppf().getInt(PEER_TOTAL_NODES);
          int threads = info.getJppf().getInt(PEER_TOTAL_THREADS);
          if ((nodes != newNodes) || (threads != newThreads)) {
            if (debugEnabled) log.debug("newNodes={}, newThreads={} for " + peer, newNodes, newThreads);
            info.getJppf().setProperty(PEER_TOTAL_NODES, Integer.toString(newNodes));
            info.getJppf().setProperty(PEER_TOTAL_THREADS, Integer.toString(newThreads));
          }
        }
      } catch(Exception e) {
        if (debugEnabled) log.debug("error getting attributes of " + peer, e);
      }
    }
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
}
