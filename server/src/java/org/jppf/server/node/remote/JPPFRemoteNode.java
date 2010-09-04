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
package org.jppf.server.node.remote;

import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.classloader.*;
import org.jppf.comm.socket.SocketClient;
import org.jppf.node.NodeRunner;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 */
public class JPPFRemoteNode extends JPPFNode
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFRemoteNode.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Default constructor.
	 */
	public JPPFRemoteNode()
	{
		super();
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
			System.out.println("Attempting connection to the node server");
			socketInitializer.initializeSocket(socketClient);
			if (!socketInitializer.isSuccessfull()) throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver");
			System.out.println("Reconnected to the node server");
			if (debugEnabled) log.debug("end socket initializer");
		}
		nodeIO = new RemoteNodeIO(this);
	}

	/**
	 * Initialize this node's data channel.
	 * @throws Exception if an error is raised during initialization.
	 * @see org.jppf.server.node.JPPFNode#closeDataChannel()
	 */
	protected void closeDataChannel() throws Exception
	{
		if (socketClient != null) socketClient.close();
	}

	/**
	 * Create the class loader for this node.
	 * @return a {@link JPPFClassLoader} instance.
	 */
	protected AbstractJPPFClassLoader createClassLoader()
	{
		if (debugEnabled) log.debug("Initializing classloader");
		if (classLoader == null) classLoader = NodeRunner.getJPPFClassLoader();
		return classLoader;
	}

	/**
	 * @param uuidPath the uuid path containing the key to the container.
	 * Instatiate the callback used to create the class loader in each {@link JPPFRemoteContainer}.
	 * @return a {@link Callable} instance.
	 */
	protected Callable<AbstractJPPFClassLoader> newClassLoaderCreator(final List<String> uuidPath)
	{
		return new Callable<AbstractJPPFClassLoader>()
		{
			public AbstractJPPFClassLoader call()
			{
				return new JPPFClassLoader(getClassLoader(), uuidPath);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	protected JPPFRemoteContainer newJPPFContainer(List<String> uuidPath, AbstractJPPFClassLoader cl) throws Exception
	{
		return new JPPFRemoteContainer(socketClient, uuidPath, cl);
	}
}
