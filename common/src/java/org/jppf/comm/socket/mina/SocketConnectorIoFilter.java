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

package org.jppf.comm.socket.mina;

import org.apache.commons.logging.*;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.*;

/**
 * Tranforms IO messages and into <code>BundleWrapper</code> instances and vice-versa.
 * @author Laurent Cohen
 */
public class SocketConnectorIoFilter extends IoFilterAdapter
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SocketConnectorIoFilter.class);
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
		if (debugEnabled) log.debug("session " + session.getId() + ", message = " + message); 
		if (message != null) nextFilter.messageReceived(session, message);
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
		if (debugEnabled) log.debug("session " + session.getId() + ", message = " + request.getMessage()); 
		nextFilter.filterWrite(session, new DefaultWriteRequest(request.getMessage()));
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
		IoBuffer message = (IoBuffer) request.getMessage();
		if (debugEnabled) log.debug("session " + session.getId() + ", message = " + message);
		//if (!message.hasRemaining())
		nextFilter.messageSent(session, new DefaultWriteRequest(message));
	}
}
