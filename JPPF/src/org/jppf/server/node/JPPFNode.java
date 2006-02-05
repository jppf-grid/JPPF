/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.server.node;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.classloader.*;
import org.jppf.comm.socket.*;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 */
public class JPPFNode implements Runnable
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFNode.class);
	/**
	 * Maximum number of containers kept by this node's cache.
	 */
	private static final int MAX_CONTAINERS = 1000;
	/**
	 * Utility for deserialization and serialization.
	 */
	private ObjectSerializer serializer = null;
	/**
	 * Utility for deserialization and serialization.
	 */
	private SerializationHelper helper = null;
	/**
	 * Class loader used for dynamic loading and updating of client classes.
	 */
	private JPPFClassLoader classLoader = null;
	/**
	 * Wrapper around the underlying server connection.
	 */
	private SocketWrapper socketClient = null;
	/**
	 * Mapping of containers to their corresponding application uuid.
	 */
	private Map<String, JPPFContainer> containerMap = new HashMap<String, JPPFContainer>();
	/**
	 * A list retaining the container in chronological order of their creation.
	 */
	private List<JPPFContainer> containerList = new LinkedList<JPPFContainer>();
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private SocketInitializer socketInitializer = new SocketInitializer();

	/**
	 * Main processing loop of this node.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		while (true)
		{
			try
			{
				init();
				perform();
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
				try
				{
					socketClient.close();
				}
				catch(Exception ex)
				{
					log.error(ex.getMessage(), ex);
				}
			}
		}
	}
	
	/**
	 * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
	 * receives it, executes it and sends the results back.
	 * @throws Exception if an error was raised from the underlying socket connection or the class loader.
	 */
	public void perform() throws Exception
	{
		while (true)
		{
			Pair<JPPFTask, JPPFTaskWrapper> pair = readTask();
			JPPFTask task = pair.first();
			JPPFTaskWrapper wrapper = pair.second();

			try
			{
				task.run();
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
				task.setException(e);
			}
			writeResults(wrapper, task);
		}
	}
	
	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void init() throws Exception
	{
		if (socketClient == null) initSocketClient();
		System.out.println("JPPFNode.init(): Attempting connection to the JPPF driver");
		socketInitializer.initializeSocket(socketClient);
		System.out.println("JPPFNode.init(): Reconnected to the JPPF driver");
	}
	
	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketClient() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.server.host", "localhost");
		int port = props.getInt("node.server.port", 11113);
		initHelper();
		socketClient = new SocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
		socketClient.setSerializer(serializer);
	}
	
	/**
	 * Read a task from the socket connection, along with its header information.
	 * @return a pair of <code>JPPFTask</code> and <code>JPPFTaskWrapper</code> instances.
	 * @throws Exception if a error is raised while reading the task data.
	 */
	private Pair<JPPFTask, JPPFTaskWrapper> readTask() throws Exception
	{
		JPPFBuffer buf = socketClient.receiveBytes(0);
		log.debug("Total length read is "+buf.getLength()+" bytes");
		Object[] result = readObjects(buf.getBuffer());
		JPPFTaskWrapper wrapper = (JPPFTaskWrapper) result[0];
		JPPFTask task = (JPPFTask) result[1];
		DataProvider dataProvider = (DataProvider) result[2];
		task.setDataProvider(dataProvider);
		return new Pair<JPPFTask, JPPFTaskWrapper>(task, wrapper);
	}
	
	/**
	 * Deseralize the objects read from the socket, and reload the appropriate classes if any class change
	 * is detected.<br>
	 * A class change is triggered when an <code>InvalidClassException</code> is caught. Upon catching this exception,
	 * the class loader is reinitialized and the class are reloaded.  
	 * @param bytes the array of bytes from which the objects are deserialized.
	 * @return an array of 3 objects desrialied from the byte array.
	 * @throws Exception if the classes could not be reloaded or an error occurred during deserialization.
	 */
	private Object[] readObjects(byte[] bytes) throws Exception
	{
		Object[] result = null;
		boolean reload = false;
		try
		{
			result = deserializeObjects(bytes);
		}
		catch(IncompatibleClassChangeError err)
		{
			reload = true;
			log.debug(err.getMessage()+"; reloading classes", err);
		}
		catch(InvalidClassException e)
		{
			reload = true;
			log.debug(e.getMessage()+"; reloading classes", e);
		}
		if (reload)
		{
			resetHelper();
			socketClient.setSerializer(serializer);
			result = deserializeObjects(bytes);
		}
		return result;
	}
	
	/**
	 * Perform the deserialization of the objects received through the socket connection.
	 * @param bytes the buffer containing the serialized objects.
	 * @return an array of deserialized objects.
	 * @throws Exception if an error occurs while deserializing.
	 */
	private Object[] deserializeObjects(byte[] bytes) throws Exception
	{
		Object[] result = new Object[3];
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(bais);
		JPPFTaskWrapper wrapper = (JPPFTaskWrapper) helper.readNextObject(dis, false);
		result[0] = wrapper;
		String uuid = wrapper.getAppUuid();
		result[1] = getContainer(uuid).deserializeObject(dis, true);
		result[2] = getContainer(uuid).deserializeObject(dis, true);
		dis.close();
		return result;
	}

	/**
	 * Write the execution results to the socket stream.
	 * @param wrapper the task wrapper to send along.
	 * @param task the task with its result field updated.
	 * @throws Exception if an error occurs while writitng to the socket stream.
	 */
	private void writeResults(JPPFTaskWrapper wrapper, JPPFTask task) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		helper.writeNextObject(wrapper, dos, false);
		helper.writeNextObject(task, dos, true);
		dos.flush();
		dos.close();
		JPPFBuffer buf = new JPPFBuffer(baos.toByteArray(), baos.size());
		socketClient.sendBytes(buf);
	}

	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @return a <code>ClassLoader</code> used for loading the classes of the framework.
	 */
	private JPPFClassLoader getClassLoader()
	{
		if (classLoader == null)
		{
			classLoader = new JPPFClassLoader(NodeLauncher.getJPPFClassLoader());
		}
		return classLoader;
	}
	
	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @throws Exception if an error occcurs while instantiating the class loader.
	 */
	private void initHelper() throws Exception
	{
		Class c = getClassLoader().loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
		Object o = c.newInstance();
		serializer = (ObjectSerializer) o;
		c = getClassLoader().loadJPPFClass("org.jppf.utils.SerializationHelperImpl");
		o = c.newInstance();
		helper = (SerializationHelper) o;
	}
	
	/**
	 * Reset the current class loader so already loaded classes can be dynamically updated.
	 * @throws Exception if an error occcurs while instantiating the class loader.
	 */
	private void resetHelper() throws Exception
	{
		classLoader = null;
		initHelper();
	}
	
	/**
	 * Get a reference to the JPPF container associated with an application uuid.
	 * @param uuid the uuuid to find the container for.
	 * @return a <code>JPPFContainer</code> instance.
	 * @throws Exception if an error occcurs while getting the container.
	 */
	private JPPFContainer getContainer(String uuid) throws Exception
	{
		JPPFContainer container = containerMap.get(uuid);
		if (container == null)
		{
			log.debug("Creating new container for appuuid="+uuid);
			container = new JPPFContainer(uuid);
			if (containerList.size() >= MAX_CONTAINERS)
			{
				JPPFContainer toRemove = containerList.remove(0);
				containerMap.remove(toRemove.getAppUuid());
			}
			containerList.add(container);
			containerMap.put(uuid, container);
		}
		return container;
	}

	/**
	 * Starting point for this JPPFNode.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		new JPPFNode().run();
	}

	/**
	 * Wake up the thread waiting on this node's initialization.
	 */
	public synchronized void wakeUp()
	{
		notify();
	}
}