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

import static org.jppf.classloader.JPPFResourceWrapper.State.*;
import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.Vector;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * This class represents the state of a new class server connection, whose type is yet undetermined.
 * @author Laurent Cohen
 */
class DefiningChannelTypeState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(DefiningChannelTypeState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether management features are enabled for this driver.
	 */
	private static boolean managementEnabled =
		JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true);

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
	 * @param wrapper the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public ClassTransition performTransition(ChannelWrapper<?> wrapper) throws Exception
	{
		// we don't know yet which whom we are talking, is it a node or a provider?
		ClassContext context = (ClassContext) wrapper.getContext();
		if (context.readMessage(wrapper))
		{
			JPPFResourceWrapper resource = context.deserializeResource();
			if (debugEnabled) log.debug("channel: " + wrapper + " read resource [" + resource.getName() + "] done");
			if (PROVIDER_INITIATION.equals(resource.getState()))
			{
				if (debugEnabled) log.debug("initiating provider: " + wrapper);
				String uuid = resource.getUuidPath().getFirst();
				// it is a provider
				server.addProviderConnection(uuid, wrapper);
				context.setUuid(uuid);
				context.setPendingRequests(new Vector<ChannelWrapper<?>>());
				context.setMessage(null);
				if (managementEnabled)
				{
					resource.setManagementId(driver.getJmxServer().getId());
				}
				context.serializeResource(wrapper);
				return TO_SENDING_INITIAL_PROVIDER_RESPONSE;
			}
			else if (NODE_INITIATION.equals(resource.getState()))
			{
				if (debugEnabled) log.debug("initiating node: " + wrapper);
				String uuid = (String) resource.getData("node.uuid");
				if (uuid != null)
				{
					context.setUuid(uuid);
					server.addNodeConnection(uuid, wrapper);
				}
				// send the uuid of this driver to the node or node peer.
				resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
				resource.setProviderUuid(driver.getUuid());
				context.serializeResource(wrapper);
				
				return TO_SENDING_INITIAL_NODE_RESPONSE;
			}
		}
		return TO_DEFINING_TYPE;
	}
}
