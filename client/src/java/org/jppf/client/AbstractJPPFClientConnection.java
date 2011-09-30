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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.nio.channels.AsynchronousCloseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.*;

import org.jppf.JPPFException;
import org.jppf.classloader.NonDelegatingClassLoader;
import org.jppf.client.event.*;
import org.jppf.client.loadbalancer.LoadBalancer;
import org.jppf.comm.socket.*;
import org.jppf.io.IOHelper;
import org.jppf.security.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration
 * commands, and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether
 * classes from the submitting application should be dynamically reloaded or not
 * depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClientConnection implements JPPFClientConnection
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(AbstractJPPFClientConnection.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Name of the SerializationHelper implementation class.
	 */
	private static String SERIALIZATION_HELPER_IMPL = "org.jppf.utils.SerializationHelperImpl";
	/**
	 * Used to prevent parallel deserialization.
	 */
	private static Lock lock = new ReentrantLock();
	/**
	 * Determines whether tasks deserialization should be sequential rather than parallel.
	 */
	private static final boolean SEQUENTIAL_DESERIALIZATION = JPPFConfiguration.getProperties().getBoolean("jppf.sequential.deserialization", false);
	/**
	 * Handler for the connection to the task server.
	 */
	protected TaskServerConnectionHandler taskServerConnection = null;
	/**
	 * Enables loading local classes onto remote nodes.
	 */
	protected ClassServerDelegate delegate = null;
	/**
	 * Unique identifier of the remote driver.
	 */
	protected String uuid = null;
	/**
	 * The name or IP address of the host the JPPF driver is running on.
	 */
	protected String host = null;
	/**
	 * The TCP port the JPPF driver listening to for submitted tasks.
	 */
	protected int port = -1;
	/**
	 * The TCP port the class server is listening to.
	 */
	protected int classServerPort = -1;
	/**
	 * Security credentials associated with the application.
	 */
	protected JPPFSecurityContext credentials = null;
	/**
	 * Total count of the tasks submitted by this client.
	 */
	protected int totalTaskCount = 0;
	/**
	 * Configuration name for this local client.
	 */
	protected String name = null;
	/**
	 * Priority given to the driver this client is connected to. The client is always connected to the available driver(s) with the highest
	 * priority. If multiple drivers have the same priority, they will be used as a pool and tasks will be evenly distributed among them.
	 */
	protected int priority = 0;
	/**
	 * Status of the connection.
	 */
	protected AtomicReference<JPPFClientConnectionStatus> status = new AtomicReference<JPPFClientConnectionStatus>(CONNECTING);
	/**
	 * Used to synchronize access to the status.
	 */
	protected Object statusLock = new Object();
	/**
	 * List of status listeners for this connection.
	 */
	protected List<ClientConnectionStatusListener> listeners = new ArrayList<ClientConnectionStatusListener>();
	/**
	 * Holds the tasks, data provider and submission mode for the current execution.
	 */
	protected JPPFJob job = null;
	/**
	 * Determines whether this connection has been shut down;
	 */
	protected boolean isShutdown = false;
	/**
	 * The JPPF client that owns this connection.
	 */
	protected AbstractGenericClient client = null;

	/**
	 * Configure this client connection with the specified parameters.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param host the name or IP address of the host the JPPF driver is running on.
	 * @param driverPort the TCP port the JPPF driver listening to for submitted tasks.
	 * @param classServerPort the TCP port the class server is listening to.
	 * @param priority the assigned to this client connection.
	 */
	protected void configure(String uuid, String name, String host, int driverPort, int classServerPort, int priority)
	{
		this.uuid = uuid;
		this.host = NetworkUtils.getHostName(host);
		this.port = driverPort;
		this.priority = priority;
		this.classServerPort = classServerPort;
		this.name = name;
		this.taskServerConnection = new TaskServerConnectionHandler(this, this.host, this.port);
	}

	/**
	 * Initialize this client connection.
	 * @see org.jppf.client.JPPFClientConnection#init()
	 */
	@Override
    public abstract void init();

	/**
	 * Initialize this client's security credentials.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initCredentials() throws Exception
	{
		StringBuilder sb = new StringBuilder("Client:");
		sb.append(VersionUtils.getLocalIpAddress()).append(':');
		TypedProperties props = JPPFConfiguration.getProperties();
		sb.append(props.getInt("class.server.port", 11111)).append(':');
		sb.append(port).append(':');
		credentials = new JPPFSecurityContext(uuid, sb.toString(), new JPPFCredentials());
	}

	/**
	 * Send tasks to the server for execution.
	 * @param job - the job to execute remotely.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public void sendTasks(JPPFJob job) throws Exception
	{
		try
		{
			sendTasks(new JPPFTaskBundle(), job);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			throw e;
		}
		catch(Error e)
		{
			log.error(e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Send tasks to the server for execution.
	 * @param header the task bundle to send to the driver.
	 * @param job the job to execute remotely.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public void sendTasks(JPPFTaskBundle header, JPPFJob job) throws Exception
	{
		ObjectSerializer ser = makeHelper().getSerializer();
		int count = job.getTasks().size();
		TraversalList<String> uuidPath = new TraversalList<String>();
		uuidPath.add(client.getUuid());
		header.setUuidPath(uuidPath);
		if (debugEnabled) log.debug("[client: " + name + "] sending job '" + job.getId() + "' with " + count + " tasks, uuidPath=" + uuidPath.getList());
		header.setTaskCount(count);
		header.setRequestUuid(job.getJobUuid());
		header.setParameter(BundleParameter.JOB_ID, job.getId());
		header.setParameter(BundleParameter.JOB_UUID, job.getJobUuid());
		header.setJobSLA(job.getJobSLA());
		header.setParameter(BundleParameter.JOB_METADATA, job.getJobMetadata());

		SocketWrapper socketClient = taskServerConnection.getSocketClient();
		IOHelper.sendData(socketClient, header, ser);
		IOHelper.sendData(socketClient, job.getDataProvider(), ser);
		for (JPPFTask task : job.getTasks()) IOHelper.sendData(socketClient, task, ser);
		socketClient.flush();
	}

	/**
	 * Receive results of tasks execution.
	 * @return a pair of objects representing the executed tasks results, and the index
	 * of the first result within the initial task execution request.
	 * @throws Exception if an error is raised while reading the results from the server.
	 */
	public Pair<List<JPPFTask>, Integer> receiveResults() throws Exception
	{
		try
		{
			SocketWrapper socketClient = taskServerConnection.getSocketClient();
			ObjectSerializer ser = makeHelper().getSerializer();
			JPPFTaskBundle bundle = (JPPFTaskBundle) IOHelper.unwrappedData(socketClient, ser);
			int count = bundle.getTaskCount();
			if (debugEnabled) log.debug("received bundle with " + count + " tasks for job '" + bundle.getId() + '\'');
			//List<JPPFTask> taskList = new ArrayList<JPPFTask>();
			List<JPPFTask> taskList = new LinkedList<JPPFTask>();
			if (SEQUENTIAL_DESERIALIZATION) lock.lock();
			try
			{
				for (int i=0; i<count; i++) taskList.add((JPPFTask) IOHelper.unwrappedData(socketClient, ser));
			}
			finally
			{
				if (SEQUENTIAL_DESERIALIZATION) lock.unlock();
			}
	
			int startIndex = (taskList.isEmpty()) ? -1 : taskList.get(0).getPosition();
			// if an exception prevented the node from executing the tasks
			Throwable t = (Throwable) bundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM);
			if (t != null)
			{
				if (debugEnabled) log.debug("server returned exception parameter in the header for job '" + bundle.getId() + "' : " + t);
				Exception e = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
				for (JPPFTask task: taskList) task.setException(e);
			}
            return new Pair<List<JPPFTask>, Integer>(taskList, startIndex);
		}
		catch(AsynchronousCloseException e)
		{
			log.debug(e.getMessage(), e);
			throw e;
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			throw e;
		}
		catch(Error e)
		{
			log.error(e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Receive results of tasks execution.
	 * @return a pair of objects representing the executed tasks results, and the index
	 * of the first result within the initial task execution request.
	 * @param cl the cintext classloader to use to deserialize the results.
	 * @throws Exception if an error is raised while reading the results from the server.
	 */
	public Pair<List<JPPFTask>, Integer> receiveResults(ClassLoader cl) throws Exception
	{
		ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
		if (cl != null) Thread.currentThread().setContextClassLoader(cl);
		Pair<List<JPPFTask>, Integer> results = null;
		try
		{
			results = receiveResults();
		}
		finally
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(prevCl);
		}
		return results;
	}

	/**
	 * Instantiate a <code>SerializationHelper</code> using the current context class loader.
	 * @return a <code>SerializationHelper</code> instance.
	 * @throws Exception if the serialiozation helper could not be instantiated.
	 */
	protected SerializationHelper makeHelper() throws Exception
	{
		return makeHelper(null);
	}

	/**
	 * Instantiate a <code>SerializationHelper</code> using the current context class loader.
	 * @param cl the class loader to usew to load the seriaization helper class.
	 * @return a <code>SerializationHelper</code> instance.
	 * @throws Exception if the serialiozation helper could not be instantiated.
	 */
	protected SerializationHelper makeHelper(ClassLoader cl) throws Exception
	{
		if (cl == null) cl = Thread.currentThread().getContextClassLoader();
		NonDelegatingClassLoader ndCl = new NonDelegatingClassLoader(null, cl);
		String helperClassName = getSerializationHelperClassName();
		Class clazz = null;
		if (cl != null)
		{
			try
			{
				clazz = ndCl.loadClassDirect(helperClassName);
			}
			catch(ClassNotFoundException e)
			{
				log.error(e.getMessage(), e);
			}
		}
		if (clazz == null)
		{
			cl = this.getClass().getClassLoader();
			clazz = cl.loadClass(helperClassName);
		}

        return (SerializationHelper) clazz.newInstance();
	}

	/**
	 * Get the name of the serialization helper implementation class name to use.
	 * @return the fully qualified class name of a <code>SerializationHelper</code> implementation.
	 */
	protected String getSerializationHelperClassName()
	{
		return JPPFConfiguration.getProperties().getString("jppf.serialization.helper.class", SERIALIZATION_HELPER_IMPL);
	}

	/**
	 * Get the priority assigned to this connection.
	 * @return a priority as an int value.
	 * @see org.jppf.client.JPPFClientConnection#getPriority()
	 */
	@Override
    public int getPriority()
	{
		return priority;
	}

	/**
	 * Set the priority assigned to this connection.
	 * @param priority a priority as an int value.
	 */
	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	/**
	 * Get the status of this connection.
	 * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.JPPFClientConnection#getStatus()
	 */
	@Override
    public JPPFClientConnectionStatus getStatus()
	{
		return status.get();
	}

	/**
	 * Set the status of this connection.
	 * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.JPPFClientConnection#setStatus(org.jppf.client.JPPFClientConnectionStatus)
	 */
	@Override
    public void setStatus(JPPFClientConnectionStatus status)
	{
		JPPFClientConnectionStatus oldStatus = getStatus();
		this.status.set(status);
		if (!status.equals(oldStatus)) fireStatusChanged(oldStatus);
	}

	/**
	 * Add a connection status listener to this connection's list of listeners.
	 * @param listener the listener to add to the list.
	 * @see org.jppf.client.JPPFClientConnection#addClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	@Override
    public void addClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		synchronized(listeners)
		{
			listeners.add(listener);
		}
	}

	/**
	 * Remove a connection status listener from this connection's list of listeners.
	 * @param listener the listener to remove from the list.
	 * @see org.jppf.client.JPPFClientConnection#removeClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	@Override
    public void removeClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
	}

	/**
	 * Notify all listeners that the status of this connection has changed.
	 * @param oldStatus the connection status before the change.
	 */
	protected void fireStatusChanged(JPPFClientConnectionStatus oldStatus)
	{
		ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
		ClientConnectionStatusListener[] array = null;
		synchronized(listeners)
		{
			array = listeners.toArray(new ClientConnectionStatusListener[listeners.size()]);
		}
		for (ClientConnectionStatusListener listener: array)
		{
			listener.statusChanged(event);
		}
	}

	/**
	 * Shutdown this client and retrieve all pending executions for resubmission.
	 * @return a list of <code>JPPFJob</code> instances to resubmit.
	 * @see org.jppf.client.JPPFClientConnection#close()
	 */
	@Override
    public abstract List<JPPFJob> close();

	/**
	 * Get the name assigned tothis client connection.
	 * @return the name as a string.
	 * @see org.jppf.client.JPPFClientConnection#getName()
	 */
	@Override
    public String getName()
	{
		return name;
	}

	/**
	 * Get a string representation of this client connection.
	 * @return a string representing this connection.
	 * @see java.lang.Object#toString()
	 */
	@Override
    public String toString()
	{
		return name + " : " + status;
	}

	/**
	 * Create a socket initializer.
	 * @return an instance of a class implementing <code>SocketInitializer</code>.
	 */
	protected abstract SocketInitializer createSocketInitializer();

	/**
	 * Get the object that holds the tasks, data provider and submission mode for the current execution.
	 * @return a <code>JPPFJob</code> instance.
	 */
	public JPPFJob getCurrentJob()
	{
		return job;
	}

	/**
	 * Set the object that holds the tasks, data provider and submission mode for the current execution.
	 * @param currentExecution a <code>ClientExecution</code> instance.
	 */
	public void setCurrentJob(JPPFJob currentExecution)
	{
		this.job = currentExecution;
	}

	/**
	 * Get the handler for the connection to the task server.
	 * @return a <code>TaskServerConnectionHandler</code> instance.
	 */
	public TaskServerConnectionHandler getTaskServerConnection()
	{
		return taskServerConnection;
	}


	/**
	 * Invoked to notify of a status change event on a client connection.
	 * @param event the event to notify of.
	 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
	 */
	public void delegateStatusChanged(ClientConnectionStatusEvent event)
	{
		JPPFClientConnectionStatus s1 = event.getClientConnectionStatusHandler().getStatus();
		JPPFClientConnectionStatus s2 = taskServerConnection.getStatus();
		processStatusChanged(s1, s2);
	}

	/**
	 * Invoked to notify of a status change event on a client connection.
	 * @param event the event to notify of.
	 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
	 */
	public void taskServerConnectionStatusChanged(ClientConnectionStatusEvent event)
	{
		JPPFClientConnectionStatus s1 = event.getClientConnectionStatusHandler().getStatus();
		JPPFClientConnectionStatus s2 = delegate.getStatus();
		processStatusChanged(s2, s1);
	}

	/**
	 * Handle a status change from either the class server delegate or the task server connection
	 * and determine whether it triggers a status change for the client connection.
	 * @param delegateStatus status of the class server delegate conneciton.
	 * @param taskConnectionStatus status of the task server connection.
	 */
	protected void processStatusChanged(JPPFClientConnectionStatus delegateStatus, JPPFClientConnectionStatus taskConnectionStatus)
	{
		if (FAILED.equals(delegateStatus)) setStatus(FAILED);
		else if (ACTIVE.equals(delegateStatus))
		{
			if (ACTIVE.equals(taskConnectionStatus) && !ACTIVE.equals(this.getStatus())) setStatus(ACTIVE);
			else if (!taskConnectionStatus.equals(this.getStatus())) setStatus(taskConnectionStatus);
		}
		else
		{
			if (ACTIVE.equals(taskConnectionStatus)) setStatus(delegateStatus);
			else
			{
				int n = delegateStatus.compareTo(taskConnectionStatus);
				if ((n < 0) && !delegateStatus.equals(this.getStatus())) setStatus(delegateStatus);
				else if (!taskConnectionStatus.equals(this.getStatus())) setStatus(taskConnectionStatus);
			}
		}
	}

	/**
	 * Get the load balancer that distributes the load between local and remote execution.
	 * @return a {@link LoadBalancer} instance.
	 */
	public LoadBalancer getLoadBalancer()
	{
		return client.getLoadBalancer();
	}

	/**
	 * Get the JPPF client that owns this connection.
	 * @return an <code>AbstractGenericClient</code> instance.
	 */
	public AbstractGenericClient getClient()
	{
		return client;
	}

	/**
	 * Get the unique identifier of the remote driver.
	 * @return the uuid as a string.
	 */
	public String getUuid()
	{
		return uuid;
	}
}
