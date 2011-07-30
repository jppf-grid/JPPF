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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClassLoaderLifeCycle extends URLClassLoader
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(AbstractJPPFClassLoaderLifeCycle.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	protected static final ReentrantLock LOCK = new ReentrantLock();
	/**
	 * Determines whether this class loader should handle dynamic class updating.
	 */
	protected static final AtomicBoolean INITIALIZING = new AtomicBoolean(false);
	/**
	 * The executor that handles asynchronous resource requests.
	 */
	protected static ExecutorService executor;
	/**
	 * Determines whether this class loader should handle dynamic class updating.
	 */
	protected boolean dynamic = false;
	/**
	 * The unique identifier for the submitting application.
	 */
	protected List<String> uuidPath = new ArrayList<String>();
	/**
	 * Uuid of the orignal task bundle that triggered a resource loading request. 
	 */
	protected String requestUuid = null;
	/**
	 * The cache handling resources temporarily stored to file.
	 */
	protected ResourceCache cache = new ResourceCache();
	/**
	 * The object used to serialize and deserialize resources.
	 */
	protected ObjectSerializer serializer = null;

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 */
	protected AbstractJPPFClassLoaderLifeCycle(ClassLoader parent)
	{
		super(StringUtils.ZERO_URL, parent);
		if (parent instanceof AbstractJPPFClassLoaderLifeCycle) dynamic = true;
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 * @param uuidPath unique identifier for the submitting application.
	 */
	protected AbstractJPPFClassLoaderLifeCycle(ClassLoader parent, List<String> uuidPath)
	{
		this(parent);
		this.uuidPath = uuidPath;
	}

	/**
	 * Initialize the underlying socket connection.
	 */
	protected abstract void init();
	/**
	 * Reset and reinitialize the connection ot the server. 
	 */
	public abstract void reset();

	/**
	 * Load the specified class from a socket connection.
	 * @param map contains the necessary resource request data.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return a <code>JPPFResourceWrapper</code> containing the resource content.
	 * @throws ClassNotFoundException if the class could not be loaded from the remote server.
	 */
	protected JPPFResourceWrapper loadResourceData(Map<String, Object> map, boolean asResource) throws ClassNotFoundException
	{
		JPPFResourceWrapper resource = null;
		try
		{
			if (debugEnabled) log.debug("loading remote definition for resource [" + map.get("name") + "]");
			resource = loadResourceData0(map, asResource);
		}
		catch(IOException e)
		{
			if (debugEnabled) log.debug("connection with class server ended, re-initializing, exception is:", e);
			throw new JPPFNodeReconnectionNotification("connection with class server ended, re-initializing, exception is:", e);
		}
		catch(ClassNotFoundException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
		return resource;
	}

	/**
	 * Load the specified class from a socket connection.
	 * @param map contains the necessary resource request data.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return a <code>JPPFResourceWrapper</code> containing the resource content.
	 * @throws Exception if the connection was lost and could not be reestablished.
	 */
	protected  JPPFResourceWrapper loadResourceData0(Map<String, Object> map, boolean asResource) throws Exception
	{
		if (debugEnabled) log.debug("loading remote definition for resource [" + map.get("name") + "], requestUuid = " + requestUuid);
		JPPFResourceWrapper resource = loadRemoteData(map, false);
		if (debugEnabled) log.debug("remote definition for resource [" + map.get("name") + "] "+ (resource.getDefinition()==null ? "not " : "") + "found");
		return resource;
	}

	/**
	 * Load the specified class from a socket connection.
	 * @param map contains the necessary resource request data.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return a <code>JPPFResourceWrapper</code> containing the resource content.
	 * @throws Exception if the connection was lost and could not be reestablished.
	 */
	protected abstract JPPFResourceWrapper loadRemoteData(Map<String, Object> map, boolean asResource) throws Exception;

	/**
	 * Determine whether the socket client is being initialized.
	 * @return true if the socket client is being initialized, false otherwise.
	 */
	static boolean isInitializing()
	{
		return INITIALIZING.get();
	}

	/**
	 * Set the socket client initialization status.
	 * @param initFlag true if the socket client is being initialized, false otherwise.
	 */
	static void setInitializing(boolean initFlag)
	{
		INITIALIZING.set(initFlag);
	}

	/**
	 * Set the uuid for the orignal task bundle that triggered this resource request. 
	 * @param requestUuid the uuid as a string.
	 */
	public void setRequestUuid(String requestUuid)
	{
		this.requestUuid = requestUuid;
	}

	/**
	 * Terminate this classloader and clean the resources it uses.
	 */
	public abstract void close();

	/**
	 * Get the object used to serialize and deserialize resources.
	 * @return an {@link ObjectSerializer} instance.
	 * @throws Exception if any error occurs.
	 */
	protected ObjectSerializer getSerializer() throws Exception
	{
		if (serializer == null) serializer = (ObjectSerializer) getParent().loadClass("org.jppf.comm.socket.BootstrapObjectSerializer").newInstance();
		return serializer;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addURL(URL url)
	{
		super.addURL(url);
	}

	/**
	 * Encapsulates a remote resource request submitted asynchronously
	 * via the single-thread executor.
	 */
	protected abstract class AbstractResourceRequest implements Runnable
	{
		/**
		 * Used to collect any throwable raised during communication with the server. 
		 */
		protected Throwable throwable = null;
		/**
		 * The request to send.
		 */
		protected JPPFResourceWrapper request = null;
		/**
		 * The response received.
		 */
		protected JPPFResourceWrapper response = null;

		/**
		 * Initialize with the specified request.
		 * @param request the request to send.
		 */
		public AbstractResourceRequest(JPPFResourceWrapper request)
		{
			this.request = request;
		}

		/**
		 * Get the throwable eventually raised during communication with the server. 
		 * @return a {@link Throwable} instance.
		 */
		public Throwable getThrowable()
		{
			return throwable;
		}

		/**
		 * Get the response received.
		 * @return a {@link JPPFResourceWrapper} instance.
		 * @throws Exception if any error occurs.
		 */
		public JPPFResourceWrapper getResponse() throws Exception
		{
			return response;
		}
	}
}
