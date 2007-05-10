/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.node;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
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
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFClassLoader.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
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
	private static SocketInitializer socketInitializer = new SocketInitializer();
	/**
	 * Determines whether this class loader should handle dynamic class updating.
	 */
	private static boolean initializing = false;

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
		lock.lock();
		System.out.println("JPPFClassLoader.init(): attempting connection to the class server");
		try
		{
			if (socketClient == null) initSocketClient();
			socketInitializer.initializeSocket(socketClient);

			// we need to do this in order to dramaticaly simplify the 
			// state machine of ClassServer
			try
			{
				if (debugEnabled) log.debug("sending node initiation message");
				JPPFResourceWrapper resource = new JPPFResourceWrapper();
				resource.setState(JPPFResourceWrapper.State.NODE_INITIATION);
				socketClient.send(resource);
				socketClient.receive();
				if (debugEnabled) log.debug("received sending node initiation response");
			}
			catch(ClassNotFoundException e)
			{
				throw new RuntimeException(e);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			System.out.println("JPPFClassLoader.init(): Reconnected to the class server");
		}
		finally
		{
			setInitializing(false);
			lock.unlock();
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
	 * Load a class from the classpath.
	 * @param name the name of the class to load.
	 * @return a Class instance.
	 * @throws ClassNotFoundException if the class could not be found.
	 * @see java.lang.ClassLoader#loadClass(java.lang.String)
	 */
  /*
	public synchronized Class<?> loadClass(String name) throws ClassNotFoundException
  {
		// First, check if the class has already been loaded
		Class c = findLoadedClass(name);
		ClassLoader parent = getParent();
		if (c == null)
		{
			try
			{
				if (parent != null)
				{
					c = parent.loadClass(name);
				}
				else
				{
					c = findBootstrapClass0(name);
				}
			}
			catch (ClassNotFoundException e)
			{
				// If still not found, then invoke findClass in order to find the class.
				c = findClass(name);
			}
		}
		if (resolve)
		{
			resolveClass(c);
		}
		return c;
	}
	*/

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
		}
		return b;
	}

	/**
	 * Load the specified class from a socket connection.
	 * @param name the binary name of the class to load, such as specified in the JLS.
	 * @param asResource true if the resource is loaded using getResource(), false otherwise. 
	 * @return an array of bye containing the class' byte code.
	 * @throws ClassNotFoundException if the class could not be loaded from the remote server.
	 * @throws IOException if the connection was lost and could not be reestablished.
	 */
	private byte[] loadResourceData0(String name, boolean asResource) throws ClassNotFoundException, IOException
	{
		byte[] b = null;
		try
		{
			if (debugEnabled) log.debug("loading remote definition for resource [" + name + "]");
			lock.lock();
			JPPFResourceWrapper resource = new JPPFResourceWrapper();
			resource.setState(JPPFResourceWrapper.State.NODE_REQUEST);
			resource.setDynamic(dynamic);
			TraversalList<String> list = new TraversalList<String>(uuidPath);
			resource.setUuidPath(list);
			if (list.size() > 0) list.setPosition(uuidPath.size()-1);
			resource.setName(name);
			resource.setAsResource(asResource);
			
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
						url = file.toURL();
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
	 * Get a stream from a resource file in the classpath of this class loader.
	 * @param name name of the resource to obtain a stram from. 
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
	private static synchronized boolean isInitializing()
	{
		return initializing;
	}

	/**
	 * Set the socket client initialization status.
	 * @param initFlag true if the socket client is being initialized, false otherwise.
	 */
	private static synchronized void setInitializing(boolean initFlag)
	{
		initializing = initFlag;
	}
}
