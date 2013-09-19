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

package org.jppf.server.nio.classloader.client;

import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.net.*;
import java.nio.channels.*;

import javax.net.ssl.*;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;
import org.jppf.ssl.SSLHelper;
import org.slf4j.*;

/**
 * State of sending an initial request to a peer server. This server is seen as a node by the peer,
 * whereas the peer is seen as a client. Therefore, the information sent must allow the remote peer to
 * register a node class loader channel.
 * @author Laurent Cohen
 */
public class SendingPeerChannelIdentifierState extends ClassServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SendingPeerChannelIdentifierState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state with a specified NioServer.
   * @param server the NioServer this state relates to.
   */
  public SendingPeerChannelIdentifierState(final ClassNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public ClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    ClassContext context = (ClassContext) channel.getContext();
    if (channel.isReadable() && !channel.isLocal())
    {
      throw new ConnectException("provider " + channel + " has been disconnected");
    }
    if (context.writeIdentifier(channel))
    {
      if (debugEnabled) log.debug("sent peer channel identitifer to server {}", channel);
      if (context.isSsl()) configureSSL(channel);
      JPPFResourceWrapper resource = new JPPFResourceWrapper();
      resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
      String uuid = JPPFDriver.getInstance().getUuid();
      resource.setData("node.uuid", uuid);
      resource.setData("peer", Boolean.TRUE);
      resource.setProviderUuid(uuid);
      context.setResource(resource);
      context.serializeResource();
      return TO_SENDING_PEER_INITIATION_REQUEST;
    }
    return TO_SENDING_PEER_CHANNEL_IDENTIFIER;
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
    SSLContext sslContext = server.getSSLContext();
    Socket socket = socketChannel.socket();
    SSLEngine engine = sslContext.createSSLEngine(socket.getInetAddress().getHostAddress(), socket.getPort());
    SSLParameters params = SSLHelper.getSSLParameters();
    engine.setUseClientMode(true);
    engine.setSSLParameters(params);
    SSLHandler sslHandler = new SSLHandler(channel, engine);
    context.setSSLHandler(sslHandler);
  }
}
