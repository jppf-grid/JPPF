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

import static org.jppf.utils.StringUtils.getRemoteHost;
import static org.jppf.server.nio.multiplexer.MultiplexerTransition.*;

import java.io.IOException;
import java.nio.channels.*;

import org.apache.commons.logging.*;

/**
 * In this state, the channel is waiting for the port number to which data should be forwarded locally.
 * @author Laurent Cohen
 */
public class IdentifyingInboundChannelState extends MultiplexerServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(IdentifyingInboundChannelState.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public IdentifyingInboundChannelState(MultiplexerNioServer server)
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
		MultiplexerContext context = (MultiplexerContext) key.attachment();
		if (debugEnabled) log.debug("exec() for " + getRemoteHost(channel));
		if (context.readMessage((ReadableByteChannel) channel))
		{
			int port = context.readOutBoundPort();
			if (debugEnabled) log.debug("read port number for " + getRemoteHost(channel) + ": " + port);
			if (port <= 0)
			{
				throw new IOException("outbound port could not be read from this channel");
			}
			OutboundChannelHandler handler = new OutboundChannelHandler(server, "localhost", port, key);
			OutboundChannelInitializer init = new OutboundChannelInitializer(server, key, handler);
			context.setMessage(null);
			server.setKeyOps(key, 0);
			new Thread(init).start();
			return TO_RECEIVING;
		}
		return TO_IDENTIFYING_INBOUND_CHANNEL;
	}
}
