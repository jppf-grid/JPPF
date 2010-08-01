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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.net.ConnectException;

import org.apache.commons.logging.*;
import org.jppf.classloader.LocalClassLoaderChannel;
import org.jppf.server.nio.ChannelWrapper;

/**
 * State of sending the initial response to a newly created provider channel.
 * @author Laurent Cohen
 */
class SendingProviderInitialResponseState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendingProviderInitialResponseState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public SendingProviderInitialResponseState(ClassNioServer server)
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
		ClassContext context = (ClassContext) wrapper.getContext();
		if (wrapper.isReadable() && !(wrapper instanceof LocalClassLoaderChannel))
		{
			throw new ConnectException("provider " + wrapper + " has been disconnected");
		}
		if (context.writeMessage(wrapper))
		{
			if (debugEnabled) log.debug("sent management to provider: " + wrapper);
			//context.setMessage(null);
			return TO_IDLE_PROVIDER;
		}
		return TO_SENDING_INITIAL_PROVIDER_RESPONSE;
	}
}
