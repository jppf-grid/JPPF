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

import static org.jppf.server.nio.classloader.ChannelTransition.*;

import java.net.ConnectException;
import java.nio.channels.*;

import org.apache.log4j.Logger;
import org.jppf.utils.StringUtils;

/**
 * State of sending the initial response to a newly created node channel.
 * @author Laurent Cohen
 */
public class SendingInitialResponseState extends ClassServerState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(SendingInitialResponseState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public SendingInitialResponseState(ClassNioServer server)
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
		SocketChannel channel = (SocketChannel) key.channel();
		ClassContext context = (ClassContext) key.attachment();
		if (key.isReadable())
		{
			throw new ConnectException("node " + StringUtils.getRemostHost(channel) + " has been disconnected");
		}
		if (context.writeMessage(channel))
		{
			if (debugEnabled) log.debug("sent uuid to node: " + StringUtils.getRemostHost(channel));
			context.setMessage(null);
			return TO_WAITING_NODE_REQUEST;
		}
		return TO_SENDING_INITIAL_RESPONSE;
	}
}
