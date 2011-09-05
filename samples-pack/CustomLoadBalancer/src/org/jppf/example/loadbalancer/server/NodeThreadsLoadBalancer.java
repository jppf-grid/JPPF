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

package org.jppf.example.loadbalancer.server;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.JPPFDriver;
import org.jppf.server.scheduler.bundle.*;
import org.slf4j.*;

/**
 * This implementation of a load-balancing algorithm illustrates the use of
 * the {@link NodeAwareness} and {@link JobAwereness} APIs, allowing the algorithm to work
 * based on known information about the nodes and the jobs.
 * <p>In this implementation, we assume each job provides the following metadata:
 * <ul>
 * <li>"task.memory": the maximum memory footprint of each task in bytes</li> 
 * <li>"task.time": the maximum duration of a task in milliseconds</li> 
 * <li>"allowed.time": the maximum allowed time for execution of a single set of tasks on a node, in milliseconds</li>
 * <li>"id": the id of the current job being executed, used for debugging and logging</li>
 * </ul>
 * @author Laurent Cohen
 */
public class NodeThreadsLoadBalancer extends AbstractBundler implements NodeAwareness
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(NodeThreadsLoadBalancer.class);
	/**
	 * Holds information about the node's environment and configuration.
	 */
	private JPPFSystemInformation nodeConfiguration = null;
	/**
	 * The current number of tasks to send to the node.
	 */
	private int bundleSize = 1;

	/**
	 * Creates a new instance with the specified parameters profile.
	 * @param profile the parameters of the load-balancing algorithm.
	 */
	public NodeThreadsLoadBalancer(LoadBalancingProfile profile)
	{
		super(profile);
		if (log.isDebugEnabled()) log.debug("creating CustomLoadBalancer #" + this.bundlerNumber);
	}

	/**
	 * Make a copy of this bundler.
	 * Wich parts are actually copied depends on the implementation.
	 * @return a new <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	public Bundler copy()
	{
		return new NodeThreadsLoadBalancer(null);
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
		computeBundleSize();
		if (log.isDebugEnabled()) log.debug("setting node configuration on bundler #" + bundlerNumber + ": " + nodeConfiguration);
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
	 * Compute the number of tasks to send to the node. This is the actual algorithm implementation.
	 */
	private void computeBundleSize()
	{
		if (log.isDebugEnabled()) log.debug("computing bundle size for bundler #" + this.bundlerNumber);
		JPPFSystemInformation nodeConfig = getNodeConfiguration();
		if (nodeConfig == null) bundleSize = 1;
		else
		{
			// get the number of processing threads in the node 
			int nbThreads = getNodeConfiguration().getJppf().getInt("processing.threads", -1);
			// if number of threads is not defined, we assume it is the number of available processors
			if (nbThreads <= 0) nbThreads = getNodeConfiguration().getRuntime().getInt("availableProcessors");
			bundleSize = nbThreads <= 0 ? 1 : nbThreads;
		}
		// log the new bundle size
		if (log.isDebugEnabled()) log.debug("bundler #" + this.bundlerNumber + " computed new bundle size = " + bundleSize);
	}

	/**
	 * Release the resources used by this bundler.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#dispose()
	 */
	public void dispose()
	{
		if (log.isDebugEnabled()) log.debug("disposing bundler #" + this.bundlerNumber);
		this.nodeConfiguration = null;
	}
}
