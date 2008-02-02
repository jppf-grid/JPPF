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

package org.jppf.server.nio.multiplexer;

import static org.jppf.server.nio.multiplexer.MultiplexerTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.net.ConnectException;
import java.nio.channels.*;

import org.apache.commons.logging.*;

/**
 * 
 * @author Laurent Cohen
 */
public class SendingMultiplexingInfoState extends MultiplexerServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendingMultiplexingInfoState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public SendingMultiplexingInfoState(MultiplexerNioServer server)
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
	public MultiplexerTransition performTransition(SelectionKey key) throws Exception
	{
		SelectableChannel channel = key.channel();
		if (key.isReadable())
		{
			throw new ConnectException("node " + getRemoteHost(channel) + " has been disconnected");
		}
		MultiplexerContext context = (MultiplexerContext) key.attachment();
		if (context.writeMessage((WritableByteChannel) channel))
		{
			if (debugEnabled) log.debug("node: " + getRemoteHost(channel) + ", response sent to the node");
			context.setMessage(null);
			return TO_IDLE;
		}
		return TO_SENDING;
	}
}
