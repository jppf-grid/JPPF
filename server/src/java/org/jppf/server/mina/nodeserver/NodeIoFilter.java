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

import java.net.ConnectException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.*;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.*;
import org.jppf.server.mina.MinaContext;
import org.jppf.server.nio.nodeserver.NodeState;
import org.jppf.utils.StringUtils;

/**
 * Tranforms IO messages into <code>BundleWrapper</code> instances and vice-versa.
 * @author Laurent Cohen
 */
class NodeIoFilter extends IoFilterAdapter
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeIoFilter.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Handle the reception of a new message.
	 * @param nextFilter next filter to forward to.
	 * @param session the session to which the message applies.
	 * @param message the message object.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageReceived(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, java.lang.Object)
	 */
	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception
	{
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.CONTEXT);
		NodeState state = context.getState();
		if (NodeState.SEND_INITIAL_BUNDLE.equals(state) || NodeState.SENDING_BUNDLE.equals(state))
		{
			throw new ConnectException("Node " + StringUtils.getRemoteHost(session.getRemoteAddress()) + " has been disconnected");
		}
		if (context.getNodeMessage() == null) context.setNodeMessage(new NodeMessage());
		IoBuffer buffer = (IoBuffer) message;
		boolean complete = context.getNodeMessage().read(buffer);
		if (debugEnabled) log.debug(" session " + session.getId() + " : read " + context.getNodeMessage().count + " bytes from buffer, read complete: " + complete +
			", message = " + buffer.flip());
		session.setAttribute(MinaContext.READ_COMPLETE, complete);
		nextFilter.messageReceived(session, context.getBundle());
	}

	/**
	 * Send a message.
	 * @param nextFilter filter to forward to.
	 * @param session the session to which the message applies.
	 * @param request encapsulates the data to send.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.filterchain.IoFilterAdapter#filterWrite(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
	 */
	public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception
	{
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.CONTEXT);
		if (context.getNodeMessage() == null) context.setNodeMessage(new NodeMessage());
		int size = 32 * 1024;
		ByteBuffer toWrap = ByteBuffer.allocate(size);
		IoBuffer buffer = IoBuffer.wrap(toWrap);
		boolean complete = context.getNodeMessage().write(buffer, session.getId());
		session.setAttribute(MinaContext.WRITE_COMPLETE, complete);
		buffer.flip();
		if (debugEnabled) log.debug("     session " + session.getId() + " : write complete: " + complete + ", written " + buffer.limit() +
			" bytes, message = " + request.getMessage() + ", writeCount = " + context.getNodeMessage().writeCount + ", buffer = " + buffer + ", writeSuspended = " + session.isWriteSuspended());
		DefaultWriteRequest newRequest = new DefaultWriteRequest(buffer, request.getFuture(), request.getDestination());
		nextFilter.filterWrite(session, newRequest);
		if (debugEnabled) log.debug("session " + session.getId() + " : after nextFilter.write()");
	}

	/**
	 * Notify that a message was sent.
	 * @param nextFilter filter to forward to.
	 * @param session the session to which the message applies.
	 * @param request encapsulates the data to send.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.filterchain.IoFilterAdapter#messageSent(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession, org.apache.mina.core.write.WriteRequest)
	 */
	public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception
	{
		if (debugEnabled) log.debug("    session " + session.getId() + " : message = " + request.getMessage());
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.CONTEXT);

		Boolean transitionStarted = (Boolean) session.getAttribute(MinaContext.TRANSITION_STARTED, Boolean.TRUE);
		Boolean writeComplete = (Boolean) session.getAttribute(MinaContext.WRITE_COMPLETE, Boolean.TRUE);
		if (transitionStarted)
		{
			if (!writeComplete)
			{
				// write next message chunk
				session.write(context.getBundle());
			}
			else
			{
				nextFilter.messageSent(session, new DefaultWriteRequest(context.getBundle(), request.getFuture(), request.getDestination()));
			}
		}
	}
}
