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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.net.ConnectException;
import java.nio.channels.*;

import org.apache.log4j.Logger;
import org.jppf.server.JPPFTaskBundle;
import org.jppf.utils.StringUtils;

/**
 * This class represents the state of waiting for some action.
 * @author Laurent Cohen
 */
public class SendingBundleState extends NodeServerState
{
	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(SendingBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public SendingBundleState(NodeNioServer server)
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
	public NodeTransition performTransition(SelectionKey key) throws Exception
	{
		SocketChannel channel = (SocketChannel) key.channel();
		//if (debugEnabled) log.debug("exec() for " + getRemostHost(channel));
		if (key.isReadable())
		{
			throw new ConnectException("node " + StringUtils.getRemostHost(channel) + " has been disconnected");
		}

		NodeContext context = (NodeContext) key.attachment();
		if (context.getMessage() == null)
		{
			// check whether the bundler settings have changed.
			if (context.getBundler().getTimestamp() < server.getBundler().getTimestamp())
			{
				context.setBundler(server.getBundler().copy());
			}
			JPPFTaskBundle bundle = server.getQueue().nextBundle(context.getBundler().getBundleSize());
			if (bundle != null)
			{
				if (debugEnabled) log.debug("got bundle from the queue for " + StringUtils.getRemostHost(channel));
				// to avoid cycles in peer-to-peer routing of jobs.
				if (bundle.getUuidPath().contains(context.getUuid()))
				{
					if (debugEnabled) log.debug("cycle detected in peer-to-peer bundle routing: "+bundle.getUuidPath().getList());
					context.resubmitBundle(bundle);
					context.setBundle(null);
					server.addIdleChannel(channel);
					return TRANSITION_TO_IDLE;
				}
				context.setBundle(bundle);
				context.serializeBundle();
			}
			else
			{
				server.addIdleChannel(channel);
				return TRANSITION_TO_IDLE;
			}
		}
		if (context.writeMessage(channel))
		{
			if (debugEnabled) log.debug("sent entire bundle for " + StringUtils.getRemostHost(channel));
			context.setMessage(null);
			return TRANSITION_TO_WAITING;
		}
		if (debugEnabled) log.debug("part yet to send for " + StringUtils.getRemostHost(channel));
		return TRANSITION_TO_SENDING;
	}
}
