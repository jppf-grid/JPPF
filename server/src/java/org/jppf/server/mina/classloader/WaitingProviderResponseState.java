/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

import static org.jppf.server.nio.classloader.ClassTransition.*;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.mina.MinaContext;

/**
 * This class represents the state of waiting for the response from a provider.
 * @author Laurent Cohen
 */
public class WaitingProviderResponseState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(WaitingProviderResponseState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public WaitingProviderResponseState(MinaClassServer server)
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
		IoSession destination = context.getCurrentRequest();
		if (debugEnabled) log.debug("read response from provider: session " + session.getId() +
			" complete, sending to node session " + destination.getId() + ", resource: " + context.getResource().getName());
		JPPFResourceWrapper resource = context.deserializeResource();
		// putting the definition in cache
		if ((resource.getDefinition() !=  null) && (resource.getCallable() == null))
			((MinaClassServer) server).setCacheContent(context.getUuid(), resource.getName(), resource.getDefinition());
		// fowarding it to channel that requested
		ClassContext destinationContext = getContext(destination);
		resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
		destinationContext.setMessage(null);
		destinationContext.setResource(resource);
		destinationContext.serializeResource();
		context.setMessage(null);
		context.setCurrentRequest(null);
		server.transitionSession(destination, TO_SENDING_NODE_RESPONSE);
		destination.write(destinationContext.getMessage());
		if (!context.getPendingRequests().isEmpty())
		{
			server.transitionSession(session, TO_SENDING_PROVIDER_REQUEST);
			ClassServerState s = (ClassServerState) server.getFactory().getState(context.getState());
			session.setAttribute(MinaContext.TRANSITION_STARTED, s.startTransition(session));
			session.write(context);
		}
		else
		{
			server.transitionSession(session, TO_IDLE_PROVIDER);
		}
	}
}
