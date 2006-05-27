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
package org.jppf.classloader;

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.node.JPPFBootstrapException;
import org.jppf.server.*;
import org.jppf.utils.*;

/**
 * This class is a an important part of the remote class loading mechanism. Its goal is to manage connections that
 * request classes to be loaded by the nodes, as well as the client connections that will serve those requests.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class ClassServer extends JPPFNIOServer
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ClassServer.class);
	/**
	 * A mapping of the remote resource provider connections handled by this socket server, to their unique uuid.<br>
	 * Provider connections represent connections form the clients only. The mapping to a uuid is required to determine in
	 * which application classpath to look for the requested resources.
	 */
	protected Map<String, SocketChannel> providerConnections = new Hashtable<String, SocketChannel>();
	/**
	 * The cache of class definition, this is done to not flood the provider when it dispatch many tasks. it use
	 * WeakHashMap to minimize the OutOfMemory.
	 */
	Map<CacheClassKey, CacheClassContent> classCache = new WeakHashMap<CacheClassKey, CacheClassContent>();

	/**
	 * Initialize this class server with the port it will listen to.
	 * @param port the prot number as an int value.
	 * @throws JPPFBootstrapException if this server could not be initialized.
	 */
	public ClassServer(int port) throws JPPFBootstrapException
	{
		super(port, "ClassServer Thread");
	}

	/**
	 * Close and remove all connections accepted by this server.
	 */
	public synchronized void removeAllConnections()
	{
		if (!stop) return;
		providerConnections.clear();
		super.removeAllConnections();
	}

	/**
	 * Start this class server from the command line.
	 * @param args not used.
	 */
	public static void main(String... args)
	{
		try
		{
			TypedProperties props = JPPFConfiguration.getProperties();
			new ClassServer(props.getInt("class.server.port", 11111)).start();
		}
		catch(Throwable t)
		{
			log.error(t.getMessage(), t);
			t.printStackTrace();
		}
	}

	/**
	 * Get the initial state of a connection to the class server.
	 * @return a <code>State</code> instance.
	 * @see org.jppf.server.JPPFNIOServer#getInitialState()
	 */
	protected ChannelState getInitialState()
	{
		return DefiningType;
	}

	/**
	 * Get the IO operations a class server connection is initially interested in.
	 * @return {@link java.nio.channels.SelectionKey.OP_READ SelectionKey.OP_READ}.
	 * @see org.jppf.server.JPPFNIOServer#getInitialInterest()
	 */
	protected int getInitialInterest()
	{
		return SelectionKey.OP_READ;
	}

	/**
	 * Get the initial content to send over the connection.
	 * @return a <code>Request</code> instance.
	 * @see org.jppf.server.JPPFNIOServer#getInitialContent()
	 */
	protected Object getInitialContent()
	{
		return new Request();
	}

	/**
	 * Called after a connection to the class server has been accepted by the server socket channel. This method does
	 * nothing.
	 * @param client the <code>SocketChannel</code> that was accepted.
	 * @see org.jppf.server.JPPFNIOServer#postAccept(java.nio.channels.SocketChannel)
	 */
	protected void postAccept(SocketChannel client)
	{
	}

	// ====================================================
	// classes related to the state machine of channels
	// ====================================================
	/**
	 * Initial state of all channel, where it's type is not yet defined, it can be a provider or a node channel.
	 */
	ChannelState DefiningType = new CDefiningType(this);
	/**
	 * State of a channel to a node, it is waiting for request from node.
	 */
	ChannelState WaitingRequest = new CWaitingRequest(this);
	/**
	 * State of a channel to a node where there is a class definition been send.
	 */
	ChannelState SendingNodeData = new CSendingNodeData(this);
	/**
	 * State of channel with providers, where the request is been send to a provider.
	 */
	ChannelState SendingRequest = new CSendingRequest(this);
	/**
	 * State of channel with providers, where the provider is sending the class definition.
	 */
	ChannelState ReceivingResource = new CReceivingResource(this);

	/**
	 * Create a <code>ByteBuffer</code> filled with the specified data.
	 * Before being returned, the buffer's position is set to 0.
	 * @param data the data used to fill the buffer.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	ByteBuffer createByteBuffer(byte[] data)
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(data.length + 4);
		buffer.putInt(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}
}
