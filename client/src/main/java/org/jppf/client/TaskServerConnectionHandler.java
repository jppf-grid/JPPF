/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.JPPFException;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.comm.socket.SocketClient;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class handle the sending and receiving of jobs to and from a JPPF driver.
 * @author Laurent Cohen
 * @exclude
 */
public class TaskServerConnectionHandler extends AbstractClientConnectionHandler {
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
  TaskServerConnectionHandler(final JPPFClientConnection owner, final String host, final int port) {
    super(owner, owner.getName() + " - TasksServer");
    this.host = host;
    this.port = port;
  }

  /**
   * Initialize the connection.
   * @throws Exception if an error is raised while initializing the connection.
   */
  @Override
  public void init() throws Exception {
    boolean done = false;
    while (!done && !isClosed()) {
      if (socketClient == null) initSocketClient();
      final boolean sysoutEnabled = owner.getConnectionPool().getClient().isSysoutEnabled();
      String msg = String.format("[client: %s] Attempting connection to the task server at %s:%d", name, host, port);
      if (sysoutEnabled) System.out.println(msg);
      log.info(msg);
      if (!socketInitializer.initialize(socketClient)) throw new JPPFException(String.format("[%s] Could not reconnect to the JPPF task server", name));
      if (!InterceptorHandler.invokeOnConnect(socketClient, JPPFChannelDescriptor.CLIENT_JOB_DATA_CHANNEL))
        throw new JPPFException(String.format("[%s] Could not reconnect to the JPPF task server due to interceptor failure", name));
      try {
        if (debugEnabled) log.debug("sending JPPF identifier {}", JPPFIdentifiers.asString(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL));
        socketClient.writeInt(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL);
        if (owner.isSSLEnabled()) createSSLConnection();
        ((JPPFClientConnectionImpl) owner).sendHandshakeJob();
        owner.getConnectionPool().setJmxPort(owner.getConnectionPool().getDriverPort());
        msg = "[client: " + name + "] Reconnected to the JPPF task server";
        if (sysoutEnabled) System.out.println(msg);
        log.info(msg);
        done = true;
      } catch (final Exception e) {
        final String format = "error initializing connection to job server: {}";
        if (debugEnabled) log.debug(format, ExceptionUtils.getStackTrace(e));
        else log.warn(format, ExceptionUtils.getMessage(e));
      }
    }
  }

  /**
   * Initialize the underlying socket connection of this connection handler.
   * @throws Exception if an error is raised during initialization.
   */
  @Override
  public void initSocketClient() throws Exception {
    socketClient = new SocketClient();
    socketClient.setHost(host);
    socketClient.setPort(port);
  }
}
