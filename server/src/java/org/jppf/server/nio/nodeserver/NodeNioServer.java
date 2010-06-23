/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.*;
import org.jppf.io.*;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.*;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
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
	private static Log log = LogFactory.getLog(NodeNioServer.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The algorithm that dynamically computes the task bundle size.
	 */
	private AtomicReference<Bundler> bundlerRef;
	/**
	 * The uuid for the task bundle sent to a newly connected node.
	 */
	static final String INITIAL_BUNDLE_UUID = JPPFDriver.getInstance().getUuid();
	/**
	 * The the task bundle sent to a newly connected node.
	 */
	private BundleWrapper initialBundle = null;
	/**
	 * Holds the currently idle channels.
	 */
	private List<SelectableChannel> idleChannels = new ArrayList<SelectableChannel>();
	/**
	 * A reference to the driver's tasks queue.
	 */
	private JPPFQueue queue = null;
	/**
	 * Used to create bundler instances.
	 */
	private JPPFBundlerFactory bundlerFactory = new JPPFBundlerFactory();
	/**
	 * .
	 */
	private final TaskQueueChecker taskQueueChecker;
	/**
	 * Reference to the driver.
	 */
	private static JPPFDriver driver = JPPFDriver.getInstance();

	/**
	 * Initialize this server with a specified port number.
	 * @param port the port this socket server is listening to.
	 * @throws Exception if the underlying server socket can't be opened.
	 */
	public NodeNioServer(int port) throws Exception
	{
		this(new int[] { port });
	}

	/**
	 * Initialize this server with the specified port numbers.
	 * @param ports the ports this socket server is listening to.
	 * @throws Exception if the underlying server socket can't be opened.
	 */
	public NodeNioServer(int[] ports) throws Exception
	{
		super(ports, "NodeServer Thread", false);
		taskQueueChecker = new TaskQueueChecker(this);
		this.selectTimeout = 1L;
		Bundler bundler = bundlerFactory.createBundlerFromJPPFConfiguration();
		this.bundlerRef = new AtomicReference<Bundler>(bundler);
		((JPPFPriorityQueue) getQueue()).addQueueListener(new QueueListener()
		{
			public void newBundle(QueueEvent event)
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
		return driver.isShuttingDown();
	}

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param key the selection key for the socket channel to process.
	 * @see org.jppf.server.nio.NioServer#postAccept(java.nio.channels.SelectionKey)
	 */
	public void postAccept(SelectionKey key)
	{
		driver.getStatsManager().newNodeConnection();
		NodeContext context = (NodeContext) key.attachment();
		try
		{
			context.setBundle(getInitialBundle());
			//context.setBundler(bundler.copy());
			transitionManager.transitionChannel(key, NodeTransition.TO_SEND_INITIAL);
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
			closeNode((SocketChannel) key.channel(), context);
		}
	}

	/**
	 * This method is invoked after all selected keys have been processed.
	 */
	protected void postSelect()
	{
		if (idleChannels.isEmpty() || getQueue().isEmpty()) return;
		transitionManager.submit(taskQueueChecker);
	}

	/**
	 * Add a channel to the list of idle channels.
	 * @param channel the channel to add to the list.
	 */
	public void addIdleChannel(SelectableChannel channel)
	{
		if (debugEnabled) log.debug("Adding idle chanel " + channel);
		synchronized(idleChannels)
		{
			idleChannels.add(channel);
		}
	}

	/**
	 * Remove a channel from the list of idle channels.
	 * @param channel the channel to remove from the list.
	 */
	public void removeIdleChannel(SelectableChannel channel)
	{
		if (debugEnabled) log.debug("Removing idle chanel " + channel);
		synchronized(idleChannels)
		{
			idleChannels.remove(channel);
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
	 * @return a <code>BundleWrapper</code> instance, with no task in it.
	 */
	private BundleWrapper getInitialBundle()
	{
		if (initialBundle == null)
		{
			try
			{
				JPPFSecurityContext cred = driver.getCredentials();
				SerializationHelper helper = new SerializationHelperImpl();
				// serializing a null data provider.
				JPPFBuffer buf = helper.getSerializer().serialize(null);
				byte[] dataProviderBytes = new byte[4 + buf.getLength()];
				ByteBuffer bb = ByteBuffer.wrap(dataProviderBytes);
				bb.putInt(buf.getLength());
				bb.put(buf.getBuffer());
				JPPFTaskBundle bundle = new JPPFTaskBundle();
				bundle.setBundleUuid(INITIAL_BUNDLE_UUID);
				bundle.setRequestUuid("0");
				bundle.getUuidPath().add(driver.getUuid());
				bundle.setTaskCount(0);
				bundle.setState(JPPFTaskBundle.State.INITIAL_BUNDLE);
				initialBundle = new BundleWrapper(bundle);
				initialBundle.setDataProvider(new ByteBufferLocation(dataProviderBytes, 0, dataProviderBytes.length));
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
		return initialBundle;
	}

	/**
	 * Close a connection to a node.
	 * @param channel a <code>SocketChannel</code> that encapsulates the connection.
	 * @param context the context data associated with the channel.
	 */
	public static void closeNode(SocketChannel channel, NodeContext context)
	{
		if (context != null) context.close();
		try
		{
			channel.close();
		}
		catch (IOException e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.error(e);
		}
		try
		{
			driver.getStatsManager().nodeConnectionClosed();
			//if ((context != null) && (context.getNodeUuid() != null))
			{
				driver.removeNodeInformation(new ChannelWrapper<SocketChannel>(channel));
				driver.getNodeNioServer().removeIdleChannel(channel);
			}
		}
		catch (Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.error(e);
		}
	}

	/**
	 * Get the algorithm that dynamically computes the task bundle size.
	 * @return a <code>Bundler</code> instance.
	 */
	public Bundler getBundler()
	{
		return bundlerRef.get();
	}

	/**
	 * Set the algorithm that dynamically computes the task bundle size.
	 * @param bundler a <code>Bundler</code> instance.
	 */
	public void setBundler(Bundler bundler)
	{
		bundlerRef.set(bundler);
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

	/**
	 * Get a reference to the driver's job manager.
	 * @return a <code>JPPFQueue</code> instance.
	 */
	protected JPPFJobManager getJobManager()
	{
		return driver.getJobManager();
	}

	/**
	 * Get the factory object used to create bundler instances.
	 * @return an instance of <code>JPPFBundlerFactory</code>.
	 */
	public JPPFBundlerFactory getBundlerFactory()
	{
		return bundlerFactory;
	}

	/**
	 * Get the list of currently idle channels.
	 * @return a list of <code>SelectableChannel</code> instances.
	 */
	public List<SelectableChannel> getIdleChannels()
	{
		return idleChannels;
	}
}
