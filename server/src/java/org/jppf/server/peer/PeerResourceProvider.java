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
import org.jppf.classloader.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.nio.ChannelWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.client.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class represents an initializer for the connection to the class server of a remote JPPF driver (peer driver).
 * @author Laurent Cohen
 * @author Martin JANDA
 */
class PeerResourceProvider extends AbstractPeerConnectionHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(PeerResourceProvider.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The NioServer to which the channel is registered.
   */
  private final ClientClassNioServer server;
  /**
   * Context attached to the channel.
   */
  private ClientClassContext context;

  /**
   * Initialize this peer provider with the specified configuration name.
   * @param peerNameBase the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param server the NioServer to which the channel is registered.
   * @param secure {@code true} if the connection is established over SSL, {@code false} otherwise.
   * @param connectionUuid the connection uuid, common to client class server and job server connections.
   */
  public PeerResourceProvider(final String peerNameBase, final JPPFConnectionInformation connectionInfo, final ClientClassNioServer server, final boolean secure, final String connectionUuid) {
    super(peerNameBase, connectionInfo, secure, connectionUuid, JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL);
    this.server = server;
  }

  @Override
  void postInit() throws Exception {
    try {
      SocketChannel socketChannel = socketClient.getChannel();
      socketClient.setChannel(null);
      ChannelWrapper<?> channel = server.accept(null, socketChannel, null, secure, true);
      context = (ClientClassContext) channel.getContext();
      context.setPeer(true);
      context.setConnectionUuid(connectionUuid);
      if (debugEnabled) log.debug("registered class server channel " + channel);
      if (secure) {
        context.setSsl(true);
        server.configureSSL(channel);
      }
      JPPFResourceWrapper resource = new JPPFResourceWrapper();
      resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
      String uuid = JPPFDriver.getInstance().getUuid();
      context.setConnectionUuid(connectionUuid);
      resource.setData(ResourceIdentifier.NODE_UUID, uuid);
      resource.setData(ResourceIdentifier.PEER, Boolean.TRUE);
      resource.setProviderUuid(uuid);
      context.setResource(resource);
      context.serializeResource();
      server.getTransitionManager().transitionChannel(channel, ClientClassTransition.TO_SENDING_PEER_INITIATION_REQUEST);
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
