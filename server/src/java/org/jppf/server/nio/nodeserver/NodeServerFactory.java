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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeState.*;
import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.util.*;

import org.jppf.nio.*;
import org.jppf.utils.collections.*;

/**
 * Utility class used to specify the possible states of a node server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
final class NodeServerFactory extends NioServerFactory<NodeState, NodeTransition>
{
  /**
   * Initialize this factory with the specified server.
   * @param server the server for which to initialize.
   */
  public NodeServerFactory(final NodeNioServer server)
  {
    super(server);
  }

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioState instances.
   * @see org.jppf.nio.NioServerFactory#createStateMap()
   */
  @Override
  public Map<NodeState, NioState<NodeTransition>> createStateMap()
  {
    Map<NodeState, NioState<NodeTransition>> map = new EnumMap<>(NodeState.class);
    map.put(SEND_INITIAL_BUNDLE, new SendInitialBundleState((NodeNioServer) server));
    map.put(WAIT_INITIAL_BUNDLE, new WaitInitialBundleState((NodeNioServer) server));
    map.put(SENDING_BUNDLE, new SendingBundleState((NodeNioServer) server));
    map.put(WAITING_RESULTS, new WaitingResultsState((NodeNioServer) server));
    map.put(IDLE, new IdleState((NodeNioServer) server));
    return map;
  }

  /**
   * Create the map of all possible transitions.
   * @return a mapping of the transitions enumeration to the corresponding NioTransition instances.
   * @see org.jppf.nio.NioServerFactory#createTransitionMap()
   */
  @Override
  public Map<NodeTransition, NioTransition<NodeState>> createTransitionMap()
  {
    Map<NodeTransition, NioTransition<NodeState>> map = new EnumMap<>(NodeTransition.class);
    map.put(TO_SENDING_BUNDLE, transition(SENDING_BUNDLE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAITING_RESULTS, transition(WAITING_RESULTS, R));
    //map.put(TO_SEND_INITIAL, transition(SEND_INITIAL_BUNDLE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_SEND_INITIAL, transition(SEND_INITIAL_BUNDLE, W));
    map.put(TO_WAIT_INITIAL, transition(WAIT_INITIAL_BUNDLE, R));
    map.put(TO_IDLE, transition(IDLE, NioConstants.CHECK_CONNECTION ? R : 0));
    map.put(TO_IDLE_PEER, transition(IDLE, 0));
    return map;
  }

  @Override
  protected CollectionMap<NodeState, NodeState> createAllowedTransitionsMap()
  {
    CollectionMap<NodeState, NodeState> map = new EnumSetEnumMap<>(NodeState.class);
    map.addValues(SEND_INITIAL_BUNDLE, SEND_INITIAL_BUNDLE, WAIT_INITIAL_BUNDLE);
    map.addValues(WAIT_INITIAL_BUNDLE, WAIT_INITIAL_BUNDLE, IDLE, WAITING_RESULTS);
    map.addValues(IDLE, IDLE, SENDING_BUNDLE);
    map.addValues(SENDING_BUNDLE, SENDING_BUNDLE, WAITING_RESULTS, IDLE);
    map.addValues(WAITING_RESULTS, WAITING_RESULTS, IDLE);
    return map;
  }
}
