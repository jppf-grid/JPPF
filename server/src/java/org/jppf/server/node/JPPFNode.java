/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
package org.jppf.server.node;

import static org.jppf.server.protocol.BundleParameter.*;

import java.io.InvalidClassException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.*;
import org.jppf.*;
import org.jppf.comm.socket.SocketClient;
import org.jppf.management.*;
import org.jppf.node.*;
import org.jppf.node.event.NodeEventType;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class JPPFNode extends AbstractMonitoredNode
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFNode.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether dumping byte arrays in the log is enabled.
	 */
	private boolean dumpEnabled = JPPFConfiguration.getProperties().getBoolean("byte.array.dump.enabled", false);
	/**
	 * Maximum number of containers kept by this node's cache.
	 */
	private static final int MAX_CONTAINERS = 1000;
	/**
	 * Utility for deserialization and serialization.
	 */
	private ObjectSerializer serializer = null;
	/**
	 * Class loader used for dynamic loading and updating of client classes.
	 */
	private JPPFClassLoader classLoader = null;
	/**
	 * Mapping of containers to their corresponding application uuid.
	 */
	private Map<String, JPPFContainer> containerMap = new HashMap<String, JPPFContainer>();
	/**
	 * A list retaining the container in chronological order of their creation.
	 */
	private LinkedList<JPPFContainer> containerList = new LinkedList<JPPFContainer>();
	/**
	 * Current build number for this node.
	 */
	private int buildNumber = -1;
	/**
	 * The task execution manager for this node.
	 */
	private NodeExecutionManager executionManager = null;
	/**
	 * Holds the count of currently executing tasks.
	 * Used to determine when this node is busy or idle.
	 */
	private AtomicInteger executingCount = new AtomicInteger(0);
	/**
	 * The administration and monitoring MBean for this node.
	 */
	private JPPFNodeAdmin nodeAdmin = null;
	/**
	 * Determines whether JMX management and monitoring is enabled for this node.
	 */
	private boolean jmxEnabled = JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true);

	/**
	 * Main processing loop of this node.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		uuid = new JPPFUuid().toString();
		buildNumber = VersionUtils.getBuildNumber();
		stopped = false;
		int n = 0;
		if (debugEnabled) log.debug("Start of node main loop");
		while (!stopped)
		{
			try
			{
				init();
				if (n == 0)
				{
					System.out.println("Node sucessfully initialized");
					n++;
				}
				perform();
			}
			catch(SecurityException e)
			{
				//log.error(e.getMessage(), e);
				throw new JPPFError(e);
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
				try
				{
					socketClient.close();
					socketClient = null;
				}
				catch(Exception ex)
				{
					log.error(ex.getMessage(), ex);
				}
			}
		}
		if (debugEnabled) log.debug("End of node main loop");
		if (notifying) fireNodeEvent(NodeEventType.DISCONNECTED);
	}

	/**
	 * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
	 * receives it, executes it and sends the results back.
	 * @throws Exception if an error was raised from the underlying socket connection or the class loader.
	 */
	public void perform() throws Exception
	{
		if (debugEnabled) log.debug("Start of node secondary loop");
		while (!stopped)
		{
			Pair<JPPFTaskBundle, List<JPPFTask>> pair = readTask();
			if (notifying) fireNodeEvent(NodeEventType.START_EXEC);
			JPPFTaskBundle bundle = pair.first();
			if (JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				if (debugEnabled) log.debug("setting initial bundle uuid");
				bundle.setBundleUuid(uuid);
				Map<BundleParameter, Object> params = BundleTuningUtils.getBundleTunningParameters();
				if (params != null) bundle.getParametersMap().putAll(params);
				if (isJmxEnabled())
				{
					TypedProperties props = JPPFConfiguration.getProperties();
					bundle.setParameter(NODE_MANAGEMENT_HOST_PARAM, NetworkUtils.getManagementHost());
					bundle.setParameter(NODE_MANAGEMENT_PORT_PARAM, props.getInt("jppf.management.port", 11198));
					bundle.setParameter(NODE_MANAGEMENT_ID_PARAM, NodeLauncher.getJmxServer().getId());
				}
				JPPFSystemInformation info = new JPPFSystemInformation();
				info.populate();
				bundle.setParameter(NODE_SYSTEM_INFO_PARAM, info);
			}
			List<JPPFTask> taskList = pair.second();
			boolean notEmpty = (taskList != null) && (taskList.size() > 0);
			if (debugEnabled) log.debug("received " + (notEmpty ? "a non-" : "an ") + "empty bundle");
			if (notEmpty) executionManager.execute(bundle, taskList);
			writeResults(bundle, taskList);
			if (notEmpty)
			{
				setTaskCount(getTaskCount() + taskList.size());
				if (debugEnabled) log.debug("tasks executed: "+getTaskCount());
				//log.info("tasks executed: "+getTaskCount());
			}
			int p = bundle.getBuildNumber();
			if (buildNumber < p)
			{
				JPPFNodeReloadNotification notif = new JPPFNodeReloadNotification("detected new build number: " + p
					+ "; previous build number: " + buildNumber);
				VersionUtils.setBuildNumber(p);
				throw notif;
			}
		}
		if (debugEnabled) log.debug("End of node secondary loop");
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void init() throws Exception
	{
		if (debugEnabled) log.debug("start node initialization");
		initHelper();
		boolean mustInit = (socketClient == null);
		if (mustInit)	initSocketClient();
		initCredentials();
		if ((nodeAdmin == null) && isJmxEnabled())
		{
			nodeAdmin = new JPPFNodeAdmin(JPPFNode.this);
			String mbeanName = JPPFAdminMBean.NODE_MBEAN_NAME;
			addNodeListener(nodeAdmin);
			NodeLauncher.getJmxServer().registerMbean(mbeanName, nodeAdmin, JPPFNodeAdminMBean.class);
		}
		if (notifying) fireNodeEvent(NodeEventType.START_CONNECT);
		if (mustInit)
		{
			if (debugEnabled) log.debug("start socket initialization");
			System.out.println("PeerNode.init(): Attempting connection to the JPPF driver");
			socketInitializer.initializeSocket(socketClient);
			System.out.println("PeerNode.init(): Reconnected to the JPPF driver");
			if (debugEnabled) log.debug("end socket initialization");
		}
		if (notifying) fireNodeEvent(NodeEventType.END_CONNECT);
		executionManager = new NodeExecutionManager(this);
		if (debugEnabled) log.debug("end node initialization");
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketClient() throws Exception
	{
		if (debugEnabled) log.debug("Initializing socket");
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.server.host", "localhost");
		int port = props.getInt("node.server.port", 11113);
		socketClient = new SocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
		socketClient.setSerializer(serializer);
		if (debugEnabled) log.debug("end socket client initialization");
	}

	/**
	 * Initialize the security credentials associated with this JPPF node.
	 */
	private void initCredentials()
	{
	}
	
	/**
	 * Read a task from the socket connection, along with its header information.
	 * @return a pair of <code>JPPFTaskBundle</code> and a <code>List</code> of <code>JPPFTask</code> instances.
	 * @throws Exception if an error is raised while reading the task data.
	 */
	private Pair<JPPFTaskBundle, List<JPPFTask>> readTask() throws Exception
	{
		Object[] result = readObjects();
		JPPFTaskBundle bundle = (JPPFTaskBundle) result[0];
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()) &&
			(bundle.getParameter(NODE_EXCEPTION_PARAM) == null))
		{
			DataProvider dataProvider = (DataProvider) result[1];
			for (int i=0; i<bundle.getTaskCount(); i++)
			{
				JPPFTask task = (JPPFTask) result[2 + i];
				task.setDataProvider(dataProvider);
				taskList.add(task);
			}
		}
		return new Pair<JPPFTaskBundle, List<JPPFTask>>(bundle, taskList);
	}

	/**
	 * Deseralize the objects read from the socket, and reload the appropriate classes if any class change is detected.<br>
	 * A class change is triggered when an <code>InvalidClassException</code> is caught. Upon catching this exception,
	 * the class loader is reinitialized and the class are reloaded.
	 * @return an array of objects deserialized from the socket stream.
	 * @throws Exception if the classes could not be reloaded or an error occurred during deserialization.
	 */
	private Object[] readObjects() throws Exception
	{
		Object[] result = null;
		boolean reload = false;
		try
		{
			result = deserializeObjects();
		}
		catch(IncompatibleClassChangeError err)
		{
			reload = true;
			if (debugEnabled) log.debug(err.getMessage() + "; reloading classes", err);
		}
		catch(InvalidClassException e)
		{
			reload = true;
			if (debugEnabled) log.debug(e.getMessage() + "; reloading classes", e);
		}
		if (reload)
		{
			if (debugEnabled) log.debug("reloading classes");
			classLoader = null;
			initHelper();
			socketClient.setSerializer(serializer);
			result = deserializeObjects();
		}
		return result;
	}

	/**
	 * Perform the deserialization of the objects received through the socket connection.
	 * @return an array of objects deserialized from the socket stream.
	 * @throws Exception if an error occurs while deserializing.
	 */
	private Object[] deserializeObjects() throws Exception
	{
		socketClient.skip(4);
		List<Object> list = new ArrayList<Object>();
		byte[] data = socketClient.receiveBytes(0).getBuffer();
		JPPFTaskBundle bundle = (JPPFTaskBundle) helper.getSerializer().deserialize(data);
		list.add(bundle);
		try
		{
			bundle.setNodeExecutionTime(System.currentTimeMillis());
			int count = bundle.getTaskCount();
			if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				JPPFContainer cont = getContainer(bundle.getUuidPath().getList());
				cont.getClassLoader().setRequestUuid(bundle.getRequestUuid());
				cont.deserializeObject(socketClient, list, 1+count);
			}
			else
			{
				// skip null data provider
				socketClient.receiveBytes(0);
			}
		}
		catch(ClassNotFoundException e)
		{
			log.error("Exception occurred while deserializing the tasks", e);
			bundle.setTaskCount(0);
			bundle.setParameter(NODE_EXCEPTION_PARAM, e);
		}
		catch(NoClassDefFoundError e)
		{
			log.error("Exception occurred while deserializing the tasks", e);
			bundle.setTaskCount(0);
			bundle.setParameter(NODE_EXCEPTION_PARAM, e);
		}
		return list.toArray(new Object[0]);
	}

	/**
	 * Write the execution results to the socket stream.
	 * @param bundle the task wrapper to send along.
	 * @param tasks the list of tasks with their result field updated.
	 * @throws Exception if an error occurs while writtng to the socket stream.
	 */
	private void writeResults(JPPFTaskBundle bundle, List<JPPFTask> tasks) throws Exception
	{
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);
		List<JPPFBuffer> list = new ArrayList<JPPFBuffer>();
		list.add(helper.toBytes(bundle, false));
		for (JPPFTask task : tasks) list.add(helper.toBytes(task, false));
		int size = 0;
		for (JPPFBuffer buf: list) size += 4 + buf.getLength();

		socketClient.writeInt(size);
		for (JPPFBuffer buf: list) socketClient.sendBytes(buf);
	}

	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @return a <code>ClassLoader</code> used for loading the classes of the framework.
	 */
	private JPPFClassLoader getClassLoader()
	{
		if (classLoader == null)
		{
			if (debugEnabled) log.debug("Initializing classloader");
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
		if (debugEnabled) log.debug("Initializing serializer");
		Class<?> c = getClassLoader().loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
		Object o = c.newInstance();
		serializer = (ObjectSerializer) o;
		c = getClassLoader().loadJPPFClass("org.jppf.utils.SerializationHelperImpl");
		o = c.newInstance();
		helper = (SerializationHelper) o;
	}

	/**
	 * Get a reference to the JPPF container associated with an application uuid.
	 * @param uuidPath the uuid path containing the key to the container.
	 * @return a <code>JPPFContainer</code> instance.
	 * @throws Exception if an error occcurs while getting the container.
	 */
	public JPPFContainer getContainer(List<String> uuidPath) throws Exception
	{
		String uuid = uuidPath.get(0);
		JPPFContainer container = containerMap.get(uuid);
		if (container == null)
		{
			if (debugEnabled) log.debug("Creating new container for appuuid=" + uuid);
			container = new JPPFContainer(uuidPath);
			if (containerList.size() >= MAX_CONTAINERS)
			{
				JPPFContainer toRemove = containerList.removeFirst();
				containerMap.remove(toRemove.getAppUuid());
			}
			containerList.add(container);
			containerMap.put(uuid, container);
		}
		return container;
	}

	/**
	 * Stop this node and release the resources it is using.
	 * @param closeSocket determines whether the underlying socket should be closed.
	 * @see org.jppf.node.MonitoredNode#stopNode(boolean)
	 */
	public void stopNode(boolean closeSocket)
	{
		if (debugEnabled) log.debug("stopping node");
		stopped = true;
		executionManager.shutdown();
		if (closeSocket)
		{
			try
			{
				socketClient.close();
			}
			catch(Exception ex)
			{
				log.error(ex.getMessage(), ex);
			}
			socketClient = null;
		}
		try
		{
			String mbeanName = JPPFAdminMBean.NODE_MBEAN_NAME;
			NodeLauncher.getJmxServer().unregisterMbean(mbeanName);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		setNodeAdmin(null);
		classLoader = null;
	}

	/**
	 * Decrement the count of currently executing tasks and determine whether
	 * an idle notification should be sent.
	 */
	synchronized void decrementExecutingCount()
	{
		if (executingCount.decrementAndGet() == 0)
		{
			fireNodeEvent(NodeEventType.END_EXEC);
		}
		fireNodeEvent(NodeEventType.TASK_EXECUTED);
	}
	
	/**
	 * Increment the count of currently executing tasks and determine whether
	 * a busy notification should be sent.
	 */
	synchronized void incrementExecutingCount()
	{
		if (executingCount.incrementAndGet() == 1)
		{
			fireNodeEvent(NodeEventType.START_EXEC);
		}
	}

	/**
	 * Get the administration and monitoring MBean for this node.
	 * @return a <code>JPPFNodeAdmin</code>m instance.
	 */
	public synchronized JPPFNodeAdmin getNodeAdmin()
	{
		return nodeAdmin;
	}

	/**
	 * Set the administration and monitoring MBean for this node.
	 * @param nodeAdmin a <code>JPPFNodeAdmin</code>m instance.
	 */
	public synchronized void setNodeAdmin(JPPFNodeAdmin nodeAdmin)
	{
		this.nodeAdmin = nodeAdmin;
	}

	/**
	 * Get the task execution manager for this node.
	 * @return a <code>NodeExecutionManager</code> instance.
	 */
	public NodeExecutionManager getExecutionManager()
	{
		return executionManager;
	}

	/**
	 * Determines whether JMX management and monitoring is enabled for this node.
	 * @return true if JMX is enabled, false otherwise. 
	 */
	public boolean isJmxEnabled()
	{
		return jmxEnabled;
	}
}
