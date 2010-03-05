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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.net.ConnectException;
import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.io.BundleWrapper;
import org.jppf.server.protocol.JPPFTaskBundle;

/**
 * This class represents the state of waiting for some action.
 * @author Laurent Cohen
 */
public class SendingBundleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendingBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
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
		SelectableChannel channel = key.channel();
		//if (debugEnabled) log.debug("exec() for " + getRemostHost(channel));
		if (key.isReadable())
		{
			throw new ConnectException("node " + getRemoteHost(channel) + " has been disconnected");
		}

		NodeContext context = (NodeContext) key.attachment();
		if (context.getNodeMessage() == null)
		{
			BundleWrapper bundleWrapper = context.getBundle();
			JPPFTaskBundle bundle = (bundleWrapper == null) ? null : bundleWrapper.getBundle();
			if (bundle != null)
			{
				if (debugEnabled) log.debug("got bundle from the queue for " + getRemoteHost(channel));
				// to avoid cycles in peer-to-peer routing of jobs.
				if (bundle.getUuidPath().contains(context.getUuid()))
				{
					if (debugEnabled) log.debug("cycle detected in peer-to-peer bundle routing: " + bundle.getUuidPath().getList());
					context.resubmitBundle(bundleWrapper);
					context.setBundle(null);
					server.addIdleChannel(channel);
					return TO_IDLE;
				}
				bundle.setExecutionStartTime(System.currentTimeMillis());
				context.serializeBundle();
			}
			else
			{
				server.addIdleChannel(channel);
				return TO_IDLE;
			}
		}
		if (context.getNodeMessage().write((WritableByteChannel) channel))
		{
			if (debugEnabled) log.debug("sent entire bundle to node " + getRemoteHost(channel));
			context.setNodeMessage(null);
			//JPPFDriver.getInstance().getJobManager().jobDispatched(context.getBundle(), channel);
			return TO_WAITING;
		}
		if (debugEnabled) log.debug("part yet to send to node " + getRemoteHost(channel));
		return TO_SENDING;
	}
}
