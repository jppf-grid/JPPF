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

import org.apache.commons.logging.*;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.*;
import org.jppf.server.JPPFDriver;
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
	 * A single thread used for asynchronous submission of the write operations.
	 * This is used to avoid doing write operaations in the same thread as the
	 * Mina events emitter.
	 */
	//private ExecutorService executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("Node writes executor"));

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
		NodeServerState state = server.factory.getState(context.getState());
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
		context.setBundle(server.getInitialBundle());
		server.transitionSession(session, NodeTransition.TO_SEND_INITIAL);
		NodeServerState state = server.factory.getState(context.getState());
		session.setAttribute(MinaContext.TRANSITION_STARTED, state.startTransition(session));
	}

	/**
	 * Perform the final step of a state transition.
	 * @param session the session representing the connection to the node.
	 * @throws Exception if any error occurs.
	 */
	private void endTransition(IoSession session) throws Exception
	{
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.CONTEXT);
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
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.CONTEXT);
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

	/**
	 * Called when a session becomes idle.
	 * @param session the session that is idle.
	 * @param status the session's idle status.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#sessionIdle(org.apache.mina.core.session.IoSession, org.apache.mina.core.session.IdleStatus)
	 */
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception
	{
		if (debugEnabled) log.debug("session " + session.getId() + " idle, status = " + status);
	}

	/**
	 * Tasks submitted to an executor for asynchronous I/O writes.
	 */
	public class WriteMessageTask implements Runnable
	{
		/**
		 * The session that performs the write operation.
		 */
		private IoSession session = null;
		/**
		 * The message to write.
		 */
		private Object message = null;

		/**
		 * Initialize this task with the specified parameters.
		 * @param session the session that performs the write operation.
		 * @param message the message to write.
		 */
		public WriteMessageTask(IoSession session, Object message)
		{
			this.session = session;
			this.message = message;
		}

		/**
		 * Execute this task.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			session.write(message);
		}
	}
}
