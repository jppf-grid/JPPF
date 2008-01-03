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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.net.ConnectException;
import java.nio.channels.*;

import org.apache.commons.logging.*;

/**
 * This class represents the state of sending the initial hand-shaking data to a newly connected node.
 * @author Laurent Cohen
 */
public class SendInitialBundleState extends NodeServerState
{
	/**
	 * Log4j logger for this class.
	 */
	protected static final Log LOG = LogFactory.getLog(SendInitialBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
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
	 * @param key the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public NodeTransition performTransition(SelectionKey key) throws Exception
	{
		SelectableChannel channel = key.channel();
		//if (debugEnabled) log.debug("exec() for " + getRemoteHost(channel));
		if (key.isReadable())
		{
			throw new ConnectException("node " + getRemoteHost(channel) + " has been disconnected");
		}

		NodeContext context = (NodeContext) key.attachment();
		if (context.getMessage() == null)
		{
			if (DEBUG_ENABLED) LOG.debug("serializing initial bundle for " + getRemoteHost(channel));
			context.serializeBundle();
		}
		if (context.writeMessage((WritableByteChannel) channel))
		{
			if (DEBUG_ENABLED) LOG.debug("sent entire initial bundle for " + getRemoteHost(channel));
			context.setMessage(null);
			context.setBundle(null);
			return TO_WAIT_INITIAL;
		}
		if (DEBUG_ENABLED) LOG.debug("part yet to send for " + getRemoteHost(channel));
		return TO_SEND_INITIAL;
	}
}
