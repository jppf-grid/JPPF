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
import org.jppf.server.nio.classloader.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ClassIoHandler extends MinaIoHandler<ClassState, ClassTransition>
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ClassIoHandler.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this io handler with the specified node server.
	 * @param server the node server.
	 */
	public ClassIoHandler(MinaClassServer server)
	{
		super(server);
	}

	/**
	 * Invoked when a message has been received.
	 * @param session the session to which the message applies.
	 * @param message the message that was received.
	 * @throws Exception if any error occurs while processing the message.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#messageReceived(org.apache.mina.core.session.IoSession, java.lang.Object)
	 */
	public void messageReceived(IoSession session, Object message) throws Exception
	{
		Boolean readComplete = (Boolean) session.getAttribute(MinaContext.READ_COMPLETE, Boolean.TRUE);
		Boolean transitionStarted = (Boolean) session.getAttribute(MinaContext.TRANSITION_STARTED, Boolean.TRUE);
		if (debugEnabled) log.debug("session " + session.getId() + " : transitionStarted = " + transitionStarted + ", readComplete = " + readComplete);
		if (readComplete && transitionStarted) endTransition(session);
	}

	/**
	 * Invoked when a message has been sent.
	 * @param session the session to which the message applies.
	 * @param message the message that was sent.
	 * @throws Exception if any error occurs while processing the message.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#messageSent(org.apache.mina.core.session.IoSession, java.lang.Object)
	 */
	public void messageSent(IoSession session, Object message) throws Exception
	{
		if (debugEnabled) log.debug("    session " + session.getId());
		endTransition(session);
		ClassContext context = (ClassContext) session.getAttribute(MinaContext.CONTEXT);
		ClassServerState state = (ClassServerState) server.getFactory().getState((ClassState) context.getState());
		session.setAttribute(MinaContext.TRANSITION_STARTED, state.startTransition(session));
	}

	/**
	 * Invoked when a session has been created and connected to a remote peer.
	 * @param session the created session.
	 * @throws Exception if any error occurs while processing the new session.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#sessionOpened(org.apache.mina.core.session.IoSession)
	 */
	public void sessionOpened(IoSession session) throws Exception
	{
		if (debugEnabled) log.debug("session: " + session); 
		ClassContext context = new ClassContext();
		session.setAttribute(MinaContext.CONTEXT, context);
		server.transitionSession(session, ClassTransition.TO_DEFINING_TYPE);
		ClassServerState state = (ClassServerState) server.getFactory().getState((ClassState) context.getState());
		session.setAttribute(MinaContext.TRANSITION_STARTED, state.startTransition(session));
	}

	/**
	 * Called when a session is created.
	 * @param session the session that was created.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#sessionCreated(org.apache.mina.core.session.IoSession)
	 */
	public void sessionCreated(IoSession session) throws Exception
	{
		if (debugEnabled) log.debug("session " + session.getId() + " created");
	}

	/**
	 * Invoked when an exception is caught during IO processing.
	 * @param session the session for which the exception occurred.
	 * @param cause the cause exception.
	 * @throws Exception if any error occurs while processing the exception.
	 * @see org.jppf.server.mina.MinaIoHandler#exceptionCaught(org.apache.mina.core.session.IoSession, java.lang.Throwable)
	 */
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception
	{
		log.error("session " + session.getId() + " : " + cause.getMessage(), cause);
		ClassContext context = (ClassContext) session.getAttribute(MinaContext.CONTEXT);
		ClassState state = (ClassState) context.getState();
		if (ClassState.WAITING_PROVIDER_RESPONSE.equals(state) || ClassState.SENDING_PROVIDER_REQUEST.equals(state))
		{
			MinaClassServer server = (MinaClassServer) this.server;
			ClassServerState serverState = (ClassServerState) server.getFactory().getState(state);
			if (debugEnabled) log.debug("an exception occurred while reading response from provider: session " + session.getId());
			((MinaClassServer) server).removeProviderConnection(context.getUuid(), session);
			IoSession currentRequest = context.getCurrentRequest();
			if ((currentRequest != null) || !context.getPendingRequests().isEmpty())
			{
				if (debugEnabled) log.debug("provider: session " + session.getId() + " sending null response for disconnected provider");
				if (currentRequest != null)
				{
					context.setCurrentRequest(null);
					serverState.sendNullResponse(currentRequest);
				}
				while (!context.getPendingRequests().isEmpty())
					serverState.sendNullResponse(context.getPendingRequests().remove(0));
			}
		}
		context.handleException(session);
	}
}
