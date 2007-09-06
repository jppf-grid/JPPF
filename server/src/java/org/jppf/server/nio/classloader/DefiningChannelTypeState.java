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

import static org.jppf.node.JPPFResourceWrapper.State.*;
import static org.jppf.server.nio.classloader.ClassTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.nio.channels.*;
import java.util.Vector;

import org.apache.commons.logging.*;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.JPPFDriver;

/**
 * This class represents the state of a new class server connection, whose type is yet undetermined.
 * @author Laurent Cohen
 */
public class DefiningChannelTypeState extends ClassServerState
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(DefiningChannelTypeState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	public DefiningChannelTypeState(ClassNioServer server)
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
		// we don't know yet which whom we are talking, is it a node or a provider?
		SocketChannel channel = (SocketChannel) key.channel();
		ClassContext context = (ClassContext) key.attachment();
		if (context.readMessage(channel))
		{
			JPPFResourceWrapper resource = context.deserializeResource();
			if (debugEnabled) log.debug("channel: " + getRemoteHost(channel) + " read resource [" + resource.getName() + "] done");
			if (PROVIDER_INITIATION.equals(resource.getState()))
			{
				if (debugEnabled) log.debug("initiating provider: " + getRemoteHost(channel));
				String uuid = resource.getUuidPath().getFirst();
				// it is a provider
				server.providerConnections.put(uuid, channel);
				context.setUuid(uuid);
				context.setPendingRequests(new Vector<SelectionKey>());
				context.setMessage(null);
				resource.setManagementId(JPPFDriver.getInstance().getJmxServer().getId());
				context.serializeResource();
				return TO_SENDING_INITIAL_PROVIDER_RESPONSE;
			}
			else if (NODE_INITIATION.equals(resource.getState()))
			{
				if (debugEnabled) log.debug("initiating node: " + getRemoteHost(channel));
				// send the uuid of this driver to the node or node peer.
				resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
				resource.setProviderUuid(JPPFDriver.getInstance().getUuid());
				context.serializeResource();
				return TO_SENDING_INITIAL_RESPONSE;
			}
		}
		return TO_DEFINING_TYPE;
	}
}
