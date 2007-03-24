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
package org.jppf.server.node;

import static org.jppf.server.protocol.AdminRequestConstants.*;

import java.io.InvalidClassException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.jppf.*;
import org.jppf.comm.socket.SocketClient;
import org.jppf.node.*;
import org.jppf.node.event.NodeEvent.EventType;
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
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFNode.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
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
	private List<JPPFContainer> containerList = new LinkedList<JPPFContainer>();
	/**
	 * Current build number for this node.
	 */
	private int buildNumber = -1;
	/**
	 * The Thread Pool that really process the tasks
	 */
	private ExecutorService threadPool;
	/**
	 * Holds the count of currently executing tasks.
	 * Used to determine when this node is busy or idle.
	 */
	private AtomicInteger executingCount = new AtomicInteger(0);

	/**
	 * Main processing loop of this node.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		uuid = new JPPFUuid().toString();
		buildNumber = VersionUtils.getBuildNumber();
		stopped = false;
		if (debugEnabled) log.debug("Start of node main loop");
		while (!stopped)
		{
			try
			{
				init();
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
		if (notifying) fireNodeEvent(EventType.DISCONNECTED);
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
			if (notifying) fireNodeEvent(EventType.START_EXEC);
			JPPFTaskBundle bundle = pair.first();
			if (JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
			{
				if (debugEnabled) log.debug("setting initial bundle uuid");
				bundle.setBundleUuid(uuid);
				Map<String, Object> params = getBundleTunningParameters();
				if (params != null) bundle.getParametersMap().putAll(params);
			}
			List<JPPFTask> taskList = pair.second();
			boolean notEmpty = (taskList != null) && (taskList.size() > 0);
			if (debugEnabled) log.debug("received " + (notEmpty ? "a non-" : "an ") + "empty bundle");
			if (notEmpty)
			{
				//if (debugEnabled) log.debug("End of node secondary loop");
				if (debugEnabled) log.debug("node["+socketClient.getSocket().getLocalPort()+"] executing "+taskList.size()+" tasks");
				List<Future> futureList = new ArrayList<Future>(taskList.size());
				for (JPPFTask task : taskList)
				{
					NodeTaskWrapper taskWrapper = new NodeTaskWrapper(this, task, bundle.getUuidPath().getList());
					futureList.add(threadPool.submit(taskWrapper));
				}
				for (Future future : futureList) future.get();
			}
			writeResults(bundle, taskList);
			if (notEmpty)
			{
				taskCount += taskList.size();
				if (debugEnabled) log.debug("tasks executed: "+taskCount);
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
		boolean mustInit = false;
		initHelper();
		if (socketClient == null)
		{
			mustInit = true;
			initSocketClient();
		}
		initCredentials();
		if (notifying) fireNodeEvent(EventType.START_CONNECT);
		if (mustInit)
		{
			if (debugEnabled) log.debug("start socket initialization");
			System.out.println("PeerNode.init(): Attempting connection to the JPPF driver");
			socketInitializer.initializeSocket(socketClient);
			System.out.println("PeerNode.init(): Reconnected to the JPPF driver");
			if (debugEnabled) log.debug("end socket initialization");
		}
		if (notifying) fireNodeEvent(EventType.END_CONNECT);
		TypedProperties props = JPPFConfiguration.getProperties();
		int poolSize = props.getInt("processing.threads", 1);
		threadPool = Executors.newFixedThreadPool(poolSize);
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
	 * @throws Exception if a error is raised while reading the task data.
	 */
	private Pair<JPPFTaskBundle, List<JPPFTask>> readTask() throws Exception
	{
		JPPFBuffer buf = socketClient.receiveBytes(0);
		if (debugEnabled) log.debug("Total length read is " + buf.getLength() + " bytes");
		Object[] result = readObjects(buf.getBuffer());
		JPPFTaskBundle bundle = (JPPFTaskBundle) result[0];
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
		{
			DataProvider dataProvider = (DataProvider) result[1];
			for (int i = 0; i < bundle.getTaskCount(); i++)
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
		if (dumpEnabled)
			log.debug("Deserializing " + bytes.length + " bytes :\n" + StringUtils.dumpBytes(bytes, 0, bytes.length));
		List<Object> list = new ArrayList<Object>();
		int pos = helper.fromBytes(bytes, 0, false, list, 1);
		JPPFTaskBundle bundle = (JPPFTaskBundle) list.get(0);
		
		bundle.setNodeExecutionTime(System.currentTimeMillis());
		int count = bundle.getTaskCount();
		if (!JPPFTaskBundle.State.INITIAL_BUNDLE.equals(bundle.getState()))
		{
			JPPFContainer cont = getContainer(bundle.getUuidPath().getList());
			pos = cont.deserializeObject(bytes, pos, true, list, 1+count);
		}
		return list.toArray(new Object[0]);
	}

	/**
	 * Write the execution results to the socket stream.
	 * @param bundle the task wrapper to send along.
	 * @param tasks the list of tasks with their result field updated.
	 * @throws Exception if an error occurs while writitng to the socket stream.
	 */
	private void writeResults(JPPFTaskBundle bundle, List<JPPFTask> tasks) throws Exception
	{
		long elapsed = System.currentTimeMillis() - bundle.getNodeExecutionTime();
		bundle.setNodeExecutionTime(elapsed);
		List<JPPFBuffer> list = new ArrayList<JPPFBuffer>();
		list.add(helper.toBytes(bundle, false));
		for (JPPFTask task : tasks) list.add(helper.toBytes(task, true));
		int size = 0;
		for (JPPFBuffer buf: list) size += 4 + buf.getLength();
		byte[] data = new byte[size];
		int pos = 0;
		for (JPPFBuffer buf: list) pos = helper.copyToBuffer(buf.getBuffer(), data, pos, buf.getLength());
		JPPFBuffer buf = new JPPFBuffer(data, size);
		if (dumpEnabled)
			log.debug("Serialized " + buf.getLength() + " bytes :\n" + StringUtils.dumpBytes(buf.getBuffer(), 0, buf.getLength()));
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
		Class c = getClassLoader().loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
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
				JPPFContainer toRemove = containerList.remove(0);
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
		threadPool.shutdownNow();
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
		//socketClient.setSocket(null);
		//socketClient = null;
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
			fireNodeEvent(EventType.END_EXEC);
		}
		fireNodeEvent(EventType.TASK_EXECUTED);
	}
	
	/**
	 * Increment the count of currently executing tasks and determine whether
	 * a busy notification should be sent.
	 */
	synchronized void incrementExecutingCount()
	{
		if (executingCount.incrementAndGet() == 1)
		{
			fireNodeEvent(EventType.START_EXEC);
		}
	}

	/**
	 * Get a configured bundle size tuning profile form the configuration file.
	 * @return an <code>AnnealingTuneProfile</code> instance, or null if no profile was configured.
	 */
	public static Map<String, Object> getBundleTunningParameters()
	{
		TypedProperties cfg = JPPFConfiguration.getProperties();
		String s = cfg.getString("task.bundle.strategy");
		if (s == null) return null;
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(BUNDLE_TUNING_TYPE_PARAM, s);
		params.put(BUNDLE_SIZE_PARAM, cfg.getInt("task.bundle.size", 10));
		if ("autotuned".equalsIgnoreCase(s))
		{
			String profile = cfg.getString("task.bundle.autotuned.strategy", "smooth");
			String prefix = "strategy." + profile + ".";
			params.put(MIN_SAMPLES_TO_ANALYSE, cfg.getInt(prefix + "minSamplesToAnalyse", 500));
			params.put(MIN_SAMPLES_TO_CHECK_CONVERGENCE, cfg.getInt(prefix + "minSamplesToCheckConvergence", 300));
			params.put(MAX_DEVIATION, cfg.getDouble(prefix + "maxDeviation", 0.2d));
			params.put(MAX_GUESS_TO_STABLE, cfg.getInt(prefix + "maxGuessToStable", 10));
			params.put(SIZE_RATIO_DEVIATION, cfg.getFloat(prefix + "sizeRatioDeviation", 1.5f));
			params.put(DECREASE_RATIO, cfg.getFloat(prefix + "decreaseRatio", 0.2f));
		}
		
		return params;
	}
}
