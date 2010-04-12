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

import static org.jppf.server.nio.classloader.ClassTransition.*;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;

/**
 * This class represents the state of sending a request to a provider.
 * @author Laurent Cohen
 */
class SendingProviderRequestState extends ClassServerState
{
	/**
	 * Logger for this class.
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
	public SendingProviderRequestState(MinaClassServer server)
	{
		super(server);
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
		ClassContext context = getContext(session);
		/*
		if (!session.isReaderIdle())
		{
			((MinaClassServer) server).removeProviderConnection(context.getUuid(), session);
			IoSession currentRequest = context.getCurrentRequest();
			if (debugEnabled) log.debug("provider: session" + session.getId() + " sending null response for disconnected provider");
			if ((currentRequest != null) || !context.getPendingRequests().isEmpty())
			{
				if (currentRequest != null)
				{
					if (debugEnabled) log.debug("provider: " + session.getId() +
						" disconnected while serving request [" + context.getResource().getName() +
						"] for node session " + context.getCurrentRequest().getId());
					context.setCurrentRequest(null);
					sendNullResponse(currentRequest);
				}
				for (int i=0; i<context.getPendingRequests().size(); i++)
					sendNullResponse(context.getPendingRequests().remove(0));
			}
			throw new ConnectException("provider " + session.getId() + " has been disconnected");
		}
		*/
		if ((context.getCurrentRequest() == null) && !context.getPendingRequests().isEmpty())
		{
			IoSession request = context.getPendingRequests().remove(0);
			ClassContext requestContext = getContext(request);
			context.setMessage(null);
			context.setResource(requestContext.getResource());
			if (debugEnabled)
			{
				log.debug("provider session " + session.getId() + " serving new resource request [" +
					context.getResource().getName() + "] from node: session " + request.getId());
			}
			context.serializeResource();
			context.setCurrentRequest(request);
		}
		if (context.getCurrentRequest() == null)
		{
			if (debugEnabled) log.debug("provider: " + session.getId() + " has no request to process, returning to idle mode");
			context.setMessage(null);
			server.transitionSession(session, TO_IDLE_PROVIDER);
			return false;
		}
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
		ClassContext context = getContext(session);
		if (debugEnabled)
		{
			log.debug("request sent to the provider " + session.getId() + " from node " + context.getCurrentRequest().getId() + 
					", resource: " + context.getResource().getName() + ", requestUuid = " + context.getResource().getRequestUuid());
		}
		//context.setMessage(new ClassServerMessage());
		context.setMessage(null);
		server.transitionSession(session, TO_WAITING_PROVIDER_RESPONSE);
	}
}
