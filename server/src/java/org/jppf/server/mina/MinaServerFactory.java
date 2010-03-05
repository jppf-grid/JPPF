/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.server.mina;

import java.util.Map;

import org.jppf.server.nio.NioTransition;

/**
 * Utility class used to specify the possible states of a node server connection, as well as the possible
 * transitions between those states.
 * @param <S> the type of the states to use.
 * @param <T> the type of the transitions to use.
 * @author Laurent Cohen
 */
public abstract class MinaServerFactory<S extends Enum<S>, T extends Enum<T>>
{
	/**
	 * A short name for no channel operations.
	 */
	public static final int NONE = 0;
	/**
	 * A short name for read channel operations.
	 */
	public static final int R = 1;
	/**
	 * A short name for wirte channel operations.
	 */
	public static final int W = 2;
	/**
	 * A short name for read and write channel operations.
	 */
	public static final int RW = 3;
	/**
	 * Map of all states for a class server.
	 */
	protected Map<S, MinaState> stateMap = null;
	/**
	 * Map of all states for a class server.
	 */
	protected Map<T, NioTransition<S>> transitionMap = null;
	/**
	 * The node server.
	 */
	protected MinaServer<S, T> server = null;

	/**
	 * Initialize this factory with the specified server.
	 * @param server the server for which to initialize.
	 */
	public MinaServerFactory(MinaServer<S, T> server)
	{
		this.server = server;
		stateMap = createStateMap();
		transitionMap = createTransitionMap();
	}

	/**
	 * Create the map of all possible states.
	 * @return a mapping of the states enumeration to the corresponding NioState instances.
	 */
	public abstract Map<S, MinaState> createStateMap();

	/**
	 * Create the map of all possible transitions.
	 * @return a mapping of the transitions enumeration to the corresponding NioTransition instances.
	 * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
	 */
	public abstract Map<T, NioTransition<S>> createTransitionMap();

	/**
	 * Create a transition to the specified state for the specified IO operations.
	 * @param state resulting state of the transition.
	 * @param ops the operations allowed.
	 * @return an <code>NioTransition&lt;ClassState&gt;</code> instance.
	 */
	protected NioTransition<S> transition(S state, int ops)
	{
		return new NioTransition<S>(state, ops);
	}

	/**
	 * Get a state given its name.
	 * @param name the name of the state to lookup.
	 * @return an <code>NioState</code> instance.
	 */
	public MinaState getState(S name)
	{
		return stateMap.get(name);
	}

	/**
	 * Get a transition given its name.
	 * @param name the name of the transition to lookup.
	 * @return an <code>NioTransition</code> instance.
	 */
	public NioTransition<S> getTransition(T name)
	{
		return transitionMap.get(name);
	}
}
