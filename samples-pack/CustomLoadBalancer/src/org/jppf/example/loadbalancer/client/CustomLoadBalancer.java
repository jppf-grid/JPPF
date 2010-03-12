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

package org.jppf.example.loadbalancer.client;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.JPPFJobMetadata;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.TypedProperties;

/**
 * 
 * @author Laurent Cohen
 */
public class CustomLoadBalancer extends AbstractBundler implements NodeAwareness, JobAwareness
{
	/**
	 * Holds information about the node's environment and configuration.
	 */
	private JPPFSystemInformation nodeConfiguration = null;
	/**
	 * Holds metadata about the cuirrent job being dispatched.
	 */
	private JPPFJobMetadata jobMetadata = null;
	/**
	 * The current number of tasks to send to the node.
	 */
	private int bundleSize = 1;

	/**
	 * Creates a new instance with the specified parameters profile.
	 * @param profile the parameters of the load-balancing algorithm,
	 */
	public CustomLoadBalancer(LoadBalancingProfile profile)
	{
		super(profile);
	}

	/**
	 * Make a copy of this bundler.
	 * Wich parts are actually copied depends on the implementation.
	 * @return a new <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	public Bundler copy()
	{
		return new CustomLoadBalancer(null);
	}

	/**
	 * Get the current number of tasks to send to the node.
	 * @return  the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize()
	{
		return bundleSize;
	}

	/**
	 * Get the corresponding node's system information.
	 * @return a {@link JPPFSystemInformation} instance.
	 * @see org.jppf.server.scheduler.bundle.NodeAwareness#getNodeConfiguration()
	 */
	public JPPFSystemInformation getNodeConfiguration()
	{
		return nodeConfiguration;
	}

	/**
	 * Set the corresponding node's system information.
	 * @param nodeConfiguration a {@link JPPFSystemInformation} instance.
	 * @see org.jppf.server.scheduler.bundle.NodeAwareness#setNodeConfiguration(org.jppf.management.JPPFSystemInformation)
	 */
	public void setNodeConfiguration(JPPFSystemInformation nodeConfiguration)
	{
		this.nodeConfiguration = nodeConfiguration;
	}

	/**
	 * Get the max bundle size that can be used for this bundler.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
	 */
	protected int maxSize()
	{
		return JPPFDriver.getQueue() == null ? 300 : JPPFDriver.getQueue().getMaxBundleSize();
	}

	/**
	 * Get the current job's metadata.
	 * @return a {@link JPPFJobMetadata} instance.
	 * @see org.jppf.server.scheduler.bundle.JobAwareness#getJobMetadata()
	 */
	public JPPFJobMetadata getJobMetadata()
	{
		return jobMetadata;
	}

	/**
	 * Set the current job's metadata.
	 * @param metadata a {@link JPPFJobMetadata} instance.
	 * @see org.jppf.server.scheduler.bundle.JobAwareness#setJobMetadata(org.jppf.server.protocol.JPPFJobMetadata)
	 */
	public void setJobMetadata(JPPFJobMetadata metadata)
	{
		this.jobMetadata = metadata;
		// compute the number of tasks to send to the node,
		// based on the new job mmetadata
		computeBundleSize();
	}

	/**
	 * Compute the number of tasks to send to the job.
	 */
	private void computeBundleSize()
	{
		// Get the job metadata in an easy to use format
		TypedProperties props = new TypedProperties(getJobMetadata().getAll());
		// the maximum memory footprint of each task in bytes
		long taskMemory = props.getLong("task.memory", 10000);
		// fetch the length of a task in milliseconds
		long taskTime = props.getLong("task.time", 10);
		// fetch the maximum allowed time for execution of a single set of tasks on a node
		long allowedTime = props.getLong("allowed.time", 10);
		// get the number of processing threads in the node 
		int nbThreads = getNodeConfiguration().getJppf().getInt("processing.threads", -1);
		// if number of threads is not defined, we assume it is the number of available processors
		if (nbThreads <= 0) nbThreads = getNodeConfiguration().getRuntime().getInt("availableProcessors");
		// max node heap size of the node in bytes
		long nodeMemory = getNodeConfiguration().getRuntime().getLong("maxMemory");
		// we assume 20 MB of the node's memory is taken by JPPF code
		nodeMemory -= 20 * 1024 * 1024;
		// max number of tasks that can fit in the node's heap
		int maxTasks = (int) (nodeMemory / taskMemory);
		// the maximum time needed to execute maxTasks tasks on nbThreads parallel threads
		long maxTime = taskTime * maxTasks / nbThreads;
		// if maxTime is not a multiple of nbThreads, make the adjustement
		if ((maxTasks % nbThreads) != 0) maxTime += taskTime;
		// if max time is longer than the allowed time, reduce the number of tasks by the appropriate amount
		if (maxTime > allowedTime)
		{
			maxTasks = (int) ((maxTasks * allowedTime) / maxTime); 
		}
		// finally, store the computation result
		bundleSize = maxTasks;
	}

	/**
	 * Release the resources used by this bundler.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#dispose()
	 */
	public void dispose()
	{
		this.jobMetadata = null;
		this.nodeConfiguration = null;
	}
}
