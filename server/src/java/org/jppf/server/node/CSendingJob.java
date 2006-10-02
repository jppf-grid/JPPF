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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import org.apache.log4j.Logger;
import org.jppf.server.*;

/**
 * This class represents the state of waiting for some action.
 * @author Domingos Creado
 */
class CSendingJob implements ChannelState
{
	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(CSendingJob.class);
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
	CSendingJob(JPPFNodeServer server)
	{
		this.server = server;
	}

	/**
	 * Send an execution request to the node.
	 * @param key the selector key associated with the socket channel to the node.
	 * @param context a container for the tasks to execute.
	 * @throws IOException if an error occurred while sending the request.
	 * @see org.jppf.server.ChannelState#exec(java.nio.channels.SelectionKey, org.jppf.server.ChannelContext)
	 */
	public void exec(SelectionKey key, ChannelContext context) throws IOException
	{
		try
		{
			SocketChannel channel = (SocketChannel) key.channel();
			if (debugEnabled) log.debug("exec() for "+server.getRemostHost(channel));
			if (key.isReadable())
			{
				//as the OS will select it for read when the channel is suddenly 
				//closed by peer, and we are not expecting any read... the channel was closed by node
				nodeClosing(channel, context);
				return;
			}

			NodeChannelContext nodeContext = (NodeChannelContext) key.attachment();
			if (context.content == null)
			{
				// check whether the bundler settings have changed.
				if (nodeContext.bundler.getTimestamp() < server.getBundler().getTimestamp())
				{
					nodeContext.bundler = server.getBundler().copy();
				}
				JPPFTaskBundle bundle = server.getQueue().nextBundle(nodeContext.bundler.getBundleSize());
				if (bundle != null)
				{
					// to avoid cycles in peer-to-peer routing of jobs.
					if (bundle.getUuidPath().contains(context.uuid))
					{
						if (debugEnabled) log.debug("cycle detected in peer-to-peer bundle routing: "+bundle.getUuidPath().getList());
						server.resubmitBundle(bundle);
						return;
					}
					try
					{
						server.sendTask(channel, key, context, bundle);
					}
					catch (Exception e)
					{
						log.error(e.getMessage(), e);
						if (debugEnabled) log.debug("closing channel and resubmitting bundle");
						server.closeNode(channel);
						server.resubmitBundle(bundle);
					}
				}
				else
				{
					key.interestOps(SelectionKey.OP_READ);
					server.availableNodes.add(channel);
					if (!server.getQueue().isEmpty()) server.newBundle(null);
				}
				return;
			}
				// the buffer with the bundle serialized and part transfered
			ByteBuffer task = ((TaskRequest) context.content).getSending();
			try 
			{
				channel.write(task);
			}
			catch (IOException e)
			{
				if (debugEnabled) log.debug("closing channel");
				nodeClosing(channel, context);
				throw e;
			}
				//is anything more to send to SO buffer?
			if (!task.hasRemaining())
			{
				//we finally have sent everything to node, it will do the work and send back to us.
				context.state = server.WaitingResult;
				//we will just wait for the bundle back
				key.interestOps(SelectionKey.OP_READ);
			}
		}
		catch (IOException e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Invoked when an error was detected for a node connection.
	 * @param channel the SocketChannel for the connection.
	 * @param context container for the data the server was sending to the node. 
	 */
	void nodeClosing(SocketChannel channel, ChannelContext context)
	{
		server.closeNode(channel);
		TaskRequest out = (TaskRequest) context.content;
		if (out != null) {
			JPPFTaskBundle bundle = out.getBundle();
			if ((bundle != null) && !JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState())) {
				server.resubmitBundle(bundle);
			}
		}
	}

}