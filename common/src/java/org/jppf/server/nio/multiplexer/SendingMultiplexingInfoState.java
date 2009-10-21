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

package org.jppf.server.nio.multiplexer;

import static org.jppf.server.nio.multiplexer.MultiplexerTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.server.nio.NioMessage;

/**
 * This state is for sending a port number to a remote multiplexer, which will then establish
 * a local connection (to a JPPF driver) using this port.
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
			throw new ConnectException("multiplexer channel " + getRemoteHost(channel) + " has been disconnected");
		}
		if (debugEnabled) log.debug("exec() for " + getRemoteHost(channel));
		MultiplexerContext context = (MultiplexerContext) key.attachment();
		if (context.getMessage() == null)
		{
			MultiplexerContext linkedContext = (MultiplexerContext) context.getLinkedKey().attachment();
			NioMessage msg = new NioMessage();
			msg.length = 4;
			msg.buffer = ByteBuffer.wrap(new byte[4]);
			msg.buffer.putInt(linkedContext.getBoundPort());
			msg.buffer.flip();
			context.setMessage(msg);
		}
		if (context.writeMessage((WritableByteChannel) channel))
		{
			if (debugEnabled) log.debug("message sent to remote multiplexer " + getRemoteHost(channel));
			context.setMessage(null);
			return TO_SENDING_OR_RECEIVING;
		}
		return TO_SENDING_MULTIPLEXING_INFO;
	}
}
