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

package org.jppf.server.mina.nodeserver;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.*;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.*;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.mina.*;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.*;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class MinaNodeServer extends MinaServer<NodeState, NodeTransition>
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(MinaNodeServer.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The uuid for the task bundle sent to a newly connected node.
	 */
	public static final String INITIAL_BUNDLE_UUID = JPPFDriver.getInstance().getUuid();
	/**
	 * The the task bundle sent to a newly connected node.
	 */
	private BundleWrapper initialBundle = null;
	/**
	 * The algorithm that dynamically computes the task bundle size.
	 */
	private AtomicReference<Bundler> bundlerRef;
	/**
	 * Holds the currently idle channels.
	 */
	private List<IoSession> idleChannels = new ArrayList<IoSession>();
	/**
	 * A reference to the driver's tasks queue.
	 */
	private AbstractJPPFQueue queue = null;
	/**
	 * Used to create bundler instances.
	 */
	private JPPFBundlerFactory bundlerFactory = new JPPFBundlerFactory();
	/**
	 * A separate thread that runs continuously and checks for jobs that can
	 * be sent to available nodes.
	 */
	private JobQueueChecker queueChecker = new JobQueueChecker(this);

	/**
	 * Initialiaze this server with the specified port number.
	 * @param port the port to listen to.
	 */
	public MinaNodeServer(int port)
	{
		this(new int[] { port });
	}

	/**
	 * Initialiaze this server with the specified port numbers.
	 * @param ports the ports to listen to.
	 */
	public MinaNodeServer(int[] ports)
	{
		super(ports);
	}

	/**
	 * Initialize the server.
	 * @throws Exception if any error occurs.
	 */
	public void start() throws Exception
	{
		Bundler bundler = bundlerFactory.createBundlerFromJPPFConfiguration();
		this.bundlerRef = new AtomicReference<Bundler>(bundler);
		List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
		for (int port: ports) addresses.add(new InetSocketAddress(port));
		int size = Runtime.getRuntime().availableProcessors() + 1;
		processor = new NioProcessor(createExecutor("Node Server Processor", size));
		acceptor = new NioSocketAcceptor(createExecutor("Node Server Acceptor", 1), processor);
		SocketSessionConfig cfg = acceptor.getSessionConfig();
		cfg.setReceiveBufferSize(SocketWrapper.SOCKET_RECEIVE_BUFFER_SIZE);
		cfg.setSendBufferSize(SocketWrapper.SOCKET_RECEIVE_BUFFER_SIZE);
		cfg.setReuseAddress(false);
		cfg.setKeepAlive(false);
		cfg.setIdleTime(IdleStatus.BOTH_IDLE, 0);
		acceptor.getFilterChain().addLast("nodeMessageFilter", new NodeIoFilter());
		/*
		OrderedThreadPoolExecutor x = new OrderedThreadPoolExecutor(16, 16, 1L, TimeUnit.HOURS, new JPPFThreadFactory("NodeIoProcessor"));
		ExecutorFilter ef = new ExecutorFilter(x);
		acceptor.getFilterChain().addLast("executor", ef);
    */
		acceptor.setHandler(new NodeIoHandler(this));
		new Thread(queueChecker, "Node Queue Checker").start();
		getQueue().addListener(new QueueListener()
		{
			public void newBundle(QueueEvent event)
			{
				queueChecker.wakeUp();
			}
		});
		acceptor.bind(addresses);
	}

	/**
	 * Close this node server.
	 */
	public void close()
	{
		queueChecker.setStopped(true);
		Map<Long, IoSession> sessions = acceptor.getManagedSessions();
		for (Map.Entry<Long, IoSession> entry: sessions.entrySet()) entry.getValue().close(true);
		acceptor.dispose();
	}

	/**
	 * Add a channel to the list of idle channels.
	 * @param channel the channel to add to the list.
	 */
	public void addIdleChannel(IoSession channel)
	{
		if (debugEnabled) log.debug("Adding idle channel " + channel);
		synchronized(idleChannels)
		{
			idleChannels.add(channel);
			if (debugEnabled) log.debug("" + idleChannels.size() + " channels idle");
		}
		queueChecker.wakeUp();
	}

	/**
	 * Remove a channel from the list of idle channels.
	 * @param channel the channel to remove from the list.
	 */
	public void removeIdleChannel(IoSession channel)
	{
		if (debugEnabled) log.debug("Removing idle channel " + channel);
		synchronized(idleChannels)
		{
			idleChannels.remove(channel);
		}
	}

	/**
	 * Remove a channel from the list of idle channels.
	 * @param index index of the channel to remove from the list.
	 * @return the channel that was removed from the list.
	 */
	public IoSession removeIdleChannel(int index)
	{
		IoSession session = null;
		synchronized(idleChannels)
		{
			session = idleChannels.remove(index);
		}
		if (debugEnabled) log.debug("Removing idle channel " + session);
		return session;
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
	protected AbstractJPPFQueue getQueue()
	{
		if (queue == null) queue = (AbstractJPPFQueue) JPPFDriver.getQueue();
		return queue;
	}

	/**
	 * Get a reference to the driver's job manager.
	 * @return a <code>JPPFQueue</code> instance.
	 */
	protected JPPFJobManager getJobManager()
	{
		return JPPFDriver.getInstance().getJobManager();
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
	public List<IoSession> getIdleChannels()
	{
		return idleChannels;
	}

	/**
	 * Get the task bundle sent to a newly connected node,
	 * so that it can check whether it is up to date, without having
	 * to wait for an actual request to be sent.
	 * @return a <code>BundleWrapper</code> instance, with no task in it.
	 */
	BundleWrapper getInitialBundle()
	{
		if (initialBundle == null)
		{
			try
			{
				JPPFSecurityContext cred = JPPFDriver.getInstance().getCredentials();
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
				bundle.getUuidPath().add(JPPFDriver.getInstance().getUuid());
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
	 * @param session a <code>IoSession</code> that encapsulates the connection.
	 * @param context the context data associated with the channel.
	 */
	public void closeNode(IoSession session, NodeContext context)
	{
		try
		{
			session.close(true);
			JPPFDriver.getInstance().getStatsManager().nodeConnectionClosed();
			if (context.getNodeUuid() != null)
			{
				ChannelWrapper cw = new IoSessionWrapper(session);
				JPPFDriver.getInstance().removeNodeInformation(cw);
				removeIdleChannel(session);
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Create the factory holding all the states and transition mappings.
	 * @return an <code>MinaServerFactory</code> instance.
	 */
	protected MinaServerFactory<NodeState, NodeTransition> createFactory()
	{
		return new NodeServerFactory(this);
	}
}
