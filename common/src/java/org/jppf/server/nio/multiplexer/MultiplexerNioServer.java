/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.server.nio.multiplexer;

import java.nio.channels.*;

import org.jppf.JPPFException;
import org.jppf.server.nio.*;

/**
 * 
 * @author Laurent Cohen
 */
public class MultiplexerNioServer extends NioServer<MultiplexerState, MultiplexerTransition, MultiplexerNioServer>
{
	/**
	 * Name given to this thread.
	 */
	private static final String THIS_NAME = "MultiplexerServer Thread";

	/**
	 * Initialize this server.
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public MultiplexerNioServer() throws JPPFException
	{
		super(THIS_NAME);
	}

	/**
	 * Initialize this server with a specified port number and name.
	 * @param port the port this socket server is listening to.
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public MultiplexerNioServer(int port) throws JPPFException
	{
		this(new int[] { port });
	}

	/**
	 * Initialize this server with the specified port numbers and name.
	 * @param ports the ports this socket server is listening to.
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public MultiplexerNioServer(int[] ports) throws JPPFException
	{
		super(ports, THIS_NAME);
	}

	/**
	 * Create the factory holding all the states and transition mappings.
	 * @return an <code>NioServerFactory</code> instance.
	 * @see org.jppf.server.nio.NioServer#createFactory()
	 */
	protected NioServerFactory<MultiplexerState, MultiplexerTransition, MultiplexerNioServer> createFactory()
	{
		return new MultiplexerServerFactory(this);
	}

	/**
	 * Define a context for a newly created channel.
	 * @return an <code>NioContext</code> instance.
	 * @see org.jppf.server.nio.NioServer#createNioContext()
	 */
	public NioContext createNioContext()
	{
		return new MultiplexerContext();
	}

	/**
	 * Get the IO operations a connection is initially interested in.
	 * @return a bit-wise combination of the interests, taken from
	 * {@link java.nio.channels.SelectionKey SelectionKey} constants definitions.
	 * @see org.jppf.server.nio.NioServer#getInitialInterest()
	 */
	public int getInitialInterest()
	{
		return 0;
	}

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param key the selection key for the socket channel to process.
	 * @param serverChannel the ServerSocketChannel that accepted the channel.
	 * @see org.jppf.server.nio.NioServer#postAccept(java.nio.channels.SelectionKey, java.nio.channels.ServerSocketChannel)
	 */
	public void postAccept(SelectionKey key, ServerSocketChannel serverChannel)
	{
		int port = serverChannel.socket().getLocalPort();
		MultiplexerContext context = (MultiplexerContext) key.attachment();
		postAccept(key);
	}

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param key the selection key for the socket channel to process.
	 * @see org.jppf.server.nio.NioServer#postAccept(java.nio.channels.SelectionKey)
	 */
	public void postAccept(SelectionKey key)
	{
	}
}
