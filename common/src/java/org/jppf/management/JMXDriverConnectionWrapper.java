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

package org.jppf.management;

import java.util.*;

import org.jppf.job.JobInformation;
import org.jppf.server.JPPFStats;
import org.jppf.server.job.management.*;
import org.jppf.server.scheduler.bundle.LoadBalancingInformation;

/**
 * Node-specific connection wrapper, implementing a user-friendly interface for the monitoring
 * and management of the node. Note that this class implements all the methods in the interface
 * {@link org.jppf.server.job.management.DriverJobManagementMBean DriverJobManagementMBean}, without implementing the interface itself.
 * @author Laurent Cohen
 */
public class JMXDriverConnectionWrapper extends JMXConnectionWrapper implements JPPFDriverAdminMBean
{
	/**
	 * Signature of the method invoked on the MBean.
	 */
	public static final String[] MBEAN_SIGNATURE = new String[] {Map.class.getName()};

	/**
	 * Initialize a local connection to the MBean server.
	 */
	public JMXDriverConnectionWrapper()
	{
		local = true;
	}

	/**
	 * Initialize the connection to the remote MBean server.
	 * @param host the host the server is running on.
	 * @param port the RMI port used by the server.
	 */
	public JMXDriverConnectionWrapper(final String host, final int port)
	{
		super(host, port, JPPFAdminMBean.DRIVER_SUFFIX);
		local = false;
	}

	/**
	 * Request the JMX connection information for all the nodes attached to the server.
	 * @return a collection of <code>NodeManagementInfo</code> instances.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#nodesInformation()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Collection<JPPFManagementInfo> nodesInformation() throws Exception
	{
		return (Collection<JPPFManagementInfo>) invoke(DRIVER_MBEAN_NAME, "nodesInformation", (Object[]) null, (String[]) null);
	}

	/**
	 * Get the latest statistics snapshot from the JPPF driver.
	 * @return a <code>JPPFStats</code> instance.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#statistics()
	 */
	@Override
	public JPPFStats statistics() throws Exception
	{
		return (JPPFStats) invoke(DRIVER_MBEAN_NAME, "statistics", (Object[]) null, (String[]) null);
	}

	/**
	 * Perform a shutdown or restart of the server.
	 * @param shutdownDelay the delay before shutting down the server, once the command is received.
	 * @param restartDelay the delay before restarting, once the server is shutdown. If it is < 0, no restart occurs.
	 * @return an acknowledgement message.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#restartShutdown(java.lang.Long, java.lang.Long)
	 */
	@Override
	public String restartShutdown(final Long shutdownDelay, final Long restartDelay) throws Exception
	{
		return (String) invoke(DRIVER_MBEAN_NAME, "restartShutdown",
				new Object[] {shutdownDelay, restartDelay}, new String[] {Long.class.getName(), Long.class.getName()});
	}

	/**
	 * Change the bundle size tuning settings.
	 * @param algorithm the name opf the load-balancing algorithm to set.
	 * @param parameters the algorithm's parameters.
	 * @return an acknowledgement or error message.
	 * @throws Exception if an error occurred while updating the settings.
	 * @see org.jppf.management.JPPFDriverAdminMBean#changeLoadBalancerSettings(java.lang.String, java.util.Map)
	 */
	@Override
	public String changeLoadBalancerSettings(final String algorithm, final Map parameters) throws Exception
	{
		return (String) invoke(DRIVER_MBEAN_NAME, "changeLoadBalancerSettings",
				new Object[] {algorithm, parameters}, new String[] {String.class.getName(), Map.class.getName()});
	}

	/**
	 * Obtain the current load-balancing settings.
	 * @return an instance of <code>LoadBalancingInformation</code>.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JPPFDriverAdminMBean#loadBalancerInformation()
	 */
	@Override
	public LoadBalancingInformation loadBalancerInformation() throws Exception
	{
		return (LoadBalancingInformation) invoke(DRIVER_MBEAN_NAME, "loadBalancerInformation", (Object[]) null, (String[]) null);
	}

	/**
	 * Cancel the job with the specified id.
	 * @param jobId the id of the job to cancel.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#cancelJob(java.lang.String)
	 */
	public void cancelJob(final String jobId) throws Exception
	{
		invoke(DriverJobManagementMBean.MBEAN_NAME, "cancelJob", new Object[] { jobId }, new String[] { "java.lang.String" });
	}

	/**
	 * Suspend the job with the specified id.
	 * @param jobId the id of the job to suspend.
	 * @param requeue true if the sub-jobs running on each node should be canceled and requeued,
	 * false if they should be left to execute until completion.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#suspendJob(java.lang.String,java.lang.Boolean)
	 */
	public void suspendJob(final String jobId, final Boolean requeue) throws Exception
	{
		invoke(DriverJobManagementMBean.MBEAN_NAME, "suspendJob", new Object[] { jobId, requeue }, new String[] { "java.lang.String", "java.lang.Boolean" });
	}

	/**
	 * Resume the job with the specified id.
	 * @param jobId the id of the job to resume.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#resumeJob(java.lang.String)
	 */
	public void resumeJob(final String jobId) throws Exception
	{
		invoke(DriverJobManagementMBean.MBEAN_NAME, "resumeJob", new Object[] { jobId }, new String[] { "java.lang.String" });
	}

	/**
	 * Update the maximum number of nodes a node can run on.
	 * @param jobId the id of the job to update.
	 * @param maxNodes the new maximum number of nodes for the job.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#updateMaxNodes(java.lang.String, java.lang.Integer)
	 */
	public void updateMaxNodes(final String jobId, final Integer maxNodes) throws Exception
	{
		invoke(DriverJobManagementMBean.MBEAN_NAME, "updateMaxNodes", new Object[] { jobId, maxNodes }, new String[] { "java.lang.String", "java.lang.Integer" });
	}

	/**
	 * Update the priority of a job.
	 * @param jobId the id of the job to update.
	 * @param newPriority the new priority of the job.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#updateMaxNodes(java.lang.String, java.lang.Integer)
	 */
	public void updateJobPriority(final String jobId, final Integer newPriority) throws Exception
	{
		invoke(DriverJobManagementMBean.MBEAN_NAME, "updatePriority", new Object[] { jobId, newPriority }, new String[] { "java.lang.String", "java.lang.Integer" });
	}

	/**
	 * Get the set of ids for all the jobs currently queued or executing.
	 * @return an array of ids as strings.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#getAllJobIds()
	 */
	public String[] getAllJobIds() throws Exception
	{
		//return (String[]) invoke(DriverJobManagementMBean.MBEAN_NAME, "getAllJobIds", (Object[]) null, (String[]) null);
		return (String[]) getAttribute(DriverJobManagementMBean.MBEAN_NAME, "AllJobIds");
	}

	/**
	 * Get an object describing the job with the specified id.
	 * @param jobId the id of the job to get information about.
	 * @return an instance of <code>JobInformation</code>.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#getJobInformation(java.lang.String)
	 */
	public JobInformation getJobInformation(final String jobId) throws Exception
	{
		return (JobInformation) invoke(DriverJobManagementMBean.MBEAN_NAME, "getJobInformation", new Object[] { jobId }, new String[] { "java.lang.String" });
	}

	/**
	 * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
	 * @param jobId the id of the job for which to find node information.
	 * @return an array of <code>NodeManagementInfo</code>, <code>JobInformation</code> instances.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#getNodeInformation(java.lang.String)
	 */
	public NodeJobInformation[] getNodeInformation(final String jobId) throws Exception
	{
		return (NodeJobInformation[]) invoke(DriverJobManagementMBean.MBEAN_NAME, "getNodeInformation", new Object[] { jobId }, new String[] { "java.lang.String" });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void resetStatistics() throws Exception
	{
		invoke(DRIVER_MBEAN_NAME, "resetStatistics", (Object[]) null, (String[]) null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPPFSystemInformation systemInformation() throws Exception
	{
		return (JPPFSystemInformation) invoke(DRIVER_MBEAN_NAME, "systemInformation", (Object[]) null, (String[]) null);
	}
}
