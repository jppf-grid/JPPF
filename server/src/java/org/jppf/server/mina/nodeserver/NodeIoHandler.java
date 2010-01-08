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

package org.jppf.server.mina.nodeserver;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.*;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.jppf.server.mina.MinaContext;
import org.jppf.server.nio.nodeserver.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeIoHandler extends IoHandlerAdapter
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeIoHandler.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The node server.
	 */
	private MinaNodeServer server = null;
	/**
	 * Count of sessions created.
	 */
	private static AtomicInteger sessionCount = new AtomicInteger(0);

	/**
	 * Initialize this io handler with the specified node server.
	 * @param server the node server.
	 */
	public NodeIoHandler(MinaNodeServer server)
	{
		this.server = server;
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
		if (debugEnabled) log.debug("session: " + session); 
		Boolean b = (Boolean) session.getAttribute("readComplete", Boolean.TRUE);
		Boolean transitionStarted = (Boolean) session.getAttribute("transitionStarted", Boolean.TRUE);
		if (b && transitionStarted) endTransition(session);
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
		if (debugEnabled) log.debug("session: " + session);
		Boolean transitionStarted = (Boolean) session.getAttribute("transitionStarted", Boolean.TRUE);
		if (transitionStarted)
		{
			Boolean b = (Boolean) session.getAttribute("writeComplete", Boolean.TRUE);
			if (!b) session.write(message);
			else endTransition(session);
		}
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
		NodeContext context = new NodeContext();
		session.setAttribute(MinaContext.SESSION_CONTEXT_KEY, context);
		context.setBundle(server.getInitialBundle());
		server.transitionSession(session, NodeTransition.TO_SEND_INITIAL);
		NodeServerState state = server.factory.getState(context.getState());
		session.setAttribute("transitionStarted", state.startTransition(session));
	}

	/**
	 * Perform the final step of a state transition.
	 * @param session the session representing the connection to the node.
	 * @throws Exception if any error occurs.
	 */
	private void endTransition(IoSession session) throws Exception
	{
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.SESSION_CONTEXT_KEY);
		NodeState s = context.getState();
		NodeServerState state = server.factory.getState(s);
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
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.SESSION_CONTEXT_KEY);
		context.handleException(session);
		log.error("session " + session.getId() + " : " + cause.getMessage(), new Exception(cause));
	}
}
