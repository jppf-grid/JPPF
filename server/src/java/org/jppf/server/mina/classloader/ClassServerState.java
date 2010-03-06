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

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.server.mina.*;
import org.jppf.server.nio.classloader.ClassTransition;

/**
 * Common abstract superclass for all states of a node that executes tasks. 
 * @author Laurent Cohen
 */
public abstract class ClassServerState extends MinaState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ClassServerState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	protected ClassServerState(MinaClassServer server)
	{
		super(server);
	}

	/**
	 * Get the node context attahced tot he specified session.
	 * @param session the IO session to get the context from.
	 * @return a <code>NodeContext</code> instance.
	 */
	protected ClassContext getContext(IoSession session)
	{
		return (ClassContext) session.getAttribute(MinaContext.CONTEXT);
	}

	/**
	 * Send a null response to a request node connection. This method is called for a provider
	 * that was disconnected but still has pending requests, so as not to block the requesting channels.
	 * @param request the selection key wrapping the requesting channel.
	 * @throws Exception if an error occurs while setting the new requester's state.
	 */
	protected void sendNullResponse(IoSession request) throws Exception
	{
		if (debugEnabled) log.debug("disconnected provider: sending null response to node " + request.getId());
		ClassContext requestContext = getContext(request);
		requestContext.getResource().setDefinition(null);
		requestContext.serializeResource();
		server.transitionSession(request, ClassTransition.TO_SENDING_NODE_RESPONSE);
		request.write(requestContext.getMessage());
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @return true if the transition could be applied, false otherwise. If true, then <code>endTransition()</code> will be called.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.mina.MinaState#startTransition(org.apache.mina.core.session.IoSession)
	 */
	public boolean startTransition(IoSession session) throws Exception
	{
		return true;
	}

	/**
	 * End the transition associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.mina.MinaState#endTransition(org.apache.mina.core.session.IoSession)
	 */
	public void endTransition(IoSession session) throws Exception
	{
	}
}
