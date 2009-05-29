/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.event.*;
import org.jppf.client.loadbalancer.LoadBalancer;
import org.jppf.comm.discovery.*;
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
	private ThreadPoolExecutor executor = null;
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
	 * 
	 */
	private JPPFMulticastReceiverThread receiverThread = null;

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public JPPFClient()
	{
		super();
		if (debugEnabled) log.debug("********** in JPPFClient() **********");
		initPools();
	}

	/**
	 * Initialize this client with an automatically generated application UUID.
	 * @param listeners listenrs to add to this JPPF client.
	 */
	public JPPFClient(ClientListener...listeners)
	{
		super();
		if (debugEnabled) log.debug("**********  in JPPFClient(ClientListener[]) **********");
		for (ClientListener listener: listeners) addClientListener(listener);
		initPools();
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	public JPPFClient(String uuid)
	{
		super(uuid);
		if (debugEnabled) log.debug("********** in JPPFClient(uuid) **********");
		initPools();
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	public void initPools()
	{
		if (config.getBoolean("jppf.discovery.enabled", true)) initPoolsFromAutoDiscovery();
		else initPoolsFromConfig();
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	public void initPoolsFromConfig()
	{
		try
		{
			LinkedBlockingQueue queue = new LinkedBlockingQueue();
			executor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, new JPPFThreadFactory("JPPF Client"));
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
				int n = config.getInt(name + ".jppf.pool.size", 1);
				if (n <= 0) n = 1;
				for (int i=1; i<=n; i++)
				{
					JPPFClientConnection c =
						new JPPFClientConnectionImpl(uuid, (n > 1) ? name + "-" + i : name, config);
					newConnection(c);
				}
			}
			waitForPools(true);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Read connection information from the information and broadcasted 
	 * by the server and initialize the connection pools accordingly.
	 */
	public void initPoolsFromAutoDiscovery()
	{
		try
		{
			LinkedBlockingQueue queue = new LinkedBlockingQueue();
			executor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, new JPPFThreadFactory("JPPF Client"));
			receiverThread = new JPPFMulticastReceiverThread();
			new Thread(receiverThread).start();
			waitForPools(false);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Invoked when a new connection is created.
	 * @param c the connection that failed.
	 * @see org.jppf.client.AbstractJPPFClient#newConnection(org.jppf.client.JPPFClientConnection)
	 */
	public void newConnection(JPPFClientConnection c)
	{
		log.info("Connection [" + c.getName() + "] created");
		super.newConnection(c);
		c.addClientConnectionStatusListener(this);
		c.setStatus(JPPFClientConnectionStatus.NEW);
		int priority = c.getPriority();
		ClientPool pool = pools.get(priority);
		if (pool == null)
		{
			pool = new ClientPool();
			pool.priority = priority;
			pools.put(priority, pool);
		}
		pool.clientList.add(c);
		allConnections.add(c);
		int n = allConnections.size();
		if (executor.getCorePoolSize() < n)
		{
			executor.setMaximumPoolSize(n);
			executor.setCorePoolSize(n);
		}
		executor.submit(new ConnectionInitializer(c));
	}

	/**
	 * Wait a maximum time specified in the configuration until at least one connection is initialized.
	 * After this time, control is returned to the main application, no matter how many connections are initialized. 
	 * @param returnOnEmptyPool determines whether this method should return immediately when the pool of connections is empty.
	 */
	private void waitForPools(boolean returnOnEmptyPool)
	{
		if (returnOnEmptyPool && pools.isEmpty()) return;
		long maxWait = JPPFConfiguration.getProperties().getLong("jppf.client.max.init.time", 5000L);
		if (maxWait <= 0) return;
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
				if (debugEnabled) log.debug(ignored.getMessage(), ignored);
			}
			elapsed += System.currentTimeMillis() - start;
		}
	}

	/**
	 * Submit the request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param policy an execution policy that determines on which node(s) the tasks will be permitted to run.
	 * @param priority a value used by the JPPF driver to prioritize queued jobs.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.AbstractJPPFClient#submit(java.util.List, org.jppf.task.storage.DataProvider, org.jppf.node.policy.ExecutionPolicy)
	 */
	public List<JPPFTask> submit(List<JPPFTask> taskList, DataProvider dataProvider, ExecutionPolicy policy, int priority) throws Exception
	{
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) getClientConnection(true);
		if (c != null)
		{
			JPPFResultCollector collector = new JPPFResultCollector(taskList.size());
			c.submit(taskList, dataProvider, collector, policy, priority);
			return collector.waitForResults();
		}
		if (LOCAL_EXEC_ENABLED)
		{
			JPPFResultCollector collector = new JPPFResultCollector(taskList.size());
			ClientExecution exec = new ClientExecution(taskList, dataProvider, true, collector, policy);
			exec.priority = priority;
			JPPFClient.getLoadBalancer().execute(exec, c);
			return collector.waitForResults();
		}
		throw new JPPFException("Cannot execute: no driver connection available and local execution is disabled");
	}

	/**
	 * Submit a non-blocking request to the server.
	 * @param taskList the list of tasks to execute remotely.
	 * @param dataProvider the provider of the data shared among tasks, may be null.
	 * @param listener listener to notify whenever a set of results have been received.
	 * @param policy an execution policy that determines on which node(s) the tasks will be permitted to run.
	 * @param priority a value used by the JPPF driver to prioritize queued jobs.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.AbstractJPPFClient#submitNonBlocking(java.util.List, org.jppf.task.storage.DataProvider, org.jppf.client.event.TaskResultListener, org.jppf.node.policy.ExecutionPolicy)
	 */
	public void submitNonBlocking(List<JPPFTask> taskList, DataProvider dataProvider, TaskResultListener listener, ExecutionPolicy policy, int priority)
		throws Exception
	{
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) getClientConnection(true);
		if (c != null)
		{
			c.submit(taskList, dataProvider, listener, policy, priority);
			return;
		}
		if (LOCAL_EXEC_ENABLED)
		{
			ClientExecution exec = new ClientExecution(taskList, dataProvider, false, listener, policy);
			exec.priority = priority;
			JPPFClient.getLoadBalancer().execute(exec, c);
			return;
		}
		throw new JPPFException("Cannot execute: no driver connection available and local execution is disabled");
	}

	/**
	 * Send a request to get the statistics collected by the JPPF server.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if an error occurred while trying to get the server statistics.
	 */
	public JPPFStats requestStatistics() throws Exception
	{
		JPPFClientConnectionImpl conn = (JPPFClientConnectionImpl) getClientConnection(true);
		return (conn == null) ? null : conn.getJmxConnection().statistics();
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
		if (receiverThread != null) receiverThread.setStopped(true);
		if (executor != null) executor.shutdownNow();
		if (loadBalancer != null) loadBalancer.stop();
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

	/**
	 * 
	 */
	private class JPPFMulticastReceiverThread extends ThreadSynchronization implements Runnable
	{
		/**
		 * Contains the set of retrieved connection information objects.
		 */
		private Set<JPPFConnectionInformation> infoSet = new HashSet<JPPFConnectionInformation>();
		/**
		 * Count of distinct retrieved connection informaiton objects.
		 */
		private AtomicInteger count = new AtomicInteger(0);

		/**
		 * Lookup server configurations from UDP multicasts.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			JPPFMulticastReceiver receiver = new JPPFMulticastReceiver();
			try
			{
				while (!isStopped())
				{
					JPPFConnectionInformation info = receiver.receive();
					if ((info != null) && !infoSet.contains(info))
					{
						if (debugEnabled) log.debug("Found connection information: " + info);
						infoSet.add(info);
						JPPFClientConnection c = new JPPFClientConnectionImpl(uuid, "driver-"+count.incrementAndGet(), info);
						newConnection(c);
					}
				}
				//Thread.sleep(50L);
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
			finally
			{
				if (receiver != null) receiver.setStopped(true);
			}
		}
	}
}
