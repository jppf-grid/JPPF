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

package org.jppf.server.nio.classloader.node;

import static org.jppf.server.nio.classloader.node.NodeClassState.*;
import static org.jppf.server.nio.classloader.node.NodeClassTransition.*;

import java.util.*;

import org.jppf.nio.*;
import org.jppf.server.nio.classloader.ClassNioServer;
import org.jppf.utils.collections.*;

/**
 * Utility class used to specify the possible states of a class server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
final class NodeClassServerFactory	extends NioServerFactory<NodeClassState, NodeClassTransition>
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
   * @see org.jppf.nio.NioServerFactory#createStateMap()
   */
  @Override
  public Map<NodeClassState, NioState<NodeClassTransition>> createStateMap()
  {
    Map<NodeClassState, NioState<NodeClassTransition>> map = new EnumMap<>(NodeClassState.class);
    map.put(WAITING_INITIAL_NODE_REQUEST, new WaitingInitialNodeRequestState((NodeClassNioServer) server));
    map.put(SENDING_INITIAL_NODE_RESPONSE, new SendingInitialNodeResponseState((NodeClassNioServer) server));
    map.put(SENDING_NODE_RESPONSE, new SendingNodeResponseState((NodeClassNioServer) server));
    map.put(IDLE_NODE, new SendingNodeResponseState((NodeClassNioServer) server));
    map.put(WAITING_NODE_REQUEST, new WaitingNodeRequestState((NodeClassNioServer) server));
    map.put(NODE_WAITING_PROVIDER_RESPONSE, new NodeWaitingProviderResponseState((NodeClassNioServer) server));
    return map;
  }

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioStateInstances.
   * @see org.jppf.nio.NioServerFactory#createTransitionMap()
   */
  @Override
  public Map<NodeClassTransition, NioTransition<NodeClassState>> createTransitionMap()
  {
    Map<NodeClassTransition, NioTransition<NodeClassState>> map = new EnumMap<>(NodeClassTransition.class);
    map.put(TO_WAITING_INITIAL_NODE_REQUEST, transition(WAITING_INITIAL_NODE_REQUEST, R));
    map.put(TO_SENDING_INITIAL_NODE_RESPONSE, transition(SENDING_INITIAL_NODE_RESPONSE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAITING_NODE_REQUEST, transition(WAITING_NODE_REQUEST, R));
    map.put(TO_SENDING_NODE_RESPONSE, transition(SENDING_NODE_RESPONSE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_IDLE_NODE, transition(IDLE_NODE, 0));
    map.put(TO_NODE_WAITING_PROVIDER_RESPONSE, transition(NODE_WAITING_PROVIDER_RESPONSE, RW));
    return map;
  }

  @Override
  protected CollectionMap<NodeClassState, NodeClassState> createAllowedTransitionsMap()
  {
    CollectionMap<NodeClassState, NodeClassState> map = new EnumSetEnumMap<>(NodeClassState.class);
    map.addValues(WAITING_INITIAL_NODE_REQUEST, WAITING_INITIAL_NODE_REQUEST, SENDING_INITIAL_NODE_RESPONSE);
    map.addValues(SENDING_INITIAL_NODE_RESPONSE, SENDING_INITIAL_NODE_RESPONSE, WAITING_NODE_REQUEST);
    map.addValues(WAITING_NODE_REQUEST, WAITING_NODE_REQUEST, IDLE_NODE, SENDING_NODE_RESPONSE);
    map.addValues(IDLE_NODE, NODE_WAITING_PROVIDER_RESPONSE);
    map.addValues(NODE_WAITING_PROVIDER_RESPONSE, IDLE_NODE, SENDING_NODE_RESPONSE);
    map.addValues(SENDING_NODE_RESPONSE, SENDING_NODE_RESPONSE, WAITING_NODE_REQUEST);
    return map;
  }
}
