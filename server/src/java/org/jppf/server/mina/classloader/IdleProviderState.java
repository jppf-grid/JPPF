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

import org.apache.mina.core.session.IoSession;

/**
 * This class represents the state of waiting for the next request for a provider.
 * @author Laurent Cohen
 */
public class IdleProviderState extends ClassServerState
{
	/**
	 * Initialize this state with a specified NioServer.
	 * @param server the NioServer this state relates to.
	 */
	public IdleProviderState(MinaClassServer server)
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
		/*
		if (!session.isReaderIdle())
		{
			ClassContext context = getContext(session);
			((MinaClassServer) server).removeProviderConnection(context.getUuid(), session);
			throw new ConnectException("provider session " + session.getId() + " has been disconnected");
		}
		*/
		return true;
	}
}
