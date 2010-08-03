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

import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.comm.socket.*;
import org.jppf.data.transform.*;
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
		init();
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
	 * Initialize the underlying socket connection.
	 */
	protected void init()
	{
		if (!isInitializing())
		{
			try
			{
				LOCK.lock();
				if (debugEnabled) log.debug("initializing connection");
				setInitializing(true);
				System.out.println("JPPFClassLoader.init(): attempting connection to the class server");
				if (socketClient == null) initSocketClient();
				socketInitializer.initializeSocket(socketClient);
				if (!socketInitializer.isSuccessfull())
					throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver");

				// we need to do this in order to dramatically simplify the state machine of ClassServer
				try
				{
					if (debugEnabled) log.debug("sending node initiation message");
					JPPFResourceWrapper resource = new JPPFResourceWrapper();
					resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
					ObjectSerializer serializer = socketClient.getSerializer();
					JPPFBuffer buf = serializer.serialize(resource);
					byte[] data = buf.getBuffer();
					data = JPPFDataTransformFactory.transform(true, data);
					socketClient.sendBytes(new JPPFBuffer(data, data.length));
					socketClient.flush();
					if (debugEnabled) log.debug("node initiation message sent, getting response");
					socketClient.receiveBytes(0);
					if (debugEnabled) log.debug("received node initiation response");
				}
				catch (IOException e)
				{
					throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver", e);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
				System.out.println("JPPFClassLoader.init(): Reconnected to the class server");
			}
			finally
			{
				LOCK.unlock();
				setInitializing(false);
			}
		}
		else
		{
			if (debugEnabled) log.debug("waiting for end of connection initialization");
			// wait until initialization is over.
			try
			{
				LOCK.lock();
			}
			finally
			{
				LOCK.unlock();
			}
		}
	}

	/**
	 * Terminate this classloader and clean the resources it uses.
	 * @see org.jppf.classloader.AbstractJPPFClassLoader#close()
	 */
	public void close()
	{
		LOCK.lock();
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
			LOCK.unlock();
		}
	}

	/**
	 * Load the specified class from a socket connection.
	 * @param map contains the necessary resource request data.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return a <code>JPPFResourceWrapper</code> containing the resource content.
	 * @throws Exception if the connection was lost and could not be reestablished.
	 */
	protected JPPFResourceWrapper loadRemoteData(Map<String, Object> map, boolean asResource) throws Exception
	{
		JPPFResourceWrapper resource = new JPPFResourceWrapper();
		resource.setState(JPPFResourceWrapper.State.NODE_REQUEST);
		resource.setDynamic(dynamic);
		TraversalList<String> list = new TraversalList<String>(uuidPath);
		resource.setUuidPath(list);
		if (list.size() > 0) list.setPosition(uuidPath.size()-1);
		for (Map.Entry<String, Object> entry: map.entrySet()) resource.setData(entry.getKey(), entry.getValue());
		resource.setAsResource(asResource);
		resource.setRequestUuid(requestUuid);

		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		ObjectSerializer serializer = getSerializer();
		JPPFBuffer buf = serializer.serialize(resource);
		byte[] data = buf.getBuffer();
		if (transform != null) data = JPPFDataTransformFactory.transform(transform, true, data);
		socketClient.writeInt(data.length);
		socketClient.write(data, 0, data.length);
		socketClient.flush();
		buf = socketClient.receiveBytes(0);
		data = buf.getBuffer();
		if (transform != null) data = JPPFDataTransformFactory.transform(transform, false, data);
		resource = (JPPFResourceWrapper) serializer.deserialize(data);
		return resource;
	}
}
