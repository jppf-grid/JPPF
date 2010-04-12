/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassState.*;
import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.*;

import org.jppf.server.nio.*;

/**
 * Utility class used to specify the possible states of a class server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
final class ClassServerFactory	extends NioServerFactory<ClassState, ClassTransition>
{
	/**
	 * Initialize this factory with the specified server.
	 * @param server the server for which to initialize.
	 */
	public ClassServerFactory(ClassNioServer server)
	{
		super(server);
	}

	/**
	 * Create the map of all possible states.
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 * @see org.jppf.server.nio.NioServerFactory#createStateMap()
	 */
	public Map<ClassState, NioState<ClassTransition>> createStateMap()
	{
		Map<ClassState, NioState<ClassTransition>> map =
			new EnumMap<ClassState, NioState<ClassTransition>>(ClassState.class);
		map.put(DEFINING_TYPE, new DefiningChannelTypeState((ClassNioServer) server));
		map.put(SENDING_INITIAL_PROVIDER_RESPONSE, new SendingProviderInitialResponseState((ClassNioServer) server));
		map.put(SENDING_INITIAL_NODE_RESPONSE, new SendingNodeInitialResponseState((ClassNioServer) server));
		map.put(SENDING_NODE_RESPONSE, new SendingNodeResponseState((ClassNioServer) server));
		map.put(SENDING_PROVIDER_REQUEST, new SendingProviderRequestState((ClassNioServer) server));
		map.put(WAITING_NODE_REQUEST, new WaitingNodeRequestState((ClassNioServer) server));
		map.put(WAITING_PROVIDER_RESPONSE, new WaitingProviderResponseState((ClassNioServer) server));
		map.put(IDLE_PROVIDER, new IdleProviderState((ClassNioServer) server));
		return map;
	}

	/**
	 * Create the map of all possible states.
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
	 */
	public Map<ClassTransition, NioTransition<ClassState>> createTransitionMap()
	{
		Map<ClassTransition, NioTransition<ClassState>> map =
			new EnumMap<ClassTransition, NioTransition<ClassState>>(ClassTransition.class);
		map.put(TO_DEFINING_TYPE, transition(DEFINING_TYPE, R));
		map.put(TO_SENDING_INITIAL_PROVIDER_RESPONSE, transition(SENDING_INITIAL_PROVIDER_RESPONSE, RW));
		map.put(TO_SENDING_INITIAL_NODE_RESPONSE, transition(SENDING_INITIAL_NODE_RESPONSE, RW));
		map.put(TO_WAITING_NODE_REQUEST, transition(WAITING_NODE_REQUEST, R));
		map.put(TO_SENDING_NODE_RESPONSE, transition(SENDING_NODE_RESPONSE, RW));
		map.put(TO_SENDING_PROVIDER_REQUEST, transition(SENDING_PROVIDER_REQUEST, RW));
		map.put(TO_WAITING_PROVIDER_RESPONSE, transition(WAITING_PROVIDER_RESPONSE, R));
		map.put(TO_IDLE_NODE, transition(SENDING_NODE_RESPONSE, 0));
		map.put(TO_IDLE_PROVIDER, transition(IDLE_PROVIDER, R));
		return map;
	}

	/**
	 * Create a transition to the specified state for the specified IO operations.
	 * @param state resulting state of the transition.
	 * @param ops the operations allowed.
	 * @return an <code>NioTransition&lt;ClassState&gt;</code> instance.
	 */
	private NioTransition<ClassState> transition(ClassState state, int ops)
	{
		return new NioTransition<ClassState>(state, ops);
	}
}
