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

package org.jppf.server.nio.multiplexer;

import static org.jppf.server.nio.multiplexer.MultiplexerTransition.*;

import java.net.ConnectException;

import org.jppf.server.nio.ChannelWrapper;
import org.slf4j.*;

/**
 * State of sending data on a channel.
 * @author Laurent Cohen
 */
public class SendingState extends MultiplexerServerState
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(SendingState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public SendingState(MultiplexerNioServer server)
	{
		super(server);
	}

	/**
	 * {@inheritDoc}
	 */
	public MultiplexerTransition performTransition(ChannelWrapper<?> wrapper) throws Exception
	{
		if (wrapper.isReadable())
		{
			throw new ConnectException("multiplexer " + wrapper + " has been disconnected");
		}
		MultiplexerContext context = (MultiplexerContext) wrapper.getContext();
		if (context.writeMessage(wrapper))
		{
			if (debugEnabled) log.debug(wrapper.toString() + " message sent");
			context.setMultiplexerMessage(null);
			return TO_SENDING_OR_RECEIVING;
		}
		return TO_SENDING;
	}
}
