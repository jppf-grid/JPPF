/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
import java.nio.channels.SocketChannel;
import java.util.Vector;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.SocketChannelClient;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.JPPFIdentifiers;
import org.slf4j.*;

/**
 * This class represents a connection to the class server of a remote JPPF driver (peer driver).
 * @author Laurent Cohen
 */
class PeerResourceProvider extends AbstractSocketChannelHandler
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(PeerResourceProvider.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The name of the peer in the configuration file.
	 */
	private final String peerName;
	/**
	 * Peer connection information.
	 */
	private final JPPFConnectionInformation connectionInfo;

	/**
	 * Initialize this peer provider with the specified configuration name.
	 * @param peerName the name of the peer in the configuration file.
	 * @param connectionInfo peer connection information.
	 * @param server the NioServer to which the channel is registred.
	 */
	public PeerResourceProvider(final String peerName, final JPPFConnectionInformation connectionInfo, final NioServer server)
	{
		super(server);
		if(peerName == null || peerName.isEmpty()) throw new IllegalArgumentException("peerName is blank");
		if(connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");

		this.peerName = peerName;
		this.connectionInfo = connectionInfo;
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	@Override
	public synchronized void postInit() throws Exception
	{
		try
		{
			socketClient.writeInt(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL);
			JPPFResourceWrapper resource = new JPPFResourceWrapper();
			resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
			socketClient.send(resource);
			socketClient.flush();
			if (debugEnabled) log.debug("sent node initiation");
			// get a response containing the uuid of the contacted peer
			resource = (JPPFResourceWrapper) socketClient.receive();
			if (debugEnabled) log.debug("received node initiation response");

			SocketChannel channel = socketClient.getChannel();
			socketClient.setChannel(null);
			ClassContext context = (ClassContext) server.createNioContext();
			//context.setState(ClassState.SENDING_PROVIDER_REQUEST);
			context.setState(ClassState.IDLE_PROVIDER);
			context.setPendingRequests(new Vector<ChannelWrapper<?>>());
			context.setUuid(resource.getProviderUuid());
			ChannelWrapper wrapper = server.getTransitionManager().registerChannel(channel, 0, context, null);
			((ClassNioServer) server).addProviderConnection(resource.getProviderUuid(), wrapper);
			if (debugEnabled) log.debug("registered class server channel");
		}
		catch (IOException e)
		{
			log.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize the socket channel client.
	 * @return a non-connected <code>SocketChannelClient</code> instance.
	 * @throws Exception if an error is raised during initialization.
	 */
	@Override
	public SocketChannelClient initSocketChannel() throws Exception
	{
		String host = connectionInfo.host == null || connectionInfo.host.isEmpty() ? "localhost" : connectionInfo.host;
		int port = connectionInfo.serverPorts[0];
		return new SocketChannelClient(host, port, false);
	}
}
