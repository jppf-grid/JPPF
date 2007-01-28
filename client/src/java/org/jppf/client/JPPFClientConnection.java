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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;
import static org.jppf.server.protocol.AdminRequest.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.jppf.*;
import org.jppf.client.event.*;
import org.jppf.comm.socket.*;
import org.jppf.security.*;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * This class provides an API to submit execution requests and administration
 * commands, and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether
 * classes from the submitting application should be dynamically reloaded or not
 * depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClientConnection
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(JPPFClientConnection.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The socket client used to communicate over a socket connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Enables loading local classes onto remote nodes.
	 */
	private ClassServerDelegate delegate = null;
	/**
	 * Utility for deserialization and serialization.
	 */
	private SerializationHelper helper = null;
	/**
	 * Unique identifier for this JPPF client.
	 */
	private String appUuid = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private SocketInitializer socketInitializer = new SocketInitializer();
	/**
	 * Used to synchronize request submissions performed by mutliple threads.
	 */
	ReentrantLock lock = new ReentrantLock();
	/**
	 * The pool of threads used for submitting execution requests.
	 */
	private ThreadPoolExecutor executor =
		new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>());
	/**
	 * The name or IP address of the host the JPPF driver is running on.
	 */
	private String host = null;
	/**
	 * The TCP port the JPPF driver listening to for submitted tasks.
	 */
	private int port = -1;
	/**
	 * The TCP port the class server is listening to.
	 */
	private int classServerPort = -1;
	/**
	 * Security credentials associated with the application.
	 */
	JPPFSecurityContext credentials = null;
	/**
	 * Total count of the tasks submitted by this client.
	 */
	private int totalTaskCount = 0;
	/**
	 * Configuration name for this local client.
	 */
	String name = null;
	/**
	 * Priority given to the driver this client is connected to.
	 * The client is always connected to the available driver(s) with the highest
	 * priority. If multiple drivers have the same priority, they will be used as a
	 * pool and tasks will be evenly distributed among them.
	 */
	private int priority = 0;
	/**
	 * Status of the connection.
	 */
	private JPPFClientConnectionStatus status = CONNECTING;
	/**
	 * List of status listeners for this connection.
	 */
	private List<ClientConnectionStatusListener> listeners = new ArrayList<ClientConnectionStatusListener>();
	/**
	 * Holds the tasks, data provider and submission mode for the current execution.
	 */
	ClientExecution currentExecution = null;
	/**
	 * Determines whether this connection has been shut down;
	 */
	private boolean isShutdown = false;

	/**
	 * Default instantiation of this class is not allowed.
	 */
	private JPPFClientConnection()
	{
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param host the name or IP address of the host the JPPF driver is running on.
	 * @param driverPort the TCP port the JPPF driver listening to for submitted tasks.
	 * @param classServerPort the TCP port the class server is listening to.
	 * @param priority the assigned to this client connection.
	 */
	public JPPFClientConnection(String uuid, String name, String host, int driverPort, int classServerPort, int priority)
	{
		this.appUuid = uuid;
		this.host = host;
		this.port = driverPort;
		this.priority = priority;
		this.classServerPort = classServerPort;
		this.name = name;
	}

	/**
	 * Initialize this client connection.
	 */
	public void init()
	{
		try
		{
			initHelper();
			delegate = new ClassServerDelegate(this, appUuid, host, classServerPort);
			initCredentials();
			delegate.start();
			initConnection();
			setStatus(ACTIVE);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			setStatus(FAILED);
		}
		catch(JPPFError e)
		{
			setStatus(FAILED);
			throw e;
		}
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void initConnection() throws Exception
	{
		try
		{
			setStatus(CONNECTING);
			if (socketClient == null) initSocketClient();
			String msg = "[client: "+name+"] JPPFClient.init(): Attempting connection to the JPPF driver";
			System.out.println(msg);
			if (debugEnabled) log.debug(msg);
			socketInitializer.initializeSocket(socketClient);
			if (!socketInitializer.isSuccessfull())
			{
				throw new JPPFException("["+name+"] Could not reconnect to the JPPF Driver");
			}
			msg = "[client: "+name+"] JPPFClient.init(): Reconnected to the JPPF driver";
			System.out.println(msg);
			if (debugEnabled) log.debug(msg);
			setStatus(ACTIVE);
		}
		catch(Exception e)
		{
			setStatus(FAILED);
			throw e;
		}
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketClient() throws Exception
	{
		socketClient = new SocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}

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
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public List<JPPFTask> submit(List<JPPFTask> taskList, DataProvider dataProvider) throws Exception
	{
		lock.lock();
		try
		{
			final List<JPPFTask> resultList = new ArrayList<JPPFTask>();
			TaskResultListener listener = new TaskResultListener()
			{
				public void resultsReceived(TaskResultEvent event)
				{
					for (JPPFTask task: event.getTaskList()) resultList.add(task);
				}
			};
			ClientExecution exec = new ClientExecution(taskList, dataProvider, true, listener);
			AsynchronousResultProcessor proc = new AsynchronousResultProcessor(this, exec);
			proc.run();
			if ((taskList != null) && (taskList.size() > 0))
			{
				totalTaskCount += taskList.size();
				if (debugEnabled) log.debug("["+name+"] submitted " + taskList.size() + " tasks for a total of " + totalTaskCount);
			}
			Collections.sort(resultList, new Comparator<JPPFTask>()
			{
				public int compare(JPPFTask o1, JPPFTask o2)
				{
					return o1.getPosition() - o2.getPosition();
				}
			});
			return resultList;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param listener listener to notify whenever a set of results have been received.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public void submitNonBlocking(List<JPPFTask> taskList, DataProvider dataProvider, TaskResultListener listener)
			throws Exception
	{
		ClientExecution exec = new ClientExecution(taskList, dataProvider, false, listener);
		AsynchronousResultProcessor proc =
			new AsynchronousResultProcessor(this, exec);
		executor.submit(proc);
		if (debugEnabled) log.debug("["+name+"] submitted " + taskList.size() + " tasks");
	}

	/**
	 * Send tasks to the server for execution.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @throws Exception if an error occurs while sending the request.
	 */
	void sendTasks(List<JPPFTask> taskList, DataProvider dataProvider) throws Exception
	{
		JPPFRequestHeader header = new JPPFRequestHeader();
		header.setRequestType(JPPFRequestHeader.Type.NON_BLOCKING_EXECUTION);
		header.setAppUuid(appUuid);
		header.setCredentials(credentials);
		int count = taskList.size();
		header.setTaskCount(count);
		List<JPPFBuffer> bufList = new ArrayList<JPPFBuffer>();
		JPPFBuffer buffer = helper.toBytes(header, false);
		int size = 4 + buffer.getLength();
		bufList.add(buffer);
		buffer = helper.toBytes(dataProvider, true); 
		size += 4 + buffer.getLength();
		bufList.add(buffer);
		for (JPPFTask task : taskList)
		{
			buffer = helper.toBytes(task, true); 
			size += 4 + buffer.getLength();
			bufList.add(buffer);
		}
		byte[] data = new byte[size];
		int pos = 0;
		for (JPPFBuffer buf: bufList)
		{
			//pos = helper.writeInt(buf.getLength(), data, pos);
			pos = helper.copyToBuffer(buf.getBuffer(), data, pos, buf.getLength());
		}

		buffer = new JPPFBuffer(data, size);
		socketClient.sendBytes(buffer);
	}

	/**
	 * Receive results of tasks execution.
	 * @return a pair of objects representing the executed tasks results, and the index
	 * of the first result within the initial task execution request.
	 * @throws Exception if an error is raised while reading the results from the server.
	 */
	Pair<List<JPPFTask>, Integer> receiveResults() throws Exception
	{
		JPPFBuffer buf = socketClient.receiveBytes(0);
		byte[] data = buf.getBuffer();
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		int pos = 0;
		int count = helper.readInt(data, pos);
		pos += 4;
		helper.fromBytes(data, pos, true, taskList, count);
		int startIndex = (taskList.isEmpty()) ? -1 : taskList.get(0).getPosition();
		Pair<List<JPPFTask>, Integer> p = new Pair<List<JPPFTask>, Integer>(taskList, startIndex);
		return p;
	}

	/**
	 * Send a request to get the statistics collected by the JPPF server.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if an error occurred while trying to get the server statistics.
	 */
	public JPPFStats requestStatistics() throws Exception
	{
		lock.lock();
		try
		{
			boolean completed = false;
			JPPFStats stats = null;
			while (!completed)
			{
				try
				{
					JPPFRequestHeader header = new JPPFRequestHeader();
					header.setAppUuid(appUuid);
					header.setCredentials(credentials);
					header.setRequestType(JPPFRequestHeader.Type.STATISTICS);
					JPPFBuffer buf = helper.toBytes(header, false);
					byte[] data = new byte[buf.getLength() + 4];
					helper.copyToBuffer(buf.getBuffer(), data, 0, buf.getLength());
					buf.setLength(data.length);
					buf.setBuffer(data);
					socketClient.sendBytes(buf);
					buf = socketClient.receiveBytes(0);
					ObjectSerializer serializer = new ObjectSerializerImpl();
					stats = (JPPFStats) serializer.deserialize(buf.getBuffer());
					completed = true;
				}
				catch(IOException e)
				{
					log.error("["+name+"] "+e.getMessage(), e);
					initConnection();
				}
			}
			return stats;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Submit an admin request with the specified command name and parameters.
	 * @param password the current admin password.
	 * @param newPassword the new password if the password is to be changed, can be null.
	 * @param command the name of the command to submit.
	 * @param parameters the parameters of the command to submit, may be null.
	 * @return the reponse message from the server.
	 * @throws Exception if an error occurred while trying to send or execute the command.
	 */
	public String submitAdminRequest(String password, String newPassword, String command, Map<String, Object> parameters)
			throws Exception
	{
		lock.lock();
		try
		{
			AdminRequest request = new AdminRequest();
			request.setAppUuid(appUuid);
			request.setCredentials(credentials);
			request.setRequestType(JPPFRequestHeader.Type.ADMIN);
			request.setParameter(COMMAND_PARAM, command);
			SecretKey tmpKey = CryptoUtils.generateSecretKey();
			request.setParameter(KEY_PARAM, CryptoUtils.encrypt(tmpKey.getEncoded()));
			request.setParameter(PASSWORD_PARAM, CryptoUtils.encrypt(tmpKey, password.getBytes()));
			if (newPassword != null)
			{
				request.setParameter(NEW_PASSWORD_PARAM, CryptoUtils.encrypt(tmpKey, newPassword.getBytes()));
			}
			if (parameters != null)
			{
				for (String key: parameters.keySet())
				{
					request.setParameter(key, parameters.get(key));
				}
			}
			sendAdminRequest(request);
			JPPFBuffer buf = socketClient.receiveBytes(0);
			List<AdminRequest> list = new ArrayList<AdminRequest>();
			helper.fromBytes(buf.getBuffer(), 0, false, list, 1);
			request = list.get(0);
			return (String) request.getParameter(RESPONSE_PARAM);
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Send an administration request to the server.
	 * @param request the request to send, with its parameters populated.
	 * @throws Exception if the request could not be sent.
	 */
	private void sendAdminRequest(AdminRequest request) throws Exception
	{
		JPPFBuffer buf = helper.toBytes(request, false);
		socketClient.sendBytes(buf);
	}

	/**
	 * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
	 * @throws Exception if an error occcurs while instantiating the class loader.
	 */
	private void initHelper() throws Exception
	{
		helper = new SerializationHelperImpl();
	}

	/**
	 * Get the priority assigned to this connection.
	 * @return a priority as an int value.
	 */
	public int getPriority()
	{
		return priority;
	}

	/**
	 * Get the status of this connection.
	 * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
	 */
	public JPPFClientConnectionStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of this connection.
	 * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
	 */
	public synchronized void setStatus(JPPFClientConnectionStatus status)
	{
		this.status = status;
		fireStatusChanged();
	}

	/**
	 * Add a connection status listener to this connection's list of listeners.
	 * @param listener the listener to add to the list.
	 */
	public synchronized void addClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a connection status listener from this connection's list of listeners.
	 * @param listener the listener to remove from the list.
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
	 * @return a list of <code>ClientExecution</code> instances to resubmit.
	 */
	public List<ClientExecution> close()
	{
		if (!isShutdown)
		{
			isShutdown = true;
			try
			{
				socketClient.close();
				delegate.close();
			}
			catch(IOException e)
			{
				log.error("[" + name + "] "+ e.getMessage(), e);
			}
			List<Runnable> pending = executor.shutdownNow();
			List<ClientExecution> result = new ArrayList<ClientExecution>();
			if (currentExecution != null) result.add(currentExecution);
			while (!pending.isEmpty())
			{
				AsynchronousResultProcessor proc = (AsynchronousResultProcessor) pending.remove(0);
				result.add(proc.getExecution());
			}
			return result;
		}
		return null;
	}

	/**
	 * Get the name assigned tothis client connection.
	 * @return the name as a string.
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
}
