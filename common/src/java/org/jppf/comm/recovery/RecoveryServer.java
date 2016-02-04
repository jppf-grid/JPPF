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

package org.jppf.comm.recovery;

import java.net.*;
import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This class handles the server-side management of recovery connections to remote peers.
 * @author Laurent Cohen
 */
public class RecoveryServer extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RecoveryServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The server socket.
   */
  private ServerSocket serverSocket = null;
  /**
   * The list of active connections.
   */
  private final List<ServerConnection> connections = new ArrayList<>(100);
  /**
   * Performs the connections checks at regular intervals.
   */
  private final Reaper reaper;

  /**
   * Default constructor.
   */
  public RecoveryServer() {
    TypedProperties config = JPPFConfiguration.getProperties();
    int reaperPoolSize = config.get(JPPFProperties.RECOVERY_REAPER_POOL_SIZE);
    long reaperRunInterval = config.get(JPPFProperties.RECOVERY_REAPER_RUN_INTERVAL);
    reaper = new Reaper(this, reaperPoolSize, reaperRunInterval);
  }

  @Override
  public void run() {
    TypedProperties config = JPPFConfiguration.getProperties();
    int maxRetries = config.get(JPPFProperties.RECOVERY_MAX_RETRIES);
    int socketReadTimeout = config.get(JPPFProperties.RECOVERY_READ_TIMEOUT);
    int recoveryPort = config.get(JPPFProperties.RECOVERY_SERVER_PORT);
    try {
      serverSocket = new ServerSocket(recoveryPort);
      while (!isStopped()) {
        Socket socket = serverSocket.accept();
        ServerConnection connection = new ServerConnection(socket, maxRetries, socketReadTimeout);
        reaper.newConnection(connection);
      }
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    close();
  }

  /**
   * Close this server and release the resources it is using.
   */
  public void close() {
    setStopped(true);
    synchronized(connections) {
      try {
        serverSocket.close();
      } catch(Exception e) {
        if (debugEnabled) log.debug("error closing the recovery server socket", e);
      }
      for (ServerConnection c: connections) c.close();
      connections.clear();
    }
  }

  /**
   * Get a list of all connections currently handled by this server.
   * The resulting array is independent from the original collection: changes to one has no effect on the other.
   * @return an array of {@link ServerConnection} instances.
   */
  ServerConnection[] connections() {
    synchronized(connections) {
      return connections.toArray(new ServerConnection[connections.size()]);
    }
  }

  /**
   * Add the specified connection to the list of connections handled by this server.
   * @param connection the connection to add.
   */
  void addConnection(final ServerConnection connection) {
    synchronized(connections) {
      connections.add(connection);
    }
  }

  /**
   * Remove the specified connection from the list of connections handled by this server.
   * @param connection the connection to remove.
   */
  void removeConnection(final ServerConnection connection) {
    synchronized(connections) {
      connections.remove(connection);
    }
  }

  /**
   * Get the reaper for this recover server.
   * @return a {@link Reaper} instance.
   */
  public Reaper getReaper() {
    return reaper;
  }
}
