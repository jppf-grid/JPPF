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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.NEW;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.client.event.*;
import org.jppf.comm.socket.*;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Common abstract superclass for client connections to a server.
 * @author Laurent Cohen
 * @author Jeff Rosen
 */
public abstract class AbstractClientConnectionHandler implements ClientConnectionHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractClientConnectionHandler.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The socket client uses to communicate over a socket connection.
   */
  SocketWrapper socketClient = null;
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  SocketInitializer socketInitializer = createSocketInitializer();
  /**
   * The maximum time the underlying socket may be idle, before it is considered suspect and recycled.
   */
  private long maxSocketIdleMillis;
  /**
   * The name or IP address of the host the class server is running on.
   */
  String host = null;
  /**
   * The TCP port the class server is listening to.
   */
  int port = -1;
  /**
   * The client connection which owns this connection handler.
   */
  JPPFClientConnection owner = null;
  /**
   * The name given to this connection handler.
   */
  String name = null;
  /**
   * Status of the connection.
   */
  final AtomicReference<JPPFClientConnectionStatus> status = new AtomicReference<>(NEW);
  /**
   * List of status listeners for this connection.
   */
  private final List<ClientConnectionStatusListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Initialize this connection with the specified owner.
   * @param owner the client connection which owns this connection handler.
   * @param name the name given to this connection.
   */
  protected AbstractClientConnectionHandler(final JPPFClientConnection owner, final String name)
  {
    this.owner = owner;
    //if (owner != null) this.name = owner.getName();
    this.name = name;
    long configSocketIdle = JPPFConfiguration.get(JPPFProperties.SOCKET_MAX_IDLE);
    maxSocketIdleMillis = (configSocketIdle > 10L) ? configSocketIdle * 1000L : -1L;
  }

  /**
   * Get the status of this connection.
   * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
   * @see org.jppf.client.ClientConnectionHandler#getStatus()
   */
  @Override
  public JPPFClientConnectionStatus getStatus()
  {
    return status.get();
  }

  /**
   * Set the status of this connection.
   * @param newStatus a <code>JPPFClientConnectionStatus</code> enumerated value.
   * @see org.jppf.client.ClientConnectionHandler#setStatus(org.jppf.client.JPPFClientConnectionStatus)
   */
  @Override
  public void setStatus(final JPPFClientConnectionStatus newStatus)
  {
    JPPFClientConnectionStatus oldStatus = status.getAndSet(newStatus);
    if (debugEnabled) log.debug("connection '" + name + "' status changing from " + oldStatus + " to " + newStatus);
    if (newStatus != oldStatus)
    {
      fireStatusChanged(oldStatus);
    }
  }

  /**
   * Add a connection status listener to this connection's list of listeners.
   * @param listener the listener to add to the list.
   * @see org.jppf.client.ClientConnectionHandler#addClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
   */
  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    listeners.add(listener);
  }

  /**
   * Remove a connection status listener from this connection's list of listeners.
   * @param listener the listener to remove from the list.
   * @see org.jppf.client.ClientConnectionHandler#removeClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
   */
  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * Create a socket initializer for this connection handler.
   * @return a <code>SocketInitializer</code> instance.
   */
  protected abstract SocketInitializer createSocketInitializer();

  /**
   * Notify all listeners that the status of this connection has changed.
   * @param oldStatus the connection status before the change.
   */
  protected void fireStatusChanged(final JPPFClientConnectionStatus oldStatus)
  {
    ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
    for (ClientConnectionStatusListener listener : listeners) listener.statusChanged(event);
  }

  /**
   * Get the socket client used to communicate over a socket connection.
   * @return a <code>SocketWrapper</code> instance.
   * @throws Exception if any error occurs.
   */
  @Override
  public SocketWrapper getSocketClient() throws Exception
  {
    // If the socket has been idle too long, recycle the connection.
    if ((maxSocketIdleMillis > 10000L)
        && ((System.nanoTime() - socketClient.getSocketTimestamp()) / 1_000_000L > maxSocketIdleMillis))
    {
      close();
      init();
    }
    return socketClient;
  }

  /**
   * Create the ssl connection over an established plain connection.
   * @throws Exception if any error occurs.
   */
  protected void createSSLConnection() throws Exception
  {
    socketClient = SSLHelper.createSSLClientConnection(socketClient);
  }

  @Override
  public void close()
  {
    if (debugEnabled) log.debug("closing " + name);
    listeners.clear();
    try
    {
      if (socketInitializer != null) socketInitializer.close();
      //socketInitializer = null;
      if (socketClient != null) socketClient.close();
      socketClient = null;
    }
    catch (Exception e)
    {
      log.error('[' + name + "] " + e.getMessage(), e);
    }
    if (debugEnabled) log.debug(name  + " closed");
  }

  @Override
  public boolean isClosed()
  {
    return owner.isClosed();
  }
}
