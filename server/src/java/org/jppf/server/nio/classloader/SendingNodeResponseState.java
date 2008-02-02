/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.net.ConnectException;
import java.nio.channels.*;

import org.apache.commons.logging.*;

/**
 * This class represents the state of sending a response to a node.
 * @author Laurent Cohen
 */
public class SendingNodeResponseState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendingNodeResponseState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public SendingNodeResponseState(ClassNioServer server)
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
	public ClassTransition performTransition(SelectionKey key) throws Exception
	{
		SelectableChannel channel = key.channel();
		if (key.isReadable())
		{
			throw new ConnectException("node " + getRemoteHost(channel) + " has been disconnected");
		}
		ClassContext context = (ClassContext) key.attachment();
		if (context.writeMessage((WritableByteChannel) channel))
		{
			if (debugEnabled) log.debug("node: " + getRemoteHost(channel) + ", response [" +
				context.getResource().getName() + "] sent to the node");
			context.setMessage(null);
			return TO_WAITING_NODE_REQUEST;
		}
		return TO_SENDING_NODE_RESPONSE;
	}
}
