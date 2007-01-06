/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 * @see org.jppf.server.nio.NioServerFactory#createStateMap(org.jppf.server.nio.NioServer)
	 */
	public Map<NodeState, NioState<NodeTransition>> createStateMap(NodeNioServer server)
	{
		Map<NodeState, NioState<NodeTransition>> map = new EnumMap<NodeState, NioState<NodeTransition>>(NodeState.class);
		map.put(SEND_INITIAL_BUNDLE, new SendInitialBundleState(server));
		map.put(WAIT_INITIAL_BUNDLE, new WaitInitialBundleSate(server));
		map.put(SENDING_BUNDLE, new SendingBundleState(server));
		map.put(WAITING_RESULTS, new WaitingResultsState(server));
		return map;
	}

	/**
	 * Create the map of all possible states.
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
	 */
	public Map<NodeTransition, NioTransition<NodeState>> createTransitionMap()
	{
		Map<NodeTransition, NioTransition<NodeState>> map =
			new EnumMap<NodeTransition, NioTransition<NodeState>>(NodeTransition.class);
		map.put(TRANSITION_TO_SENDING, new NioTransition<NodeState>(SENDING_BUNDLE, RW));
		map.put(TRANSITION_TO_WAITING, new NioTransition<NodeState>(WAITING_RESULTS, R));
		map.put(TRANSITION_TO_SEND_INITIAL, new NioTransition<NodeState>(SEND_INITIAL_BUNDLE, RW));
		map.put(TRANSITION_TO_WAIT_INITIAL, new NioTransition<NodeState>(WAIT_INITIAL_BUNDLE, R));
		map.put(TRANSITION_TO_IDLE, new NioTransition<NodeState>(SENDING_BUNDLE, 0));
		return map;
	}
}
