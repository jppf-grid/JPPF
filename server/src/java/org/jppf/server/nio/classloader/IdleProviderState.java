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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassTransition.TO_IDLE_PROVIDER;

import java.net.ConnectException;

import org.jppf.classloader.LocalClassLoaderChannel;
import org.jppf.server.nio.*;
import org.slf4j.*;

/**
 * This class represents the state of waiting for the next request for a provider.
 * @author Laurent Cohen
 */
class IdleProviderState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(IdleProviderState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public IdleProviderState(ClassNioServer server)
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
	public ClassTransition performTransition(ChannelWrapper<?> wrapper) throws Exception
	{
		if (wrapper.isReadable() && !(wrapper instanceof LocalClassLoaderChannel))
		{
			ClassContext context = (ClassContext) wrapper.getContext();
			server.removeProviderConnection(context.getUuid(), wrapper);
			throw new ConnectException("provider " + wrapper + " has been disconnected");
		}
		return TO_IDLE_PROVIDER;
	}
}
