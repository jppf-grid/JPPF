/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.*;
import org.jppf.io.*;
import org.jppf.management.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.JPPFTaskBundle;
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
	static final String INITIAL_BUNDLE_UUID = JPPFDriver.getInstance().getCredentials().getUuid();
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
	 * Random number generator used to randomize the choice of idle channel.
	 */
	private Random random = new Random(System.currentTimeMillis());
	/**
	 * Used to create bundler instances.
	 */
	private JPPFBundlerFactory bundlerFactory = new JPPFBundlerFactory();

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
		super(ports, "NodeServer Thread", true);
		this.selectTimeout = 1L;
		Bundler bundler = bundlerFactory.createBundlerFromJPPFConfiguration();
		this.bundlerRef = new AtomicReference<Bundler>(bundler);
		getQueue().addQueueListener(new QueueListener()
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
		return JPPFDriver.getInstance().isShuttingDown();
	}

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param key the selection key for the socket channel to process.
	 * @see org.jppf.server.nio.NioServer#postAccept(java.nio.channels.SelectionKey)
	 */
	public void postAccept(SelectionKey key)
	{
		JPPFDriver.getInstance().getStatsManager().newNodeConnection();
		SocketChannel channel = (SocketChannel) key.channel();
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
			closeNode(channel, context);
		}
	}

	/**
	 * This class ensures that idle nodes get assigned pending tasks in the queue.
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
				if (debugEnabled) log.debug(""+idleChannels.size()+" channels idle");
				List<SelectableChannel> channelList = new ArrayList<SelectableChannel>();
				channelList.addAll(idleChannels);
				boolean found = false;
				SelectableChannel channel = null;
				BundleWrapper selectedBundle = null;
				Iterator<BundleWrapper> it = getQueue().iterator();
				while (!found && it.hasNext() && !idleChannels.isEmpty())
				{
					BundleWrapper bundleWrapper = it.next();
					JPPFTaskBundle bundle = bundleWrapper.getBundle();
					int n = findIdleChannelIndex(bundle);
					if (n >= 0)
					{
						channel = idleChannels.remove(n);
						selectedBundle = bundleWrapper;
						found = true;
					}
				}
				if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle" : "found channel for bundle");
				if (channel != null)
				{
					SelectionKey key = channel.keyFor(selector);
					NodeContext context = (NodeContext) key.attachment();
					BundleWrapper bundleWrapper = getQueue().nextBundle(selectedBundle, context.getBundler().getBundleSize());
					context.setBundle(bundleWrapper);
					transitionManager.transitionChannel(key, NodeTransition.TO_SENDING);
				}
			}
		}

		/**
		 * Find a channel that can send the specified task bundle for execution.
		 * @param bundle the bundle to execute.
		 * @return the index of an available and acceptable channel, or -1 if no channel could be found.
		 */
		private int findIdleChannelIndex(JPPFTaskBundle bundle)
		{
			int n = -1;
			ExecutionPolicy rule = bundle.getExecutionPolicy();
			if (debugEnabled && (rule != null)) log.debug("Bundle has an execution policy:\n" + rule);
			List<Integer> acceptableChannels = new ArrayList<Integer>();
			List<Integer> channelsToRemove =  new ArrayList<Integer>();
			List<String> uuidPath = bundle.getUuidPath().getList();
			for (int i=0; i<idleChannels.size(); i++)
			{
				SelectableChannel ch = idleChannels.get(i);
				if (!ch.isOpen())
				{
					channelsToRemove.add(i);
					continue;
				}
				NodeContext context = (NodeContext) ch.keyFor(selector).attachment();
				if (uuidPath.contains(context.getNodeUuid())) continue;
				if (rule != null)
				{
					NodeManagementInfo mgtInfo = JPPFDriver.getInstance().getNodeInformation(ch);
					JPPFSystemInformation info = (mgtInfo == null) ? null : mgtInfo.getSystemInfo();
					if (!rule.accepts(info)) continue;
				}
				acceptableChannels.add(i);
			}
			for (Integer i: channelsToRemove) idleChannels.remove(i);
			if (debugEnabled) log.debug("found " + acceptableChannels.size() + " acceptable channels");
			if (!acceptableChannels.isEmpty())
			{
				int rnd = random.nextInt(acceptableChannels.size());
				n = acceptableChannels.remove(rnd);
			}
			return n;
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
		transitionManager.submit(r);
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
				JPPFSecurityContext cred = JPPFDriver.getInstance().getCredentials();
				SerializationHelper helper = new SerializationHelperImpl();
				// serializing a null data provider.
				JPPFBuffer buf = helper.getSerializer().serialize(null);
				ByteBuffer bb = ByteBuffer.wrap(new byte[4 + buf.getLength()]);
				bb.putInt(buf.getLength());
				bb.put(buf.getBuffer());
				JPPFTaskBundle bundle = new JPPFTaskBundle();
				bundle.setBundleUuid(INITIAL_BUNDLE_UUID);
				bundle.setRequestUuid("0");
				bundle.getUuidPath().add(JPPFDriver.getInstance().getUuid());
				bundle.setTaskCount(0);
				bundle.setCredentials(cred);
				bundle.setState(JPPFTaskBundle.State.INITIAL_BUNDLE);
				initialBundle = new BundleWrapper(bundle);
				initialBundle.setDataProvider(new ByteBufferLocation(bb));
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
	 * @param channel - a <code>SocketChannel</code> that encapsulates the connection.
	 * @param context - the context data associated with the channel.
	 */
	public static void closeNode(SocketChannel channel, NodeContext context)
	{
		try
		{
			channel.close();
			JPPFDriver.getInstance().getStatsManager().nodeConnectionClosed();
			if (context.getNodeUuid() != null)
			{
				JPPFDriver.getInstance().removeNodeInformation(channel);
				JPPFDriver.getInstance().getNodeNioServer().removeIdleChannel(channel);
			}
		}
		catch (IOException ignored)
		{
			log.error(ignored.getMessage(), ignored);
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
	 * Get the factory object used to create bundler instances.
	 * @return an instance of <code>JPPFBundlerFactory</code>.
	 */
	public JPPFBundlerFactory getBundlerFactory()
	{
		return bundlerFactory;
	}
}
