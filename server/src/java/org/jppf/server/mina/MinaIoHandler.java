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

package org.jppf.server.mina;

import org.apache.commons.logging.*;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

/**
 * 
 * @param <S> the type of the states to use.
 * @param <T> the type of the transitions to use.
 * @author Laurent Cohen
 */
public abstract class MinaIoHandler<S extends Enum<S>, T extends Enum<T>> extends IoHandlerAdapter
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(MinaIoHandler.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The server holding this handler.
	 */
	protected MinaServer<S, T> server = null;

	/**
	 * Initialize this io handler.
	 * @param server the server holding this handler.
	 */
	public MinaIoHandler(MinaServer<S, T> server)
	{
		this.server = server;
	}

	/**
	 * Perform the final step of a state transition.
	 * @param session the session representing the connection to the node.
	 * @throws Exception if any error occurs.
	 */
	protected void endTransition(IoSession session) throws Exception
	{
		MinaContext<S> context = (MinaContext<S>) session.getAttribute(MinaContext.CONTEXT);
		S s = context.getState();
		MinaState state = server.factory.getState(s);
		state.endTransition(session);
	}

	/**
	 * Invoked when an exception is caught during IO processing.
	 * @param session the session for which the exception occurred.
	 * @param cause the cause exception.
	 * @throws Exception if any error occurs while processing the exception.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#exceptionCaught(org.apache.mina.core.session.IoSession, java.lang.Throwable)
	 */
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception
	{
		MinaContext context = (MinaContext) session.getAttribute(MinaContext.CONTEXT);
		context.handleException(session);
		log.error("session " + session.getId() + " : " + cause.getMessage(), cause);
	}

	/**
	 * Called when a session is closed.
	 * @param session the session that was closed.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#sessionClosed(org.apache.mina.core.session.IoSession)
	 */
	public void sessionClosed(IoSession session) throws Exception
	{
		if (debugEnabled) log.debug("session " + session.getId() + " closed");
	}
}
