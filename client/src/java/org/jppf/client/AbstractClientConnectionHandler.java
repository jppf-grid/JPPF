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

package org.jppf.client;

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
public abstract class AbstractClientConnectionHandler implements ClientConnectionHandler {
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
  SocketInitializer socketInitializer;
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
   * Initialize this connection with the specified owner.
   * @param owner the client connection which owns this connection handler.
   * @param name the name given to this connection.
   */
  protected AbstractClientConnectionHandler(final JPPFClientConnection owner, final String name) {
    this.owner = owner;
    this.name = name;
    //long configSocketIdle = JPPFConfiguration.get(JPPFProperties.SOCKET_MAX_IDLE);
    final TypedProperties config = owner.getConnectionPool().getClient().getConfig();
    final long configSocketIdle = config.get(JPPFProperties.SOCKET_MAX_IDLE);
    maxSocketIdleMillis = (configSocketIdle > 10L) ? configSocketIdle * 1000L : -1L;
    socketInitializer = new SocketInitializerImpl(config);
  }

  /**
   * Get the socket client used to communicate over a socket connection.
   * @return a <code>SocketWrapper</code> instance.
   * @throws Exception if any error occurs.
   */
  @Override
  public SocketWrapper getSocketClient() throws Exception {
    // If the socket has been idle too long, recycle the connection.
    if ((maxSocketIdleMillis > 10000L)
        && ((System.nanoTime() - socketClient.getSocketTimestamp()) / 1_000_000L > maxSocketIdleMillis)) {
      close();
      init();
    }
    return socketClient;
  }

  /**
   * Create the ssl connection over an established plain connection.
   * @throws Exception if any error occurs.
   */
  protected void createSSLConnection() throws Exception {
    socketClient = SSLHelper.createSSLClientConnection(socketClient);
  }

  @Override
  public void close() {
    if (debugEnabled) log.debug("closing " + name);
    try {
      if (socketInitializer != null) socketInitializer.close();
      //socketInitializer = null;
      if (socketClient != null) socketClient.close();
      socketClient = null;
    } catch (final Exception e) {
      log.error('[' + name + "] " + e.getMessage(), e);
    }
    if (debugEnabled) log.debug(name  + " closed");
  }

  @Override
  public boolean isClosed() {
    return owner.isClosed();
  }

  /**
   * Get the object that performs connection attempts until the max retry time is reached or connection succeeds.
   * @return a {@link SocketInitializer} instance.
   */
  public SocketInitializer getSocketInitializer() {
    return socketInitializer;
  }
}
