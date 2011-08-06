/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.net.ConnectException;

import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * This class represents the state of waiting for some action.
 * @author Laurent Cohen
 */
class SendingBundleState extends NodeServerState
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(SendingBundleState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether TRACE logging level is enabled.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * Initialize this state.
	 * @param server the server that handles this state.
	 */
	public SendingBundleState(NodeNioServer server)
	{
		super(server);
	}

	/**
	 * Execute the action associated with this channel state.
	 * @param wrapper the selection key corresponding to the channel and selector for this state.
	 * @return a state transition as an <code>NioTransition</code> instance.
	 * @throws Exception if an error occurs while transitioning to another state.
	 * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
	 */
	public NodeTransition performTransition(ChannelWrapper<?> wrapper) throws Exception
	{
		//if (debugEnabled) log.debug("exec() for " + getRemostHost(channel));
		if (wrapper.isReadable())
		{
			if (!(wrapper instanceof LocalNodeChannel)) throw new ConnectException("node " + wrapper + " has been disconnected");
		}

		AbstractNodeContext context = (AbstractNodeContext) wrapper.getContext();
		if (context.getNodeMessage() == null)
		{
			BundleWrapper bundleWrapper = context.getBundle();
			JPPFTaskBundle bundle = (bundleWrapper == null) ? null : (JPPFTaskBundle) bundleWrapper.getJob();
			if (bundle != null)
			{
				if (debugEnabled) log.debug("got bundle from the queue for " + wrapper);
				// to avoid cycles in peer-to-peer routing of jobs.
				if (bundle.getUuidPath().contains(context.getUuid()))
				{
					if (debugEnabled) log.debug("cycle detected in peer-to-peer bundle routing: " + bundle.getUuidPath().getList());
					context.setBundle(null);
					context.resubmitBundle(bundleWrapper);
					server.addIdleChannel(wrapper);
					return TO_IDLE;
				}
				//bundle.setExecutionStartTime(System.currentTimeMillis());
				bundle.setExecutionStartTime(System.nanoTime());
				context.serializeBundle(wrapper);
			}
			else
			{
				if (debugEnabled) log.debug("null bundle for node " + wrapper);
				server.addIdleChannel(wrapper);
				return TO_IDLE;
			}
		}
		if (context.writeMessage(wrapper))
		{
			if (debugEnabled) log.debug("sent entire bundle" + context.getBundle().getJob() + " to node " + wrapper);
			context.setNodeMessage(null, wrapper);
			//JPPFDriver.getInstance().getJobManager().jobDispatched(context.getBundle(), channel);
			return TO_WAITING;
		}
		if (traceEnabled) log.trace("part yet to send to node " + wrapper);
		return TO_SENDING;
	}
}
