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

package org.jppf.jmxremote.nio;

import static org.jppf.jmxremote.nio.JMXState.*;
import static org.jppf.jmxremote.nio.JMXTransition.*;

import java.util.*;

import org.jppf.nio.*;

/**
 * Utility class used to specify the possible states of a node server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
final class JMXNioServerFactory extends NioServerFactory<JMXState, JMXTransition> {
  /**
   * Initialize this factory with the specified server.
   * @param server the server for which to initialize.
   */
  public JMXNioServerFactory(final JMXNioServer server) {
    super(server);
  }

  /**
   * Create the map of all possible states.
   * @return a mapping of the states enumeration to the corresponding NioState instances.
   */
  @Override
  public Map<JMXState, NioState<JMXTransition>> createStateMap() {
    Map<JMXState, NioState<JMXTransition>> map = new EnumMap<>(JMXState.class);
    map.put(SENDING_MESSAGE, new SendingMessageState((JMXNioServer) server));
    map.put(RECEIVING_MESSAGE, new ReceivingMessageState((JMXNioServer) server));
    map.put(IDLE, new IdleState((JMXNioServer) server));
    return map;
  }

  /**
   * Create the map of all possible transitions.
   * @return a mapping of the transitions enumeration to the corresponding NioTransition instances.
   */
  @Override
  public Map<JMXTransition, NioTransition<JMXState>> createTransitionMap() {
    Map<JMXTransition, NioTransition<JMXState>> map = new EnumMap<>(JMXTransition.class);
    map.put(TO_SENDING_MESSAGE, transition(SENDING_MESSAGE, RW));
    map.put(TO_RECEIVING_MESSAGE, transition(RECEIVING_MESSAGE, RW));
    map.put(TO_IDLE, transition(IDLE, R));
    return map;
  }
}
