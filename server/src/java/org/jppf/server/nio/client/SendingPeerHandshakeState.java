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

package org.jppf.server.nio.client;

import static org.jppf.server.nio.client.ClientTransition.*;

import java.net.ConnectException;
import java.util.Collections;

import org.jppf.io.*;
import org.jppf.management.*;
import org.jppf.nio.ChannelWrapper;
import org.jppf.node.protocol.*;
import org.jppf.server.nio.nodeserver.PeerAttributesHandler;
import org.jppf.server.protocol.ServerTaskBundleClient;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class represents the state of waiting for some action.
 * @author Laurent Cohen
 */
class SendingPeerHandshakeState extends ClientServerState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SendingPeerHandshakeState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public SendingPeerHandshakeState(final ClientNioServer server) {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public ClientTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    if (channel.isReadable()) {
      throw new ConnectException("client " + channel + " has been disconnected");
    }

    ClientContext context = (ClientContext) channel.getContext();
    if (context.getMessage() == null) {
      TaskBundle header = new JPPFTaskBundle();
      TraversalList<String> uuidPath = new TraversalList<>();
      uuidPath.add(driver.getUuid());
      header.setUuidPath(uuidPath);
      if (debugEnabled) log.debug("sending handshake job, uuidPath=" + uuidPath);
      header.setUuid(new JPPFUuid().toString());
      header.setName("handshake job");
      header.setHandshake(true);
      header.setUuid(header.getName());
      header.setParameter("connection.uuid", context.getConnectionUuid());
      header.setParameter(BundleParameter.IS_PEER, true);
      header.setParameter(BundleParameter.NODE_UUID_PARAM, driver.getUuid());
      JMXServer jmxServer = driver.getInitializer().getJmxServer(context.isSecure());
      header.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, jmxServer.getManagementPort());
      PeerAttributesHandler peerHandler = driver.getNodeNioServer().getPeerHandler();
      JPPFSystemInformation systemInformation = driver.getSystemInformation();
      systemInformation.getJppf().setInt(PeerAttributesHandler.PEER_TOTAL_THREADS, peerHandler.getTotalThreads());
      systemInformation.getJppf().setInt(PeerAttributesHandler.PEER_TOTAL_NODES, peerHandler.getTotalNodes());
      header.setParameter(BundleParameter.SYSTEM_INFO_PARAM, systemInformation);
      header.setSLA(null);
      header.setMetadata(null);
      DataLocation dataProvider = IOHelper.serializeData(null);
      ServerTaskBundleClient bundle = new ServerTaskBundleClient(header, dataProvider, Collections.<DataLocation>emptyList(), true);
      context.setBundle(bundle);
      context.serializeBundle();
    }
    if (context.writeMessage(channel)) {
      ServerTaskBundleClient bundleWrapper = context.getBundle();
      if (debugEnabled) log.debug("sent entire handshake bundle" + bundleWrapper.getJob() + " to peer " + channel);
      context.setBundle(null);
      context.setClientMessage(null);
      return TO_WAITING_PEER_HANDSHAKE_RESULTS;
    }
    return TO_SENDING_PEER_HANDSHAKE;
  }
}
