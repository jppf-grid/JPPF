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

import java.util.*;

import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Client-side connection for the recovery mechanism.
 * @author Laurent Cohen
 */
public class ClientConnection extends AbstractRecoveryConnection
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Constant for an empty array of <code>ClientConnectionListener</code>.
   */
  private static final ClientConnectionListener[] ZERO_CONNECTION_LISTENER = new ClientConnectionListener[0];
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  private SocketInitializer socketInitializer;
  /**
   * The list of listeners to this object's events.
   */
  private final List<ClientConnectionListener> listeners = new ArrayList<ClientConnectionListener>();

  /**
   * Initialize this client connection with the specified uuid.
   * @param uuid the JPPF node or client uuid.
   */
  public ClientConnection(final String uuid)
  {
    this.uuid = uuid;
  }

  @Override
  public void run()
  {
    runThread = Thread.currentThread();
    try
    {
      configure();
      if (debugEnabled) log.debug("initializing recovery client connection " + socketWrapper);
      socketInitializer = new SocketInitializerImpl();
      socketInitializer.initializeSocket(socketWrapper);
      if (!socketInitializer.isSuccessful())
      {
        log.error("Could not initialize recovery client connection " + socketWrapper);
        close();
        return;
      }
      while (!isStopped())
      {
        String message = receiveMessage(maxRetries, socketReadTimeout);
        if ((message != null) && message.startsWith("handshake")) setInitialized(true);
        String response = "checked;" + uuid;
        sendMessage(response);
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      if (!(e instanceof InterruptedException)) fireClientConnectionEvent();
      close();
    }
    if (debugEnabled) log.debug(Thread.currentThread().getName() + " stopping");
  }

  /**
   * Configure this client connection from the JPPF properties.
   */
  private void configure()
  {
    if (debugEnabled) log.debug("configuring connection");
    TypedProperties config = JPPFConfiguration.getProperties();
    String host = config.getString("jppf.server.host", "localhost");
    int port = config.getInt("jppf.recovery.server.port", 22222);
    maxRetries = config.getInt("jppf.recovery.max.retries", 2);
    socketReadTimeout = config.getInt("jppf.recovery.read.timeout", 60000);
    socketWrapper = new BootstrapSocketClient();
    socketWrapper.setHost(host);
    socketWrapper.setPort(port);
  }

  /**
   * Close this client and release any resources it is using.
   */
  @Override
  public void close()
  {
    setStopped(true);
    if (runThread != null) runThread.interrupt();
    try
    {
      if (debugEnabled) log.debug("closing connection");
      SocketWrapper tmp = socketWrapper;
      socketWrapper = null;
      if (tmp != null) tmp.close();
      if (socketInitializer != null) socketInitializer.close();
      socketInitializer = null;
      synchronized(listeners)
      {
        listeners.clear();
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addClientConnectionListener(final ClientConnectionListener listener)
  {
    if (listener == null) return;
    synchronized (listeners)
    {
      listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeClientConnectionListener(final ClientConnectionListener listener)
  {
    if (listener == null) return;
    synchronized (listeners)
    {
      listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners that an event has occurred.
   */
  private void fireClientConnectionEvent()
  {
    ClientConnectionEvent event = new ClientConnectionEvent(this);
    ClientConnectionListener[] tmp;
    synchronized (listeners)
    {
      tmp = listeners.toArray(new ClientConnectionListener[listeners.size()]);
    }
    for (ClientConnectionListener listener : tmp) listener.clientConnectionFailed(event);
  }
}
