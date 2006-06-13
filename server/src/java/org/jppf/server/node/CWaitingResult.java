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

import static org.jppf.server.JPPFStatsUpdater.*;
import java.io.*;
import java.nio.channels.*;
import java.util.*;
import org.jppf.server.*;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.utils.*;

/**
 * This class implements the state of receiving the bundle
 * back from processing at a node. The "waiting" part is 
 * provided by the selector, which will any for the first
 * bytes be ready to send to the instances of this class.
 * @author Domingos Creado
 */
class CWaitingResult implements ChannelState {

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
	public void exec(SelectionKey key, ChannelContext context) {

		SocketChannel channel = (SocketChannel) key.channel();
		NodeChannelContext nodeContext = (NodeChannelContext) context;
		TaskRequest out = (TaskRequest) nodeContext.content;
		JPPFTaskBundle bundle = out.getBundle();
		Request request = out.getRequest();
		TaskCompletionListener listener = bundle.getCompletionListener();
		try {
			//Wait the full byte[] of the bundle come to start processing.
			//This makes the integration of non-blocking with ObjectInputStream easier.
			if (server.fillRequest(channel, out.getRequest())) {
				long elapsed = System.currentTimeMillis() - request.getStart();
				DataInputStream dis =
					new DataInputStream(new ByteArrayInputStream(request.getOutput().toByteArray()));
				
				//reading the bundle as object 
				SerializationHelper helper = new SerializationHelperImpl();
				bundle = (JPPFTaskBundle) helper.readNextObject(dis, false);
				List<byte[]> taskList = new ArrayList<byte[]>();
				for (int i = 0; i < bundle.getTaskCount(); i++){
					taskList.add(helper.readNextBytes(dis));
				}
				dis.close();

				bundle.setTasks(taskList);
				
				//updating stats
				if (isStatsEnabled()) {
					nodeContext.bundler.feedback(bundle.getTaskCount(), elapsed);
					taskExecuted(bundle.getTaskCount(), elapsed, bundle.getNodeExecutionTime(), out.getBundleBytes());
				}
				
				//notifing the client thread about the end of a bundle
				if (listener != null) listener.taskCompleted(bundle);

				//now it's done...
				//we will now run the scheduler part
				
				// first check whether the bundler settings have changed.
				if (nodeContext.bundler.getTimestamp() < server.getBundler().getTimestamp())
				{
					nodeContext.bundler = server.getBundler().copy();
				}
				// verifying if there is other tasks to send to this node
				bundle = server.getQueue().nextBundle(nodeContext.bundler.getBundleSize());
				if (bundle != null) {
					try {
						server.sendTask(channel, key, context, bundle);
						return;
					} catch (Exception e) {
						server.closeNode(channel);
						server.resubmitBundle(bundle);
						bundle = null;
						throw e;
					}
				}
				
				//there is nothing to do, so this instace will wait for job
				server.availableNodes.add(channel);
				// make sure the context is reset so as not to resubmit
				// the last bundle executed by the node.
				context.content = null;
				//if the node disconnect from driver we will know soon
				context.state = this.server.SendingJob;
				key.interestOps(SelectionKey.OP_READ);
			}
		} catch (Exception e) {
			JPPFNodeServer.log.error(e.getMessage(), e);
			if (e instanceof IOException) {
				server.closeNode(channel);
			}
			if ((bundle != null)  && !JPPFNodeServer.INITIAL_BUNDLE_UUID.equals(bundle.getUuid())) {
				server.resubmitBundle(bundle);
			}
		}
	}
}