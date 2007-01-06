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

package org.jppf.server.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.classloader.ResourceProvider;
import org.jppf.server.JPPFDriver;

/**
 * Generic server for non-blocking asynchronous socket channel based communications.<br>
 * Instances of this class rely on a number of possible states for each socket channel,
 * along with the possible transitions between thoses states.<br>
 * The design of this class enforces the use of typesafe enumerations for the states
 * and transitions, so the developers must think ahead of how to implement their server
 * as a state machine.
 * @param <S> the type of the states to use.
 * @param <T> the type of the transitions to use.
 * @param <U> the type of this server.
 * @author Laurent Cohen
 */
public abstract class NioServer<S extends Enum<S>, T extends Enum<T>, U extends NioServer> extends Thread
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(NioServer.class);
	/**
	 * the selector of all socket channels open with providers or nodes.
	 */
	protected Selector selector;
	
	/**
	 * Reads resource files from the classpath.
	 */
	protected ResourceProvider resourceProvider = new ResourceProvider();
	
	/**
	 * Flag indicating that this socket server is closed.
	 */
	protected boolean stop = false;
	/**
	 * The port this socket server is listening to.
	 */
	protected int port = -1;
	/**
	 * The pool of threads used for submitting channel state transitions.
	 */
	protected ExecutorService executor = Executors.newFixedThreadPool(1);
	/**
	 * Timeout for the select() operations. A value of 0 means no timeout, i.e.
	 * the <code>Selector.select()</code> will be invoked without parameters.
	 */
	protected long selectTimeout = 0L;
	/**
	 * The factory for this server.
	 */
	protected NioServerFactory<S, T, U> factory = null;

	/**
	 * Initialize this server with a specified port number and name.
	 * @param port the port this socket server is listening to.
	 * @param name the name given to this thread.
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public NioServer(int port, String name) throws JPPFException
	{
		super(name);
		
		this.port = port;
		factory = createFactory();
		init(port);
	}

	/**
	 * Create the factory holding all the states and transition mappings.
	 * @return an <code>NioServerFactory</code> instance.
	 */
	protected abstract NioServerFactory<S, T, U> createFactory();

	/**
	 * Initialize the underlying server socket with a specified port.
	 * @param port the port the underlying server listens to.
	 * @throws JPPFException if the server socket can't be opened on the specified port.
	 */
	protected final void init(int port) throws JPPFException
	{
		Exception e = null;
		try
		{
			ServerSocketChannel server = ServerSocketChannel.open();
			int size = 32*1024;
			server.socket().setReceiveBufferSize(size);
			server.socket().bind(new InetSocketAddress(port));
			server.configureBlocking(false);
			selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
		}
		catch(IllegalArgumentException iae)
		{
			e = iae;
		}
		catch(IOException ioe)
		{
			e = ioe;
		}
		if (e != null)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * Start the underlying server socket by making it accept incoming connections.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			boolean hasTimeout = selectTimeout > 0L;
			while (!stop && !JPPFDriver.getInstance().isShuttingDown())
			{
				int n = 0;
				n =  hasTimeout ? selector.select(selectTimeout) : selector.select();
				if (n > 0) go(selector.selectedKeys());
				postSelect();
			}
			end();
		}
		catch (Throwable t)
		{
			log.error(t.getMessage(), t);
			end();
		}
	}

	/**
	 * Process the keys selected by the selector for IO operations.
	 * @param selectedKeys the set of keys thast were selected by the latest <code>select()</code> invocation.
	 * @throws Exception if an error is raised while processing the keys.
	 */
	public void go(Set<SelectionKey> selectedKeys) throws Exception
	{
		Iterator<SelectionKey> it = selectedKeys.iterator();
		while (it.hasNext())
		{
			SelectionKey key = it.next();
			it.remove();
			try
			{
				if (key.isAcceptable()) doAccept(key);
				else submitTransition(key, false);
			}
			catch (Exception e)
			{
				log.error(e.getMessage(), e);
				if (!(key.channel() instanceof ServerSocketChannel))
				{
					try
					{
						key.channel().close();
					}
					catch (Exception e2)
					{
						log.error(e2.getMessage(), e2);
					}
				}
			}
		}
	}

	/**
	 * This method is invoked after all selected keys have been processed.
	 * This implementation does nothing. Subclasses should override this method as needed.
	 */
	protected void postSelect()
	{
	}

	/**
	 * Submit the next state transition for a specified channel.
	 * @param key the selection key that references the channel.
	 * @param sequential determines whether the execution should be sequential or parallel.
	 */
	protected void submitTransition(SelectionKey key, boolean sequential)
	{
		key.interestOps(0);
		if (sequential) new StateTransitionTask<S, T, U>(key, factory).run();
		else executor.submit(new StateTransitionTask<S, T, U>(key, factory));
	}

	/**
	 * accept the incoming connection.
	 * It accept and put it in a state to define what type of peer is.
	 * @param key the selection key that represents the channel's registration witht the selector.
	 */
	private void doAccept(SelectionKey key)
	{
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel;
		try
		{
			channel = serverSocketChannel.accept();
		}
		catch (IOException ignored)
		{
			log.error(ignored.getMessage(), ignored);
			return;
		}
		if (channel == null) return;
		try
		{
			channel.configureBlocking(false);
		}
		catch (IOException e)
		{
			log.error(e.getMessage(), e);
			try
			{
				channel.close();
			}
			catch (IOException ignored)
			{
				log.error(ignored.getMessage(), ignored);
			}
			return;
		}
		NioContext context = createNioContext();
		try
		{
			SelectionKey selKey = channel.register(selector,	SelectionKey.OP_READ | SelectionKey.OP_WRITE, context);
			selKey.interestOps(getInitialInterest());
			postAccept(selKey);
		}
		catch (ClosedChannelException e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param key the selection key for the socket channel to process.
	 */
	public abstract void postAccept(SelectionKey key);

	/**
	 * Define a context for a newly created channel.
	 * @return an <code>NioContext</code> instance.
	 */
	public abstract NioContext createNioContext();

	/**
	 * Get the IO operations a connection is initially interested in.
	 * @return a bit-wise combination of the interests, taken from {@link java.nio.channels.SelectionKey SelectionKey}
	 * constants definitions.
	 */
	public abstract int getInitialInterest();
	
	/**
	 * Close the underlying server socket and stop this socket server.
	 */
	public synchronized void end()
	{
		if (!stop)
		{
			stop = true;
			removeAllConnections();
		}
	}

	/**
	 * Close and remove all connections accepted by this server.
	 */
	public synchronized void removeAllConnections()
	{
		if (!stop) return;
		for (SelectionKey connection: selector.keys())
		{
			try
			{
				connection.channel().close();
			}
			catch (IOException ignored)
			{
				log.error(ignored.getMessage(), ignored);
			}
		}
	}

	/**
	 * Get the selector for this server.
	 * @return a Selector instance.
	 */
	public Selector getSelector()
	{
		return selector;
	}

	/**
	 * Get the factory for this server.
	 * @return an <code>NioServerFactory</code> instance.
	 */
	public NioServerFactory getFactory()
	{
		return factory;
	}
}
