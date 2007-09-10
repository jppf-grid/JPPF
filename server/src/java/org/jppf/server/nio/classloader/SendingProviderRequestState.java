/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.net.ConnectException;
import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.server.nio.NioMessage;

/**
 * This class represents the state of sending a request to a provider.
 * @author Laurent Cohen
 */
public class SendingProviderRequestState extends ClassServerState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendingProviderRequestState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public SendingProviderRequestState(ClassNioServer server)
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
	public ClassTransition performTransition(SelectionKey key) throws Exception
	{
		SocketChannel channel = (SocketChannel) key.channel();
		ClassContext context = (ClassContext) key.attachment();
		if (key.isReadable())
		{
			server.removeProviderConnection(context.getUuid(), channel);
			SelectionKey currentRequest = context.getCurrentRequest();
			if (debugEnabled) log.debug("provider: " + getRemoteHost(channel) + " sending null response for disconnected provider");
			if ((currentRequest != null) || !context.getPendingRequests().isEmpty())
			{
				if (currentRequest != null)
				{
					if (debugEnabled) log.debug("provider: " + getRemoteHost(channel) +
						" disconnected while serving request [" + context.getResource().getName() +
						"] for node " + getRemoteHost((SocketChannel) context.getCurrentRequest().channel()));
					context.setCurrentRequest(null);
					sendNullResponse(currentRequest);
				}
				for (int i=0; i<context.getPendingRequests().size(); i++)
					sendNullResponse(context.getPendingRequests().remove(0));
			}
			throw new ConnectException("provider " + getRemoteHost(channel) + " has been disconnected");
		}
		if ((context.getCurrentRequest() == null) && !context.getPendingRequests().isEmpty())
		{
			SelectionKey request = context.getPendingRequests().remove(0);
			ClassContext requestContext = (ClassContext) request.attachment();
			context.setMessage(null);
			context.setResource(requestContext.getResource());
			if (debugEnabled)
			{
				log.debug("provider " + getRemoteHost(channel) + " serving new resource request [" +
					context.getResource().getName() + "] from node: " + getRemoteHost((SocketChannel) request.channel()));
			}
			context.serializeResource();
			context.setCurrentRequest(request);
		}
		if (context.getCurrentRequest() == null)
		{
			if (debugEnabled) log.debug("provider: " + getRemoteHost(channel) + " has no request to process, returning to idle mode");
			context.setMessage(null);
			return TO_IDLE_PROVIDER;
		}
		if (context.writeMessage(channel))
		{
			if (debugEnabled) log.debug("request sent to the provider " + getRemoteHost(channel) + " from node " + 
				getRemoteHost((SocketChannel) context.getCurrentRequest().channel()) + 
				", resource: " + context.getResource().getName());
			context.setMessage(new NioMessage());
			return TO_WAITING_PROVIDER_RESPONSE;
		}
		return TO_SENDING_PROVIDER_REQUEST;
	}
}
