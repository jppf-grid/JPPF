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


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.node.JPPFBootstrapException;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.*;
import org.jppf.server.JPPFQueue.QueueListener;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

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
	protected static final Logger log = Logger.getLogger(JPPFNodeServer.class);
	/**
	 * The uuid for the task bundle sent to a newly connected node.
	 */
	static final String INITIAL_BUNDLE_UUID = JPPFDriver.getInstance().getCredentials().getUuid();
	/**
	 * A reference to the driver's tasks queue.
	 */
	private JPPFQueue queue = null;
	/**
	 * List of nodes that are available for executing tasks.
	 */
	List<SocketChannel> availableNodes = new LinkedList<SocketChannel>();
	/**
	 * The algorithm that dynamically computes the task bundle size.
	 */
	private Bundler bundler;
	/**
	 * The the task bundle sent to a newly connected node.
	 */
	private JPPFTaskBundle initialBundle = null;

	/**
	 * Initialize this socket server with a specified execution service and port
	 * number.
	 * 
	 * @param port the port this socket server is listening to.
	 * @param bundler the algorithm that deals with bundle size
	 * @throws JPPFBootstrapException if the underlying server socket can't be opened.
	 */
	public JPPFNodeServer(int port, Bundler bundler) throws JPPFBootstrapException {
		super(port, "NodeServer Thread");
		getQueue().addListener(this);
		this.bundler = bundler;
	}

	/**
	 * Get a reference to the driver's tasks queue.
	 * 
	 * @return a <code>JPPFQueue</code> instance.
	 */
	JPPFQueue getQueue() {
		if (queue == null)
			queue = JPPFDriver.getInstance().getTaskQueue();
		return queue;
	}

	/**
	 * Get the IO operations a node connection is initially interested in.
	 * @return {@link java.nio.channels.SelectionKey.OP_READ SelectionKey.OP_READ}.
	 * @see org.jppf.server.JPPFNIOServer#getInitialInterest()
	 */
	protected int getInitialInterest() {
		return SelectionKey.OP_READ;
	}

	/**
	 * Get the initial state of a connection to a node.
	 * @return a <code>State</code> instance.
	 * @see org.jppf.server.JPPFNIOServer#getInitialState()
	 */
	protected ChannelState getInitialState() {
		return SendingJob;
	}

	/**
	 * Get the initial content to send over the connection.
	 * @return null.
	 * @see org.jppf.server.JPPFNIOServer#getInitialContent()
	 */
	protected Object getInitialContent() {
		return null;
	}

	/**
	 * Create a context for a newly connected channel.
	 * Subclasses can override this method to add specific content to the context.
	 * @return a <code>ChannelContext</code> instance.
	 * @see org.jppf.server.JPPFNIOServer#createChannelContext()
	 */
	protected ChannelContext createChannelContext()
	{
		NodeChannelContext context = new NodeChannelContext();
		context.bundler = bundler.copy();
		return context;
	}

	/**
	 * Called after a connection to a node has been accepted by the server socket channel.
	 * @param client the <code>SocketChannel</code> that was accepted.
	 * @see org.jppf.server.JPPFNIOServer#postAccept(java.nio.channels.SocketChannel)
	 */
	protected void postAccept(SocketChannel client) {
		JPPFStatsUpdater.newNodeConnection();
		SelectionKey key = client.keyFor(selector);
		ChannelContext context = (ChannelContext) key.attachment();
		try
		{
			sendTask(client, key, context, getInitialBundle());
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
			closeNode(client);
		}
	}

	/**
	 * Callback method when a task bundle is added to the queue.
	 * @param queue the queue to which a bundle was added.
	 * @see org.jppf.server.JPPFQueue.QueueListener#newBundle(org.jppf.server.JPPFQueue)
	 */
	public void newBundle(JPPFQueue queue)
	{
		if (availableNodes.isEmpty()) return;
		SocketChannel aNode = availableNodes.remove(0);
		SelectionKey key = aNode.keyFor(selector);
		NodeChannelContext context = (NodeChannelContext) key.attachment();
		// check whether the bundler settings have changed.
		if (context.bundler.getTimestamp() < bundler.getTimestamp())
		{
			context.bundler = bundler.copy();
		}
		JPPFTaskBundle bundle = queue.nextBundle(context.bundler.getBundleSize());
		if (bundle != null)
		{
			try
			{
				sendTask(aNode, key, context, bundle);
				selector.wakeup();
			}
			catch (Exception e)
			{
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
	ChannelState SendingJob = new CSendingJob(this);

	/**
	 * State of a node connection after a task bundle has been sent.
	 * Waits for and reads the results of the bundle execution.
	 */
	ChannelState WaitingResult = new CWaitingResult(this);

	/**
	 * State of a node connection after a task bundle has been sent.
	 * Waits for and reads the results of the bundle execution.
	 */
	ChannelState WaitingInitialInfo = new CWaitingInitialInfo(this);

	/**
	 * Resubmit a task bundle at the head of the queue. This method is invoked
	 * when a node is disconnected while it was executing a task bundle.
	 * 
	 * @param bundle the task bundle to resubmit.
	 */
	void resubmitBundle(JPPFTaskBundle bundle) {
		
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
	void closeNode(SocketChannel aNode) {
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
	 * @param channel the channel with the node
	 * @param key the key of the channel with the main selector
	 * @param context the context attached to the channel
	 * @param bundle the task to be send
	 * @throws Exception if any error occur
	 */
	void sendTask(SocketChannel channel, SelectionKey key,
			ChannelContext context, JPPFTaskBundle bundle) throws Exception {

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

		long size = 0L;
		if (bundle.getDataProvider() != null) {
			size += bundle.getDataProvider().length;
		}
		helper.writeNextObject(bundle, dos, false);
		helper.writeNextBytes(dos, bundle.getDataProvider(), 0, 
				bundle.getDataProvider().length);
		if (bundle.getTasks() != null) {
			for (byte[] task : bundle.getTasks()) {
				size += task.length;
				helper.writeNextBytes(dos, task, 0, task.length);
			}
		}
		dos.flush();
		dos.close();

		// it is now converted to byte[]
		byte[] data = baos.toByteArray();
		ByteBuffer sending = ByteBuffer.allocateDirect(data.length + 4);
		sending.putInt(data.length);
		sending.put(data);
		sending.flip();
		
		//make the first shot
		channel.write(sending);
		if (sending.hasRemaining())
		{
			//we must wait until the SO buffer be ready to the more data
			// or a busy-loop can occur
			context.content = new TaskRequest(new Request(), sending, bundle,	size);
			context.state = SendingJob;
			//the read option is to detect sudden node dead
			key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		}
		else
		{
			//the bundle could be fully transfered to SO buffer
			// so we will just wait for node starts sending the results
			// Probably here would be a nice place to get a SLA agent running
			if (JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				context.state = WaitingInitialInfo;
			}
			else
			{
				context.state = WaitingResult;
			}
			context.content = new TaskRequest(new Request(), sending, bundle, size);
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	/**
	 * Get the task bundle sent to a newly connected node,
	 * so that it can check whether it is up to date, without having
	 * to wait for an actual request to be sent.
	 * @return a <code>JPPFTaskBundle</code> instance, with no task in it.
	 */
	private JPPFTaskBundle getInitialBundle()
	{
		if (initialBundle == null)
		{
			try
			{
				DataProvider dp = null;
				ByteArrayOutputStream baos = new ByteArrayOutputStream() {
					public synchronized byte[] toByteArray() {
						return buf;
					}
				};
				JPPFSecurityContext cred = JPPFDriver.getInstance().getCredentials();
				DataOutputStream dos = new DataOutputStream(baos);
				SerializationHelper helper = new SerializationHelperImpl();
				helper.writeNextObject(dp, dos, true);
				dos.close();
				byte[] dpBytes = baos.toByteArray();
				JPPFTaskBundle bundle = new JPPFTaskBundle();
				bundle.setUuid(INITIAL_BUNDLE_UUID);
				bundle.setRequestUuid("0");
				bundle.setAppUuid(INITIAL_BUNDLE_UUID);
				bundle.setTaskCount(0);
				bundle.setTasks(new ArrayList<byte[]>());
				bundle.setDataProvider(dpBytes);
				bundle.setCredentials(cred);
				bundle.setState(JPPFTaskBundle.State.INITIAL_BUNDLE);
				initialBundle =  bundle;
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
		return initialBundle;
	}

	/**
	 * Get the algorithm that dynamically computes the task bundle size.
	 * @return a <code>Bundler</code> instance.
	 */
	public synchronized Bundler getBundler()
	{
		return bundler;
	}

	/**
	 * Set the algorithm that dynamically computes the task bundle size.
	 * @param bundler a <code>Bundler</code> instance.
	 */
	public synchronized void setBundler(Bundler bundler)
	{
		this.bundler = bundler;
	}
}
