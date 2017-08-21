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
package org.jppf.server.peer;

import java.nio.channels.SocketChannel;

import org.jppf.JPPFRuntimeException;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.nio.ChannelWrapper;
import org.jppf.server.nio.client.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class represent the initializer for the job data channel of a peer driver connection.
 * @author Laurent Cohen
 * @author Domingos Creado
 * @author Martin JANDA
 */
class PeerNode extends AbstractPeerConnectionHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(PeerNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The NioServer to which the channel is registered.
   */
  private final ClientNioServer server;
  /**
   * Context attached to the channel.
   */
  private ClientContext context;

  /**
   * Initialize this peer provider with the specified configuration name.
   * @param peerNameBase the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param server the NioServer to which the channel is registered.
   * @param secure {@code true} if the connection is established over SSL, {@code false} otherwise.
   * @param connectionUuid the connection uuid, common to client class server and job server connections.
   */
  public PeerNode(final String peerNameBase, final JPPFConnectionInformation connectionInfo, final ClientNioServer server, final boolean secure, final String connectionUuid) {
    super(peerNameBase, connectionInfo, secure, connectionUuid, JPPFIdentifiers.NODE_JOB_DATA_CHANNEL);
    this.server = server;
    printConnectionMessage = true;
  }

  @Override
  void postInit() throws Exception {
    try {
      SocketChannel socketChannel = socketClient.getChannel();
      socketClient.setChannel(null);
      ChannelWrapper<?> channel = server.accept(socketChannel, null, secure);
      context = (ClientContext) channel.getContext();
      context.setPeer(true);
      context.setConnectionUuid(connectionUuid);
      context.setOnCloseAction(onCloseAction);
      if (debugEnabled) log.debug("registered peer client channel " + channel);
      if (secure) {
        context.setSsl(true);
        server.configureSSL(channel);
      }
      server.getTransitionManager().transitionChannel(channel, ClientTransition.TO_SENDING_PEER_HANDSHAKE);
      socketClient = null;
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new JPPFRuntimeException(e);
    }
  }

  @Override
  public void close() {
    if (debugEnabled) log.debug("closing {}, context={} ", this, context);
    if (context != null) {
      context.setOnCloseAction(null);
      context.handleException(context.getChannel(), null);
      context = null;
    }
  }
}
