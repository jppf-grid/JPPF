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

import org.apache.mina.core.session.IoSession;
import org.jppf.server.*;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.mina.MinaContext;

/**
 * Common abstract superclass for all states of a node that executes tasks. 
 * @author Laurent Cohen
 */
public abstract class NodeServerState
{
	/**
	 * The server that handles this state.
	 */
	protected MinaNodeServer server = null;
	/**
	 * The driver stats manager.
	 */
	protected JPPFDriverStatsManager statsManager = null;
	/**
	 * The job manager.
	 */
	protected JPPFJobManager jobManager = null;

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public NodeServerState(MinaNodeServer server)
	{
		this.server = server;
		statsManager = JPPFDriver.getInstance().getStatsManager();
		jobManager = JPPFDriver.getInstance().getJobManager();
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

	/**
	 * Get the node context attahced tot he specified session.
	 * @param session the IO session to get the context from.
	 * @return a <code>NodeContext</code> instance.
	 */
	public NodeContext getContext(IoSession session)
	{
		return (NodeContext) session.getAttribute(MinaContext.SESSION_CONTEXT_KEY);
	}
}
