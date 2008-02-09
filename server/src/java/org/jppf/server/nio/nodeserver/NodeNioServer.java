/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

import java.io.IOException;
import java.nio.channels.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.*;
import org.jppf.server.JPPFQueue.QueueListener;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.utils.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class NodeNioServer extends NioServer<NodeState, NodeTransition>
{
	/**
	 * Logger for this class.
	 */
	protected static final Log LOG = LogFactory.getLog(NodeNioServer.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
	/**
	 * The algorithm that dynamically computes the task bundle size.
	 */
	private Bundler bundler;

	/**
	 * The uuid for the task bundle sent to a newly connected node.
	 */
	static final String INITIAL_BUNDLE_UUID = JPPFDriver.getInstance().getCredentials().getUuid();
	/**
	 * The the task bundle sent to a newly connected node.
	 */
	private JPPFTaskBundle initialBundle = null;

	/**
	 * Holds the currently idle channels.
	 */
	//private List<SocketChannel> idleChannels = new ArrayList<SocketChannel>();
	private List<SelectableChannel> idleChannels = new ArrayList<SelectableChannel>();
	/**
	 * A reference to the driver's tasks queue.
	 */
	private JPPFQueue queue = null;
	/**
	 * Random number generator used to randomize the choice of idle channel.
	 */
	private Random random = new Random(System.currentTimeMillis());

	/**
	 * Initialize this server with a specified port number and name.
	 * @param port the port this socket server is listening to.
	 * @param bundler the bundler used to compute the size of the bundles sent for execution..
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public NodeNioServer(int port, Bundler bundler) throws JPPFException
	{
		this(new int[] { port }, bundler);
	}

	/**
	 * Initialize this server with the specified port numbers and name.
	 * @param ports the ports this socket server is listening to.
	 * @param bundler the bundler used to compute the size of the bundles sent for execution..
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public NodeNioServer(int[] ports, Bundler bundler) throws JPPFException
	{
		super(ports, "NodeServer Thread", false);
		//this.selectTimeout = 1L;
		this.bundler = bundler;
		getQueue().addListener(new QueueListener()
		{
			public void newBundle(JPPFQueue queue)
			{
				selector.wakeup();
			}
		});
	}

	/**
	 * Create the factory holding all the states and transition mappings.
	 * @return an <code>NioServerFactory</code> instance.
	 * @see org.jppf.server.nio.NioServer#createFactory()
	 */
	protected NioServerFactory<NodeState, NodeTransition> createFactory()
	{
		return new NodeServerFactory(this);
	}

	/**
	 * Determine whether a stop condition external to this server has been reached.
	 * @return true if the driver is shutting down, false otherwise.
	 * @see org.jppf.server.nio.NioServer#externalStopCondition()
	 */
	protected boolean externalStopCondition()
	{
		return JPPFDriver.getInstance().isShuttingDown();
	}

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param key the selection key for the socket channel to process.
	 * @see org.jppf.server.nio.NioServer#postAccept(java.nio.channels.SelectionKey)
	 */
	public void postAccept(SelectionKey key)
	{
		JPPFStatsUpdater.newNodeConnection();
		SocketChannel channel = (SocketChannel) key.channel();
		NodeContext context = (NodeContext) key.attachment();
		try
		{
			context.setBundle(getInitialBundle());
			//context.setBundler(bundler.copy());
			context.setState(NodeState.SEND_INITIAL_BUNDLE);
			key.interestOps(SelectionKey.OP_WRITE|SelectionKey.OP_READ);
		}
		catch (Exception e)
		{
			LOG.error(e.getMessage(), e);
			closeNode(channel);
		}
	}

	/**
	 * This class ensures that idle node get assigned pending tasks in the queue.
	 */
	private class TaskQueueChecker implements Runnable
	{
		/**
		 * Perform the assignment of tasks.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			synchronized(idleChannels)
			{
				if (idleChannels.isEmpty() || getQueue().isEmpty()) return;
				if (DEBUG_ENABLED) LOG.debug(""+idleChannels.size()+" channels idle");
				while (!getQueue().isEmpty() && !idleChannels.isEmpty())
				{
					try
					{
						lock.lock();
						int n = random.nextInt(idleChannels.size());
						SelectableChannel channel = idleChannels.remove(n);
						SelectionKey key = channel.keyFor(selector);
						selector.wakeup();
						key.interestOps(SelectionKey.OP_WRITE|SelectionKey.OP_READ);
					}
					finally
					{
						lock.unlock();
					}
				}
			}
		}
	}

	/**
	 * .
	 */
	private final Runnable r = new TaskQueueChecker();

	/**
	 * This method is invoked after all selected keys have been processed.
	 */
	protected void postSelect()
	{
		if (idleChannels.isEmpty() || getQueue().isEmpty()) return;
		executor.submit(r);
	}

	/**
	 * Add a channel to the list of idle channels.
	 * @param channel the channel to add to the list.
	 */
	public void addIdleChannel(SelectableChannel channel)
	{
		synchronized(idleChannels)
		{
			idleChannels.add(channel);
		}
	}

	/**
	 * Define a context for a newly created channel.
	 * @return an <code>NioContext</code> instance.
	 * @see org.jppf.server.nio.NioServer#createNioContext()
	 */
	public NioContext createNioContext()
	{
		return new NodeContext();
	}

	/**
	 * Get the IO operations a connection is initially interested in.
	 * @return a bit-wise combination of the interests, taken from
	 * {@link java.nio.channels.SelectionKey SelectionKey} constants definitions.
	 * @see org.jppf.server.nio.NioServer#getInitialInterest()
	 */
	public int getInitialInterest()
	{
		return SelectionKey.OP_READ;
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
				JPPFSecurityContext cred = JPPFDriver.getInstance().getCredentials();
				SerializationHelper helper = new SerializationHelperImpl();
				JPPFBuffer buf = helper.toBytes(null, true);
				byte[] dpBytes = new byte[4 + buf.getLength()];
				helper.copyToBuffer(buf.getBuffer(), dpBytes, 0, buf.getLength());
				JPPFTaskBundle bundle = new JPPFTaskBundle();
				bundle.setBundleUuid(INITIAL_BUNDLE_UUID);
				bundle.setRequestUuid("0");
				bundle.getUuidPath().add(JPPFDriver.getInstance().getUuid());
				bundle.setTaskCount(0);
				bundle.setTasks(new ArrayList<byte[]>());
				bundle.setDataProvider(dpBytes);
				bundle.setCredentials(cred);
				bundle.setState(JPPFTaskBundle.State.INITIAL_BUNDLE);
				initialBundle =  bundle;
			}
			catch(Exception e)
			{
				LOG.error(e.getMessage(), e);
			}
		}
		return initialBundle;
	}

	/**
	 * Close a connection to a node.
	 * @param channel a <code>SocketChannel</code> that encapsulates the connection.
	 */
	public static void closeNode(SocketChannel channel)
	{
		try
		{
			JPPFStatsUpdater.nodeConnectionClosed();
			JPPFDriver.getInstance().removeNodeInformation(channel);
			channel.close();
		}
		catch (IOException ignored)
		{
			LOG.error(ignored.getMessage(), ignored);
		}
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

	/**
	 * Get a reference to the driver's tasks queue.
	 * @return a <code>JPPFQueue</code> instance.
	 */
	protected JPPFQueue getQueue()
	{
		if (queue == null) queue = JPPFDriver.getQueue();
		return queue;
	}
}
