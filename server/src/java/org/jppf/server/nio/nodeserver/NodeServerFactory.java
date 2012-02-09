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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeState.*;
import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.util.*;

import org.jppf.server.nio.*;

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
   * @see org.jppf.server.nio.NioServerFactory#createStateMap()
   */
  @Override
  public Map<NodeState, NioState<NodeTransition>> createStateMap()
  {
    Map<NodeState, NioState<NodeTransition>> map = new EnumMap<NodeState, NioState<NodeTransition>>(NodeState.class);
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
   * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
   */
  @Override
  public Map<NodeTransition, NioTransition<NodeState>> createTransitionMap()
  {
    Map<NodeTransition, NioTransition<NodeState>> map =
      new EnumMap<NodeTransition, NioTransition<NodeState>>(NodeTransition.class);
    map.put(TO_SENDING, transition(SENDING_BUNDLE, NioChecks.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAITING, transition(WAITING_RESULTS, R));
    map.put(TO_SEND_INITIAL, transition(SEND_INITIAL_BUNDLE, NioChecks.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAIT_INITIAL, transition(WAIT_INITIAL_BUNDLE, R));
    map.put(TO_IDLE, transition(IDLE, NioChecks.CHECK_CONNECTION ? R : 0));
    //map.put(TO_IDLE, transition(SENDING_BUNDLE, R));
    return map;
  }
}
