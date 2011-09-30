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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.TO_IDLE;

import java.net.ConnectException;

import org.jppf.server.nio.ChannelWrapper;
import org.slf4j.*;

/**
 * This class represents the state of waiting for some action.
 * @author Laurent Cohen
 */
class IdleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(IdleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public IdleState(NodeNioServer server)
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
	@Override
    public NodeTransition performTransition(ChannelWrapper<?> wrapper) throws Exception
	{
		if (debugEnabled) log.debug("exec() for " + wrapper);
		if (CHECK_CONNECTION && wrapper.isReadable())
		{
			/*
			if (debugEnabled)
			{
				SelectionKey key = ((SelectionKeyWrapper) wrapper).getChannel();
				SocketChannel channel = (SocketChannel) key.channel();
				ByteBuffer buf = ByteBuffer.allocate(32768);
				int n = channel.read(buf);
				log.debug("readable channel: read " + n + " bytes");
			}
			*/
			if (!(wrapper instanceof LocalNodeChannel)) throw new ConnectException("node " + wrapper + " has been disconnected");
		}
		return TO_IDLE;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean autoChangeInterestOps()
	{
		return false;
	}
}
