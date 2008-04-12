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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.FAILED;
import static org.jppf.server.protocol.BundleParameter.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.SecretKey;

import org.apache.commons.logging.*;
import org.jppf.JPPFError;
import org.jppf.client.event.TaskResultListener;
import org.jppf.comm.socket.*;
import org.jppf.management.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.security.CryptoUtils;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.TypedProperties;

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
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFClientConnectionImpl.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Signature of the method invoked on the MBean.
	 */
	private static final String[] MBEAN_SIGNATURE = new String[] {JPPFManagementRequest.class.getName()};
	/**
	 * Used to synchronize request submissions performed by mutliple threads.
	 */
	private ReentrantLock lock = new ReentrantLock();
	/**
	 * The pool of threads used for submitting execution requests.
	 */
	private ThreadPoolExecutor executor =
		new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>());
	/**
	 * Provides access to the management functions of the driver.
	 */
	private JMXConnectionWrapper jmxConnection = null;
	/**
	 * Contains the configuration properties for this client connection.
	 */
	private TypedProperties props = null;

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 * @param name configuration name for this local client.
	 * @param props the configuration properties for this connection.
	 */
	public JPPFClientConnectionImpl(String uuid, String name, TypedProperties props)
	{
		this.props = props;
		String prefix = name + ".";
		configure(uuid, name, props.getString(prefix + "jppf.server.host", "localhost"),
			props.getInt(prefix + "app.server.port", 11112),
			classServerPort = props.getInt(prefix + "class.server.port", 11111),
			props.getInt(prefix + "priority", 0));
	}

	/**
	 * Initialize this client connection.
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
				//setStatus(delegate.getStatus());
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
	 * Initialize the jmx connection using the specifed jmx server id.
	 * @param id the unique id of the jmx server to connect to.
	 */
	public void initializeJmxConnection(String id)
	{
		String prefix = name + ".";
		String mHost = props.getString(prefix + "jppf.management.host", "localhost");
		int port = props.getInt(prefix + "jppf.management.port", 11198);
		jmxConnection = new JMXConnectionWrapper(mHost, port);
		jmxConnection.connect();
	}

	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param listener listener to notify whenever a set of results have been received.
	 * @param policy an execution policy that deternmines on which node(s) the tasks will be permitted to run.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.JPPFClientConnection#submit(java.util.List, org.jppf.task.storage.DataProvider, org.jppf.client.event.TaskResultListener)
	 */
	public void submit(List<JPPFTask> taskList, DataProvider dataProvider, TaskResultListener listener, ExecutionPolicy policy)
			throws Exception
	{
		ClientExecution exec = new ClientExecution(taskList, dataProvider, false, listener, policy);
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
		Map<BundleParameter, Object> params = new EnumMap<BundleParameter, Object>(BundleParameter.class);
		params.put(COMMAND_PARAM, READ_STATISTICS);
    return (JPPFStats) processManagementRequest(params);
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
	public String submitAdminRequest(String password, String newPassword, BundleParameter command, 
		Map<BundleParameter, Object> parameters) throws Exception
	{
			parameters.put(PASSWORD_PARAM, password);
			parameters.put(NEW_PASSWORD_PARAM, newPassword);
			parameters.put(COMMAND_PARAM, command);
			return (String) processManagementRequest(parameters);
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
				if (jmxConnection != null) jmxConnection.close();
			}
			catch(Exception e)
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

	/**
	 * Get the lock used to synchronize request submissions performed by mutliple threads.
	 * @return  a <code>ReentrantLock</code> instance.
	 */
	public ReentrantLock getLock()
	{
		return lock;
	}

	/**
	 * Get the object that provides access to the management functions of the driver.
	 * @return a <code>JMXConnectionWrapper</code> instance.
	 */
	public JMXConnectionWrapper getJmxConnection()
	{
		return jmxConnection;
	}

	/**
	 * Process a management or monitoring request.
	 * @param parameters the parameters of the request to process
	 * @return the result of the request.
	 * @throws Exception if an error occurred while performing the request.
	 */
	public Object processManagementRequest(Map<BundleParameter, Object> parameters) throws Exception
	{
		if (!READ_STATISTICS.equals(parameters.get(COMMAND_PARAM)) &&
				!REFRESH_NODE_INFO.equals(parameters.get(COMMAND_PARAM)))
		{
			String password = (String) parameters.get(PASSWORD_PARAM);
			SecretKey tmpKey = CryptoUtils.generateSecretKey();
			parameters.put(KEY_PARAM, CryptoUtils.encrypt(tmpKey.getEncoded()));
			parameters.put(PASSWORD_PARAM, CryptoUtils.encrypt(tmpKey, password.getBytes()));
			String newPassword = (String) parameters.get(NEW_PASSWORD_PARAM);
			if (newPassword != null)
			{
				parameters.put(NEW_PASSWORD_PARAM, CryptoUtils.encrypt(tmpKey, newPassword.getBytes()));
			}
		}
		JPPFManagementRequest<BundleParameter, Object> request =
			new JPPFManagementRequest<BundleParameter, Object>(parameters);
		JPPFManagementResponse response = (JPPFManagementResponse) getJmxConnection().invoke(
				JPPFAdminMBean.DRIVER_MBEAN_NAME, "performAdminRequest", new Object[] {request}, MBEAN_SIGNATURE);
		if (response == null) return null;
		if (response.getException() == null) return response.getResult();
		throw response.getException();
	}

	/**
	 * Get the information to connect to the JMX servers of the nodes attached to a driver.
	 * This method works by querying the driver for the information, through a JMX request. 
	 * @return a coolection of <code>NodeManagementInfo</code>, which encapsulate the nodes JMX
	 * server connection information.
	 * @throws Exception if the connection ot the drivers JMX failed.
	 */
	public Collection<NodeManagementInfo> getNodeManagementInfo() throws Exception
	{
		Map<BundleParameter, Object> params = new HashMap<BundleParameter, Object>();
		params.put(BundleParameter.COMMAND_PARAM, BundleParameter.REFRESH_NODE_INFO);
		Collection<NodeManagementInfo> nodeList =
			(Collection<NodeManagementInfo>) processManagementRequest(params);
		return nodeList;
	}
}
