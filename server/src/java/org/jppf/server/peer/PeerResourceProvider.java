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

import java.net.*;
import java.nio.channels.*;
import java.util.Vector;

import javax.net.ssl.*;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.SocketChannelClient;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * This class represents a connection to the class server of a remote JPPF driver (peer driver).
 * @author Laurent Cohen
 */
class PeerResourceProvider extends AbstractSocketChannelHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PeerResourceProvider.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private boolean debugEnabled = log.isDebugEnabled();
  /**
   * The name of the peer in the configuration file.
   */
  private final String peerName;
  /**
   * Peer connection information.
   */
  private final JPPFConnectionInformation connectionInfo;
  /**
   * 
   */
  private boolean sslEnabled = JPPFConfiguration.getProperties().getBoolean("jppf.peer.ssl.enabled", false);

  /**
   * Initialize this peer provider with the specified configuration name.
   * @param peerName the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param server the NioServer to which the channel is registered.
   */
  public PeerResourceProvider(final String peerName, final JPPFConnectionInformation connectionInfo, final ClassNioServer server)
  {
    super(server);
    if (peerName == null || peerName.isEmpty()) throw new IllegalArgumentException("peerName is blank");
    if (connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");

    this.peerName = peerName;
    this.connectionInfo = connectionInfo;
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  @Override
  public synchronized void postInit() throws Exception
  {
    try
    {
      ClassContext context = (ClassContext) server.createNioContext();
      context.setPendingRequests(new Vector<ChannelWrapper<?>>());
      context.setPeer(true);
      SocketChannel socketChannel = socketClient.getChannel();
      socketClient.setChannel(null);
      ChannelWrapper<?> channel = server.getTransitionManager().registerChannel(socketChannel, context);
      if (debugEnabled) log.debug("registered class server channel " + channel);
      if (sslEnabled) configureSSL(channel);
      server.getTransitionManager().transitionChannel(channel, ClassTransition.TO_SENDING_PEER_CHANNEL_IDENTIFIER);
      socketClient = null;
    }
    catch (Exception e)
    {
      log.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Initialize the socket channel client.
   * @return a non-connected <code>SocketChannelClient</code> instance.
   * @throws Exception if an error is raised during initialization.
   */
  @Override
  public SocketChannelClient initSocketChannel() throws Exception
  {
    String host = connectionInfo.host == null || connectionInfo.host.isEmpty() ? "localhost" : connectionInfo.host;
    host = InetAddress.getByName(host).getHostName();
    int port = sslEnabled ? connectionInfo.sslServerPorts[0] : connectionInfo.serverPorts[0];
    return new SocketChannelClient(host, port, false);
  }

  /**
   * Configure the SSL options for the specified channel.
   * @param  channel the channel for which to configure SSL.
   * @throws Exception if any error occurs.
   */
  private void configureSSL(final ChannelWrapper<?> channel) throws Exception
  {
    SocketChannel socketChannel = (SocketChannel) ((SelectionKey) channel.getChannel()).channel();
    ClassContext context = (ClassContext) channel.getContext();
    SSLContext sslContext = JPPFDriver.getInstance().getAcceptorServer().getSSLContext();
    Socket socket = socketChannel.socket();
    SSLEngine engine = sslContext.createSSLEngine(socket.getInetAddress().getHostAddress(), socket.getPort());
    SSLParameters params = SSLHelper.getSSLParameters();
    engine.setUseClientMode(true);
    engine.setSSLParameters(params);
    SSLEngineManager engineManager = new SSLEngineManager(socketChannel, engine);
    context.setSSLEngineManager(engineManager);
  }
}
