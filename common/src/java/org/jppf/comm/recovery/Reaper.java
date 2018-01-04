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

package org.jppf.comm.recovery;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.*;

/**
 * This class checks, at regular intervals, the recovery-specific connections to remote peers,
 * and detects whether the corresponding peer is dead.
 * @author Laurent Cohen
 */
public class Reaper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Reaper.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Reaper thread pool.
   */
  private ExecutorService threadPool = null;
  /**
   * The server that handles the connections to the remote peers.
   */
  private RecoveryServer server = null;
  /**
   * The timer that performs scheduled connections checks at regular intervals.
   */
  private Timer timer = null;
  /**
   * The list of listeners to this object's events.
   */
  private final List<ReaperListener> listeners = new ArrayList<>();

  /**
   * Initialize this reaper with the specified recovery server.
   * @param server the server that handles the connections to the remote peers.
   * @param poolSize this reaper's thread pool size.
   * @param runInterval the interval between two runs of this reaper.
   */
  public Reaper(final RecoveryServer server, final int poolSize, final long runInterval) {
    this.server = server;
    threadPool = Executors.newFixedThreadPool(poolSize, new JPPFThreadFactory("Reaper"));
    timer = new Timer("Reaper timer");
    timer.schedule(new ReaperTask(), 0L, runInterval);
  }

  /**
   * Submit a new connection for immediate check and get the corresponding node or client uuid.
   * @param connection the connection to check.
   */
  void newConnection(final ServerConnection connection) {
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        if (!InterceptorHandler.invokeOnAccept(connection.getSocketWrapper())) {
          if (debugEnabled) log.debug("connection denied by interceptor: {}", connection);
          return;
        }
        connection.run();
        checkConnection(connection);
        if (connection.isOk()) server.addConnection(connection);
      }
    };
    new Thread(r).start();
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addReaperListener(final ReaperListener listener) {
    if (listener == null) return;
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeReaperListener(final ReaperListener listener) {
    if (listener == null) return;
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners that a connection has failed.
   * @param connection the server-side connection that failed.
   */
  private void fireReaperEvent(final ServerConnection connection) {
    final ReaperEvent event = new ReaperEvent(connection);
    synchronized (listeners) {
      for (ReaperListener listener : listeners) listener.connectionFailed(event);
    }
  }

  /**
   * Check a connection after an attempt to reach the remote peer.
   * @param connection the connection to check.
   */
  private void checkConnection(final ServerConnection connection) {
    if (!connection.isOk()) {
      server.removeConnection(connection);
      fireReaperEvent(connection);
    } else if (!connection.isInitialized()) {
      fireReaperEvent(connection);
      connection.setInitialized(true);
    }
  }

  /**
   * The timer task that submits the connection checks to the the executor.
   */
  private class ReaperTask extends TimerTask {
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      final ServerConnection[] connections = server.connections();
      final CompletionService<?> completer = new ExecutorCompletionService<>(threadPool, new ArrayBlockingQueue<Future<Object>>(connections.length));
      for (ServerConnection c : connections) completer.submit(c, null);
      for (int i=0; i<connections.length; i++) {
        try {
          completer.take();
        } catch (final Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
        }
      }
      for (ServerConnection c : connections) checkConnection(c);
    }
  }
}
