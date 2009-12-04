/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.server.peer;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Vector;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.*;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.*;

/**
 * This class represents a connection to the class server of a remote JPPF driver (peer driver).
 * @author Laurent Cohen
 */
public class PeerResourceProvider extends AbstractSocketChannelHandler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(PeerResourceProvider.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The name of the peer in the configuration file.
	 */
	private String peerName = null;

	/**
	 * Initialize this peer provider with the specified configuration name.
	 * @param peerName the name of the peer in the configuration file.
	 * @param server the NioServer to which the channel is registred.
	 */
	public PeerResourceProvider(String peerName, NioServer server)
	{
		super(server);
		this.peerName = peerName;
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void postInit() throws Exception
	{
		try
		{
			JPPFResourceWrapper resource = new JPPFResourceWrapper();
			resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
			socketClient.send(resource);
			if (debugEnabled) log.debug("sent node initiation");
			// get a response containing the uuid of the contacted peer
			resource = (JPPFResourceWrapper) socketClient.receive();
			if (debugEnabled) log.debug("received node initiation response");

			SocketChannel channel = socketClient.getChannel();
			socketClient.setChannel(null);
			ClassContext context = (ClassContext) server.createNioContext();
			//context.setState(ClassState.SENDING_PROVIDER_REQUEST);
			context.setState(ClassState.IDLE_PROVIDER);
			context.setPendingRequests(new Vector<SelectionKey>());
			context.setUuid(resource.getProviderUuid());
			server.getTransitionManager().registerChannel(channel, 0, context);
			((ClassNioServer) server).addProviderConnection(resource.getProviderUuid(), channel);
			if (debugEnabled) log.debug("registered class server channel");
		}
		catch (IOException e)
		{
			log.error(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize the socket channel client.
	 * @return a non-connected <code>SocketChannelClient</code> instance.
	 * @throws Exception if an error is raised during initialization.
	 */
	public SocketChannelClient initSocketChannel() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.peer." + peerName + ".server.host", "localhost");
		int port = props.getInt("class.peer." + peerName + ".server.port", 11111);
		SocketChannelClient client = new SocketChannelClient(host, port, false);
		return client;
	}
}
