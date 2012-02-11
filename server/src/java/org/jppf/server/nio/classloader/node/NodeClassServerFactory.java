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

package org.jppf.server.nio.classloader.node;

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
final class NodeClassServerFactory	extends NioServerFactory<ClassState, ClassTransition>
{
  /**
   * Initialize this factory with the specified server.
   * @param server the server for which to initialize.
   */
  public NodeClassServerFactory(final ClassNioServer server)
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
    Map<ClassState, NioState<ClassTransition>> map =
        new EnumMap<ClassState, NioState<ClassTransition>>(ClassState.class);
    map.put(WAITING_INITIAL_NODE_REQUEST, new WaitingInitialNodeRequestState((ClassNioServer) server));
    map.put(SENDING_INITIAL_NODE_RESPONSE, new SendingNodeInitialResponseState((ClassNioServer) server));
    map.put(SENDING_NODE_RESPONSE, new SendingNodeResponseState((ClassNioServer) server));
    map.put(WAITING_NODE_REQUEST, new WaitingNodeRequestState((ClassNioServer) server));
    map.put(NODE_WAITING_PROVIDER_RESPONSE, new NodeWaitingProviderResponseState((ClassNioServer) server));
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
    Map<ClassTransition, NioTransition<ClassState>> map =
        new EnumMap<ClassTransition, NioTransition<ClassState>>(ClassTransition.class);
    map.put(TO_WAITING_INITIAL_NODE_REQUEST, transition(WAITING_INITIAL_NODE_REQUEST, R));
    map.put(TO_SENDING_INITIAL_NODE_RESPONSE, transition(SENDING_INITIAL_NODE_RESPONSE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAITING_NODE_REQUEST, transition(WAITING_NODE_REQUEST, R));
    map.put(TO_SENDING_NODE_RESPONSE, transition(SENDING_NODE_RESPONSE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_IDLE_NODE, transition(SENDING_NODE_RESPONSE, 0));
    map.put(TO_NODE_WAITING_PROVIDER_RESPONSE, transition(NODE_WAITING_PROVIDER_RESPONSE, 0));
    return map;
  }
}
