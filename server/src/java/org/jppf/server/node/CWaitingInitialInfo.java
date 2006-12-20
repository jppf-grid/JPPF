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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jppf.server.ChannelContext;
import org.jppf.server.ChannelState;
import org.jppf.server.JPPFTaskBundle;
import org.jppf.server.Request;
import org.jppf.utils.SerializationHelper;
import org.jppf.utils.SerializationHelperImpl;

/**
 * This class implements the state of receiving information from the node as a
 * response to sending the initial bundle. The expected information includes the
 * nodes security credentials and authorizations.
 * @author Domingos Creado
 * @author Laurent Cohen
 */
class CWaitingInitialInfo implements ChannelState
{
	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(CWaitingInitialInfo.class);
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
	CWaitingInitialInfo(JPPFNodeServer server)
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
		try
		{
			if (server.fillRequest(channel, out.getRequest()))
			{
				DataInputStream dis = 
					new DataInputStream(new ByteArrayInputStream(request.getOutput().toByteArray()));
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
				context.uuid = bundle.getBundleUuid();
				// there is nothing to do, so this instance will wait for a job
				server.availableNodes.add(channel);
				// make sure the context is reset so as not to resubmit
				// the last bundle executed by the node.
				context.content = null;
				// if the node disconnect from driver we will know soon
				context.state = server.SendingJob;
				//key.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
				key.interestOps(SelectionKey.OP_READ);
				if (!server.getQueue().isEmpty()) server.newBundle(null);
			}
		}
		catch(Exception e)
		{
			JPPFNodeServer.log.error(e.getMessage(), e);
			if (e instanceof IOException)
			{
				server.closeNode(channel);
			}
			if ((bundle != null) && !JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				server.resubmitBundle(bundle);
			}
		}
	}
}