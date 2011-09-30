/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.nio.multiplexer.generic;

import static org.jppf.server.nio.multiplexer.generic.MultiplexerState.*;
import static org.jppf.server.nio.multiplexer.generic.MultiplexerTransition.*;

import java.util.*;

import org.jppf.server.nio.*;

/**
 * Utility class used to specify the possible states of a multiplexer connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
public final class MultiplexerServerFactory
	extends NioServerFactory<MultiplexerState, MultiplexerTransition>
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
	 * @return a mapping of the states enumeration to the corresponding NioState instances.
	 * @see org.jppf.server.nio.NioServerFactory#createStateMap()
	 */
	@Override
    public Map<MultiplexerState, NioState<MultiplexerTransition>> createStateMap()
	{
		MultiplexerNioServer server = (MultiplexerNioServer) this.server;
		Map<MultiplexerState, NioState<MultiplexerTransition>> map =
			new EnumMap<MultiplexerState, NioState<MultiplexerTransition>>(MultiplexerState.class);
		map.put(SENDING_OR_RECEIVING, new SendingOrReceivingState(server));
		map.put(SENDING, new SendingState(server));
		map.put(IDLE, new IdleState(server));
		map.put(RECEIVING, new ReceivingState(server));
		map.put(IDENTIFYING_INBOUND_CHANNEL, new IdentifyingInboundChannelState(server));
		map.put(SENDING_MULTIPLEXING_INFO, new SendingMultiplexingInfoState(server));
		return map;
	}

	/**
	 * Create the map of all possible transitions.
	 * @return a mapping of the transitions enumeration to the corresponding NioTransition instances.
	 * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
	 */
	@Override
    public Map<MultiplexerTransition, NioTransition<MultiplexerState>> createTransitionMap()
	{
		Map<MultiplexerTransition, NioTransition<MultiplexerState>> map =
			new EnumMap<MultiplexerTransition, NioTransition<MultiplexerState>>(MultiplexerTransition.class);
		map.put(TO_SENDING_OR_RECEIVING, transition(SENDING_OR_RECEIVING, R));
		map.put(TO_SENDING, transition(SENDING, RW));
		map.put(TO_RECEIVING, transition(RECEIVING, R));
		map.put(TO_IDENTIFYING_INBOUND_CHANNEL, transition(IDENTIFYING_INBOUND_CHANNEL, R));
		map.put(TO_SENDING_MULTIPLEXING_INFO, transition(SENDING_MULTIPLEXING_INFO, RW));
		map.put(TO_IDLE, transition(IDLE, 0));
		return map;
	}

	/**
	 * Create a transition to the specified state for the specified IO operations.
	 * @param state resulting state of the transition.
	 * @param ops the operations allowed.
	 * @return an <code>NioTransition&lt;ClassState&gt;</code> instance.
	 */
	private static NioTransition<MultiplexerState> transition(MultiplexerState state, int ops)
	{
		return new NioTransition<MultiplexerState>(state, ops);
	}
}
