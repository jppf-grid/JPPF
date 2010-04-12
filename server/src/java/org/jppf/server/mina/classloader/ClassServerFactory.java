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

package org.jppf.server.mina.classloader;

import static org.jppf.server.nio.classloader.ClassState.*;
import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.*;

import org.jppf.server.mina.*;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;

/**
 * Utility class used to specify the possible states of a class server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
final class ClassServerFactory	extends MinaServerFactory<ClassState, ClassTransition>
{
	/**
	 * Initialize this factory with the specified server.
	 * @param server the server for which to initialize.
	 */
	public ClassServerFactory(MinaClassServer server)
	{
		super(server);
	}

	/**
	 * Create the map of all possible states.
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 * @see org.jppf.server.nio.NioServerFactory#createStateMap()
	 */
	public Map<ClassState, MinaState> createStateMap()
	{
		MinaClassServer server = (MinaClassServer) this.server;
		Map<ClassState, MinaState> map =
			new EnumMap<ClassState, MinaState>(ClassState.class);
		map.put(DEFINING_TYPE, new DefiningChannelTypeState(server));
		map.put(SENDING_INITIAL_PROVIDER_RESPONSE, new SendingProviderInitialResponseState(server));
		map.put(SENDING_INITIAL_NODE_RESPONSE, new SendingInitialResponseState(server));
		map.put(SENDING_NODE_RESPONSE, new SendingNodeResponseState(server));
		map.put(SENDING_PROVIDER_REQUEST, new SendingProviderRequestState(server));
		map.put(WAITING_NODE_REQUEST, new WaitingNodeRequestState(server));
		map.put(WAITING_PROVIDER_RESPONSE, new WaitingProviderResponseState(server));
		map.put(IDLE_PROVIDER, new IdleProviderState(server));
		map.put(IDLE_NODE, new IdleNodeState(server));
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
		map.put(TO_IDLE_NODE, transition(IDLE_NODE, NONE));
		map.put(TO_IDLE_PROVIDER, transition(IDLE_PROVIDER, NONE));
		return map;
	}
}
