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

package org.jppf.server.scheduler.bundle.impl;

import org.apache.commons.logging.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.scheduler.bundle.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeProcessingThreadsBundler extends AbstractBundler implements Bundler, NodeAwareness
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(NodeProcessingThreadsBundler.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * The bundle size provided by this bundler.
	 */
	private int size = 1;
	/**
	 * The node's configuration.
	 */
	private JPPFSystemInformation nodeConfiguration = null;

	/**
	 * Creates a new instance with the specified parameters profile.
	 * @param profile the parameters of the load-balancing algorithm,
	 */
	public NodeProcessingThreadsBundler(LoadBalancingProfile profile)
	{
		super(profile);
	}

	/**
	 * Make a copy of this bundler.
	 * @return a new <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	public Bundler copy()
	{
		return new NodeProcessingThreadsBundler(profile != null ? profile.copy() : null);
	}

	/**
	 * Feedback the bundler with the result of using the bundle with the specified size.
	 * @param nbTasks number of tasks that were executed.
	 * @see org.jppf.server.scheduler.bundle.Bundler#feedback(int, double)
	 * @param totalTime the total execution and transport time.
	 */
	public void feedback(int nbTasks, double totalTime)
	{
	}

	/**
	 * Get the current size of bundle.
	 * @return  the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize()
	{
		return size;
	}

	/**
	 * Get the corresponding node's system information.
	 * @return a {@link org.jppf.management.JPPFSystemInformation JPPFSystemInformation} instance.
	 * @see org.jppf.server.scheduler.bundle.NodeAwareness#getNodeConfiguration()
	 */
	public JPPFSystemInformation getNodeConfiguration()
	{
		return nodeConfiguration;
	}

	/**
	 * Set the corresponding node's system information.
	 * @param nodeConfiguration a {@link org.jppf.management.JPPFSystemInformation JPPFSystemInformation} instance.
	 * @see org.jppf.server.scheduler.bundle.NodeAwareness#setNodeConfiguration(org.jppf.management.JPPFSystemInformation)
	 */
	public void setNodeConfiguration(JPPFSystemInformation nodeConfiguration)
	{
		this.nodeConfiguration = nodeConfiguration;
		int n = nodeConfiguration.getJppf().getInt("processing.threads", 0);
		if (n <= 0) n = nodeConfiguration.getRuntime().getInt("availableProcessors", 1);
		size = n;
		if (debugEnabled) log.debug("bundle size set to " + size);
	}

	/**
	 * Get the max bundle size that can be used for this bundler.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
	 */
	protected int maxSize()
	{
		return 0;
	}
}
