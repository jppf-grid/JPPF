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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassState.*;
import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.nio.channels.SelectionKey;
import java.util.*;

import org.jppf.server.nio.*;

/**
 * Utility class used to specify the possible states of a class server connection, as well as the possible
 * transitions between those states.
 * @author Laurent Cohen
 */
public final class ClassServerFactory
	extends NioServerFactory<ClassState, ClassTransition, ClassNioServer>
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
	 * @param server the server to which the states refer.
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 * @see org.jppf.server.nio.NioServerFactory#createStateMap(org.jppf.server.nio.NioServer)
	 */
	public Map<ClassState, NioState<ClassTransition>> createStateMap(ClassNioServer server)
	{
		Map<ClassState, NioState<ClassTransition>> map =
			new EnumMap<ClassState, NioState<ClassTransition>>(ClassState.class);
		map.put(DEFINING_TYPE, new DefiningChannelTypeState(server));
		map.put(SENDING_INITIAL_PROVIDER_RESPONSE, new SendingProviderInitialResponseState(server));
		map.put(SENDING_INITIAL_RESPONSE, new SendingInitialResponseState(server));
		map.put(SENDING_NODE_RESPONSE, new SendingNodeResponseState(server));
		map.put(SENDING_PROVIDER_REQUEST, new SendingProviderRequestState(server));
		map.put(WAITING_NODE_REQUEST, new WaitingNodeRequestState(server));
		map.put(WAITING_PROVIDER_RESPONSE, new WaitingProviderResponseState(server));
		map.put(IDLE_PROVIDER, new ClassServerState(server)
		{
			public ClassTransition performTransition(SelectionKey key) throws Exception
			{
				return TO_IDLE_PROVIDER;
			}
		});
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
		map.put(TO_DEFINING_TYPE, new NioTransition<ClassState>(DEFINING_TYPE, R));
		map.put(TO_SENDING_INITIAL_PROVIDER_RESPONSE, new NioTransition<ClassState>(SENDING_INITIAL_PROVIDER_RESPONSE, RW));
		map.put(TO_SENDING_INITIAL_RESPONSE, new NioTransition<ClassState>(SENDING_INITIAL_RESPONSE, RW));
		map.put(TO_WAITING_NODE_REQUEST, new NioTransition<ClassState>(WAITING_NODE_REQUEST, R));
		map.put(TO_SENDING_NODE_RESPONSE, new NioTransition<ClassState>(SENDING_NODE_RESPONSE, RW));
		map.put(TO_SENDING_PROVIDER_REQUEST, new NioTransition<ClassState>(SENDING_PROVIDER_REQUEST, RW));
		map.put(TO_WAITING_PROVIDER_RESPONSE, new NioTransition<ClassState>(WAITING_PROVIDER_RESPONSE, R));
		map.put(TO_IDLE_NODE, new NioTransition<ClassState>(SENDING_NODE_RESPONSE, 0));
		//map.put(TO_IDLE_PROVIDER, new NioTransition<ClassState>(SENDING_PROVIDER_REQUEST, 0));
		map.put(TO_IDLE_PROVIDER, new NioTransition<ClassState>(IDLE_PROVIDER, 0));
		return map;
	}
}
