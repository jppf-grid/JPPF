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

package org.jppf.server.nio.heartbeat;

import static org.jppf.server.nio.heartbeat.HeartbeatState.*;
import static org.jppf.server.nio.heartbeat.HeartbeatTransition.*;

import java.util.*;

import org.jppf.nio.*;

/**
 * Utility class used to specify the possible states of a node server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
class HeartBeatServerFactory extends NioServerFactory<HeartbeatState, HeartbeatTransition> {
  /**
   * Initialize this factory with the specified server.
   * @param server the server for which to initialize.
   */
  public HeartBeatServerFactory(final HeartbeatNioServer server) {
    super(server);
  }

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioState instances.
   */
  @Override
  public Map<HeartbeatState, NioState<HeartbeatTransition>> createStateMap() {
    final Map<HeartbeatState, NioState<HeartbeatTransition>> map = new EnumMap<>(HeartbeatState.class);
    map.put(SEND_INITIAL_MESSAGE, new SendInitialMessageState((HeartbeatNioServer) server));
    map.put(WAIT_INITIAL_RESPONSE, new WaitInitialResponseState((HeartbeatNioServer) server));
    map.put(SEND_MESSAGE, new SendMessageState((HeartbeatNioServer) server));
    map.put(WAIT_RESPONSE, new WaitResponseState((HeartbeatNioServer) server));
    map.put(IDLE, new IdleState((HeartbeatNioServer) server));
    return map;
  }

  /**
   * Create the map of all possible transitions.
   * @return a mapping of the transitions enumeration to the corresponding NioTransition instances.
   */
  @Override
  public Map<HeartbeatTransition, NioTransition<HeartbeatState>> createTransitionMap() {
    final Map<HeartbeatTransition, NioTransition<HeartbeatState>> map = new EnumMap<>(HeartbeatTransition.class);
    map.put(TO_SEND_MESSAGE, transition(SEND_MESSAGE, NioConstants.CHECK_CONNECTION ? RW : W));
    map.put(TO_WAIT_RESPONSE, transition(WAIT_RESPONSE, R));
    map.put(TO_SEND_INITIAL_MESSAGE, transition(SEND_INITIAL_MESSAGE, W));
    map.put(TO_WAIT_INITIAL_RESPONSE, transition(WAIT_INITIAL_RESPONSE, R));
    map.put(TO_IDLE, transition(IDLE, NioConstants.CHECK_CONNECTION ? R : 0));
    map.put(TO_IDLE_PEER, transition(IDLE, 0));
    return map;
  }
}
