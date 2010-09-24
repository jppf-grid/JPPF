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

import java.util.List;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.client.event.*;
import org.jppf.client.loadbalancer.LoadBalancer;
import org.jppf.comm.discovery.JPPFConnectionInformation;
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
public class JPPFClient extends AbstractGenericClient
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
	 * The load balancer for local versus remote execution.
	 */
	private static LoadBalancer loadBalancer = new LoadBalancer();
	/**
	 * Determines whether local execution is enabled.
	 */
	public static final boolean LOCAL_EXEC_ENABLED = JPPFConfiguration.getProperties().getBoolean("jppf.local.execution.enabled", true);

	/**
	 * Initialize this client with an automatically generated application UUID.
	 */
	public JPPFClient()
	{
		super(JPPFConfiguration.getProperties());
	}

	/**
	 * Initialize this client with an automatically generated application UUID.
	 * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
	 */
	public JPPFClient(ClientListener...listeners)
	{
		this();
		for (ClientListener listener: listeners) addClientListener(listener);
	}

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 */
	public JPPFClient(String uuid)
	{
		super(uuid, JPPFConfiguration.getProperties());
	}

	/**
	 * Initialize this client with the specified application UUID and new connection listeners.
	 * @param uuid the unique identifier for this local client.
	 * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
	 */
	public JPPFClient(String uuid, ClientListener...listeners)
	{
		this(uuid);
		for (ClientListener listener: listeners) addClientListener(listener);
	}

	/**
	 * Initialize this client's configuration.
	 * @param configuration an object holding the JPPF configuration.
	 */
	protected void initConfig(Object configuration)
	{
		config = (TypedProperties) configuration;
	}

	/**
	 * Create a new driver connection based on the specified parameters.
	 * @param uuid the uuid of the JPPF client.
	 * @param name the name of the connection.
	 * @param info the driver connection information.
	 * @return an instance of a subclass of {@link AbstractJPPFClientConnection}.
	 */
	protected AbstractJPPFClientConnection createConnection(String uuid, String name, JPPFConnectionInformation info)
	{
		return new JPPFClientConnectionImpl(uuid, name, info);
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
				JPPFClient.getLoadBalancer().execute(job, null);
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
				JPPFClient.getLoadBalancer().execute(job, null);
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
		if (loadBalancer != null) loadBalancer.stop();
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
