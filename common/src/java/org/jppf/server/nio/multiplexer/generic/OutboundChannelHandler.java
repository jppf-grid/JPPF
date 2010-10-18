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

package org.jppf.server.nio.multiplexer.generic;

import java.nio.channels.*;

import org.jppf.comm.socket.SocketChannelClient;
import org.jppf.server.nio.*;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * They handle (re)connection services when needed.
 * Instances of this class are used when a multiplexer needs to connect to a remote multiplexer.
 * @author Laurent Cohen
 */
public class OutboundChannelHandler extends AbstractSocketChannelHandler
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(OutboundChannelHandler.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The key associated with the initial connection.
	 */
	private ChannelWrapper initialKey = null;

	/**
	 * Initialize the channel with the specified host and port.
	 * @param server the NioServer to which the channel is registred.
	 * @param host the remote host to connect to.
	 * @param port the port to connect to on the remote host.
	 * @param initialKey the key associated with the initial connection.
	 */
	public OutboundChannelHandler(NioServer server, String host, int port, ChannelWrapper initialKey)
	{
		super(server, host, port);
		this.initialKey = initialKey;
	}

	/**
	 * Initialize the channel client.
	 * @return a non-connected <code>SocketChannelClient</code> instance.
	 * @throws Exception if an error is raised during initialization.
	 * @see org.jppf.server.nio.AbstractSocketChannelHandler#initSocketChannel()
	 */
	protected SocketChannelClient initSocketChannel() throws Exception
	{
		return new SocketChannelClient(host, port, false);
	}

	/**
	 * This method is called after the channel is successfully conntected.
	 * @throws Exception if an error is raised while performing the initialization.
	 * @see org.jppf.server.nio.AbstractSocketChannelHandler#postInit()
	 */
	@SuppressWarnings("unchecked")
	protected void postInit() throws Exception
	{
		SocketChannel channel = socketClient.getChannel();
		socketClient.setChannel(null);
		MultiplexerContext context = (MultiplexerContext) server.createNioContext();
		context.setLinkedKey(initialKey);
		context.setState(MultiplexerState.SENDING_OR_RECEIVING);
		server.getTransitionManager().registerChannel(channel, SelectionKey.OP_READ, context,
			new StateTransitionManager.ChannelRegistrationAction()
			{
				public void run()
				{
					MultiplexerContext initialContext = (MultiplexerContext ) initialKey.getContext();
					initialContext.setLinkedKey(key);
					server.getTransitionManager().transitionChannel(initialKey, MultiplexerTransition.TO_SENDING_OR_RECEIVING);
				}
			}
		);
		if (debugEnabled) log.debug("registered outbound channel " + StringUtils.getRemoteHost(channel));
	}
}
