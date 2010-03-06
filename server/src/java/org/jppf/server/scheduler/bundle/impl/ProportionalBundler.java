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

package org.jppf.server.scheduler.bundle.impl;

import org.apache.commons.logging.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.proportional.*;

/**
 * This bundler implementation computes bundle sizes propertional to the mean execution
 * time for each node to the power of n, where n is an integer value specified in the configuration file as "proportionality factor".<br>
 * The scope of this bundler is all nodes, which means that it computes the size for all nodes,
 * unless an override is specified by the nodes.<br>
 * The mean execution time is computed as a moving average over a number of tasks, specified in the bundling
 * algorithm profile configuration as &quot;minSamplesToAnalyse&quot;<br>
 * This algorithm is well suited for relatively small networks (a few dozen nodes at most). It generates an overhead
 * everytime the performance data for a node is updated. In the case of a small network, this overhead is not
 * large enough to impact the overall performance significantly.
 * @author Laurent Cohen
 */
public class ProportionalBundler extends AbstractProportionalBundler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ProportionalBundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
	 */
	public ProportionalBundler(LoadBalancingProfile profile)
	{
		super(profile);
	}

	/**
	 * Make a copy of this bundler
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	public Bundler copy()
	{
		return new ProportionalBundler((ProportionalTuneProfile) profile);
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
}
