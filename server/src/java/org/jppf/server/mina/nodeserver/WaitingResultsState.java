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

import static org.jppf.server.nio.nodeserver.NodeTransition.TO_IDLE;
import static org.jppf.utils.StringUtils.getRemoteHost;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.server.mina.IoSessionWrapper;
import org.jppf.server.protocol.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node. 
 * @author Laurent Cohen
 */
public class WaitingResultsState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(WaitingResultsState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public WaitingResultsState(MinaNodeServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @return true if the transition could be applied, false otherwise.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public boolean startTransition(IoSession session) throws Exception
	{
		session.setAttribute("transitionStarted", true);
		NodeContext context = getContext(session);
		if (debugEnabled) log.debug("session " + session.getId() + " starting " + context.getState());
		if (context.getNodeMessage() == null) context.setNodeMessage(new NodeMessage());
		return true;
	}


	/**
	 * End the transition associated with this channel state.
	 * @param session the current session to which the state applies.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.mina.nodeserver.NodeServerState#endTransition(org.apache.mina.core.session.IoSession)
	 */
	public void endTransition(IoSession session) throws Exception
	{
		NodeContext context = getContext(session);
		if (debugEnabled) log.debug("session " + session.getId() + " : ending " + context.getState() + ", read bundle from node " + getRemoteHost(session.getRemoteAddress()) + " done");
		BundleWrapper bundleWrapper = context.getBundle();
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		BundleWrapper newBundleWrapper = context.deserializeBundle();
		JPPFTaskBundle newBundle = newBundleWrapper.getBundle();
		// if an exception prevented the node from executing the tasks
		if (newBundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM) != null)
		{
			newBundle.setTasks(bundle.getTasks());
			newBundle.setTaskCount(bundle.getTaskCount());
		}
		// updating stats
		else
		{
			long elapsed = System.currentTimeMillis() - bundle.getExecutionStartTime();
			statsManager.taskExecuted(newBundle.getTaskCount(), elapsed, newBundle.getNodeExecutionTime(), context.getNodeMessage().getLength());
			context.getBundler().feedback(newBundle.getTaskCount(), elapsed);
		}
		jobManager.jobReturned(bundleWrapper, new IoSessionWrapper(session));
		Boolean requeue = (Boolean) newBundle.getParameter(BundleParameter.JOB_REQUEUE);
		if ((requeue != null) && requeue)
		{
			bundle.setParameter(BundleParameter.JOB_REQUEUE, true);
			bundle.getJobSLA().setSuspended(true);
			context.resubmitBundle(bundleWrapper);
		}
		else
		{
			// notify the client thread about the end of a bundle
			TaskCompletionListener listener = bundle.getCompletionListener();
			if (listener != null) listener.taskCompleted(newBundleWrapper);
		}
		// there is nothing left to do, so this instance will wait for a task bundle
		// make sure the context is reset so as not to resubmit the last bundle executed by the node.
		context.setNodeMessage(null);
		context.setBundle(null);
		server.transitionSession(session, TO_IDLE);
		((MinaNodeServer) server).addIdleChannel(session);
	}
}
