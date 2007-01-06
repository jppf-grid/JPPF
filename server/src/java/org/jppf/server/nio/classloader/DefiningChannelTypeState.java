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

import static org.jppf.node.JPPFResourceWrapper.State.*;
import static org.jppf.server.nio.classloader.ChannelTransition.*;

import java.nio.channels.*;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.StringUtils;

/**
 * This class represents the state of a new class server connection, whose type is yet undetermined.
 * @author Laurent Cohen
 */
public class DefiningChannelTypeState extends ClassServerState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(DefiningChannelTypeState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	public DefiningChannelTypeState(ClassNioServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param key the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public ChannelTransition performTransition(SelectionKey key) throws Exception
	{
		// we don't know yet which whom we are talking, is it a node or a provider?
		SocketChannel channel = (SocketChannel) key.channel();
		ClassContext context = (ClassContext) key.attachment();
		if (context.readMessage(channel))
		{
			JPPFResourceWrapper resource = context.deserializeResource();
			if (debugEnabled) log.debug("channel: " + StringUtils.getRemostHost(channel) + " read resource [" + resource.getName() + "] done");
			if (PROVIDER_INITIATION.equals(resource.getState()))
			{
				if (debugEnabled) log.debug("initiating provider: " + StringUtils.getRemostHost(channel));
				String uuid = resource.getUuidPath().getFirst();
				// it is a provider
				server.providerConnections.put(uuid, channel);
				context.setUuid(uuid);
				context.setPendingRequests(new Vector<SelectionKey>());
				context.setMessage(null);
				return TO_IDLE_PROVIDER;
			}
			else if (NODE_INITIATION.equals(resource.getState()))
			{
				if (debugEnabled) log.debug("initiating node: " + StringUtils.getRemostHost(channel));
				// send the uuid of this driver to the node or node peer.
				resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
				resource.setProviderUuid(JPPFDriver.getInstance().getUuid());
				context.serializeResource();
				return TO_SENDING_INITIAL_RESPONSE;
			}
		}
		return TO_DEFINING_TYPE;
	}
}
