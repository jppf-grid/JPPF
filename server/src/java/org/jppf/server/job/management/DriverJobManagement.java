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

package org.jppf.server.job.management;

import java.util.*;

import javax.management.NotificationBroadcasterSupport;

import org.apache.commons.logging.*;
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.*;
import org.jppf.server.protocol.*;

/**
 * Implementation of the job management bean.
 * @author Laurent Cohen
 */
public class DriverJobManagement extends NotificationBroadcasterSupport implements DriverJobManagementMBean
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(DriverJobManagement.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Reference to the driver's job manager.
	 */
	private JPPFJobManager jobManager = null;
	/**
	 * Reference to the driver.
	 */
	private JPPFDriver driver = JPPFDriver.getInstance();

	/**
	 * Initialize this MBean.
	 */
	public DriverJobManagement()
	{
		getJobManager().addJobListener(new JobEventNotifier());
	}

	/**
	 * Cancel the job with the specified id.
	 * @param jobUuid the id of the job to cancel.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#cancelJob(java.lang.String)
	 */
	public void cancelJob(String jobUuid) throws Exception
	{
		BundleWrapper bundleWrapper = getJobManager().getBundleForJob(jobUuid);
		if (debugEnabled) log.debug("Request to cancel jobId = '" + bundleWrapper.getBundle().getId() + "'");
		if (debugEnabled) log.debug("bundleWrapper=" + bundleWrapper);
		if (bundleWrapper != null)
		{
			JPPFTaskBundle bundle = bundleWrapper.getBundle();
			BundleWrapper queuedWrapper = JPPFDriver.getQueue().nextBundle(bundleWrapper, bundle.getTaskCount());
			//if ((queuedWrapper != null) && (bundle.getCompletionListener() != null)) bundle.getCompletionListener().taskCompleted(bundleWrapper);
		}
		cancelJobInNodes(jobUuid, false);
	}

	/**
	 * Suspend the job with the specified id.
	 * @param jobUuid the id of the job to suspend.
	 * @param requeue true if the sub-jobs running on each node should be canceled and requeued,
	 * false if they should be left to execute until completion.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#suspendJob(java.lang.String,java.lang.Boolean)
	 */
	public void suspendJob(String jobUuid, Boolean requeue) throws Exception
	{
		BundleWrapper bundleWrapper = getJobManager().getBundleForJob(jobUuid);
		if (debugEnabled) log.debug("Request to suspend jobId = '" + bundleWrapper.getBundle().getId() + "'");
		if (bundleWrapper == null) return;
		if (bundleWrapper.getBundle().getJobSLA().isSuspended()) return;
		bundleWrapper.getBundle().getJobSLA().setSuspended(true);
		getJobManager().jobUpdated(bundleWrapper);
		if (requeue) cancelJobInNodes(jobUuid, true);
	}

	/**
	 * Resume the job with the specified id.
	 * @param jobUuid the id of the job to resume.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#resumeJob(java.lang.String)
	 */
	public void resumeJob(String jobUuid) throws Exception
	{
		BundleWrapper bundleWrapper = getJobManager().getBundleForJob(jobUuid);
		if (debugEnabled) log.debug("Request to resume jobId = '" + bundleWrapper.getBundle().getId() + "'");
		if (bundleWrapper == null) return;
		if (!bundleWrapper.getBundle().getJobSLA().isSuspended()) return;
		bundleWrapper.getBundle().getJobSLA().setSuspended(false);
		getJobManager().jobUpdated(bundleWrapper);
	}

	/**
	 * Update the maximum number of nodes a node can run on.
	 * @param jobUuid the id of the job to update.
	 * @param maxNodes the new maximum number of nodes for the job.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#updateMaxNodes(java.lang.String, java.lang.Integer)
	 */
	public void updateMaxNodes(String jobUuid, Integer maxNodes) throws Exception
	{
		BundleWrapper bundleWrapper = getJobManager().getBundleForJob(jobUuid);
		if (debugEnabled) log.debug("Request to update maxNodes to " + maxNodes + " for jobId = '" + bundleWrapper.getBundle().getId() + "'");
		if (bundleWrapper == null) return;
		if (maxNodes <= 0) return;
		bundleWrapper.getBundle().getJobSLA().setMaxNodes(maxNodes);
		getJobManager().jobUpdated(bundleWrapper);
	}

	/**
	 * Get the set of ids for all the jobs currently queued or executing.
	 * @return a set of ids as strings.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#getAllJobIds()
	 */
	public String[] getAllJobIds() throws Exception
	{
		Set<String> set = getJobManager().getAllJobIds();
		return set.toArray(new String[0]);
	}

	/**
	 * Get an object describing the job with the specified id. 
	 * @param jobUuid the id of the job to get information about.
	 * @return an instance of <code>JobInformation</code>.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#getJobInformation(java.lang.String)
	 */
	public JobInformation getJobInformation(String jobUuid) throws Exception
	{
		BundleWrapper bundleWrapper = getJobManager().getBundleForJob(jobUuid);
		if (bundleWrapper == null) return null;
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		Boolean pending = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING);
		JobInformation job = new JobInformation(jobUuid, bundle.getId(),
			bundle.getTaskCount(), bundle.getInitialTaskCount(), bundle.getJobSLA().getPriority(),
			bundle.getJobSLA().isSuspended(), (pending != null) && pending);
		job.setMaxNodes(bundle.getJobSLA().getMaxNodes());
		return job;
	}

	/**
	 * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
	 * @param jobUuid the id of the job for which to find node information.
	 * @return a list of <code>NodeManagementInfo</code> instances.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.job.management.DriverJobManagementMBean#getNodeInformation(java.lang.String)
	 */
	public NodeJobInformation[] getNodeInformation(String jobUuid) throws Exception
	{
		List<ChannelBundlePair> nodes = getJobManager().getNodesForJob(jobUuid);
		if (nodes == null) return null;
		NodeJobInformation[] result = new NodeJobInformation[nodes.size()];
		for (int i=0; i<nodes.size(); i++)
		{
			JPPFManagementInfo nodeInfo = driver.getNodeInformation(nodes.get(i).first());
			JPPFTaskBundle bundle = nodes.get(i).second().getBundle();
			Boolean pending = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING);
			JobInformation jobInfo = new JobInformation(jobUuid, bundle.getId(),
				bundle.getTaskCount(), bundle.getInitialTaskCount(), bundle.getJobSLA().getPriority(),
				bundle.getJobSLA().isSuspended(), (pending != null) && pending);
			jobInfo.setMaxNodes(bundle.getJobSLA().getMaxNodes());
			result[i] = new NodeJobInformation(nodeInfo, jobInfo);
		}
		return result;
	}

	/**
	 * Cancel all sub-jobs of the job with the specified id, by issuing a cancel command
	 * to each corresponding node. 
	 * @param jobUuid the id of the job to cancel.
	 * @param requeue specifies whether the sub-jobs should be requeued.
	 */
	private void cancelJobInNodes(String jobUuid, boolean requeue)
	{
		List<ChannelBundlePair> list = getJobManager().getNodesForJob(jobUuid);
		if (debugEnabled) log.debug("Cancelling jobId = '" + jobUuid + "' in nodes: " + list);
		if (list == null) return;
		for (ChannelBundlePair pair: list)
		{
			CancelJobTask task = new CancelJobTask(jobUuid, pair.first(), requeue);
			new Thread(task).start();
		}
	}

	/**
	 * Get a reference to the driver's job manager.
	 * @return a <code>JPPFJobManager</code> instance.
	 */
	private JPPFJobManager getJobManager()
	{
		if (jobManager == null) jobManager = driver.getJobManager();
		return jobManager;
	}

	/**
	 * A job manager listeners that sends a notification through the mbean for each job manager event.
	 */
	private class JobEventNotifier implements JobListener
	{
		/**
		 * Called when a new job is put in the job queue.
		 * @param event encapsulates the information about the event.
		 * @see org.jppf.job.JobListener#jobQueued(org.jppf.job.JobNotification)
		 */
		public void jobQueued(JobNotification event)
		{
			sendNotification(event);
		}

		/**
		 * Called when a job is complete and has been sent back to the client.
		 * @param event - encapsulates the information about the event.
		 * @see org.jppf.job.JobListener#jobEnded(org.jppf.job.JobNotification)
		 */
		public void jobEnded(JobNotification event)
		{
			sendNotification(event);
		}

		/**
		 * Called when the current number of tasks in a job was updated.
		 * @param event - encapsulates the information about the event.
		 * @see org.jppf.job.JobListener#jobUpdated(org.jppf.job.JobNotification)
		 */
		public void jobUpdated(JobNotification event)
		{
			sendNotification(event);
		}

		/**
		 * Called when all or part of a job is is sent to a node for execution.
		 * @param event - encapsulates the information about the event.
		 * @see org.jppf.job.JobListener#jobDispatched(org.jppf.job.JobNotification)
		 */
		public void jobDispatched(JobNotification event)
		{
			sendNotification(event);
		}

		/**
		 * Called when all or part of a job has returned from irs execution on a node.
		 * @param event - encapsulates the information about the event.
		 * @see org.jppf.job.JobListener#jobReturned(org.jppf.job.JobNotification)
		 */
		public void jobReturned(JobNotification event)
		{
			sendNotification(event);
		}
	}
}
