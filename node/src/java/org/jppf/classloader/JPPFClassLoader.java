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
package org.jppf.classloader;

import java.util.List;

import org.apache.commons.logging.*;
import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.comm.socket.*;
import org.jppf.utils.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public class JPPFClassLoader extends AbstractJPPFClassLoader
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFClassLoader.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Wrapper for the underlying socket connection.
	 */
	private static SocketWrapper socketClient = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private static SocketInitializer socketInitializer = new SocketInitializerImpl();

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 */
	public JPPFClassLoader(ClassLoader parent)
	{
		super(parent);
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 * @param uuidPath unique identifier for the submitting application.
	 */
	public JPPFClassLoader(ClassLoader parent, List<String> uuidPath)
	{
		super(parent, uuidPath);
	}

	/**
	 * Initialize the connection with the class server.
	 * @see org.jppf.classloader.AbstractJPPFClassLoader#initIoHandler()
	 */
	protected void initIoHandler()
	{
		setInitializing(true);
		if (debugEnabled) log.debug("initializing connection");
		System.out.println("JPPFClassLoader.init(): attempting connection to the class server");
		if (socketClient == null) initSocketClient();
		socketInitializer.initializeSocket(socketClient);
		if (!socketInitializer.isSuccessfull()) throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver");
		ioHandler = new BootstrapSocketIOHandler(socketClient);
	}

	/**
	 * Initialize the underlying socket connection.
	 */
	private void initSocketClient()
	{
		if (debugEnabled) log.debug("initializing socket connection");
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.server.host", "localhost");
		int port = props.getInt("class.server.port", 11111);
		socketClient = new BootstrapSocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}
	
	/**
	 * Terminate this classloader and clean the resources it uses.
	 * @see org.jppf.classloader.AbstractJPPFClassLoader#close()
	 */
	public void close()
	{
		lock.lock();
		try
		{
			if (socketInitializer != null) socketInitializer.close();
			if (socketClient != null)
			{
				try
				{
					socketClient.close();
				}
				catch(Exception e)
				{
					if (debugEnabled) log.debug(e.getMessage(), e);
				}
				socketClient = null;
			}
		}
		finally
		{
			lock.unlock();
		}
	}
}
