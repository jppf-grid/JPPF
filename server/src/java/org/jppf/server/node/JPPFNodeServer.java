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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jppf.node.JPPFBootstrapException;
import org.jppf.server.JPPFDriver;
import org.jppf.server.JPPFNIOServer;
import org.jppf.server.JPPFQueue;
import org.jppf.server.JPPFStatsUpdater;
import org.jppf.server.JPPFTaskBundle;
import org.jppf.server.JPPFQueue.QueueListener;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.utils.SerializationHelper;
import org.jppf.utils.SerializationHelperImpl;

/**
 * This is a node server. It deals with job dispatching and results retrieve. It
 * distribute equaly bundles beteween available jobs (considering that node have
 * the same processing time of jobs). It probably will receive and algorithm to
 * weight each node, or a scheduling algorithm using the mean processing time.
 * 
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class JPPFNodeServer extends JPPFNIOServer implements QueueListener {

	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(JPPFNodeServer.class);

	/**
	 * A reference to the driver's tasks queue.
	 */
	private JPPFQueue queue = null;

	/**
	 * List of nodes that are available for executing tasks.
	 */
	private List<SocketChannel> availableNodes = new LinkedList<SocketChannel>();

	/**
	 * Initialize this socket server with a specified execution service and port
	 * number.
	 * 
	 * @param port
	 *            the port this socket server is listening to.
	 * @throws JPPFBootstrapException
	 *             if the underlying server socket can't be opened.
	 */
	public JPPFNodeServer(int port) throws JPPFBootstrapException {
		super(port, "NodeServer Thread");
		getQueue().addListener(this);
	}

	/**
	 * Get a reference to the driver's tasks queue.
	 * 
	 * @return a <code>JPPFQueue</code> instance.
	 */
	private JPPFQueue getQueue() {
		if (queue == null)
			queue = JPPFDriver.getInstance().getTaskQueue();
		return queue;
	}

	/**
	 * @see org.jppf.server.JPPFNIOServer#getInitialInterest()
	 */
	protected int getInitialInterest() {
		return SelectionKey.OP_READ;
	}

	/**
	 * @see org.jppf.server.JPPFNIOServer#getInitialState()
	 */
	protected State getInitialState() {
		return SendingJob;
	}

	/**
	 * @see org.jppf.server.JPPFNIOServer#getInitialContent()
	 */
	protected Object getInitialContent() {
		return null;
	}

	/**
	 * 
	 * @param client
	 * @see org.jppf.server.JPPFNIOServer#postAccept(java.nio.channels.SocketChannel)
	 */
	protected void postAccept(SocketChannel client) {
		JPPFStatsUpdater.newNodeConnection();
		JPPFTaskBundle bundle = getQueue().nextBundle();
		if (bundle != null) {
			SelectionKey key = client.keyFor(selector);
			Context context = (Context) key.attachment();
			try {
				sendTask(client, key, context, bundle);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				closeNode(client);
				resubmitBundle(bundle);
			}
		} else {
			availableNodes.add(client);
		}
	}

	
	/**
	 * 
	 * @param queue
	 * @see org.jppf.server.JPPFQueue.QueueListener#newBundle(org.jppf.server.JPPFQueue)
	 */
	public void newBundle(JPPFQueue queue) {
		if (availableNodes.isEmpty()) {
			return;
		}
		JPPFTaskBundle bundle = queue.nextBundle();
		if (bundle != null) {
			SocketChannel aNode = availableNodes.remove(0);
			SelectionKey key = aNode.keyFor(selector);
			Context context = (Context) key.attachment();
			try {
				sendTask(aNode, key, context, bundle);
				selector.wakeup();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				closeNode(aNode);
				resubmitBundle(bundle);
			}
		}
	}

	
	/**
	 * There are only two states in this automata.<br>
	 * <pre>
	 *   +--------------+    job sent    +-----------------+    
	 *   |   Sending    | ------------>  |     Waiting     |
	 *   |     jobs     | <-----------   |     results     |
	 *   +--------------+  bundle fully  +-----------------+
	 *                    trans. back
	 * </pre>
	 * See {@link org.jppf.server.JPPFNIOServer JPPFNIOServer} for more 
	 * details on the architecture of the framework.
	 */
	private State SendingJob = new CSendingJob();

	/**
	 * 
	 */
	private State WaitingResult = new CWaitingResult();

	/**
	 * This class represents the state of waiting for some action.
	 */
	private class CSendingJob implements State {

		/**
		 * 
		 * @param key
		 * @param context
		 * @throws IOException
		 * @see org.jppf.server.JPPFNIOServer.State#exec(java.nio.channels.SelectionKey, org.jppf.server.JPPFNIOServer.Context)
		 */
		public void exec(SelectionKey key, Context context) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			
			if (key.isReadable()) {
				//as the SO will select it for read when the channel is suddenly 
				//closed by peer, and we are not expecting any read...
				// the channel was closed by node
				closeNode(channel);
				TaskRequest out = (TaskRequest) context.content;
				if (out != null) {
					JPPFTaskBundle bundle = out.getBundle();
					if (bundle != null) {
						resubmitBundle(bundle);
					}
				}
				return;
			}
			
			if (context.content == null)
				return;
			
			// the buffer with the bundle serialized and part transfered
			ByteBuffer task = ((TaskRequest) context.content).getSending();

			try {
				channel.write(task);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				closeNode(channel);
				
				// putting back the task into queue
				TaskRequest out = (TaskRequest) context.content;
				JPPFTaskBundle bundle = out.getBundle();
				resubmitBundle(bundle);
				throw e;
			}
			
			//is anything more to send to SO buffer?
			if (!task.hasRemaining()) {
				//we finally have sent everything to node
				// it will do the work and send back to us.
				context.state = WaitingResult;
				
				//we will just wait for the bundle back
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	/**
	 * This class implements the state of receiving the bundle
	 * back from processing at a node. The "wainting" part in 
	 * provided by the selector, which will any for the first
	 * bytes be ready to send to the instances of this class.
	 */
	private class CWaitingResult implements State {

		/**
		 * 
		 * @param key
		 * @param context
		 * @see org.jppf.server.JPPFNIOServer.State#exec(java.nio.channels.SelectionKey, org.jppf.server.JPPFNIOServer.Context)
		 */
		public void exec(SelectionKey key, Context context) {

			SocketChannel channel = (SocketChannel) key.channel();
			TaskRequest out = (TaskRequest) context.content;
			JPPFTaskBundle bundle = out.getBundle();
			Request request = out.getRequest();
			TaskCompletionListener listener = bundle.getCompletionListener();
			try {
				//We will wait the full byte[] of the bundle come to start 
				//processing. This make the integration of non-blocking with
				// ObjectInputStream easier.
				if (fillRequest(channel, out.getRequest())) {

					long elapsed = System.currentTimeMillis()
							- request.getStart();
					
					DataInputStream dis = new DataInputStream(
							new ByteArrayInputStream(request.getOutput()
									.toByteArray()));
					
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
						taskExecuted(bundle.getTaskCount(), elapsed, bundle
								.getNodeExecutionTime(), out.getBundleBytes());
					}
					
					//notifing the client thread about the end of a bundle
					
					listener.taskCompleted(bundle);

					//now it's done...
					//we will now run the scheduler part
					
					// verifying if there is other tasks to send to this node
					bundle = getQueue().nextBundle();
					if (bundle != null) {
						try {
							sendTask(channel, key, context, bundle);
							return;
						} catch (Exception e) {
							closeNode(channel);
							resubmitBundle(bundle);
							bundle = null;
							throw e;
						}
					}
					
					//there is nothing to do, so this instace will wait for job
					availableNodes.add(channel);
					// make sure the context is reset so as not to resubmit
					// the last bundle executed by the node.
					context.content = null;
					//if the node disconnect from driver we will know soon
					context.state = SendingJob;
					key.interestOps(SelectionKey.OP_READ);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				if (e instanceof IOException) {
					closeNode(channel);
				}
				if (bundle != null) {
					resubmitBundle(bundle);
				}
			}
		}
	}

	/**
	 * Resubmit a task bundle at the head of the queue. This method is invoked
	 * when a node is disconnected while it was executing a task bundle.
	 * 
	 * @param bundle
	 *            the task bundle to resubmit.
	 */
	private void resubmitBundle(JPPFTaskBundle bundle) {
		
		bundle.setPriority(10);
        // to not enter in a loop
		getQueue().removeListener(this);
		getQueue().addBundle(bundle);
		getQueue().addListener(this);
	}

	/**
	 * Close a connection to a node.
	 * 
	 * @param aNode
	 *            a <code>SocketChannel</code> that encapsulates the
	 *            connection.
	 */
	private void closeNode(SocketChannel aNode) {
		availableNodes.remove(aNode);
		try {
			JPPFStatsUpdater.nodeConnectionClosed();
			aNode.close();
            // all keys are cancelled when a channel is closed
		} catch (IOException ignored) {
			log.error(ignored.getMessage(), ignored);
		}
	}

	/**
	 * Send through the specified channel the specified bundle. It sends a piece
	 * of data and verify if it is finished, If it is fully transfered at first
	 * try the key is updated to SelectKey.OP_READ and the context is update
	 * with the bundle transfered and setup to wait for task complete. If the
	 * data is not fully transfered it enqueue the buffer to continue the
	 * transfer when the channel is ready to receive more data.
	 * 
	 * @param channel
	 *            the channel with the node
	 * @param key
	 *            the key of the channel with the main selector
	 * @param context
	 *            the context attached to the channel
	 * @param bundle
	 *            the task to be send
	 * @throws Exception
	 *             if any error occur
	 */
	private void sendTask(SocketChannel channel, SelectionKey key,
			Context context, JPPFTaskBundle bundle) throws Exception {

		//the preparing part of sending a bundle
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream() {
			// overriden for performance reasons - no need to create a copy of
			// the buffer here.
			public synchronized byte[] toByteArray() {
				return buf;
			}
		};
		DataOutputStream dos = new DataOutputStream(baos);
		SerializationHelper helper = new SerializationHelperImpl();

		long size = bundle.getDataProvider().length;
		helper.writeNextObject(bundle, dos, false);
		helper.writeNextBytes(dos, bundle.getDataProvider(), 0, bundle
				.getDataProvider().length);
		for (byte[] task : bundle.getTasks()) {
			size += task.length;
			helper.writeNextBytes(dos, task, 0, task.length);
		}

		dos.flush();
		dos.close();

		// it is now converted to byte[]
		byte[] data = baos.toByteArray();
		ByteBuffer sending = ByteBuffer.allocateDirect(data.length + 4);
		//FIXME: probably it is a better idea to make a double buffer with 
		// directly allocated buffers and a heap based buffer
		 
		sending.putInt(data.length);
		sending.put(data);
		sending.flip();
		
		//make the first shoot
		channel.write(sending);
		
		if (sending.hasRemaining()) {
			//we must wait until the SO buffer be ready to the more data
			// or a busy-loop can occur
			context.content = new TaskRequest(new Request(), sending, bundle,
					size);
			context.state = SendingJob;
			//the read option is to detect sudden node dead
			key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
			
		} else {
			//the bundle could be fully tranfered to SO buffer
			// so we will just wait for node starts sending the results
			
			// Probably here would be a nice place to get a SLA agent running 
			context.state = WaitingResult;
			context.content = new TaskRequest(new Request(), sending, bundle,
					size);
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	/**
	 * 
	 */
	private class TaskRequest {
		/**
		 * 
		 */
		private Request request;

		/**
		 * 
		 */
		private ByteBuffer sending;

		/**
		 * 
		 */
		private JPPFTaskBundle bundle;

		/**
		 * 
		 */
		private long bundleBytes;

		/**
		 * @param request
		 * @param sending
		 * @param bundle
		 * @param bundleBytes
		 */
		public TaskRequest(Request request, ByteBuffer sending,
				JPPFTaskBundle bundle, long bundleBytes) {
			super();
			this.bundle = bundle;
			this.request = request;
			this.sending = sending;
			this.bundleBytes = bundleBytes;
		}

		/**
		 * 
		 * @return
		 */
		public Request getRequest() {
			return request;
		}

		/**
		 * 
		 * @return
		 */
		public ByteBuffer getSending() {
			return sending;
		}

		/**
		 * 
		 * @return
		 */
		public JPPFTaskBundle getBundle() {
			return bundle;
		}

		/**
		 * 
		 * @return
		 */
		public long getBundleBytes() {
			return bundleBytes;
		}
	}
}
