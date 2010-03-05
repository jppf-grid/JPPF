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

package org.jppf.server.mina;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;

/**
 * This class represents the state of sending the initial hand-shaking data to a newly connected node.
 * @author Laurent Cohen
 */
public abstract class MinaState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(MinaState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The server that handles this state.
	 */
	protected MinaServer server = null;

	/**
	 * Initialize this state.
	 */
	public MinaState()
	{
	}

	/**
	 * Initialize this state.
	 * @param server the server to which this state applies.
	 */
	public MinaState(MinaServer server)
	{
		this.server = server;
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @return true if the transition could be applied, false otherwise. If true, then <code>endTransition()</code> will be called.
	 * @throws Exception if an error occurs while transitioning to another state.
	 */
	public abstract boolean startTransition(IoSession session) throws Exception;


	/**
	 * End the transition associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @throws Exception if an error occurs while transitioning to another state.
	 */
	public abstract void endTransition(IoSession session) throws Exception;
}
