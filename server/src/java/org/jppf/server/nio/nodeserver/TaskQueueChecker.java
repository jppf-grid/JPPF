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
package org.jppf.server.nio.nodeserver;

import java.util.*;
import java.util.concurrent.locks.Lock;

import org.jppf.management.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.ChannelJobPair;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.AbstractJPPFQueue;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.fixedsize.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 */
class TaskQueueChecker extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(TaskQueueChecker.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Random number generator used to randomize the choice of idle channel.
	 */
	private Random random = new Random(System.currentTimeMillis());
	/**
	 * The owner of this queue checker.
	 */
	private final NodeNioServer server;
	/**
	 * Reference to the job queue.
	 */
	private final AbstractJPPFQueue queue;
	/**
	 * Lock on the job queue.
	 */
	private final Lock queueLock;
	/**
	 * The list of idle node channels.
	 */
	private final List<ChannelWrapper<?>> idleChannels;
	/**
	 * Reference to the driver.
	 */
	private final JPPFDriver driver = JPPFDriver.getInstance();
	/**
	 * Determines whether this instance is already running its run() method.
	 */
	private boolean running = false;

	/**
	 * Initialize this task queue checker with the specified node server. 
	 * @param server the owner of this queue checker.
	 */
	public TaskQueueChecker(NodeNioServer server)
	{
		this.server = server;
		this.idleChannels = server.getIdleChannels();
		this.queue = (AbstractJPPFQueue) server.getQueue();
		this.queueLock = queue.getLock();
	}

	/**
	 * Perform the assignment of tasks.
	 * @see java.lang.Runnable#run()
	 */
	@Override
    public void run()
	{
		while (!isStopped())
		{
			if (!dispatch()) goToSleep(1000L, 10000);
		}
	}

	/**
	 * Perform the assignment of tasks.
	 * @return true if a job was dispatched, false otherwise.
	 * @see java.lang.Runnable#run()
	 */
	public boolean dispatch()
	{
		boolean dispatched = false;
		try
		{
			synchronized(idleChannels)
			{
				if (idleChannels.isEmpty() || server.getQueue().isEmpty()) return false;
				if (debugEnabled) log.debug(""+idleChannels.size()+" channels idle");
				ChannelWrapper<?> channel = null;
				BundleWrapper selectedBundle = null;
				queueLock.lock();
				try
				{
					Iterator<BundleWrapper> it = queue.iterator();
					while ((channel == null) && it.hasNext() && !idleChannels.isEmpty())
					{
						BundleWrapper bundleWrapper = it.next();
						channel = retrieveChannel(bundleWrapper);
						if (channel != null) selectedBundle = bundleWrapper;
					}
					if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle" : "channel found for bundle: " + channel);
					if (channel != null)
					{
						dispatchJobToChannel(channel, selectedBundle);
						dispatched = true;
					}
				}
				catch(Exception ex)
				{
					log.error("An error occurred while attempting to dispatch task bundles. This is most likely due to an error in the load balancer implementation.", ex);
				}
				finally
				{
					queueLock.unlock();
				}
			}
		}
		catch (Exception ex)
		{
			log.error("An error occured while preparing for bundle creation and dispatching.", ex);
		}
		return dispatched;
	}

	/**
	 * Retrieve a suitable channel for the specirfied job.
	 * @param bundleWrapper the job to execute.
	 * @return a channel for a node on which to execute the job.
	 * @throws Exception if any error occurs.
	 */
	private ChannelWrapper<?> retrieveChannel(ServerJob bundleWrapper) throws Exception
	{
		ChannelWrapper<?> channel = null;
		JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
		if (checkJobState(bundle))
		{
			int n = findIdleChannelIndex(bundle);
			if (n >= 0) channel = server.removeIdleChannel(n);
		}
		return channel;
	}

	/**
	 * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
	 * @param channel the node channel to dispatch the job to.
	 * @param selectedBundle the job to dispatch.
	 */
	private void dispatchJobToChannel(ChannelWrapper<?> channel, BundleWrapper selectedBundle)
	{
		if (debugEnabled) log.debug("dispatching jobUuid=" + selectedBundle.getJob().getJobUuid() + " to nodeUuid=" + ((AbstractNodeContext) channel.getContext()).nodeUuid);
		synchronized(channel)
		{
			AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
			int size = 1;
			try
			{
				updateBundler(server.getBundler(), (JPPFTaskBundle) selectedBundle.getJob(), context);
				size = context.getBundler().getBundleSize();
			}
			catch (Exception e)
			{
				log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1", e);
				FixedSizeProfile profile = new FixedSizeProfile();
				profile.setSize(1);
				server.setBundler(new FixedSizeBundler(profile));
			}
			BundleWrapper bundleWrapper = server.getQueue().nextBundle(selectedBundle, size);
			context.setBundle(bundleWrapper);
			server.getTransitionManager().transitionChannel(channel, NodeTransition.TO_SENDING);
			driver.getJobManager().jobDispatched(context.getBundle(), channel);
		}
	}

	/**
	 * Find a channel that can send the specified task bundle for execution.
	 * @param bundle the bundle to execute.
	 * @return the index of an available and acceptable channel, or -1 if no channel could be found.
	 */
	private int findIdleChannelIndex(JPPFTaskBundle bundle)
	{
		List<ChannelWrapper<?>> idleChannels = server.getIdleChannels();
		int n = -1;
		ExecutionPolicy rule = bundle.getJobSLA().getExecutionPolicy();
		if (debugEnabled && (rule != null)) log.debug("Bundle " + bundle + " has an execution policy:\n" + rule);
		List<Integer> acceptableChannels = new ArrayList<Integer>();
		List<Integer> channelsToRemove =  new ArrayList<Integer>();
		List<String> uuidPath = bundle.getUuidPath().getList();
		for (int i=0; i<idleChannels.size(); i++)
		{
			ChannelWrapper<?> ch = idleChannels.get(i);
			if (!ch.isOpen())
			{
				channelsToRemove.add(i);
				if (debugEnabled) log.debug("channel is not opened: " + ch);
				continue;
			}
			AbstractNodeContext context = (AbstractNodeContext) ch.getContext();
			if (uuidPath.contains(context.getNodeUuid()))
			{
				if (log.isTraceEnabled())
				{
					log.trace("bundle uuid path already contains node " + ch + " : uuidPath=" + uuidPath + ", nodeUuid=" + context.getNodeUuid());
				}
				continue;
			}
			if (rule != null)
			{
				JPPFManagementInfo mgtInfo = driver.getNodeHandler().getNodeInformation(ch);
				JPPFSystemInformation info = (mgtInfo == null) ? null : mgtInfo.getSystemInfo();
				boolean b = false;
				try
				{
					b = rule.accepts(info);
				}
				catch(Exception ex)
				{
					log.error("An error occurred while running the execution policy to determine node participation.",ex);
				}
				if (debugEnabled) log.debug("rule execution is *" + b + "* for jobUuid=" + bundle.getJobUuid() + ", nodeUuid=" + mgtInfo.getId());
				if (!b) continue;
			}
			acceptableChannels.add(i);
		}
		for (Integer i: channelsToRemove) server.removeIdleChannel(i);
		if (debugEnabled) log.debug("found " + acceptableChannels.size() + " acceptable channels");
		if (!acceptableChannels.isEmpty())
		{
			int rnd = random.nextInt(acceptableChannels.size());
			n = acceptableChannels.remove(rnd);
		}
		return n;
	}

	/**
	 * Check if the job state allows it to be dispatched on another node.
	 * There are two cases when this method will return false: when the job is suspended and
	 * when the job is already executing on its maximum allowed number of nodes.
	 * @param bundle the bundle from which to get the job information.
	 * @return true if the job can be dispatched to at least one more node, false otherwise.
	 */
	private boolean checkJobState(JPPFTaskBundle bundle)
	{
		JPPFJobSLA sla = bundle.getJobSLA();
		if (debugEnabled)
		{
			String s = StringUtils.buildString("job '", bundle.getId(), "' : ",
				"suspended=", sla.isSuspended(), ", pending=", bundle.getParameter(BundleParameter.JOB_PENDING, Boolean.FALSE),
				", expired=", bundle.getParameter(BundleParameter.JOB_EXPIRED, Boolean.FALSE));
			log.debug(s);
		}
		if (sla.isSuspended()) return false;
		boolean b = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING, Boolean.FALSE);
		if (b) return false;
		b = (Boolean) bundle.getParameter(BundleParameter.JOB_EXPIRED, Boolean.FALSE);
		if (b) return false;
		String jobId = bundle.getJobUuid();
		int maxNodes = sla.getMaxNodes();
		List<ChannelJobPair> list = server.getJobManager().getNodesForJob(jobId);
		int n = list.size();
		if (debugEnabled) log.debug("current nodes = " + n + ", maxNodes = " + maxNodes);
		return n < maxNodes;
	}

	/**
	 * Perform the checks on the bundler before submitting a job.
	 * @param bundler the bundler to check and update.
	 * @param taskBundle the job.
	 * @param context the current node context.
	 */
	private void updateBundler(Bundler bundler, JPPFTaskBundle taskBundle, AbstractNodeContext context)
	{
		context.checkBundler(server.getBundler());
		if (context.getBundler() instanceof JobAwareness)
		{
			JPPFJobMetadata metadata = taskBundle.getJobMetadata();
			((JobAwareness) context.getBundler()).setJobMetadata(metadata);
		}
	}

	/**
	 * Determine whether this instance is already running its run() method.
	 * @return true if this instance is running, false otherwise.
	 */
	public synchronized boolean isRunning()
	{
		return running;
	}

	/**
	 * Specifiy whether this instance is already running its run() method.
	 * @param running true if this instance is running, false otherwise.
	 */
	public synchronized void setRunning(boolean running)
	{
		this.running = running;
	}
}
