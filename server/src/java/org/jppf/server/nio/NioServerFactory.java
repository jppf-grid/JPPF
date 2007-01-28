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
	public static int RW = SelectionKey.OP_READ|SelectionKey.OP_WRITE;
	/**
	 * A short name for read channel operations.
	 */
	public static int R = SelectionKey.OP_READ;

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
