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
package org.jppf.server.peer;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Vector;

import org.apache.commons.logging.*;
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
	private static Log log = LogFactory.getLog(PeerResourceProvider.class);
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
	private SocketInitializer socketInitializer = new SocketInitializerImpl();

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
