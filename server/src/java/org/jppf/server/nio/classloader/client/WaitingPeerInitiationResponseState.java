/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import static org.jppf.server.nio.classloader.client.ClientClassTransition.*;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.nio.ChannelWrapper;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * State of receive a response to the initial request to a peer server. This server is seen as a node by the peer,
 * whereas the peer is seen as a client. Therefore, the information received must allow this server to
 * register a client class loader channel.
 * @author Laurent Cohen
 */
class WaitingPeerInitiationResponseState extends ClientClassServerState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WaitingPeerInitiationResponseState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this state with a specified NioServer.
   * @param server the NioServer this state relates to.
   */
  public WaitingPeerInitiationResponseState(final ClientClassNioServer server) {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   */
  @Override
  public ClientClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    ClientClassContext context = (ClientClassContext) channel.getContext();
    if (context.readMessage(channel)) {
      JPPFResourceWrapper resource = context.deserializeResource();
      String uuid = resource.getProviderUuid();
      if (debugEnabled) log.debug("read initial response from peer " + channel + ", providerUuid=" + uuid);
      context.setUuid(uuid);
      ((ClientClassNioServer) server).addProviderConnection(uuid, channel);
      return context.isPeer() ? TO_IDLE_PEER_PROVIDER : TO_IDLE_PROVIDER;
    }
    return TO_WAITING_PEER_INITIATION_RESPONSE;
  }
}
