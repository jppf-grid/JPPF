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

import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.io.BundleWrapper;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.server.scheduler.bundle.Bundler;

/**
 * This class represents the state of waiting for some action.
 * @author Laurent Cohen
 */
public class SendingBundleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SendingBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public SendingBundleState(MinaNodeServer server)
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
		/*
		if (!session.isReaderIdle())
		{
			throw new ConnectException("node " + getRemoteHost(session.getRemoteAddress()) + " has been disconnected");
		}
		*/
		NodeContext context = getContext(session);
		if (debugEnabled) log.debug("session " + session.getId() + " starting " + context.getState());
		if (context.getNodeMessage() == null)
		{
			// check whether the bundler settings have changed.
			if (context.getBundler().getTimestamp() < server.getBundler().getTimestamp())
			{
				context.getBundler().dispose();
				Bundler bundler = server.getBundler().copy();
				bundler.setup();
				context.setBundler(bundler);
			}
			BundleWrapper bundleWrapper = context.getBundle();
			JPPFTaskBundle bundle = (bundleWrapper == null) ? null : bundleWrapper.getBundle();
			if (bundle != null)
			{
				if (debugEnabled) log.debug("got bundle from the queue for " + getRemoteHost(session.getRemoteAddress()));
				// to avoid cycles in peer-to-peer routing of jobs.
				if (bundle.getUuidPath().contains(context.getUuid()))
				{
					if (debugEnabled) log.debug("cycle detected in peer-to-peer bundle routing: " + bundle.getUuidPath().getList());
					context.resubmitBundle(bundleWrapper);
					context.setBundle(null);
					server.addIdleChannel(session);
					server.transitionSession(session, TO_IDLE);
					return false;
				}
				else
				{
					bundle.setExecutionStartTime(System.currentTimeMillis());
					context.serializeBundle();
				}
			}
			else
			{
				server.transitionSession(session, TO_IDLE);
				server.addIdleChannel(session);
				return false;
			}
		}
		session.setAttribute("transitionStarted", true);
		session.write(context.getBundle());
		//WriteRequestQueue queue = session.getWriteRequestQueue();
		//queue.offer(session, new DefaultWriteRequest(context.getBundle()));
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
		if (debugEnabled) log.debug("session " + session.getId() + ": ending " + context.getState() + ", sent entire bundle to node " + getRemoteHost(session.getRemoteAddress()));
		context.setNodeMessage(null);
		//JPPFDriver.getInstance().getJobManager().jobDispatched(context.getBundle(), channel);
		server.transitionSession(session, TO_WAITING);
	}
}
