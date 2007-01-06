/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jppf.server.nio.nodeserver;

import static org.jppf.server.JPPFStatsUpdater.*;
import static org.jppf.server.nio.nodeserver.NodeTransition.*;

import java.nio.channels.*;

import org.apache.log4j.Logger;
import org.jppf.server.JPPFTaskBundle;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.utils.StringUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class WaitingResultsState extends NodeServerState
{
	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(WaitingResultsState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
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
		SocketChannel channel = (SocketChannel) key.channel();
		NodeContext context = (NodeContext) key.attachment();
		if (debugEnabled) log.debug("exec() for " + StringUtils.getRemostHost(channel));
		JPPFTaskBundle bundle = context.getBundle();
		TaskCompletionListener listener = bundle.getCompletionListener();

		// Wait the full byte[] of the bundle come to start processing.
		// This makes the integration of non-blocking with ObjectInputStream easier.
		if (context.readMessage(channel))
		{
			long elapsed = System.currentTimeMillis() - bundle.getExecutionStartTime();
			bundle = context.deserializeBundle();
			// updating stats
			if (isStatsEnabled())
			{
				taskExecuted(bundle.getTaskCount(), elapsed, bundle.getNodeExecutionTime(), context.getMessage().length);
				context.getBundler().feedback(bundle.getTaskCount(), elapsed);
			}
			// notifing the client thread about the end of a bundle
			if (listener != null) listener.taskCompleted(bundle);
			// there is nothing to do, so this instance will wait for a task bundle
			// make sure the context is reset so as not to resubmit the last bundle executed by the node.
			context.setMessage(null);
			context.setBundle(null);
			server.addIdleChannel(channel);
			return TRANSITION_TO_IDLE;
		}
		return TRANSITION_TO_WAITING;
	}
}
