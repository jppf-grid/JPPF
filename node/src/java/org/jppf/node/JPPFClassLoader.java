/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.node;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.*;
import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.comm.socket.*;
import org.jppf.utils.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public class JPPFClassLoader extends ClassLoader
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
	 * Determines whether this class loader should handle dynamic class updating.
	 */
	private boolean dynamic = false;
	/**
	 * The unique identifier for the submitting application.
	 */
	private List<String> uuidPath = new ArrayList<String>();
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private static ReentrantLock lock = new ReentrantLock();
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private static SocketInitializer socketInitializer = new SocketInitializerImpl();
	/**
	 * Determines whether this class loader should handle dynamic class updating.
	 */
	private static AtomicBoolean initializing = new AtomicBoolean(false);
	/**
	 * Uuid of the orignal task bundle that triggered a resource loading request. 
	 */
	private String requestUuid = null;

	/**
	 * Default instanciation of this class is not permitted, a valid host name and port number MUST be provided.
	 */
	private JPPFClassLoader()
	{
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 */
	public JPPFClassLoader(ClassLoader parent)
	{
		super(parent);
		if (parent instanceof JPPFClassLoader) dynamic = true;
		if (socketClient == null) init();
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 * @param uuidPath unique identifier for the submitting application.
	 */
	public JPPFClassLoader(ClassLoader parent, List<String> uuidPath)
	{
		this(parent);
		this.uuidPath = uuidPath;
	}

	/**
	 * Initialize the underlying socket connection.
	 */
	private static void init()
	{
		if (!isInitializing())
		{
			if (debugEnabled) log.debug("initializing connection");
			setInitializing(true);
		}
		else
		{
			if (debugEnabled) log.debug("waiting for end of connection initialization");
			// wait until initialization is over.
			try
			{
				lock.lock();
			}
			finally
			{
				lock.unlock();
			}
			return;
		}
		try
		{
			lock.lock();
			System.out.println("JPPFClassLoader.init(): attempting connection to the class server");
			if (socketClient == null) initSocketClient();
			socketInitializer.initializeSocket(socketClient);
			if (!socketInitializer.isSuccessfull())
				throw new JPPFNodeReconnectionNotification("Could not reconnect to the driver");

			// we need to do this in order to dramatically simplify the 
			// state machine of ClassServer
			try
			{
				if (debugEnabled) log.debug("sending node initiation message");
				JPPFResourceWrapper resource = new JPPFResourceWrapper();
				resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
				socketClient.send(resource);
				socketClient.receive();
				if (debugEnabled) log.debug("received node initiation response");
			}
			/*
			catch(ClassNotFoundException e)
			{
				throw new RuntimeException(e);
			}
			*/
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
			lock.unlock();
			setInitializing(false);
		}
	}
	
	/**
	 * Initialize the underlying socket connection.
	 */
	private static void initSocketClient()
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
	 * Find a class already loaded by this class loader.
	 * @param name the binary name of the class
	 * @return the resulting <tt>Class</tt> object
	 */
	public synchronized Class<?> findAlreadyLoadedClass(String name)
	{
		return findLoadedClass(name);
	}

	/**
	 * @param name the binary name of the class
	 * @return the resulting <tt>Class</tt> object
	 * @throws ClassNotFoundException if the class could not be found
	 */
	public synchronized Class<?> loadJPPFClass(String name) throws ClassNotFoundException
	{
		if (debugEnabled) log.debug("looking up resource [" + name + "]");
		Class<?> c = findLoadedClass(name);
		if (c == null)
		{
			ClassLoader parent = getParent();
			if (parent instanceof JPPFClassLoader)
			{
				c = ((JPPFClassLoader) parent).findAlreadyLoadedClass(name);
			}
		}
		if (c == null)
		{
			if (debugEnabled) log.debug("resource [" + name + "] not already loaded");
			c = findClass(name);
		}
		if (debugEnabled) log.debug("definition for resource [" + name + "] : " + c);
		return c;
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
			if (debugEnabled) log.debug("looking up definition for resource [" + name + "]");
			byte[] b = null;
			String resName = name.replace('.', '/') + ".class";
			b = loadResourceData(resName, false);
			if ((b == null) || (b.length == 0))
			{
				if (debugEnabled) log.debug("definition for resource [" + name + "] not found");
				throw new ClassNotFoundException("Could not load class '" + name + "'");
			}
			if (debugEnabled) log.debug("found definition for resource [" + name + "]");
			return defineClass(name, b, 0, b.length);
		}
		catch(Error e)
		{
			throw e;
		}
	}

	/**
	 * Load the specified class from a socket conenction.
	 * @param name the binary name of the class to load, such as specified in the JLS.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return an array of bye containing the class' byte code.
	 * @throws ClassNotFoundException if the class could not be loaded from the remote server.
	 */
	private byte[] loadResourceData(String name, boolean asResource) throws ClassNotFoundException
	{
		byte[] b = null;
		try
		{
			if (debugEnabled) log.debug("loading remote definition for resource [" + name + "]");
			b = loadResourceData0(name, asResource);
		}
		catch(IOException e)
		{
			if (debugEnabled) log.debug("connection with class server ended, re-initializing");
			init();
			try
			{
				b = loadResourceData0(name, asResource);
			}
			catch(IOException ex)
			{
			}
			catch(ClassNotFoundException ex)
			{
				throw ex;
			}
			catch(Exception ex)
			{
			}
		}
		catch(ClassNotFoundException e)
		{
			throw e;
		}
		catch(Exception e)
		{
		}
		return b;
	}

	/**
	 * Load the specified class from a socket connection.
	 * @param name the binary name of the class to load, such as specified in the JLS.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return an array of bye containing the class' byte code.
	 * @throws Exception if the connection was lost and could not be reestablished.
	 */
	private byte[] loadResourceData0(String name, boolean asResource) throws Exception
	{
		byte[] b = null;
		try
		{
			if (debugEnabled) log.debug("loading remote definition for resource [" + name + "], requestUuid = " + requestUuid);
			lock.lock();
			JPPFResourceWrapper resource = new JPPFResourceWrapper();
			resource.setState(JPPFResourceWrapper.State.NODE_REQUEST);
			resource.setDynamic(dynamic);
			TraversalList<String> list = new TraversalList<String>(uuidPath);
			resource.setUuidPath(list);
			if (list.size() > 0) list.setPosition(uuidPath.size()-1);
			resource.setName(name);
			resource.setAsResource(asResource);
			resource.setRequestUuid(requestUuid);
			
			socketClient.send(resource);
			resource = (JPPFResourceWrapper) socketClient.receive();
			b = resource.getDefinition();
			if (b != null)
			{
				try
				{
					b = CompressionUtils.unzip(b, 0, b.length);
				}
				catch(Exception e)
				{
					b = null;
					log.error(e.getMessage(), e);
				}
			}
			if (debugEnabled) log.debug("remote definition for resource [" + name + "] "+ (b==null ? "not " : "") + "found");
		}
		finally
		{
			lock.unlock();
		}
		return b;
	}

	/**
	 * Finds the resource with the specified name.
	 * The resource lookup order is the same as the one specified by {@link #getResourceAsStream(String)} 
	 * @param name the name of the resource to find.
	 * @return the URL of the resource.
	 * @see java.lang.ClassLoader#getResource(java.lang.String)
	 */
	public URL getResource(String name)
	{
		URL url = null;
		if (debugEnabled) log.debug("resource [" + name + "] not found locally, attempting remote lookup");
		try
		{
			final byte[] b = loadResourceData(name, true);
			boolean found = (b != null) && (b.length > 0);
			if (debugEnabled) log.debug("resource [" + name + "] " + (found ? "" : "not ") + "found remotely");
			if (found)
			{
				File file = (File) AccessController.doPrivileged(new PrivilegedAction<Object>()
				{
					public Object run()
					{
						File tmp = null;
						try
						{
							tmp = File.createTempFile("jppftemp_", "tmp");
							tmp.deleteOnExit();
							BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmp));
							bos.write(b);
							bos.flush();
							bos.close();
						}
						catch(Exception e)
						{
							log.error(e.getMessage(), e);
						}
						return tmp;
					}
				});
				if (file != null)
				{
					try
					{
						//url = file.toURL();
						url = file.toURI().toURL();
						if (debugEnabled) log.debug("resource [" + name + "] found with URL: "+url);
					}
					catch (Exception e)
					{
						log.error(e.getMessage(), e);
					}
				}
			}
		}
		catch(ClassNotFoundException e)
		{
			if (debugEnabled) log.debug("resource [" + name + "] not found remotely");
		}
		return url;
	}

	/**
	 * Get a stream from a resource file accessible form this class loader.
	 * The lookup order is defined as follows:
	 * <ul>
	 * <li>locally, in the classpath for this class loader, such as specified by {@link java.lang.ClassLoader#getResourceAsStream(java.lang.String) ClassLoader.getResourceAsStream(String)}<br>
	 * <li>if the parent of this class loader is NOT an instance of {@link JPPFClassLoader},
	 * in the classpath of the <i>JPPF driver</i>, such as specified by
	 * {@link org.jppf.classloader.ResourceProvider#getResourceAsBytes(java.lang.String, java.lang.ClassLoader) ResourceProvider.getResourceAsBytes(String, ClassLoader)}</li>
	 * (the search may eventually be sped up by looking up the driver's resource cache first)</li>
	 * <li>if the parent of this class loader IS an instance of {@link JPPFClassLoader},
	 * in the <i>classpath of the JPPF client</i>, such as specified by
	 * {@link org.jppf.classloader.ResourceProvider#getResourceAsBytes(java.lang.String, java.lang.ClassLoader) ResourceProvider.getResourceAsBytes(String, ClassLoader)}
	 * (the search may eventually be sped up by looking up the driver's resource cache first)</li>
	 * </ul>
	 * @param name name of the resource to obtain a stream from. 
	 * @return an <code>InputStream</code> instance, or null if the resource was not found.
	 * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
	 */
	public InputStream getResourceAsStream(String name)
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream(name);
		if (is == null)
		{
			if (debugEnabled) log.debug("resource [" + name + "] not found locally, attempting remote lookup");
			try
			{
				byte[] b = loadResourceData(name, false);
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
		}
		return is;
	}

	/**
	 * Determine whether the socket client is being initialized.
	 * @return true if the socket client is being initialized, false otherwise.
	 */
	private static boolean isInitializing()
	{
		return initializing.get();
	}

	/**
	 * Set the socket client initialization status.
	 * @param initFlag true if the socket client is being initialized, false otherwise.
	 */
	private static void setInitializing(boolean initFlag)
	{
		initializing.set(initFlag);
	}

	/**
	 * Get the uuid for the orignal task bundle that triggered this resource request. 
	 * @return the uuid as a string.
	 */
	public String getRequestUuid()
	{
		return requestUuid;
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
	public void close()
	{
		if (socketInitializer != null) socketInitializer.close();
		if (socketClient != null)
		{
			try
			{
				socketClient.close();
			}
			catch(Exception ignore)
			{
			}
			socketClient = null;
		}
	}
}
