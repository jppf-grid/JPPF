/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.JPPFStatsUpdater.*;
import static org.jppf.server.nio.nodeserver.NodeTransition.*;
import static org.jppf.utils.StringUtils.getRemoteHost;

import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.io.BundleWrapper;
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
	public WaitingResultsState(NodeNioServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param key the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public NodeTransition performTransition(SelectionKey key) throws Exception
	{
		SelectableChannel channel = key.channel();
		NodeContext context = (NodeContext) key.attachment();
		//if (debugEnabled) log.debug("exec() for " + getRemoteHost(channel));

		// Wait the full byte[] of the bundle come to start processing.
		// This makes the integration of non-blocking with ObjectInputStream easier.
		if (context.getNodeMessage() == null) context.setNodeMessage(new NodeMessage());
		if (context.getNodeMessage().read((ReadableByteChannel) channel))
		{
			if (debugEnabled) log.debug("read bundle from node " + getRemoteHost(channel) + " done");
			BundleWrapper bundleWrapper = context.getBundle();
			JPPFTaskBundle bundle = bundleWrapper.getBundle();
			TaskCompletionListener listener = bundle.getCompletionListener();
			BundleWrapper newBundleWrapper = context.deserializeBundle();
			JPPFTaskBundle newBundle = newBundleWrapper.getBundle();
			long elapsed = System.currentTimeMillis() - bundle.getExecutionStartTime();
			// if an exception prevented the node from executing the tasks
			if (newBundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM) != null)
			{
				newBundle.setTasks(bundle.getTasks());
				newBundle.setTaskCount(bundle.getTaskCount());
			}
			// updating stats
			else if (isStatsEnabled())
			{
				if (newBundle.getNodeExecutionTime() > 1000000)
				{
					int breakpoint = 0;
				}
				taskExecuted(newBundle.getTaskCount(), elapsed, newBundle.getNodeExecutionTime(), context.getNodeMessage().getLength());
				context.getBundler().feedback(newBundle.getTaskCount(), elapsed);
			}
			// notifing the client thread about the end of a bundle
			if (listener != null) listener.taskCompleted(newBundleWrapper);
			// there is nothing to do, so this instance will wait for a task bundle
			// make sure the context is reset so as not to resubmit the last bundle executed by the node.
			context.setNodeMessage(null);
			context.setBundle(null);
			server.addIdleChannel(channel);
			return TO_IDLE;
		}
		return TO_WAITING;
	}
}
