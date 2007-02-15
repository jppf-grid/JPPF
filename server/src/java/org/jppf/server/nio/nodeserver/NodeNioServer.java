/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

package org.jppf.server.nio.nodeserver;

import java.io.IOException;
import java.nio.channels.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.security.JPPFSecurityContext;
import org.jppf.server.*;
import org.jppf.server.JPPFQueue.QueueListener;
import org.jppf.server.nio.*;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeNioServer extends NioServer<NodeState, NodeTransition, NodeNioServer>
{
	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(NodeNioServer.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
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
	private Set<SocketChannel> idleChannels = new HashSet<SocketChannel>();
	/**
	 * A reference to the driver's tasks queue.
	 */
	private JPPFQueue queue = null;

	/**
	 * Initialize this server with a specified port number and name.
	 * @param port the port this socket server is listening to.
	 * @param bundler the bundler used to compute the size of the bundles sent for execution..
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public NodeNioServer(int port, Bundler bundler) throws JPPFException
	{
		super(port, "NodeServer Thread");
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
	protected NioServerFactory<NodeState, NodeTransition, NodeNioServer> createFactory()
	{
		return new NodeServerFactory(this);
	}

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param key the selection key for the socket channel to process.
	 */
	public void postAccept(SelectionKey key)
	{
		JPPFStatsUpdater.newNodeConnection();
		SocketChannel channel = (SocketChannel) key.channel();
		NodeContext context = (NodeContext) key.attachment();
		try
		{
			context.setBundle(getInitialBundle());
			context.setBundler(bundler.copy());
			context.setState(NodeState.SEND_INITIAL_BUNDLE);
			key.interestOps(SelectionKey.OP_WRITE|SelectionKey.OP_READ);
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
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
				if (debugEnabled) log.debug(""+idleChannels.size()+" channels idle");
				Iterator<SocketChannel> it = idleChannels.iterator();
				while (!getQueue().isEmpty() && it.hasNext())
				{
					try
					{
						lock.lock();
						SocketChannel channel = it.next();
						it.remove();
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
	public void addIdleChannel(SocketChannel channel)
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
				log.error(e.getMessage(), e);
			}
		}
		return initialBundle;
	}

	/**
	 * Close a connection to a node.
	 * @param aNode a <code>SocketChannel</code> that encapsulates the connection.
	 */
	public static void closeNode(SocketChannel aNode)
	{
		try
		{
			JPPFStatsUpdater.nodeConnectionClosed();
			aNode.close();
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
