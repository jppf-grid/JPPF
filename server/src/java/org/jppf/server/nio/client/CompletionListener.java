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

package org.jppf.server.nio.client;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.*;

/**
 * Completion listener that is used to notify that results were received from a node,
 * and they should be sent back to the client.
 * @author Laurent Cohen
 */
public class CompletionListener implements TaskCompletionListener
{
	/**
	 * The transition manager for the server to client channels.
	 */
	private static StateTransitionManager transitionManager = JPPFDriver.getInstance().getClientNioServer().getTransitionManager();
	/**
	 * The client channel.
	 */
	private ChannelWrapper<?> channel;
	/**
	 * The client context associated with the channel.
	 */
	private ClientContext context;


	/**
	 * Initialize this completion listener with the specified channel.
	 * @param channel the client channel.
	 */
	public CompletionListener(ChannelWrapper<?> channel)
	{
		this.channel = channel;
		context = (ClientContext) channel.getContext();
	}

	/**
	 * {@inheritDoc}
	 */
	public void taskCompleted(BundleWrapper result)
	{
		context.offerCompletedBundle(result);
		if (ClientState.IDLE.equals(context.getState()))
		{
			transitionManager.transitionChannel(channel, ClientTransition.TO_SENDING_RESULTS);
		}
	}
}
