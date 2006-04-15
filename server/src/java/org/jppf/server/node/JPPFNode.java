/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.server.node;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jppf.comm.socket.SocketClient;
import org.jppf.comm.socket.SocketInitializer;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.node.JPPFClassLoader;
import org.jppf.node.MonitoredNode;
import org.jppf.node.NodeLauncher;
import org.jppf.node.event.NodeEvent;
import org.jppf.node.event.NodeListener;
import org.jppf.server.JPPFTaskBundle;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.JPPFBuffer;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.ObjectSerializer;
import org.jppf.utils.Pair;
import org.jppf.utils.SerializationHelper;
import org.jppf.utils.TypedProperties;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 */
public class JPPFNode implements MonitoredNode
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = null;
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
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = false;
	/**
	 * Used to programmatically stop this node. 
	 */
	private boolean stopped = false;
	/**
	 * The list of listeners that receive notifications from this node.
	 */
	private List<NodeListener> listeners = new ArrayList<NodeListener>();
	/**
	 * This flag is true if there is at least one listener, and false otherwise.
	 */
	private boolean notifying = false;

	/**
	 * Main processing loop of this node.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		log = log = Logger.getLogger(JPPFNode.class);
		debugEnabled = log.isDebugEnabled();
		stopped = false;
		while (!stopped)
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
		if (notifying) fireNodeEvent(NodeEvent.DISCONNECTED);
	}
	
	/**
	 * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
	 * receives it, executes it and sends the results back.
	 * @throws Exception if an error was raised from the underlying socket connection or the class loader.
	 */
	public void perform() throws Exception
	{
		while (!stopped)
		{
			Pair<JPPFTaskBundle, List<JPPFTask>> pair = readTask();
			if (notifying) fireNodeEvent(NodeEvent.START_EXEC);
			JPPFTaskBundle bundle = pair.first();
			List<JPPFTask> taskList = pair.second();

			for (JPPFTask task: taskList)
			{
				try
				{
					task.run();
				}
				catch(Exception e)
				{
					log.error(e.getMessage(), e);
					task.setException(e);
				}
				if (notifying) fireNodeEvent(NodeEvent.END_EXEC);
			}
			writeResults(bundle, taskList);
		}
	}
	
	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void init() throws Exception
	{
		if (socketClient == null) initSocketClient();
		if (notifying) fireNodeEvent(NodeEvent.START_CONNECT);
		System.out.println("JPPFNode.init(): Attempting connection to the JPPF driver");
		socketInitializer.initializeSocket(socketClient);
		if (notifying) fireNodeEvent(NodeEvent.END_CONNECT);
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
	 * @return a pair of <code>JPPFTaskBundle</code> and a <code>List</code> of <code>JPPFTask</code> instances.
	 * @throws Exception if a error is raised while reading the task data.
	 */
	private Pair<JPPFTaskBundle, List<JPPFTask>> readTask() throws Exception
	{
		JPPFBuffer buf = socketClient.receiveBytes(0);
		if (debugEnabled) log.debug("Total length read is "+buf.getLength()+" bytes");
		Object[] result = readObjects(buf.getBuffer());
		JPPFTaskBundle bundle = (JPPFTaskBundle) result[0];
		DataProvider dataProvider = (DataProvider) result[1];
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		for (int i=0; i<bundle.getTaskCount(); i++)
		{
			JPPFTask task = (JPPFTask) result[2+i];
			task.setDataProvider(dataProvider);
			taskList.add(task);
		}
		return new Pair<JPPFTaskBundle, List<JPPFTask>>(bundle, taskList);
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
			if (debugEnabled) log.debug(err.getMessage()+"; reloading classes", err);
		}
		catch(InvalidClassException e)
		{
			reload = true;
			if (debugEnabled) log.debug(e.getMessage()+"; reloading classes", e);
		}
		if (reload)
		{
			classLoader = null;
			initHelper();
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
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(bais);
		JPPFTaskBundle bundle = (JPPFTaskBundle) helper.readNextObject(dis, false);
		bundle.setNodeExecutionTime(System.currentTimeMillis());
		int count = bundle.getTaskCount();
		Object[] result = new Object[2+count];
		result[0] = bundle;
		String uuid = bundle.getAppUuid();
		result[1] = getContainer(uuid).deserializeObject(dis, true);
		for (int i=0; i<count; i++)
			result[2+i] = getContainer(uuid).deserializeObject(dis, true);
		dis.close();
		return result;
	}

	/**
	 * Write the execution results to the socket stream.
	 * @param bundle the task wrapper to send along.
	 * @param tasks the list of tasks with their result field updated.
	 * @throws Exception if an error occurs while writitng to the socket stream.
	 */
	private void writeResults(JPPFTaskBundle bundle, List<JPPFTask> tasks) throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);
		helper.writeNextObject(bundle, dos, false);
		for (JPPFTask task: tasks) helper.writeNextObject(task, dos, true);
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
			if (debugEnabled) log.debug("Creating new container for appuuid="+uuid);
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
	 * Add a listener to the list of listener for this node.
	 * @param listener the listener to add.
	 * @see org.jppf.node.MonitoredNode#addNodeListener(org.jppf.node.event.NodeListener)
	 */
	public void addNodeListener(NodeListener listener)
	{
		if (listener == null) return;
		listeners.add(listener);
		notifying = true;
	}

	/**
	 * Remove a listener from the list of listener for this node.
	 * @param listener the listener to remove.
	 * @see org.jppf.node.MonitoredNode#removeNodeListener(org.jppf.node.event.NodeListener)
	 */
	public void removeNodeListener(NodeListener listener)
	{
		if (listener == null) return;
		listeners.remove(listener);
		if (listeners.size() <= 0) notifying = false;
	}

	/**
	 * Notify all listeners that an event has occurred.
	 * @param eventType the type of the event as a string.
	 * @see org.jppf.node.MonitoredNode#fireNodeEvent(java.lang.String)
	 */
	public void fireNodeEvent(String eventType)
	{
		NodeEvent event = new NodeEvent(eventType);
		for (NodeListener listener: listeners) listener.eventOccurred(event);
	}
	
	/**
	 * Stop this node and release the resources it is using.
	 * @see org.jppf.node.MonitoredNode#stopNode()
	 */
	public void stopNode()
	{
		stopped = true;
		try
		{
			socketClient.close();
		}
		catch(Exception ex)
		{
			log.error(ex.getMessage(), ex);
		}
		socketClient = null;
		classLoader = null;
	}
}
