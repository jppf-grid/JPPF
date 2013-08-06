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

package org.jppf.server.node.remote;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.comm.socket.*;
import org.jppf.node.AbstractNodeConnection;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class encapsulates the network connection of a node to a server job channel.
 * @author Laurent Cohen
 * @exclude
 */
public class RemoteNodeConnection extends AbstractNodeConnection<SocketWrapper>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RemoteNodeConnection.class);
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
  private boolean sslEnabled = false;
  /**
   * 
   */
  private final ObjectSerializer serializer;

  /**
   * Initialize this connection with the specified serializer.
   * @param serializer the serializer to use.
   */
  public RemoteNodeConnection(final ObjectSerializer serializer)
  {
    this.serializer = serializer;
  }

  @Override
  public void init() throws Exception
  {
    lock.lock();
    try
    {
      if (debugEnabled) log.debug("Initializing socket");
      TypedProperties config = JPPFConfiguration.getProperties();
      sslEnabled = config.getBoolean("jppf.ssl.enabled", false);
      String host = config.getString("jppf.server.host", "localhost");
      int port = config.getInt("jppf.server.port", sslEnabled ? 11111 : 11143);
      channel = new SocketClient();
      channel.setHost(host);
      channel.setPort(port);
      channel.setSerializer(serializer);
      if (debugEnabled) log.debug("end socket client initialization");
      System.out.println("Attempting connection to the node server at " + host + ':' + port);
      socketInitializer.initializeSocket(channel);
      if (!socketInitializer.isSuccessful())
      {
        if (debugEnabled) log.debug("socket initializer failed");
        throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver");
      }
      if (sslEnabled) channel = SSLHelper.createSSLClientConnection(channel);
      System.out.println("Reconnected to the node server");
      if (debugEnabled) log.debug("sending channel identifier");
      channel.writeInt(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL);
      if (debugEnabled) log.debug("end socket initializer");
    }
    finally
    {
      lock.unlock();
    }
  }

  @Override
  public void close() throws Exception
  {
    lock.lock();
    try
    {
      if (channel != null)
      {
        SocketWrapper tmp = channel;
        channel = null;
        tmp.close();
      }
    }
    finally
    {
      lock.unlock();
    }
  }
}
