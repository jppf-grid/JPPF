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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClassLoader extends URLClassLoader
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(AbstractJPPFClassLoader.class);
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
	public AbstractJPPFClassLoader(ClassLoader parent)
	{
		super(new URL[0], parent);
		if (parent instanceof AbstractJPPFClassLoader) dynamic = true;
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 * @param uuidPath unique identifier for the submitting application.
	 */
	public AbstractJPPFClassLoader(ClassLoader parent, List<String> uuidPath)
	{
		this(parent);
		this.uuidPath = uuidPath;
	}

	/**
	 * Initialize the underlying socket connection.
	 */
	protected abstract void init();

	/**
	 * Load a JPPF class from the server.
	 * @param name the binary name of the class
	 * @return the resulting <tt>Class</tt> object
	 * @throws ClassNotFoundException if the class could not be found
	 */
	public synchronized Class<?> loadJPPFClass(String name) throws ClassNotFoundException
	{
		try
		{
			if (name.indexOf("JPPFMBeanProvider") >= 0)
			{
				int breakpoint = 0;
			}
			if (debugEnabled) log.debug("looking up resource [" + name + "]");
			Class<?> c = findLoadedClass(name);
			if (c != null)
			{
				ClassLoader cl = c.getClassLoader();
				if (debugEnabled) log.debug("classloader = " + cl);
			}
			/*
			if (c == null)
			{
				ClassLoader parent = getParent();
				if (parent instanceof AbstractJPPFClassLoader) c = ((AbstractJPPFClassLoader) parent).findLoadedClass(name);
			}
			*/
			if (c == null)
			{
				if (debugEnabled) log.debug("resource [" + name + "] not already loaded");
				c = findClass(name);
			}
			if (debugEnabled) log.debug("definition for resource [" + name + "] : " + c);
			return c;
		}
		catch(NoClassDefFoundError e)
		{
			throw e;
		}
	}

	/**
	 * Find a class in this class loader's classpath.
	 * @param name binary name of the resource to find.
	 * @return a defined <code>Class</code> instance.
	 * @throws ClassNotFoundException if the class could not be loaded.
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	public Class<?> findClass(String name) throws ClassNotFoundException
	{
		try
		{
			LOCK.lock();
			int i = name.lastIndexOf('.');
			if (i >= 0)
			{
				String pkgName = name.substring(0, i);
				Package pkg = getPackage(pkgName);
				if (pkg == null)
				{
					definePackage(pkgName, null, null, null, null, null, null, null);
				}
			}
			if (debugEnabled) log.debug("looking up definition for resource [" + name + "]");
			byte[] b = null;
			String resName = name.replace('.', '/') + ".class";
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("name", resName);
			JPPFResourceWrapper resource = loadResourceData(map, false);
			if (resource == null) throw new ClassNotFoundException("could not find reosurce " + name);
			b = resource.getDefinition();
			if ((b == null) || (b.length == 0))
			{
				if (debugEnabled) log.debug("definition for resource [" + name + "] not found");
				throw new ClassNotFoundException("Could not load class '" + name + "'");
			}
			if (debugEnabled) log.debug("found definition for resource [" + name + ", definitionLength=" + b.length + "]");
			return defineClass(name, b, 0, b.length);
		}
		finally
		{
			LOCK.unlock();
		}
	}

	/**
	 * Load the specified class from a socket connection.
	 * @param map - contains the necessary resource request data.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return a <code>JPPFResourceWrapper</code> containing the resource content.
	 * @throws ClassNotFoundException if the class could not be loaded from the remote server.
	 */
	private JPPFResourceWrapper loadResourceData(Map<String, Object> map, boolean asResource) throws ClassNotFoundException
	{
		JPPFResourceWrapper resource = null;
		try
		{
			if (debugEnabled) log.debug("loading remote definition for resource [" + map.get("name") + "]");
			resource = loadResourceData0(map, asResource);
		}
		catch(IOException e)
		{
			if (debugEnabled) log.debug("connection with class server ended, re-initializing");
			init();
			try
			{
				resource = loadResourceData0(map, asResource);
			}
			catch(ClassNotFoundException ex)
			{
				throw ex;
			}
			catch(Exception ex)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
			}
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
	 * @param map - contains the necessary resource request data.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return a <code>JPPFResourceWrapper</code> containing the resource content.
	 * @throws Exception if the connection was lost and could not be reestablished.
	 */
	private JPPFResourceWrapper loadResourceData0(Map<String, Object> map, boolean asResource) throws Exception
	{
		if (debugEnabled) log.debug("loading remote definition for resource [" + map.get("name") + "], requestUuid = " + requestUuid);
		JPPFResourceWrapper resource = loadRemoteData(map, false);
		if (debugEnabled) log.debug("remote definition for resource [" + map.get("name") + "] "+ (resource.getDefinition()==null ? "not " : "") + "found");
		return resource;
	}

	/**
	 * Request the remote computation of a <code>JPPFCallable</code> on the client.
	 * @param callable - the serialized callable to execute remotely.
	 * @return an array of bytes containing the result of the callable's execution.
	 * @throws Exception if the connection was lost and could not be reestablished.
	 */
	public byte[] computeRemoteData(byte[] callable) throws Exception
	{
		try
		{
			LOCK.lock();
			if (debugEnabled) log.debug("requesting remote computation, requestUuid = " + requestUuid);
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("callable", callable);
			byte[] b = loadRemoteData(map, false).getCallable();
			if (debugEnabled) log.debug("remote definition for collable resource "+ (b==null ? "not " : "") + "found");
			return b;
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
	protected abstract JPPFResourceWrapper loadRemoteData(Map<String, Object> map, boolean asResource) throws Exception;

	/**
	 * Finds the resource with the specified name.
	 * The resource lookup order is the same as the one specified by {@link #getResourceAsStream(String)} 
	 * @param name the name of the resource to find.
	 * @return the URL of the resource.
	 * @see java.lang.ClassLoader#findResource(java.lang.String)
	 */
	public URL findResource(String name)
	{
		URL url = null;
		if (debugEnabled) log.debug("resource [" + name + "] not found locally, attempting remote lookup");
		try
		{
			Enumeration<URL> urlEnum = findResources(name);
			if ((urlEnum != null) && urlEnum.hasMoreElements()) url = urlEnum.nextElement();
		}
		catch(IOException e)
		{
		}
		finally
		{
			//lock.unlock();
		}
		if (debugEnabled) log.debug("resource [" + name + "] " + (url == null ? "not " : "") + "found remotely");
		return url;
	}

	/**
	 * Get a stream from a resource file accessible form this class loader.
	 * The lookup order is defined as follows:
	 * <ul>
	 * <li>locally, in the classpath for this class loader, such as specified by {@link java.lang.ClassLoader#getResourceAsStream(java.lang.String) ClassLoader.getResourceAsStream(String)}<br>
	 * <li>if the parent of this class loader is NOT an instance of {@link AbstractJPPFClassLoader},
	 * in the classpath of the <i>JPPF driver</i>, such as specified by {@link org.jppf.classloader.ResourceProvider#getResourceAsBytes(java.lang.String, java.lang.ClassLoader) ResourceProvider.getResourceAsBytes(String, ClassLoader)}</li>
	 * (the search may eventually be sped up by looking up the driver's resource cache first)</li>
	 * <li>if the parent of this class loader IS an instance of {@link AbstractJPPFClassLoader},
	 * in the <i>classpath of the JPPF client</i>, such as specified by {@link org.jppf.classloader.ResourceProvider#getResourceAsBytes(java.lang.String, java.lang.ClassLoader) ResourceProvider.getResourceAsBytes(String, ClassLoader)}
	 * (the search may eventually be sped up by looking up the driver's resource cache first)</li>
	 * </ul>
	 * @param name name of the resource to obtain a stream from. 
	 * @return an <code>InputStream</code> instance, or null if the resource was not found.
	 * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
	 */
	public InputStream getResourceAsStream(String name)
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream(name);
		//if ((is == null) && !loading.get())
		if (is == null)
		{
			if (debugEnabled) log.debug("resource [" + name + "] not found locally, attempting remote lookup");
			try
			{
				LOCK.lock();
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("name", name);
				JPPFResourceWrapper resource = loadResourceData(map, true);
				byte[] b = resource.getDefinition();
				boolean found = (b != null) && (b.length > 0);
				if (debugEnabled) log.debug("resource [" + name + "] " + (found ? "" : "not ") + "found remotely");
				if (!found) return null;
				is = new ByteArrayInputStream(b);
			}
			catch(ClassNotFoundException e)
			{
				if (debugEnabled) log.debug("resource [" + name + "] not found remotely");
				return null;
			}
			finally
			{
				LOCK.unlock();
			}
		}
		return is;
	}

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
	 * Find all resources with the specified name.
	 * @param name name of the resources to find in the clas loader's classpath. 
	 * @return An enumeration of URLs pointing to the resources found.
	 * @throws IOException if an error occurs.
	 * @see java.lang.ClassLoader#findResources(java.lang.String)
	 */
	public Enumeration<URL> findResources(String name) throws IOException
	{
		List<URL> urlList = null;
		//if (loading.get()) return null;
		if (debugEnabled) log.debug("resource [" + name + "] not found locally, attempting remote lookup");
		try
		{
			LOCK.lock();
			List<String> locationsList = cache.getResourcesLocations(name);
			if (locationsList == null)
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("name", name);
				map.put("multiple", "true");
				JPPFResourceWrapper resource = loadResourceData(map, true);
				List<byte[]> dataList = (List<byte[]>) resource.getData("resource_list");
				boolean found = (dataList != null) && !dataList.isEmpty();
				if (debugEnabled) log.debug("resource [" + name + "] " + (found ? "" : "not ") + "found remotely");
				if (found)
				{
					cache.registerResources(name, dataList);
					urlList = new ArrayList<URL>();
					locationsList = cache.getResourcesLocations(name);
				}
			}
			if (locationsList != null)
			{
				for (String path: locationsList)
				{
					File file = new File(path);
					urlList.add(file.toURI().toURL());
				}
				if (debugEnabled) log.debug("found the following URLs for resource [" + name + "] : " + urlList);
			}
		}
		catch(IOException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug("resource [" + name + "] not found remotely");
		}
		finally
		{
			LOCK.unlock();
		}
		return urlList == null ? null : new IteratorEnumeration<URL>(urlList.iterator());
	}

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
}
