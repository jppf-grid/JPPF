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

import static org.jppf.server.nio.client.ClientState.*;
import static org.jppf.server.nio.client.ClientTransition.*;

import java.util.*;

import org.jppf.nio.*;

/**
 * Utility class used to specify the possible states of a node server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
final class ClientServerFactory extends NioServerFactory<ClientState, ClientTransition> {
  /**
   * Initialize this factory with the specified server.
   * @param server the server for which to initialize.
   */
  public ClientServerFactory(final ClientNioServer server) {
    super(server);
  }

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioState instances.
   */
  @Override
  public Map<ClientState, NioState<ClientTransition>> createStateMap() {
    Map<ClientState, NioState<ClientTransition>> map = new EnumMap<>(ClientState.class);
    map.put(WAITING_HANDSHAKE, new WaitingHandshakeState((ClientNioServer) server));
    map.put(SENDING_HANDSHAKE_RESULTS, new SendingHandshakeResultsState((ClientNioServer) server));
    map.put(WAITING_JOB, new WaitingJobState((ClientNioServer) server));
    map.put(SENDING_RESULTS, new SendingResultsState((ClientNioServer) server));
    map.put(IDLE, new IdleState((ClientNioServer) server));
    map.put(SENDING_PEER_HANDSHAKE, new SendingPeerHandshakeState((ClientNioServer) server));
    map.put(WAITING_PEER_HANDSHAKE_RESULTS, new WaitingPeerHandshakeResultsState((ClientNioServer) server));
    return map;
  }

  /**
   * Create the map of all possible transitions.
   * @return a mapping of the transitions enumeration to the corresponding NioTransition instances.
   */
  @Override
  public Map<ClientTransition, NioTransition<ClientState>> createTransitionMap() {
    Map<ClientTransition, NioTransition<ClientState>> map = new EnumMap<>(ClientTransition.class);
    map.put(TO_WAITING_HANDSHAKE, transition(WAITING_HANDSHAKE, R));
    map.put(TO_SENDING_HANDSHAKE_RESULTS, transition(SENDING_HANDSHAKE_RESULTS, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAITING_JOB, transition(WAITING_JOB, R));
    map.put(TO_SENDING_RESULTS, transition(SENDING_RESULTS, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_IDLE, transition(IDLE, NioConstants.CHECK_CONNECTION ? R : 0));
    map.put(TO_SENDING_PEER_HANDSHAKE, transition(SENDING_PEER_HANDSHAKE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAITING_PEER_HANDSHAKE_RESULTS, transition(WAITING_PEER_HANDSHAKE_RESULTS, R));
    map.put(TO_IDLE_PEER, transition(IDLE, 0));
    return map;
  }
}
