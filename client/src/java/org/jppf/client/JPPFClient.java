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
package org.jppf.client;

import static org.jppf.server.protocol.AdminRequest.*;
import static org.jppf.server.protocol.JPPFRequestHeader.ADMIN;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.jppf.classloader.ClassServerDelegate;
import org.jppf.client.event.*;
import org.jppf.comm.socket.*;
import org.jppf.security.CryptoUtils;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from the submitting 
 * application should be dynamically reloaded or not, depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClient
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFClient.class);

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
	private ReentrantLock lock = new ReentrantLock();
	/**
	 * The pool of threads used for submitting execution requests.
	 */
	private ExecutorService executor = Executors.newFixedThreadPool(1);

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public JPPFClient()
	{
		this(new JPPFUuid().toString());
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	public JPPFClient(String uuid)
	{
		try
		{
			this.appUuid = uuid;
			initHelper();
			delegate = new ClassServerDelegate(uuid);
			delegate.start();
			init();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public synchronized void init() throws Exception
	{
		if (socketClient == null) initSocketClient();
		System.out.println("JPPFClient.init(): Attempting connection to the JPPF driver");
		socketInitializer.initializeSocket(socketClient);
		System.out.println("JPPFClient.init(): Reconnected to the JPPF driver");
	}
	
	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketClient() throws Exception
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.server.host", "localhost");
		int port = props.getInt("app.server.port", 11112);
		socketClient = new SocketClient();
		//socketClient = new SocketChannelClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}
	
	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 */
	public List<JPPFTask> submit(List<JPPFTask> taskList, DataProvider dataProvider)
		throws Exception
	{
		lock.lock();
		try
		{
			boolean completed = false;
			List<JPPFTask> resultList = new ArrayList<JPPFTask>();
			
			while (!completed)
			{
				try
				{
					sendTasks(taskList, dataProvider, true);
					Pair<List<JPPFTask>, Integer> p = receiveResults();
					resultList = p.first();
					completed = true;
				}
				catch(NotSerializableException e)
				{
					throw e;
				}
				catch(Exception e)
				{
					log.error(e.getMessage(), e);
					init();
				}
			}
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
	public void submitNonBlocking(List<JPPFTask> taskList, DataProvider dataProvider,
		TaskResultListener listener) throws Exception
	{
		AsynchronousResultProcessor proc =
			new AsynchronousResultProcessor(taskList, dataProvider, listener);
		executor.submit(proc);
	}
	
	/**
	 * Send tasks to the server for execution.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param blocking determines whether the request is sent in non-blocking mode, and whether
	 * the tasks results should be sent asynchronously.
	 * @throws Exception if an error occurs while sending the request.
	 */
	private void sendTasks(List<JPPFTask> taskList, DataProvider dataProvider, boolean blocking)
		throws Exception
	{
		JPPFRequestHeader header = new JPPFRequestHeader();
		if (!blocking) header.setRequestType(JPPFRequestHeader.NON_BLOCKING_EXECUTION);
		header.setAppUuid(appUuid);
		int count = taskList.size();
		header.setTaskCount(count);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		helper.writeNextObject(header, dos, false);
		helper.writeNextObject(dataProvider, dos, true);
		for (JPPFTask task: taskList) helper.writeNextObject(task, dos, true);
		dos.flush();
		dos.close();
		JPPFBuffer buf = new JPPFBuffer(baos.toByteArray(), baos.size());
		socketClient.sendBytes(buf);
	}

	/**
	 * Receive results of tasks execution.
	 * @return a pair of objects representing the executed tasks results, and the index of
	 * the first result within the initial task execution request.
	 * @throws Exception if an error is raised while reading the results from the setrver.
	 */
	private Pair<List<JPPFTask>, Integer> receiveResults() throws Exception
	{
		JPPFBuffer buf = socketClient.receiveBytes(0);
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		ByteArrayInputStream bais = new ByteArrayInputStream(buf.getBuffer(), 0, buf.getLength());
		DataInputStream dis = new DataInputStream(bais);
		int count = dis.readInt();
		int startIndex = dis.readInt();
		for (int i=0; i<count; i++)
		{
			JPPFTask task = (JPPFTask) helper.readNextObject(dis, true);
			taskList.add(task);
		}
		dis.close();
		Pair<List<JPPFTask>, Integer> p = new Pair<List<JPPFTask>, Integer>(taskList, startIndex);
		return p;
	}

	/**
	 * Send a request to get the statistics collected by the JPPF server.
	 * @return  a <code>JPPFStats</code> instance.
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
					header.setRequestType(JPPFRequestHeader.STATISTICS);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					helper.writeNextObject(header, dos, false);
					dos.flush();
					dos.close();
					JPPFBuffer buf = new JPPFBuffer(baos.toByteArray(), baos.size());
					socketClient.sendBytes(buf);
					buf = socketClient.receiveBytes(0);
					ByteArrayInputStream bais = new ByteArrayInputStream(buf.getBuffer(), 0, buf.getLength());
					DataInputStream dis = new DataInputStream(bais);
					stats = (JPPFStats) helper.readNextObject(dis, false);
					dis.close();
					completed = true;
				}
				catch(IOException e)
				{
					log.error(e.getMessage(), e);
					init();
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
	 * @param parameters the parasmeters of the command to submit, may be null.
	 * @return the reponse message from the server.
	 * @throws Exception Exception if an error occurred while trying to send or execute the command.
	 */
	public String submitAdminRequest(String password, String newPassword, String command, Map<String, Object> parameters)
		throws Exception
	{
		lock.lock();
		try
		{
			AdminRequest request = new AdminRequest();
			request.setAppUuid(appUuid);
			request.setRequestType(ADMIN);
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
			ByteArrayInputStream bais = new ByteArrayInputStream(buf.getBuffer(), 0, buf.getLength());
			DataInputStream dis = new DataInputStream(bais);
			request = (AdminRequest) helper.readNextObject(dis, false);
			dis.close();
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		helper.writeNextObject(request, dos, false);
		dos.flush();
		dos.close();
		JPPFBuffer buf = new JPPFBuffer(baos.toByteArray(), baos.size());
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
	 * 
	 */
	private class AsynchronousResultProcessor implements Runnable
	{
		/**
		 * The list of tasks to execute remotely.
		 */
		private List<JPPFTask> taskList = null;
		/**
		 * The provider of the data shared among tasks, may be null.
		 */
		private DataProvider dataProvider = null;
		/**
		 * Listener to notify whenever a set of results have been received.
		 */
		private TaskResultListener listener = null;

		/**
		 * Initialize this result processor with a specified list of tasks, data provider and result listener.
		 * @param taskList the list of tasks to execute remotely.
		 * @param dataProvider the provider of the data shared among tasks, may be null.
		 * @param listener listener to notify whenever a set of results have been received.
		 */
		public AsynchronousResultProcessor(List<JPPFTask> taskList, DataProvider dataProvider,
			TaskResultListener listener)
		{
			this.taskList = taskList;
			this.dataProvider = dataProvider;
			this.listener = listener;
		}

		/**
		 * This method executes until all partial results have been received.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			lock.lock();
			try
			{
				int count = 0;
				boolean completed = false;
				while (!completed)
				{
					try
					{
						sendTasks(taskList, dataProvider, false);
						while (count < taskList.size())
						{
							Pair<List<JPPFTask>, Integer> p = receiveResults();
							count += p.first().size();
							if (listener != null) 
								listener.resultsReceived(new TaskResultEvent(p.first(), p.second()));
						}
						completed = true;
					}
					catch(NotSerializableException e)
					{
						throw e;
					}
					catch(Exception e)
					{
						log.error(e.getMessage(), e);
						init();
					}
				}
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
			finally
			{
				lock.unlock();
			}
		}
	}
}
