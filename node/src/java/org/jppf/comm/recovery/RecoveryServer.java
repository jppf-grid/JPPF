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

package org.jppf.comm.recovery;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class handles the server-side management of recovery connections to remote peers.
 * @author Laurent Cohen
 */
public class RecoveryServer extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RecoveryServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Indicates the remote peer is a driver.
   */
  public static final char DRIVER = 'D';
  /**
   * Indicates the remote peer is a node.
   */
  public static final char NODE = 'N';
  /**
   * Indicates the remote peer is a client.
   */
  public static final char CLIENT = 'C';
  /**
   * Constant used to avoid instantiating new empty arrays.
   */
  private static final ServerConnection[] EMPTY_CONNECTION_ARRAY = new ServerConnection[0];
  /**
   * The server socket.
   */
  private ServerSocket serverSocket = null;
  /**
   * The list of active connections.
   */
  private final List<ServerConnection> connections = new ArrayList<>(100);
  /**
   * The count of connections that have been opened.
   */
  private AtomicInteger connectionCount = new AtomicInteger(0);
  /**
   * Maximum number of failed write/read attempts on a connection before the remote peer is considered dead.
   */
  private int maxRetries = 3;
  /**
   * Maximum wait time on the response from the remote peer.
   */
  private int socketReadTimeout = 6000;
  /**
   * Port this server is accepting connections on.
   */
  private int recoveryPort = 22222;
  /**
   * Performs the connections checks at regular intervals.
   */
  private Reaper reaper = null;

  /**
   * Default constructor.
   */
  public RecoveryServer()
  {
    configure();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    try
    {
      serverSocket = new ServerSocket(recoveryPort);
      while (!isStopped())
      {
        Socket socket = serverSocket.accept();
        ServerConnection connection = new ServerConnection(socket, maxRetries, socketReadTimeout);
        reaper.newConnection(connection);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    close();
  }

  /**
   * Initialize this server's parameters from the JPPF configuration.
   */
  private void configure()
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    maxRetries = config.getInt("jppf.recovery.max.retries", 3);
    socketReadTimeout = config.getInt("jppf.recovery.read.timeout", 6000);
    recoveryPort = config.getInt("jppf.recovery.server.port", 22222);
    int reaperPoolSize = config.getInt("jppf.recovery.reaper.pool.size", Runtime.getRuntime().availableProcessors());
    long reaperRunInterval = config.getLong("jppf.recovery.reaper.run.interval", 60000L);
    reaper = new Reaper(this, reaperPoolSize, reaperRunInterval);
  }

  /**
   * Close this server and release the resources it is using.
   */
  public void close()
  {
    setStopped(true);
    synchronized(connections)
    {
      try
      {
        serverSocket.close();
      }
      catch(Exception e)
      {
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
  ServerConnection[] connections()
  {
    synchronized(connections)
    {
      return connections.toArray(new ServerConnection[connections.size()]);
    }
  }

  /**
   * Add the specified connection to the list of connections handled by this server.
   * @param connection the connection to add.
   */
  void addConnection(final ServerConnection connection)
  {
    synchronized(connections)
    {
      connections.add(connection);
    }
  }

  /**
   * Remove the specified connection from the list of connections handled by this server.
   * @param connection the connection to remove.
   */
  void removeConnection(final ServerConnection connection)
  {
    synchronized(connections)
    {
      connections.remove(connection);
    }
  }

  /**
   * Get the reaper for this recover server.
   * @return a {@link Reaper} instance.
   */
  public Reaper getReaper()
  {
    return reaper;
  }
}
