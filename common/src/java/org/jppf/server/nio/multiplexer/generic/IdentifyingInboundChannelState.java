/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.nio.multiplexer.generic;

import static org.jppf.server.nio.multiplexer.generic.MultiplexerTransition.*;

import java.io.IOException;

import org.jppf.server.nio.ChannelWrapper;
import org.slf4j.*;

/**
 * In this state, the channel is waiting for the port number to which data should be forwarded locally.
 * @author Laurent Cohen
 */
public class IdentifyingInboundChannelState extends MultiplexerServerState
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(IdentifyingInboundChannelState.class);
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
	 * {@inheritDoc}
	 */
	public MultiplexerTransition performTransition(ChannelWrapper<?> wrapper) throws Exception
	{
		MultiplexerContext context = (MultiplexerContext) wrapper.getContext();
		if (debugEnabled) log.debug("exec() for " + wrapper);
		if (context.readMessage(wrapper))
		{
			int port = context.readOutBoundPort();
			if (debugEnabled) log.debug("read port number for " + wrapper + ": " + port);
			if (port <= 0)
			{
				throw new IOException("outbound port could not be read from this channel");
			}
			OutboundChannelHandler handler = new OutboundChannelHandler(server, "localhost", port, wrapper);
			MultiplexerChannelInitializer init = new MultiplexerChannelInitializer(handler);
			context.setMessage(null);
			server.getTransitionManager().transitionChannel(wrapper, TO_IDLE);
			new Thread(init).start();
			return TO_IDLE;
		}
		return TO_IDENTIFYING_INBOUND_CHANNEL;
	}
}
