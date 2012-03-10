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

package org.jppf.server.nio.classloader.client;

import static org.jppf.server.nio.classloader.ClassState.*;
import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.*;

import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;

/**
 * Utility class used to specify the possible states of a class server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
final class ClientClassServerFactory	extends NioServerFactory<ClassState, ClassTransition>
{
  /**
   * Initialize this factory with the specified server.
   * @param server the server for which to initialize.
   */
  public ClientClassServerFactory(final ClassNioServer server)
  {
    super(server);
  }

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioStateInstances.
   * @see org.jppf.server.nio.NioServerFactory#createStateMap()
   */
  @Override
  public Map<ClassState, NioState<ClassTransition>> createStateMap()
  {
    Map<ClassState, NioState<ClassTransition>> map = new EnumMap<ClassState, NioState<ClassTransition>>(ClassState.class);
    map.put(WAITING_INITIAL_PROVIDER_REQUEST, new WaitingProviderInitialRequestState((ClassNioServer) server));
    map.put(SENDING_INITIAL_PROVIDER_RESPONSE, new SendingProviderInitialResponseState((ClassNioServer) server));
    map.put(SENDING_PROVIDER_REQUEST, new SendingProviderRequestState((ClassNioServer) server));
    map.put(WAITING_PROVIDER_RESPONSE, new WaitingProviderResponseState((ClassNioServer) server));
    map.put(IDLE_PROVIDER, new IdleProviderState((ClassNioServer) server));
    map.put(SENDING_PEER_CHANNEL_IDENTIFIER, new SendingPeerChannelIdentifierState((ClassNioServer) server));
    map.put(SENDING_PEER_INITIATION_REQUEST, new SendingPeerInitiationRequestState((ClassNioServer) server));
    map.put(WAITING_PEER_INITIATION_RESPONSE, new WaitingPeerInitiationResponseState((ClassNioServer) server));
    return map;
  }

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioStateInstances.
   * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
   */
  @Override
  public Map<ClassTransition, NioTransition<ClassState>> createTransitionMap()
  {
    Map<ClassTransition, NioTransition<ClassState>> map = new EnumMap<ClassTransition, NioTransition<ClassState>>(ClassTransition.class);
    map.put(TO_WAITING_INITIAL_PROVIDER_REQUEST, transition(WAITING_INITIAL_PROVIDER_REQUEST, R));
    map.put(TO_SENDING_INITIAL_PROVIDER_RESPONSE, transition(SENDING_INITIAL_PROVIDER_RESPONSE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_SENDING_PROVIDER_REQUEST, transition(SENDING_PROVIDER_REQUEST, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAITING_PROVIDER_RESPONSE, transition(WAITING_PROVIDER_RESPONSE, R));
    map.put(TO_IDLE_PROVIDER, transition(IDLE_PROVIDER, NioConstants.CHECK_CONNECTION ? R : 0));
    map.put(TO_IDLE_PEER_PROVIDER, transition(IDLE_PROVIDER, 0));
    map.put(TO_SENDING_PEER_CHANNEL_IDENTIFIER, transition(SENDING_PEER_CHANNEL_IDENTIFIER, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_SENDING_PEER_INITIATION_REQUEST, transition(SENDING_PEER_INITIATION_REQUEST, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAITING_PEER_INITIATION_RESPONSE, transition(WAITING_PEER_INITIATION_RESPONSE, R));
    return map;
  }
}
