/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;
import static org.jppf.server.protocol.BundleParameter.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.SecretKey;

import org.apache.commons.logging.*;
import org.jppf.JPPFError;
import org.jppf.client.event.TaskResultListener;
import org.jppf.comm.socket.*;
import org.jppf.security.CryptoUtils;
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
public class JPPFClientConnectionImpl extends AbstractJPPFClientConnection
{
	/**
	 * Log4j logger for this class.
	 */
	static Log log = LogFactory.getLog(JPPFClientConnectionImpl.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
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
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param host the name or IP address of the host the JPPF driver is running on.
	 * @param driverPort the TCP port the JPPF driver listening to for submitted tasks.
	 * @param classServerPort the TCP port the class server is listening to.
	 * @param priority the assigned to this client connection.
	 */
	public JPPFClientConnectionImpl(String uuid, String name, String host, int driverPort, int classServerPort, int priority)
	{
		super(uuid, name, host, driverPort, classServerPort, priority);
	}

	/**
	 * 
	 * @see org.jppf.client.JPPFClientConnection#init()
	 */
	public void init()
	{
		try
		{
			initHelper();
			delegate = new ClassServerDelegateImpl(this, appUuid, host, classServerPort);
			delegate.init();
			initCredentials();
			if (!delegate.isClosed())
			{
				Thread t = new Thread(delegate);
				t.setName("[" + delegate.getName() + " : class delegate]");
				t.start();
				initConnection();
				setStatus(ACTIVE);
			}
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
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param listener listener to notify whenever a set of results have been received.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.JPPFClientConnection#submit(java.util.List, org.jppf.task.storage.DataProvider, org.jppf.client.event.TaskResultListener)
	 */
	public void submit(List<JPPFTask> taskList, DataProvider dataProvider, TaskResultListener listener)
			throws Exception
	{
		ClientExecution exec = new ClientExecution(taskList, dataProvider, false, listener);
		AsynchronousResultProcessor proc = new AsynchronousResultProcessor(this, exec);
		executor.submit(proc);
		if (debugEnabled) log.debug("["+name+"] submitted " + taskList.size() + " tasks");
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
					JPPFTaskBundle header = new JPPFTaskBundle();
					header.setRequestUuid(new JPPFUuid().toString());
					TraversalList<String> uuidPath = new TraversalList<String>();
					uuidPath.add(appUuid);
					header.setUuidPath(uuidPath);
					header.setCredentials(credentials);
					header.setRequestType(JPPFTaskBundle.Type.STATISTICS);
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
	public String submitAdminRequest(String password, String newPassword, BundleParameter command, Map<BundleParameter, Object> parameters)
			throws Exception
	{
		lock.lock();
		try
		{
			JPPFTaskBundle request = new JPPFTaskBundle();
			request.setRequestUuid(new JPPFUuid().toString());
			TraversalList<String> uuidPath = new TraversalList<String>();
			uuidPath.add(appUuid);
			request.setUuidPath(uuidPath);
			request.setCredentials(credentials);
			request.setRequestType(JPPFTaskBundle.Type.ADMIN);
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
				for (BundleParameter key: parameters.keySet())
				{
					request.setParameter(key, parameters.get(key));
				}
			}
			sendAdminRequest(request);
			JPPFBuffer buf = socketClient.receiveBytes(0);
			List<JPPFTaskBundle> list = new ArrayList<JPPFTaskBundle>();
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
	private void sendAdminRequest(JPPFTaskBundle request) throws Exception
	{
		JPPFBuffer buf = helper.toBytes(request, false);
		byte[] data = new byte[buf.getLength() + 4];
		helper.copyToBuffer(buf.getBuffer(), data, 0, buf.getLength());
		buf.setLength(data.length);
		buf.setBuffer(data);
		socketClient.sendBytes(buf);
	}

	/**
	 * Shutdown this client and retrieve all pending executions for resubmission.
	 * @return a list of <code>ClientExecution</code> instances to resubmit.
	 * @see org.jppf.client.JPPFClientConnection#close()
	 */
	public List<ClientExecution> close()
	{
		if (!isShutdown)
		{
			isShutdown = true;
			try
			{
				if (socketInitializer != null) socketInitializer.close();
				if (socketClient != null) socketClient.close();
				if (delegate != null) delegate.close();
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
	 * @return an instance of <code>SocketInitializerImpl</code>.
	 * @see org.jppf.client.AbstractJPPFClientConnection#createSocketInitializer()
	 */
	protected SocketInitializer createSocketInitializer()
	{
		return new SocketInitializerImpl();
	}
}
