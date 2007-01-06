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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ChannelState.*;
import static org.jppf.server.nio.classloader.ChannelTransition.*;

import java.util.*;

import org.jppf.server.nio.*;

/**
 * 
 * @author Laurent Cohen
 */
public final class ClassServerFactory
	extends NioServerFactory<ChannelState, ChannelTransition, ClassNioServer>
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
	public Map<ChannelState, NioState<ChannelTransition>> createStateMap(ClassNioServer server)
	{
		Map<ChannelState, NioState<ChannelTransition>> map =
			new EnumMap<ChannelState, NioState<ChannelTransition>>(ChannelState.class);
		map.put(DEFINING_TYPE, new DefiningChannelTypeState(server));
		map.put(SENDING_INITIAL_RESPONSE, new SendingInitialResponseState(server));
		map.put(SENDING_NODE_RESPONSE, new SendingNodeResponseState(server));
		map.put(SENDING_PROVIDER_REQUEST, new SendingProviderRequestState(server));
		map.put(WAITING_NODE_REQUEST, new WaitingNodeRequestState(server));
		map.put(WAITING_PROVIDER_RESPONSE, new WaitingProviderResponseState(server));
		return map;
	}

	/**
	 * Create the map of all possible states.
	 * @return a mapping of the states enumeration to the corresponding NioStateInstances.
	 * @see org.jppf.server.nio.NioServerFactory#createTransitionMap()
	 */
	public Map<ChannelTransition, NioTransition<ChannelState>> createTransitionMap()
	{
		Map<ChannelTransition, NioTransition<ChannelState>> map =
			new EnumMap<ChannelTransition, NioTransition<ChannelState>>(ChannelTransition.class);
		map.put(TO_DEFINING_TYPE, new NioTransition<ChannelState>(DEFINING_TYPE, R));
		map.put(TO_SENDING_INITIAL_RESPONSE, new NioTransition<ChannelState>(SENDING_INITIAL_RESPONSE, RW));
		map.put(TO_WAITING_NODE_REQUEST, new NioTransition<ChannelState>(WAITING_NODE_REQUEST, R));
		map.put(TO_SENDING_NODE_RESPONSE, new NioTransition<ChannelState>(SENDING_NODE_RESPONSE, RW));
		map.put(TO_SENDING_PROVIDER_REQUEST, new NioTransition<ChannelState>(SENDING_PROVIDER_REQUEST, RW));
		map.put(TO_WAITING_PROVIDER_RESPONSE, new NioTransition<ChannelState>(WAITING_PROVIDER_RESPONSE, R));
		map.put(TO_IDLE_NODE, new NioTransition<ChannelState>(SENDING_NODE_RESPONSE, 0));
		map.put(TO_IDLE_PROVIDER, new NioTransition<ChannelState>(SENDING_PROVIDER_REQUEST, 0));
		return map;
	}
}
