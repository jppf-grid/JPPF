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

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.event.TaskResultListener;
import org.jppf.client.loadbalancer.LoadBalancer;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClient extends AbstractJPPFClient
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFClient.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The pool of threads used for submitting execution requests.
	 */
	private ExecutorService executor = null;
	/**
	 * The load balancer for local versus remote execution.
	 */
	private static LoadBalancer loadBalancer = new LoadBalancer();
	/**
	 * The JPPF configuration properties.
	 */
	private static TypedProperties config = JPPFConfiguration.getProperties();
	/**
	 * Determines whether local execution is enabled.
	 */
	public static final boolean LOCAL_EXEC_ENABLED = config.getBoolean("jppf.local.execution.enabled", true);

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public JPPFClient()
	{
		super();
		initPools();
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	public JPPFClient(String uuid)
	{
		super(uuid);
		initPools();
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	public void initPools()
	{
		try
		{
			String driverNames = config.getString("jppf.drivers");
			if (debugEnabled) log.debug("list of drivers: " + driverNames);
			String[] names = null;
			// if config file is still used as with single client version
			if ((driverNames == null) || "".equals(driverNames.trim()))
			{
				//names = new String[] { "" };
				return;
			}
			else
			{
				names = driverNames.split("\\s");
			}
			for (String s: names)
			{
				String name = "".equals(s) ? "default" : s;
				JPPFClientConnection c = new JPPFClientConnectionImpl(uuid, name, config);
				int priority = c.getPriority();
				c.addClientConnectionStatusListener(this);
				ClientPool pool = pools.get(priority);
				if (pool == null)
				{
					pool = new ClientPool();
					pool.priority = priority;
					pools.put(priority, pool);
				}
				pool.clientList.add(c);
			}
			for (int priority: pools.keySet())
			{
				ClientPool pool = pools.get(priority);
				for (JPPFClientConnection c: pool.clientList) allConnections.add(c);
			}
			executor = Executors.newFixedThreadPool(Math.max(1, allConnections.size()), new JPPFThreadFactory("JPPF Client"));
			for (Integer priority: pools.keySet())
			{
				ClientPool pool = pools.get(priority);
				for (JPPFClientConnection c: pool.clientList)
				{
					executor.submit(new ConnectionInitializer(c));
				}
			}
			waitForPools();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Wait a maximum time specified in the configuration until the connections are initialized.
	 */
	private void waitForPools()
	{
		long maxWait = JPPFConfiguration.getProperties().getLong("jppf.client.max.init.time", 1000L);
		if (pools.isEmpty() || (maxWait <= 0)) return;
		long elapsed = 0;
		while (elapsed < maxWait)
		{
			long start = System.currentTimeMillis();
			if (getClientConnection(true) != null) break;
			try
			{
				Thread.sleep(50);
			}
			catch(Exception ignored)
			{
			}
			elapsed += System.currentTimeMillis() - start;
		}
	}

	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param policy an execution policy that determines on which node(s) the tasks will be permitted to run.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.AbstractJPPFClient#submit(java.util.List, org.jppf.task.storage.DataProvider, org.jppf.node.policy.ExecutionPolicy)
	 */
	public List<JPPFTask> submit(List<JPPFTask> taskList, DataProvider dataProvider, ExecutionPolicy policy) throws Exception
	{
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) getClientConnection(true);
		if (c != null)
		{
			//return super.submit(taskList, dataProvider, policy);
			JPPFResultCollector collector = new JPPFResultCollector(taskList.size());
			c.submit(taskList, dataProvider, collector, policy);
			return collector.waitForResults();
		}
		if (LOCAL_EXEC_ENABLED)
		{
			JPPFResultCollector collector = new JPPFResultCollector(taskList.size());
			ClientExecution exec = new ClientExecution(taskList, dataProvider, true, collector, policy);
			JPPFClient.getLoadBalancer().execute(exec, c);
			return collector.waitForResults();
		}
		/*
		if (!pools.isEmpty() || LOCAL_EXEC_ENABLED)
		{
			if (LOCAL_EXEC_ENABLED)
			{
				JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) getClientConnection(true);
				JPPFResultCollector collector = new JPPFResultCollector(taskList.size());
				ClientExecution exec = new ClientExecution(taskList, dataProvider, true, collector, policy);
				JPPFClient.getLoadBalancer().execute(exec, c);
				return collector.waitForResults();
			}
			return super.submit(taskList, dataProvider, policy);
		}
		*/
		throw new JPPFException("Cannot execute: no driver connection available and local execution is disabled");
	}

	/**
	 * Submit a non-blocking request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param listener listener to notify whenever a set of results have been received.
	 * @param policy an execution policy that determines on which node(s) the tasks will be permitted to run.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.AbstractJPPFClient#submitNonBlocking(java.util.List, org.jppf.task.storage.DataProvider, org.jppf.client.event.TaskResultListener, org.jppf.node.policy.ExecutionPolicy)
	 */
	public void submitNonBlocking(List<JPPFTask> taskList, DataProvider dataProvider, TaskResultListener listener, ExecutionPolicy policy)
		throws Exception
	{
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) getClientConnection(true);
		if (c != null)
		{
			//super.submitNonBlocking(taskList, dataProvider, listener, policy);
			c.submit(taskList, dataProvider, listener, policy);
			return;
		}
		if (LOCAL_EXEC_ENABLED)
		{
			ClientExecution exec = new ClientExecution(taskList, dataProvider, false, listener, policy);
			JPPFClient.getLoadBalancer().execute(exec, c);
			return;
		}
		throw new JPPFException("Cannot execute: no driver connection available and local execution is disabled");
		/*
		if (!pools.isEmpty() || LOCAL_EXEC_ENABLED)
		{
			if (LOCAL_EXEC_ENABLED)
			{
				JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) getClientConnection(true);
				ClientExecution exec = new ClientExecution(taskList, dataProvider, false, listener, policy);
				JPPFClient.getLoadBalancer().execute(exec, c);
			}
			else super.submitNonBlocking(taskList, dataProvider, listener, policy);
		}
		else throw new JPPFException("Cannot execute: no driver connection available and local execution is disabled");
		*/
	}

	/**
	 * Send a request to get the statistics collected by the JPPF server.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if an error occurred while trying to get the server statistics.
	 */
	public JPPFStats requestStatistics() throws Exception
	{
		JPPFClientConnectionImpl conn = (JPPFClientConnectionImpl) getClientConnection(true);
		return (conn == null) ? null : conn.requestStatistics();
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
		JPPFClientConnectionImpl conn = (JPPFClientConnectionImpl) getClientConnection(); 
		return conn.submitAdminRequest(password, newPassword, command, parameters);
	}

	/**
	 * Close this client and release all the resources it is using.
	 */
	public void close()
	{
		super.close();
		if (executor != null) executor.shutdownNow();
	}

	/**
	 * Wrapper class for the initialization of a client connection.
	 */
	private static class ConnectionInitializer implements Runnable
	{
		/**
		 * The client connection to initialize.
		 */
		private JPPFClientConnection c = null;
		/**
		 * Instantiate this connection initializer with the specified client connection.
		 * @param c the client connection to initialize.
		 */
		public ConnectionInitializer(JPPFClientConnection c)
		{
			this.c = c;
		}

		/**
		 * Perform the initialization of a client connection.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			if (debugEnabled) log.debug("initializing driver connection '"+c+"'");
			c.init();
		}
	}

	/**
	 * Get the load balancer for local versus remote execution.
	 * @return a <code>LoadBalancer</code> instance.
	 */
	public static LoadBalancer getLoadBalancer()
	{
		return loadBalancer;
	}
}
