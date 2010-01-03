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
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.server.mina.MinaContext;

/**
 * Tranforms IO messages and into <code>BundleWrapper</code> instances and vice-versa.
 * @author Laurent Cohen
 */
public class NodeIoFilter extends IoFilterAdapter
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
		//if (debugEnabled) log.debug("session: " + session); 
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.SESSION_CONTEXT_KEY);
		if (context.getNodeMessage() == null) context.setNodeMessage(new NodeMessage());
		IoBuffer buffer = (IoBuffer) message;
		boolean b = context.getNodeMessage().read(buffer);
		if (debugEnabled) log.debug("session " + uuid(session) + " : read " + context.getNodeMessage().count + " bytes from buffer, read complete: " + b); 
		session.setAttribute("readComplete", b);
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
		//if (debugEnabled) log.debug("session: " + session);
		IoBuffer buffer = IoBuffer.allocate(SocketWrapper.SOCKET_RECEIVE_BUFFER_SIZE);
		NodeContext context = (NodeContext) session.getAttribute(MinaContext.SESSION_CONTEXT_KEY);
		if (context.getNodeMessage() == null) context.setNodeMessage(new NodeMessage());
		boolean b = context.getNodeMessage().write(buffer);
		if (debugEnabled) log.debug("session " + uuid(session) + " : written " + context.getNodeMessage().count + " bytes to buffer, write complete: " + b); 
		session.setAttribute("writeComplete", b);
		if (buffer.position() > 0)
		{
			buffer.flip();
			nextFilter.filterWrite(session, new DefaultWriteRequest(buffer));
		}
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
		if (debugEnabled) log.debug("session: " + session);
		//Boolean b = (Boolean) session.getAttribute("writeComplete", Boolean.TRUE);
		//if (b)
		{
			NodeContext context = (NodeContext) session.getAttribute(MinaContext.SESSION_CONTEXT_KEY);
			nextFilter.messageSent(session, new DefaultWriteRequest(context.getBundle()));
		}
	}

	/**
	 * Get the uuid of the specified session.
	 * @param session the session to look up. 
	 * @return the uuid as a string.
	 */
	private String uuid(IoSession session)
	{
		return (String) session.getAttribute(MinaContext.SESSION_UUID_KEY);
	}
}
