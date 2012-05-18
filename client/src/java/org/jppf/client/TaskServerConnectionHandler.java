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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import org.jppf.JPPFException;
import org.jppf.comm.socket.*;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.JPPFIdentifiers;
import org.slf4j.*;

/**
 * Instances of this class handle the sending and receiving of jobs to and from a JPPF driver.
 * @author Laurent Cohen
 * @exclude
 */
public class TaskServerConnectionHandler extends AbstractClientConnectionHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TaskServerConnectionHandler.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this connection with the specified owner.
   * @param owner the client connection which owns this connection handler.
   * @param host the host to connect to.
   * @param port the port to connect to on the host.
   */
  public TaskServerConnectionHandler(final JPPFClientConnection owner, final String host, final int port)
  {
    super(owner);
    this.host = host;
    this.port = port;
  }

  /**
   * Create a socket initializer for this connection handler.
   * @return a <code>SocketInitializer</code> instance.
   * @see org.jppf.client.AbstractClientConnectionHandler#createSocketInitializer()
   */
  @Override
  protected SocketInitializer createSocketInitializer()
  {
    return new SocketInitializerImpl();
  }

  /**
   * Initialize the connection.
   * @throws Exception if an error is raised while initializing the connection.
   */
  @Override
  public void init() throws Exception
  {
    try
    {
      setStatus(CONNECTING);
      if (socketClient == null) initSocketClient();
      String msg = "[client: " + name + "] Attempting connection to the JPPF task server at " + host + ':' + port;
      System.out.println(msg);
      if (debugEnabled) log.debug(msg);
      socketInitializer.initializeSocket(socketClient);
      if (!socketInitializer.isSuccessful())
      {
        throw new JPPFException('[' + name + "] Could not reconnect to the JPPF task server");
      }
      if (owner.isSSL()) createSSLConnection();
      if (debugEnabled) log.debug("sending JPPF identifier");
      socketClient.writeInt(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL);
      // TODO do something with the information sent by the driver in the bundle 
      JPPFTaskBundle bundle = ((AbstractJPPFClientConnection) owner).sendHandshakeJob();
      msg = "[client: " + name + "] Reconnected to the JPPF task server";
      System.out.println(msg);
      if (debugEnabled) log.debug(msg);
      setStatus(ACTIVE);
    }
    catch (Exception e)
    {
      setStatus(FAILED);
      throw e;
    }
  }

  /**
   * Initialize the underlying socket connection of this connection handler.
   * @throws Exception if an error is raised during initialization.
   * @see org.jppf.client.AbstractClientConnectionHandler#initSocketClient()
   */
  @Override
  public void initSocketClient() throws Exception
  {
    socketClient = new SocketClient();
    socketClient.setHost(host);
    socketClient.setPort(port);
  }
}
