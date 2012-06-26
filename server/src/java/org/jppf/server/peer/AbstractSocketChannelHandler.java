/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package org.jppf.server.peer;

import java.net.ConnectException;

import org.jppf.comm.socket.*;
import org.jppf.server.nio.classloader.ClassNioServer;
import org.slf4j.*;

/**
 * Instances of this class act as wrapper for a connection to a JPPF component.<br>
 * They handle (re)connection services when needed.
 * @author Laurent Cohen
 */
public abstract class AbstractSocketChannelHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractSocketChannelHandler.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private boolean debugEnabled = log.isDebugEnabled();
  /**
   * The NioServer to which the channel is registered.
   */
  protected ClassNioServer server = null;
  /**
   * Wrapper around the underlying socket connection.
   */
  protected SocketChannelClient socketClient = null;
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  protected SocketInitializer socketInitializer = new SocketInitializerImpl();

  /**
   * Initialize this socket channel handler.
   * @param server the NioServer to which the channel is registered.
   */
  public AbstractSocketChannelHandler(final ClassNioServer server)
  {
    this.server = server;
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  public synchronized void init() throws Exception
  {
    if (socketClient == null) socketClient = initSocketChannel();
    String msg =  "to remote peer [" + socketClient.getHost() + ':' + socketClient.getPort() + ']';
    if (debugEnabled) log.debug("Attempting connection " + msg);
    socketInitializer.initializeSocket(socketClient);
    if (!socketInitializer.isSuccessful()) throw new ConnectException("could not connect " + msg);
    if (debugEnabled) log.debug("Connected " + msg);
    postInit();
  }

  /**
   * This method is called after the channel is successfully connected.
   * @throws Exception if an error is raised while performing the initialization.
   */
  protected abstract void postInit() throws Exception;

  /**
   * Initialize the channel client.
   * @return a non-connected <code>SocketChannelClient</code> instance.
   * @throws Exception if an error is raised during initialization.
   */
  protected abstract SocketChannelClient initSocketChannel() throws Exception;

  /**
   * Get the wrapper around the underlying socket connection.
   * @return a <code>SocketChannelClient</code> instance.
   */
  public SocketChannelClient getSocketClient()
  {
    return socketClient;
  }
}
