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

package org.jppf.server.nio;

import java.nio.channels.SelectionKey;
import java.util.Map;

/**
 * Instances of this class provide a mapping of enumerated values for states and
 * transitions to the actual corresponding objects.
 * @param <S> the type safe enumeration of the states.
 * @param <T> the type safe enumeration of the state transitions.
 * @param <U> the type of tserver this factory is for.
 * @author Laurent Cohen
 */
public abstract class NioServerFactory<S extends Enum<S>, T extends Enum<T>, U extends NioServer>
{
	/**
	 * A short name for read and write channel operations.
	 */
	public static final int RW = SelectionKey.OP_READ|SelectionKey.OP_WRITE;
	/**
	 * A short name for read channel operations.
	 */
	public static final int R = SelectionKey.OP_READ;
	/**
	 * A short name for wirte channel operations.
	 */
	public static final int W = SelectionKey.OP_WRITE;

	/**
	 * Map of all states for a class server.
	 */
	protected Map<S, NioState<T>> stateMap = null;
	/**
	 * Map of all states for a class server.
	 */
	protected Map<T, NioTransition<S>> transitionMap = null;
	/**
	 * The server for which this factory is intended.
	 */
	protected U server = null;

	/**
	 * Initialize this factory with the specified server.
	 * @param server the server for which to initialize.
	 */
	protected NioServerFactory(U server)
	{
		this.server = server;
		stateMap = createStateMap(server);
		transitionMap = createTransitionMap();
	}

	/**
	 * Create the map of all possible states.
	 * @param server the server to which the states refer.
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 */
	public abstract Map<S, NioState<T>> createStateMap(U server);

	/**
	 * Create the map of all possible states.
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 */
	public abstract Map<T, NioTransition<S>> createTransitionMap();

	/**
	 * Get a state given its name.
	 * @param name the name of the state to lookup.
	 * @return an <code>NioState</code> instance.
	 */
	public NioState<T> getState(S name)
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

	/**
	 * Get the server for which this factory is intended.
	 * @return an <code>NioServer</code> instance.
	 */
	public U getServer()
	{
		return server;
	}
}
