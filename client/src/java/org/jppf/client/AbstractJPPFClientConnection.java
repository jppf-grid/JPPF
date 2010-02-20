/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

import static org.jppf.client.JPPFClientConnectionStatus.CONNECTING;

import java.nio.channels.AsynchronousCloseException;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.event.*;
import org.jppf.comm.socket.*;
import org.jppf.data.transform.*;
import org.jppf.security.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;

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
	private static Log log = LogFactory.getLog(AbstractJPPFClientConnection.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Name of the SerializationHelper implementation class.
	 */
	private static String SERIALIZATION_HELPER_IMPL = "org.jppf.utils.SerializationHelperImpl";
	/**
	 * Handler for the connection to the task server.
	 */
	protected TaskServerConnectionHandler taskServerConnection = null;
	/**
	 * Enables loading local classes onto remote nodes.
	 */
	protected ClassServerDelegate delegate = null;
	/**
	 * Unique identifier for this JPPF client.
	 */
	protected String appUuid = null;
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
	 * Priority given to the driver this client is connected to.
	 * The client is always connected to the available driver(s) with the highest
	 * priority. If multiple drivers have the same priority, they will be used as a
	 * pool and tasks will be evenly distributed among them.
	 */
	protected int priority = 0;
	/**
	 * Status of the connection.
	 */
	protected JPPFClientConnectionStatus status = CONNECTING;
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
	 * This connection's UUID.
	 */
	private String connectionId = new JPPFUuid().toString();

	/**
	 * Default instantiation of this class is not allowed.
	 */
	protected AbstractJPPFClientConnection()
	{
	}

	/**
	 * Initialize this client connection with the specified parameters.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param host the name or IP address of the host the JPPF driver is running on.
	 * @param driverPort the TCP port the JPPF driver listening to for submitted tasks.
	 * @param classServerPort the TCP port the class server is listening to.
	 * @param priority the assigned to this client connection.
	 */
	public AbstractJPPFClientConnection(String uuid, String name, String host, int driverPort, int classServerPort, int priority)
	{
		configure(uuid, name, host, driverPort, classServerPort, priority);
	}

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
		this.appUuid = uuid;
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
	public abstract void init();

	/**
	 * Initialize this client's security credentials.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initCredentials() throws Exception
	{
		StringBuilder sb = new StringBuilder("Client:");
		sb.append(VersionUtils.getLocalIpAddress()).append(":");
		TypedProperties props = JPPFConfiguration.getProperties();
		sb.append(props.getInt("class.server.port", 11111)).append(":");
		sb.append(port).append(":");
		credentials = new JPPFSecurityContext(appUuid, sb.toString(), new JPPFCredentials());
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
			JPPFTaskBundle bundle = new JPPFTaskBundle();
			sendTasks(bundle, job);
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
	 * @param job - the job to execute remotely.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public void sendTasks(JPPFTaskBundle header, JPPFJob job) throws Exception
	{
		ObjectSerializer ser = makeHelper().getSerializer();
		int count = job.getTasks().size();
		if (debugEnabled) log.debug("[client: " + name + "] sending job '" + job.getId() + "' with " + count + " tasks");
		TraversalList<String> uuidPath = new TraversalList<String>();
		uuidPath.add(appUuid);
		header.setUuidPath(uuidPath);
		header.setTaskCount(count);
		header.setParameter(BundleParameter.JOB_ID, job.getId());
		header.setJobSLA(job.getJobSLA());

		SocketWrapper socketClient = taskServerConnection.getSocketClient();
		socketClient.sendBytes(wrappedData(header, ser));
		socketClient.sendBytes(wrappedData(job.getDataProvider(), ser));
		for (JPPFTask task : job.getTasks()) socketClient.sendBytes(wrappedData(task, ser));
		socketClient.flush();
	}

	/**
	 * Transform an object into a an array of bytes to send trough the network connection.
	 * @param o the object to transform.
	 * @param ser the object serializer to use.
	 * @return the transformed result as an array of bytes.
	 * @throws Exception if an error occurs while preparing the data.
	 */
	private JPPFBuffer wrappedData(Object o, ObjectSerializer ser) throws Exception
	{
		JPPFBuffer serialized = ser.serialize(o);
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		if (transform == null) return serialized;
		byte[] data = transform.wrap(serialized.getBuffer());
		return new JPPFBuffer(data, data.length);
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
			JPPFTaskBundle bundle = (JPPFTaskBundle) unwrappedData(socketClient.receiveBytes(0), ser);
			int count = bundle.getTaskCount();
			List<JPPFTask> taskList = new ArrayList<JPPFTask>();
			for (int i=0; i<count; i++)
			{
				taskList.add((JPPFTask) unwrappedData(socketClient.receiveBytes(0), ser));
			}
	
			int startIndex = (taskList.isEmpty()) ? -1 : taskList.get(0).getPosition();
			// if an exception prevented the node from executing the tasks
			Throwable t = (Throwable) bundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM);
			if (t != null)
			{
				Exception e = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
				for (JPPFTask task: taskList) task.setException(e);
			}
			Pair<List<JPPFTask>, Integer> p = new Pair<List<JPPFTask>, Integer>(taskList, startIndex);
			return p;
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
	 * Transform an array of bytes received from the network into an object.
	 * @param buffer the data to unwrap.
	 * @param ser the object serializer to use.
	 * @return the transformed result as an object.
	 * @throws Exception if an error occurs while preparing the data.
	 */
	private Object unwrappedData(JPPFBuffer buffer, ObjectSerializer ser) throws Exception
	{
		byte[] data = buffer.getBuffer();
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		if (transform != null) data = transform.unwrap(data);
		return ser.deserialize(data);
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
		String helperClassName = getSerializationHelperClassName();
		Class clazz = null;
		if (cl != null)
		{
			try
			{
				clazz = cl.loadClass(helperClassName);
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
		SerializationHelper helper = (SerializationHelper) clazz.newInstance();
		
		return helper;
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
	public synchronized JPPFClientConnectionStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of this connection.
	 * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.JPPFClientConnection#setStatus(org.jppf.client.JPPFClientConnectionStatus)
	 */
	public synchronized void setStatus(JPPFClientConnectionStatus status)
	{
		this.status = status;
		fireStatusChanged();
	}

	/**
	 * Add a connection status listener to this connection's list of listeners.
	 * @param listener the listener to add to the list.
	 * @see org.jppf.client.JPPFClientConnection#addClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public synchronized void addClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a connection status listener from this connection's list of listeners.
	 * @param listener the listener to remove from the list.
	 * @see org.jppf.client.JPPFClientConnection#removeClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public synchronized void removeClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Notify all listeners that the status of this connection has changed.
	 */
	protected synchronized void fireStatusChanged()
	{
		ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this);
		// to avoid ConcurrentModificationException
		ClientConnectionStatusListener[] array = listeners.toArray(new ClientConnectionStatusListener[0]);
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
	public abstract List<JPPFJob> close();

	/**
	 * Get the name assigned tothis client connection.
	 * @return the name as a string.
	 * @see org.jppf.client.JPPFClientConnection#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Get a string representation of this client connection.
	 * @return a string representing this connection.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name + " : " + status;
	}

	/**
	 * Create a socket initializer.
	 * @return an instance of a class implementing <code>SocketInitializer</code>.
	 */
	abstract protected SocketInitializer createSocketInitializer();

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
	 * Get this connection's UUID.
	 * @return the uuid as a string.
	 */
	public String getConnectionId()
	{
		return connectionId;
	}

	/**
	 * Get the handler for the connection to the task server.
	 * @return a <code>TaskServerConnectionHandler</code> instance.
	 */
	public TaskServerConnectionHandler getTaskServerConnection()
	{
		return taskServerConnection;
	}
}
