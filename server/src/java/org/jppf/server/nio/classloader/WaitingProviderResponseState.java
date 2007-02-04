/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassState.SENDING_NODE_RESPONSE;
import static org.jppf.server.nio.classloader.ClassTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.io.IOException;
import java.nio.channels.*;

import org.apache.log4j.Logger;
import org.jppf.node.JPPFResourceWrapper;

/**
 * This class represents the state of waiting for the response from a provider.
 * @author Laurent Cohen
 */
public class WaitingProviderResponseState extends ClassServerState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(WaitingProviderResponseState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public WaitingProviderResponseState(ClassNioServer server)
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
		boolean messageRead = false;
		try
		{
			messageRead = context.readMessage(channel);
		}
		catch(IOException e)
		{
			if (debugEnabled) log.debug("an exception occurred while reading response from provider: " + getRemoteHost(channel));
			server.providerConnections.remove(context.getUuid());
			SelectionKey currentRequest = context.getCurrentRequest();
			if ((currentRequest != null) || !context.getPendingRequests().isEmpty())
			{
				if (debugEnabled) log.debug("provider: " + getRemoteHost(channel) + " sending null response for disconnected provider");
				if (currentRequest != null)
				{
					context.setCurrentRequest(null);
					sendNullResponse(currentRequest);
				}
				for (int i=0; i<context.getPendingRequests().size(); i++)
					sendNullResponse(context.getPendingRequests().remove(0));
			}
			throw e;
		}
		if (messageRead)
		{
			if (debugEnabled) log.debug("read response from provider: " + getRemoteHost(channel) +
				" complete, sending to node " + getRemoteHost((SocketChannel) context.getCurrentRequest().channel()) + 
				", resource: " + context.getResource().getName());
			JPPFResourceWrapper resource = context.deserializeResource();
			// putting the definition in cache
			if (resource.getDefinition() != null)
				server.setCacheContent(context.getUuid(), resource.getName(), resource.getDefinition());
			// fowarding it to channel that requested
			SelectionKey destinationKey = context.getCurrentRequest();
			ClassContext destinationContext = (ClassContext) destinationKey.attachment();
			resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
			destinationContext.setMessage(null);
			destinationContext.setResource(resource);
			destinationContext.serializeResource();
			destinationContext.setState(SENDING_NODE_RESPONSE);
			server.setKeyOps(destinationKey, SelectionKey.OP_WRITE|SelectionKey.OP_READ);
			context.setMessage(null);
			context.setCurrentRequest(null);
			return TO_SENDING_PROVIDER_REQUEST;
		}
		return TO_WAITING_PROVIDER_RESPONSE;
	}
}
