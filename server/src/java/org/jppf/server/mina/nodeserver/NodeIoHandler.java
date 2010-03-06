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

package org.jppf.server.mina.nodeserver;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.mina.*;
import org.jppf.server.nio.nodeserver.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeIoHandler extends MinaIoHandler
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
	 * Initialize this io handler with the specified node server.
	 * @param server the node server.
	 */
	public NodeIoHandler(MinaNodeServer server)
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
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.CONTEXT);
		NodeServerState state = (NodeServerState) server.getFactory().getState(context.getState());
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
		NodeContext context = new NodeContext();
		session.setAttribute(MinaContext.CONTEXT, context);
		context.setBundle(((MinaNodeServer) server).getInitialBundle());
		server.transitionSession(session, NodeTransition.TO_SEND_INITIAL);
		NodeServerState state = (NodeServerState) server.getFactory().getState(context.getState());
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
		JPPFDriver.getInstance().getStatsManager().newNodeConnection();
	}
}
