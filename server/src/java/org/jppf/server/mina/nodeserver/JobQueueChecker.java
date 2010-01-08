/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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
package org.jppf.server.mina.nodeserver;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.*;
import org.apache.mina.core.session.IoSession;
import org.jppf.io.BundleWrapper;
import org.jppf.management.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.ChannelBundlePair;
import org.jppf.server.mina.MinaContext;
import org.jppf.server.nio.nodeserver.NodeTransition;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.AbstractJPPFQueue;
import org.jppf.utils.ThreadSynchronization;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 */
public class JobQueueChecker extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JobQueueChecker.class);
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
	private MinaNodeServer server = null;
	/**
	 * Determines whether this task is currently executing.
	 */
	private AtomicBoolean executing = new AtomicBoolean(false);
	/**
	 * 
	 */
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	
	/**
	 * Initialize this task queue checker with the specified node server. 
	 * @param server the owner of this queue checker.
	 */
	public JobQueueChecker(MinaNodeServer server)
	{
		this.server = server;
	}

	/**
	 * Perform the assignment of tasks.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		List<IoSession> idleChannels = server.getIdleChannels();
		long sleepMillis = 0L;
		int sleepNanos = 100000;
		while (!isStopped())
		{
			while (idleChannels.isEmpty() || server.getQueue().isEmpty()) goToSleep(sleepMillis, sleepNanos);
			synchronized(idleChannels)
			{
				if (idleChannels.isEmpty() || server.getQueue().isEmpty()) continue;
				if (debugEnabled) log.debug(""+idleChannels.size()+" channels idle");
				List<IoSession> channelList = new ArrayList<IoSession>();
				channelList.addAll(idleChannels);
				boolean found = false;
				IoSession channel = null;
				BundleWrapper selectedBundle = null;
				AbstractJPPFQueue queue = (AbstractJPPFQueue) server.getQueue();
				queue.getLock().lock();
				try
				{
					Iterator<BundleWrapper> it = queue.iterator();
					while (!found && it.hasNext() && !idleChannels.isEmpty())
					{
						BundleWrapper bundleWrapper = it.next();
						JPPFTaskBundle bundle = bundleWrapper.getBundle();
						if (!checkJobState(bundle)) continue;
						int n = findIdleChannelIndex(bundle);
						if (n >= 0)
						{
							//channel = idleChannels.remove(n);
							channel = server.removeIdleChannel(n);
							selectedBundle = bundleWrapper;
							found = true;
						}
					}
					try
					{
						if (debugEnabled) log.debug((channel == null ? "no channel found for bundle" : "found channel for bundle") + " id=" + getJobId(selectedBundle.getBundle()));
						if (channel != null)
						{
							NodeContext context = (NodeContext) channel.getAttribute(MinaContext.SESSION_CONTEXT_KEY);
							BundleWrapper bundleWrapper = server.getQueue().nextBundle(selectedBundle, context.getBundler().getBundleSize());
							context.setBundle(bundleWrapper);
							server.transitionSession(channel, NodeTransition.TO_SENDING);
							JPPFDriver.getInstance().getJobManager().jobDispatched(context.getBundle(), new IoSessionWrapper(channel));
							NodeServerState state = server.factory.getState(context.getState());
							//channel.setAttribute("transitionStarted", state.startTransition(channel));
							executor.submit(new ChannelTransitionTask(channel, state));
						}
					}
					catch(Exception e)
					{
						log.error(e.getMessage(), e);
					}
				}
				finally
				{
					queue.getLock().unlock();
				}
				if (channel == null) goToSleep(sleepMillis);
			}
		}
	}

	/**
	 * Find a channel that can send the specified task bundle for execution.
	 * @param bundle the bundle to execute.
	 * @return the index of an available and acceptable channel, or -1 if no channel could be found.
	 */
	private int findIdleChannelIndex(JPPFTaskBundle bundle)
	{
		List<IoSession> idleChannels = server.getIdleChannels();
		int n = -1;
		ExecutionPolicy rule = bundle.getJobSLA().getExecutionPolicy();
		//if (debugEnabled && (rule != null)) log.debug("Bundle has an execution policy:\n" + rule);
		List<Integer> acceptableChannels = new ArrayList<Integer>();
		List<Integer> channelsToRemove =  new ArrayList<Integer>();
		List<String> uuidPath = bundle.getUuidPath().getList();
		for (int i=0; i<idleChannels.size(); i++)
		{
			IoSession ch = idleChannels.get(i);
			if (!ch.isConnected())
			{
				channelsToRemove.add(i);
				continue;
			}
			NodeContext context = (NodeContext) ch.getAttribute(MinaContext.SESSION_CONTEXT_KEY);
			if (uuidPath.contains(context.getNodeUuid())) continue;
			if (rule != null)
			{
				JPPFManagementInfo mgtInfo = JPPFDriver.getInstance().getNodeInformation(new IoSessionWrapper(ch));
				JPPFSystemInformation info = (mgtInfo == null) ? null : mgtInfo.getSystemInfo();
				if (!rule.accepts(info)) continue;
			}
			acceptableChannels.add(i);
		}
		for (Integer i: channelsToRemove) idleChannels.remove(i);
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
		if (sla.isSuspended()) return false;
		Boolean b = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING);
		if ((b != null) && b) return false;
		String jobId = getJobId(bundle);
		int maxNodes = sla.getMaxNodes();
		List<ChannelBundlePair> list = server.getJobManager().getNodesForJob(jobId);
		int n = (list == null) ? 0 : list.size();
		if (debugEnabled) log.debug("jobId = " + jobId + ", current nodes = " + n + ", maxNodes = " + maxNodes);
		return n < maxNodes;
	}

	/**
	 * Get the id of the specified job.
	 * @param bundle the job to get the id from.
	 * @return the id as a string.
	 */
	private String getJobId(JPPFTaskBundle bundle)
	{
		return (String) bundle.getParameter(BundleParameter.JOB_ID);
	}

	/**
	 * 
	 */
	private class ChannelTransitionTask implements Runnable
	{
		/**
		 * The state to transition to.
		 */
		private NodeServerState state = null;
		/**
		 * The session that is transitionned.
		 */
		private IoSession session = null;
	
		/**
		 * Initialize this task.
		 * @param session the state to transition to.
		 * @param state the session that is transitionned.
		 */
		public ChannelTransitionTask(IoSession session, NodeServerState state)
		{
			this.session = session;
			this.state = state;
		}

		/**
		 * Perform the transition
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				session.setAttribute("transitionStarted", state.startTransition(session));
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}
}
