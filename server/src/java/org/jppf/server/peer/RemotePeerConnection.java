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

import java.net.InetAddress;

import org.jppf.JPPFException;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.*;
import org.jppf.node.AbstractNodeConnection;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class RemotePeerConnection extends AbstractNodeConnection<SocketWrapper>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RemotePeerConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  private SocketInitializer socketInitializer = new SocketInitializerImpl();
  /**
   * Determines whether SSL is enabled for this connection.
   */
  final boolean secure;
  /**
   * Peer connection information.
   */
  final JPPFConnectionInformation connectionInfo;
  /**
   * 
   */
  String name;
  /**
   * 
   */
  final SerializationHelper helper = new SerializationHelperImpl();

  /**
   * Initialize this connection with the specified serializer.
   * @param name the name given to this connection.
   * @param connectionInfo peer connection information.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public RemotePeerConnection(final String name, final JPPFConnectionInformation connectionInfo, final boolean secure)
  {
    this.connectionInfo = connectionInfo;
    this.secure = secure;
    this.name = name;
  }

  @Override
  public void init() throws Exception
  {
    if (debugEnabled) log.debug(name + " initializing socket client");
    lock.lock();
    try
    {
      boolean mustInit = false;
      if (channel == null)
      {
        mustInit = true;
        initchannel();
      }
      if (mustInit)
      {
        if (debugEnabled) log.debug(name + "initializing socket");
        System.out.println("Connecting to  " + name);
        socketInitializer.initializeSocket(channel);
        if (!socketInitializer.isSuccessful()) throw new JPPFException("Unable to reconnect to " + name);
        System.out.println("Reconnected to " + name);
        if (secure) channel = SSLHelper.createSSLClientConnection(channel);
        if (debugEnabled) log.debug("sending channel identifier");
        channel.writeInt(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL);
      }
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  public void initchannel() throws Exception
  {
    if (debugEnabled) log.debug(name + "initializing socket client");
    String host = connectionInfo.host == null || connectionInfo.host.isEmpty() ? "localhost" : connectionInfo.host;
    host = InetAddress.getByName(host).getHostName();
    int port = secure ? connectionInfo.sslServerPorts[0] : connectionInfo.serverPorts[0];
    channel = new SocketClient();
    channel.setHost(host);
    channel.setPort(port);
    channel.setSerializer(helper.getSerializer());
    name += '@' + host + ':' + port;
  }

  @Override
  public void close() throws Exception
  {
    lock.lock();
    try
    {
    }
    finally
    {
      lock.unlock();
    }
  }
}
