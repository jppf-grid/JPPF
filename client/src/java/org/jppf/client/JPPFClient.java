/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
		initPools();
	}

	/**
	 * Initialize this client with an automatically generated application UUID.
	 * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
	 */
	public JPPFClient(ClientListener...listeners)
	{
		super();
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
		initPools();
	}

	/**
	 * Initialize this client with the specified application UUID and new connection listeners.
	 * @param uuid the unique identifier for this local client.
	 * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
	 */
	public JPPFClient(String uuid, ClientListener...listeners)
	{
		super(uuid);
		for (ClientListener listener: listeners) addClientListener(listener);
		initPools();
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	protected void initPools()
	{
		LinkedBlockingQueue queue = new LinkedBlockingQueue();
		executor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, new JPPFThreadFactory("JPPF Client"));
		if (config.getBoolean("jppf.discovery.enabled", true)) initPoolsFromAutoDiscovery();
		else initPoolsFromConfig();
	}

	/**
	 * Read all client connection information from the configuration and initialize
	 * the connection pools accordingly.
	 */
	private void initPoolsFromConfig()
	{
		try
		{
			String driverNames = config.getString("jppf.drivers", "default-driver");
			if ((driverNames == null) || "".equals(driverNames.trim())) driverNames = "default-driver";
			if (debugEnabled) log.debug("list of drivers: " + driverNames);
			String[] names = driverNames.split("\\s");
			for (String name: names)
			{
				int n = config.getInt(name + ".jppf.pool.size", 1);
				if (n <= 0) n = 1;
				for (int i=1; i<=n; i++)
				{
					//JPPFClientConnection c = new JPPFClientConnectionImpl(uuid, (n > 1) ? name + "-" + i : name, config);
					JPPFConnectionInformation info = new JPPFConnectionInformation();
					info.host = config.getString(name + ".jppf.server.host", "localhost");
					info.classServerPorts = new int[] { config.getInt(name + ".class.server.port", 11111) };
					info.applicationServerPorts = new int[] { config.getInt(name + ".app.server.port", 11112) };
					info.managementPort = config.getInt(name + ".jppf.management.port", 11198);
					JPPFClientConnectionImpl c = new JPPFClientConnectionImpl(uuid, (n > 1) ? name + "-" + i : name, info);
					c.setPriority(config.getInt(name + ".priority", 0));
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
	private void initPoolsFromAutoDiscovery()
	{
		try
		{
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
		super.newConnection(c);
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
	 * @deprecated {@link #submit(org.jppf.client.JPPFJob) submit(JPPFJob)} should be used instead.
	 */
	public List<JPPFTask> submit(List<JPPFTask> taskList, DataProvider dataProvider, ExecutionPolicy policy, int priority) throws Exception
	{
		JPPFJobSLA sla = new JPPFJobSLA(policy, priority);
		JPPFJob job = new JPPFJob(dataProvider, sla, true, null);
		for (JPPFTask task: taskList) job.addTask(task);
		return submit(job);
	}

	/**
	 * Submit the request to the server.
	 * @param job - the job to execute remotely.
	 * @return the list of executed tasks with their results.
	 * @throws Exception if an error occurs while sending the request.
	 * @see org.jppf.client.AbstractJPPFClient#submit(org.jppf.client.JPPFJob)
	 */
	public List<JPPFTask> submit(JPPFJob job) throws Exception
	{
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) getClientConnection(true);
		if (job.isBlocking())
		{
			if (c != null)
			{
				JPPFResultCollector collector = new JPPFResultCollector(job.getTasks().size());
				job.setResultListener(collector);
				c.submit(job);
				return collector.waitForResults();
			}
			if (LOCAL_EXEC_ENABLED)
			{
				JPPFResultCollector collector = new JPPFResultCollector(job.getTasks().size());
				job.setResultListener(collector);
				JPPFClient.getLoadBalancer().execute(job, c);
				return collector.waitForResults();
			}
		}
		else
		{
			if (c != null)
			{
				c.submit(job);
				return null;
			}
			if (LOCAL_EXEC_ENABLED)
			{
				JPPFClient.getLoadBalancer().execute(job, c);
				return null;
			}
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
	 * @deprecated {@link #submit(org.jppf.client.JPPFJob) submit(JPPFJob)} should be used instead.
	 */
	public void submitNonBlocking(List<JPPFTask> taskList, DataProvider dataProvider, TaskResultListener listener, ExecutionPolicy policy, int priority)
		throws Exception
	{
		JPPFJobSLA sla = new JPPFJobSLA(policy, priority);
		JPPFJob job = new JPPFJob(dataProvider, sla, false, listener);
		for (JPPFTask task: taskList) job.addTask(task);
		submit(job);
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
	 * This class listens to information broadcast by JPPF servers on the network and uses it
	 * to establish a connection with one or more servers. 
	 */
	private class JPPFMulticastReceiverThread extends ThreadSynchronization implements Runnable
	{
		/**
		 * Contains the set of retrieved connection information objects.
		 */
		private Set<JPPFConnectionInformation> infoSet = new HashSet<JPPFConnectionInformation>();
		/**
		 * Count of distinct retrieved connection information objects.
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
						int n = JPPFConfiguration.getProperties().getInt("jppf.pool.size", 1);
						if (n < 1) n = 1;
						int currentCount = count.incrementAndGet();
						for (int i=1; i<=n; i++)
						{
							String name = "driver-" + currentCount  + (n == 1 ? "" : "-" + i);
							JPPFClientConnection c = new JPPFClientConnectionImpl(uuid, name, info);
							newConnection(c);
						}
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
