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

package org.jppf.server.mina.classloader;

import static org.jppf.node.JPPFResourceWrapper.State.*;
import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.Vector;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.utils.JPPFConfiguration;

/**
 * This class represents the state of a new class server connection, whose type is yet undetermined.
 * @author Laurent Cohen
 */
public class DefiningChannelTypeState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(DefiningChannelTypeState.class);
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
	public DefiningChannelTypeState(MinaClassServer server)
	{
		super(server);
	}

	/**
	 * End the transition associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.mina.MinaState#endTransition(org.apache.mina.core.session.IoSession)
	 */
	public void endTransition(IoSession session) throws Exception
	{
		ClassContext context = getContext(session);
		JPPFResourceWrapper resource = context.deserializeResource();
		if (debugEnabled) log.debug("session " + session.getId() + " read resource [" + resource.getName() + "] done");
		if (PROVIDER_INITIATION.equals(resource.getState()))
		{
			if (debugEnabled) log.debug("initiating provider: session " + session.getId());
			String uuid = resource.getUuidPath().getFirst();
			// it is a provider
			((MinaClassServer) server).addProviderConnection(uuid, session);
			context.setUuid(uuid);
			context.setPendingRequests(new Vector<IoSession>());
			context.setMessage(null);
			if (managementEnabled)
			{
				resource.setManagementId(driver.getJmxServer().getId());
			}
			context.serializeResource();
			server.transitionSession(session, TO_SENDING_INITIAL_PROVIDER_RESPONSE);
			session.write(context.getMessage());
		}
		else if (NODE_INITIATION.equals(resource.getState()))
		{
			if (debugEnabled) log.debug("initiating node: session " + session.getId());
			// send the uuid of this driver to the node or node peer.
			resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
			resource.setProviderUuid(driver.getUuid());
			context.serializeResource();
			server.transitionSession(session, TO_SENDING_INITIAL_NODE_RESPONSE);
			session.write(context.getMessage());
		}
	}
}
