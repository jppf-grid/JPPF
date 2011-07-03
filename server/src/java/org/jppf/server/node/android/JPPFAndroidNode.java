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
package org.jppf.server.node.android;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.comm.recovery.*;
import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 */
public class JPPFAndroidNode extends AbstractJPPFAndroidNode
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFAndroidNode.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Connection to the revoery server.
	 */
	private ClientConnection recoveryConnection = null;

	/**
	 * Default constructor.
	 */
	public JPPFAndroidNode()
	{
		super();
		classLoaderManager = new AnsdroidClassLoaderManager(this);
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 * @see org.jppf.server.node.JPPFNode#initDataChannel()
	 */
	protected void initDataChannel() throws Exception
	{
		if (socketClient == null)
		{
			if (debugEnabled) log.debug("Initializing socket");
			TypedProperties props = JPPFConfiguration.getProperties();
			String host = props.getString("jppf.server.host", "localhost");
			int port = props.getInt("node.server.port", 11113);
			socketClient = new SocketClient();
			//socketClient = new SocketConnectorWrapper();
			socketClient.setHost(host);
			socketClient.setPort(port);
			socketClient.setSerializer(serializer);
			if (debugEnabled) log.debug("end socket client initialization");
			if (debugEnabled) log.debug("start socket initializer");
			System.out.println("Attempting connection to the node server at " + host + ":" + port);
			socketInitializer.initializeSocket(socketClient);
			if (!socketInitializer.isSuccessfull())
			{
				if (debugEnabled) log.debug("socket initializer failed");
				throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver");
			}
			System.out.println("Reconnected to the node server");
			if (debugEnabled) log.debug("end socket initializer");
		}
		nodeIO = new AndroidNodeIO(this);
	}

	/**
	 * Initialize this node's data channel.
	 * @throws Exception if an error is raised during initialization.
	 * @see org.jppf.server.node.JPPFNode#closeDataChannel()
	 */
	protected void closeDataChannel() throws Exception
	{
		if (debugEnabled) log.debug("closing data channel: socketClient=" + socketClient + ", clientConnection=" + recoveryConnection);
		if (socketClient != null)
		{
			SocketWrapper tmp = socketClient;
			socketClient = null;
			tmp.close();
		}
		if (recoveryConnection != null)
		{
			//clientConnection.removeClientConnectionListener(this);
			ClientConnection tmp = recoveryConnection;
			recoveryConnection = null;
			tmp.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void clientConnectionFailed(ClientConnectionEvent event)
	{
		try
		{
			if (debugEnabled) log.debug("recovery connection failed, attempting to reconnect this node");
			closeDataChannel();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
