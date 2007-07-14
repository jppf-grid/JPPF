/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
 * 
 * @author Laurent Cohen
 */
public final class NodeServerFactory extends NioServerFactory<NodeState, NodeTransition, NodeNioServer>
{
	/**
	 * Initialize this factory with the specified server.
	 * @param server the server for which to initialize.
	 */
	public NodeServerFactory(NodeNioServer server)
	{
		super(server);
	}

	/**
	 * Create the map of all possible states.
	 * @param server the server to which the states refer.
	 * @return a mapping of the states enumeration to the corresponding NioState instances.
	 * @see org.jppf.server.nio.NioServerFactory#createStateMap(org.jppf.server.nio.NioServer)
	 */
	public Map<NodeState, NioState<NodeTransition>> createStateMap(NodeNioServer server)
	{
		Map<NodeState, NioState<NodeTransition>> map = new EnumMap<NodeState, NioState<NodeTransition>>(NodeState.class);
		map.put(SEND_INITIAL_BUNDLE, new SendInitialBundleState(server));
		map.put(WAIT_INITIAL_BUNDLE, new WaitInitialBundleState(server));
		map.put(SENDING_BUNDLE, new SendingBundleState(server));
		map.put(WAITING_RESULTS, new WaitingResultsState(server));
		return map;
	}

	/**
	 * Create the map of all possible transitions.
	 * @return a mapping of the transitions enumeration to the corresponding NioTransition instances.
	 * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
	 */
	public Map<NodeTransition, NioTransition<NodeState>> createTransitionMap()
	{
		Map<NodeTransition, NioTransition<NodeState>> map =
			new EnumMap<NodeTransition, NioTransition<NodeState>>(NodeTransition.class);
		map.put(TO_SENDING, new NioTransition<NodeState>(SENDING_BUNDLE, RW));
		map.put(TO_WAITING, new NioTransition<NodeState>(WAITING_RESULTS, R));
		map.put(TO_SEND_INITIAL, new NioTransition<NodeState>(SEND_INITIAL_BUNDLE, RW));
		map.put(TO_WAIT_INITIAL, new NioTransition<NodeState>(WAIT_INITIAL_BUNDLE, R));
		map.put(TO_IDLE, new NioTransition<NodeState>(SENDING_BUNDLE, 0));
		return map;
	}
}
