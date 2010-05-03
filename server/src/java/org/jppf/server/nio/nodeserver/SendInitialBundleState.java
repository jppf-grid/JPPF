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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.net.ConnectException;

import org.apache.commons.logging.*;
import org.jppf.server.nio.ChannelWrapper;

/**
 * This class represents the state of sending the initial hand-shaking data to a newly connected node.
 * @author Laurent Cohen
 */
class SendInitialBundleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendInitialBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public SendInitialBundleState(NodeNioServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param wrapper the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public NodeTransition performTransition(ChannelWrapper<?> wrapper) throws Exception
	{
		//if (debugEnabled) log.debug("exec() for " + getRemoteHost(channel));
		if (wrapper.isReadable())
		{
			throw new ConnectException("node " + wrapper + " has been disconnected");
		}

		AbstractNodeContext context = (AbstractNodeContext) wrapper.getContext();
		if (context.getNodeMessage() == null)
		{
			if (debugEnabled) log.debug("serializing initial bundle for " + wrapper);
			context.serializeBundle(wrapper);
		}
		if (context.writeMessage(wrapper))
		{
			if (debugEnabled) log.debug("sent entire initial bundle for " + wrapper);
			context.setNodeMessage(null);
			context.setBundle(null);
			return TO_WAIT_INITIAL;
		}
		if (debugEnabled) log.debug("part yet to send for " + wrapper);
		return TO_SEND_INITIAL;
	}
}
