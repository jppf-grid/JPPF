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
package org.jppf.classloader;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.comm.socket.*;
import org.jppf.data.transform.*;
import org.jppf.node.NodeRunner;
import org.jppf.utils.*;
import org.slf4j.*;

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
	private static Logger log = LoggerFactory.getLogger(JPPFClassLoader.class);
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
	public JPPFClassLoader(final ClassLoader parent)
	{
		super(parent);
		init();
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 * @param uuidPath unique identifier for the submitting application.
	 */
	public JPPFClassLoader(final ClassLoader parent, final List<String> uuidPath)
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
		// for backward compatibility with v2.x configurations
		int port = props.getAndReplaceInt("jppf.server.port", "class.server.port", -1, false);
		socketClient = new BootstrapSocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}

	/**
	 * Initialize the underlying socket connection.
	 */
	@Override
	protected void init()
	{
		LOCK.lock();
		try
		{
			if (INITIALIZING.compareAndSet(false, true))
			{
				synchronized(AbstractJPPFClassLoaderLifeCycle.class)
				{
					if (executor == null) executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("ClassloaderRequests"));
				}
				try
				{
					if (debugEnabled) log.debug("initializing connection");
					if (socketClient == null) initSocketClient();
					System.out.println("Attempting connection to the class server at " + socketClient.getHost() + ':' + socketClient.getPort());
					socketInitializer.initializeSocket(socketClient);
					if (!socketInitializer.isSuccessful())
					{
						socketClient = null;
						throw new JPPFNodeReconnectionNotification("Could not reconnect to the server");
					}

					// we need to do this in order to dramatically simplify the state machine of ClassServer
					try
					{
						if (debugEnabled) log.debug("sending channel identifier");
						socketClient.writeInt(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL);
						if (debugEnabled) log.debug("sending node initiation message");
						JPPFResourceWrapper resource = new JPPFResourceWrapper();
						resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
						resource.setData("node.uuid", NodeRunner.getUuid());
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
					System.out.println("Reconnected to the class server");
				}
				finally
				{
					INITIALIZING.set(false);
				}
			}
		}
		finally
		{
			LOCK.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset()
	{
		LOCK.lock();
		try
		{
			synchronized(JPPFClassLoader.class)
			{
				JPPFClassLoader.socketClient = null;
			}
			init();
		}
		finally
		{
			LOCK.unlock();
		}
	}

	/**
	 * Terminate this classloader and clean the resources it uses.
	 * @see org.jppf.classloader.AbstractJPPFClassLoader#close()
	 */
	@Override
	public void close()
	{
		LOCK.lock();
		try
		{
			synchronized(AbstractJPPFClassLoaderLifeCycle.class)
			{
				if (executor != null)
				{
					executor.shutdownNow();
					executor = null;
				}
			}
			synchronized(JPPFClassLoader.class)
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
	@Override
	protected JPPFResourceWrapper loadRemoteData(final Map<String, Object> map, final boolean asResource) throws Exception
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

		ResourceRequest request = new ResourceRequest(resource);
		Future<?> future = executor.submit(request);
		future.get();
		Throwable t = request.getThrowable();
		if (t != null)
		{
			if (t instanceof Exception) throw (Exception) t;
			throw (Error) t;
		}
		return request.getResponse();
	}

	/**
	 * Encapsulates a remote resource request submitted asynchronously
	 * via the single-thread executor.
	 */
	protected class ResourceRequest extends AbstractResourceRequest
	{
		/**
		 * The data transform to apply.
		 */
		protected JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		/**
		 * The object serializer to use.
		 */
		protected ObjectSerializer serializer;
		/**
		 * The data sent and received from the server.
		 */
		protected byte[] data;
		/**
		 * Initialize with the specified request.
		 * @param request the request to send.
		 * @throws Exception if any error occurs.
		 */
		public ResourceRequest(final JPPFResourceWrapper request) throws Exception
		{
			super(request);
			transform = JPPFDataTransformFactory.getInstance();
			serializer = getSerializer();
			data = serializer.serialize(request).getBuffer();
			if (transform != null) data = JPPFDataTransformFactory.transform(transform, true, data);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run()
		{
			try
			{
				socketClient.writeInt(data.length);
				socketClient.write(data, 0, data.length);
				socketClient.flush();
				data = socketClient.receiveBytes(0).getBuffer();
			}
			catch (Throwable t)
			{
				throwable = t;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public JPPFResourceWrapper getResponse() throws Exception
		{
			if (response == null)
			{
				if (transform != null) data = JPPFDataTransformFactory.transform(transform, false, data);
				response = (JPPFResourceWrapper) serializer.deserialize(data);
				data = null;
			}
			return response;
		}
	}
}
