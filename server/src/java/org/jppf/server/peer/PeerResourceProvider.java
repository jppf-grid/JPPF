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
package org.jppf.server.peer;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jppf.comm.socket.*;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PeerResourceProvider
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(PeerResourceProvider.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The name of the peer in the configuration file.
	 */
	private String peerName = null;
	/**
	 * Wrapper around the underlying server connection.
	 */
	private SocketChannelClient socketClient = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private SocketInitializer socketInitializer = new SocketInitializer();

	/**
	 * Initialize this peer provider with the specified configuration name.
	 * @param peerName the name of the peer in the configuration file.
	 */
	public PeerResourceProvider(String peerName)
	{
		this.peerName = peerName;
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void init() throws Exception
	{
		if (socketClient == null) initSocketChannel();
		if (debugEnabled) log.debug("Attempting connection to the class server");
		socketInitializer.initializeSocket(socketClient);
		if (debugEnabled) log.debug("Connected to the class server");
		try
		{
			JPPFResourceWrapper resource = new JPPFResourceWrapper();
			resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
			socketClient.send(resource);
			if (debugEnabled) log.debug("sent node initiation");
			// get a response containing the uuid of the contacted peer
			resource = (JPPFResourceWrapper) socketClient.receive();
			if (debugEnabled) log.debug("received node initiation response");
			ClassNioServer server = JPPFDriver.getInstance().getClassServer();
			Selector selector = server.getSelector();

			SocketChannel channel = socketClient.getChannel();
			socketClient.setChannel(null);
			ClassContext context = (ClassContext) server.createNioContext();
			//context.setState(server.WAITING_NODE_REQUEST);
			context.setState(ClassState.SENDING_PROVIDER_REQUEST);
			context.setPendingRequests(new Vector<SelectionKey>());
			context.setUuid(resource.getProviderUuid());
			try
			{
				channel.register(selector, 0, context);
				server.addProviderConnection(resource.getProviderUuid(), channel);
				//channel.register(selector, SelectionKey.OP_READ, context);
				if (debugEnabled) log.debug("registered class server channel");
			}
			catch (ClosedChannelException ignored)
			{
				log.error(ignored.getMessage(), ignored);
			}
		}
		catch (IOException e)
		{
			log.error(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketChannel() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.peer." + peerName + ".server.host", "localhost");
		int port = props.getInt("class.peer." + peerName + ".server.port", 11111);
		socketClient = new SocketChannelClient(host, port, false);
	}
}
