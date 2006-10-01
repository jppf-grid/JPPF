/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.server.node;

import static org.jppf.server.JPPFStatsUpdater.isStatsEnabled;
import static org.jppf.server.JPPFStatsUpdater.taskExecuted;
import java.io.*;
import java.nio.channels.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.server.*;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.utils.*;

/**
 * This class implements the state of receiving the bundle back from processing at a node. The "waiting" part is
 * provided by the selector, which will any for the first bytes be ready to send to the instances of this class.
 * @author Domingos Creado
 */
class CWaitingResult implements ChannelState
{
	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(CWaitingResult.class);
	/**
	 * Determines whther DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The JPPFNIOServer this state relates to.
	 */
	private JPPFNodeServer server;

	/**
	 * Initialize this state with a specified JPPFNIOServer.
	 * @param server the JPPFNIOServer this state relates to.
	 */
	CWaitingResult(JPPFNodeServer server)
	{
		this.server = server;
	}

	/**
	 * Receive the results of a task bundle execution.
	 * @param key the selector key associated with the socket channel to the node.
	 * @param context a container for the execution results.
	 * @see org.jppf.server.ChannelState#exec(java.nio.channels.SelectionKey, org.jppf.server.ChannelContext)
	 */
	public void exec(SelectionKey key, ChannelContext context)
	{
		SocketChannel channel = (SocketChannel) key.channel();
		if (debugEnabled) log.debug("exec() for "+server.getRemostHost(channel));
		NodeChannelContext nodeContext = (NodeChannelContext) context;
		TaskRequest out = (TaskRequest) nodeContext.content;
		JPPFTaskBundle bundle = out.getBundle();
		Request request = out.getRequest();
		TaskCompletionListener listener = bundle.getCompletionListener();
		try
		{
			// Wait the full byte[] of the bundle come to start processing.
			// This makes the integration of non-blocking with ObjectInputStream easier.
			if (server.fillRequest(channel, out.getRequest()))
			{
				long elapsed = System.currentTimeMillis() - bundle.getExecutionStartTime();
				DataInputStream dis = new DataInputStream(new ByteArrayInputStream(request.getOutput().toByteArray()));
				// reading the bundle as object
				SerializationHelper helper = new SerializationHelperImpl();
				bundle = (JPPFTaskBundle) helper.readNextObject(dis, false);
				List<byte[]> taskList = new ArrayList<byte[]>();
				for (int i = 0; i < bundle.getTaskCount(); i++)
				{
					taskList.add(helper.readNextBytes(dis));
				}
				dis.close();
				bundle.setTasks(taskList);
				// updating stats
				if (isStatsEnabled())
				{
					nodeContext.bundler.feedback(bundle.getTaskCount(), elapsed);
					taskExecuted(bundle.getTaskCount(), elapsed, bundle.getNodeExecutionTime(), out.getBundleBytes());
				}
				// notifing the client thread about the end of a bundle
				if (listener != null) listener.taskCompleted(bundle);
				// there is nothing to do, so this instance will wait for job
				// make sure the context is reset so as not to resubmit
				// the last bundle executed by the node.
				context.content = null;
				// if the node disconnect from driver we will know soon
				context.state = server.SendingJob;
				server.availableNodes.add(channel);
				//key.interestOps(SelectionKey.OP_READ);
				key.interestOps(0);
				if (!server.getQueue().isEmpty()) server.newBundle(server.getQueue());
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			if (e instanceof IOException)
			{
				if (debugEnabled) log.debug("closing channel");
				server.closeNode(channel);
			}
			if ((bundle != null) && !JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				if (debugEnabled) log.debug("resubmitting bundle");
				server.resubmitBundle(bundle);
			}
		}
	}
}