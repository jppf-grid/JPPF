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

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.server.nio.classloader.ClassTransition;

/**
 * State of sending the initial response to a newly created node channel.
 * @author Laurent Cohen
 */
public class SendingInitialResponseState extends ClassServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendingInitialResponseState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public SendingInitialResponseState(MinaClassServer server)
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
		if (debugEnabled) log.debug("session " + session.getId());
		/*
		if (!session.isReaderIdle())
		{
			throw new ConnectException("node session" + session.getId() + " has been disconnected");
		}
		*/
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
		if (debugEnabled) log.debug("sent uuid to node: session " + session.getId());
		context.setMessage(null);
		server.transitionSession(session, ClassTransition.TO_WAITING_NODE_REQUEST);
	}
}
