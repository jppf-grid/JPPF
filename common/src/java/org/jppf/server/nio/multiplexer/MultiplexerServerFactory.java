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

package org.jppf.server.nio.multiplexer;

import static org.jppf.server.nio.multiplexer.MultiplexerState.*;
import static org.jppf.server.nio.multiplexer.MultiplexerTransition.*;

import java.util.*;

import org.jppf.server.nio.*;

/**
 * Utility class used to specify the possible states of a node server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
public final class MultiplexerServerFactory
	extends NioServerFactory<MultiplexerState, MultiplexerTransition, MultiplexerNioServer>
{
	/**
	 * Initialize this factory with the specified server.
	 * @param server the server for which to initialize.
	 */
	public MultiplexerServerFactory(MultiplexerNioServer server)
	{
		super(server);
	}

	/**
	 * Create the map of all possible states.
	 * @param server the server to which the states refer.
	 * @return a mapping of the states enumeration to the corresponding NioState instances.
	 * @see org.jppf.server.nio.NioServerFactory#createStateMap(org.jppf.server.nio.NioServer)
	 */
	public Map<MultiplexerState, NioState<MultiplexerTransition>> createStateMap(MultiplexerNioServer server)
	{
		Map<MultiplexerState, NioState<MultiplexerTransition>> map =
			new EnumMap<MultiplexerState, NioState<MultiplexerTransition>>(MultiplexerState.class);
		map.put(SENDING, new SendingState(server));
		map.put(RECEIVING, new ReceivingState(server));
		return map;
	}

	/**
	 * Create the map of all possible transitions.
	 * @return a mapping of the transitions enumeration to the corresponding NioTransition instances.
	 * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
	 */
	public Map<MultiplexerTransition, NioTransition<MultiplexerState>> createTransitionMap()
	{
		Map<MultiplexerTransition, NioTransition<MultiplexerState>> map =
			new EnumMap<MultiplexerTransition, NioTransition<MultiplexerState>>(MultiplexerTransition.class);
		map.put(TO_SENDING, transition(SENDING, RW));
		map.put(TO_RECEIVING, transition(RECEIVING, R));
		map.put(TO_IDLE, transition(SENDING, R));
		return map;
	}


	/**
	 * Create a transition to the specified state for the specified IO operations.
	 * @param state resulting state of the transition.
	 * @param ops the operations allowed.
	 * @return an <code>NioTransition&lt;ClassState&gt;</code> instance.
	 */
	private NioTransition<MultiplexerState> transition(MultiplexerState state, int ops)
	{
		return new NioTransition<MultiplexerState>(state, ops);
	}
}
