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

package org.jppf.server.scheduler.bundle.proportional;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.server.scheduler.bundle.*;

/**
 * This bundler implementation computes bundle sizes propertional to the mean execution
 * time for each node to the power of n, where n is an integer value specified in the configuration file as "proportionality factor".<br>
 * The scope of this bundler is all nodes, which means that it computes the size for all nodes,
 * unless an override is specified by the nodes.<br>
 * The mean execution time is computed as a moving average over a number of tasks, specified in the bundling
 * algorithm profile configuration as &quot;performanceCacheSize&quot;<br>
 * This algorithm is well suited for relatively small networks (a few dozen nodes at most). It generates an overhead
 * everytime the performance data for a node is updated. In the case of a small network, this overhead is not
 * large enough to impact the overall performance significantly.
 * @author Laurent Cohen
 */
public abstract class AbstractProportionalBundler extends AbstractBundler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractProportionalBundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Mapping of individual bundler to corresponding performance data.
	 */
	private static Set<AbstractProportionalBundler> bundlers = new HashSet<AbstractProportionalBundler>();
	/**
	 * Bounded memory of the past performance updates.
	 */
	protected BundleDataHolder dataHolder = null;
	/**
	 * The current bundle size.
	 */
	protected int bundleSize = 1;

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the load-balancing algorithm,
	 */
	public AbstractProportionalBundler(LoadBalancingProfile profile)
	{
		super(profile);
		log.info("Bundler#" + bundlerNumber + ": Using Auto-Tuned bundle size");
		int bundleSize = 1;
		if (bundleSize < 1) bundleSize = 1;
		log.info("Bundler#" + bundlerNumber + ": The initial size is " + bundleSize + ", profile: "+profile);
		dataHolder = new BundleDataHolder(((ProportionalTuneProfile) profile).getPerformanceCacheSize());
	}

	/**
	 * Get the current size of bundle.
	 * @return  the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize()
	{
		return bundleSize;
	}

	/**
	 * Set the current size of bundle.
	 * @param size - the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public void setBundleSize(int size)
	{
		bundleSize = (size <= 0) ? 1 : size;
	}

	/**
	 * This method delegates the bundle size calculation to the singleton instance of <code>SimpleBundler</code>.
	 * @param size - the number of tasks executed.
	 * @param time - the time in milliseconds it took to execute the tasks.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#feedback(int, double)
	 */
	public void feedback(int size, double time)
	{
		if (size <= 0) return;
		BundlePerformanceSample sample = new BundlePerformanceSample((double) time / (double) size, size);
		synchronized(bundlers)
		{
			dataHolder.addSample(sample);
			computeBundleSizes();
		}
	}

	/**
	 * Perform context-independant initializations.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#setup()
	 */
	public void setup()
	{
		synchronized(bundlers)
		{
			bundlers.add(this);
		}
	}
	
	/**
	 * Release the resources used by this bundler.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#dispose()
	 */
	public void dispose()
	{
		synchronized(bundlers)
		{
			bundlers.remove(this);
		}
	}

	/**
	 * Get the bounded memory of the past performance updates.
	 * @return a BundleDataHolder instance.
	 */
	public BundleDataHolder getDataHolder()
	{
		return dataHolder;
	}

	/**
	 * Update the bundler sizes.
	 */
	private void computeBundleSizes()
	{
		synchronized(bundlers)
		{
			double maxMean = Double.NEGATIVE_INFINITY;
			double minMean = Double.POSITIVE_INFINITY;
			AbstractProportionalBundler minBundler = null;
			double meanSum = 0d;
			for (AbstractProportionalBundler b: bundlers)
			{
				BundleDataHolder h = b.getDataHolder();
				double m = h.getMean();
				if (m > maxMean) maxMean = m;
				if (m < minMean)
				{
					minMean = m;
					minBundler = b;
				}
			}
			//if (maxMean <= 0) maxMean = 1d;
			for (AbstractProportionalBundler b: bundlers)
			{
				BundleDataHolder h = b.getDataHolder();
				//if (h.getMean() <= 0) continue;
				meanSum += normalize(h.getMean());
				//meanSum += m;
			}
			//int max = Math.max(1, (int) (0.9d*maxSize()));
			int max = maxSize();
			int sum = 0;
			for (AbstractProportionalBundler b: bundlers)
			{
				BundleDataHolder h = b.getDataHolder();
				//if (h.getMean() <= 0) continue;
				double p = normalize(h.getMean()) / meanSum;
				int size = Math.max(1, (int) (p * max));
				//size = Math.min((int) (0.9*max), size);
				if (size >= max) size = max-1;
				b.setBundleSize(size);
				sum += size;
			}
			if ((sum < max) && (minBundler != null))
			{
				int size = minBundler.getBundleSize();
				minBundler.setBundleSize(size + (max - sum));
			}
			if (debugEnabled)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("bundler info:\n");
				sb.append("minMean = ").append(minMean).append(", maxMean = ").append(maxMean).append(", maxSize = ").append(max).append("\n");
				for (AbstractProportionalBundler b: bundlers)
				{
					sb.append("bundler #").append(b.getBundlerNumber()).append(" : ").append(b.getBundleSize()).append(":\n");
					sb.append("  ").append(b.getDataHolder()).append("\n");
				}
				log.debug(sb.toString());
			}
		}
	}

	/**
	 * 
	 * @param x .
	 * @return .
	 */
	public double normalize(double x)
	{
		//return 1d / (1d + (x <= 0d ? 0d : Math.log(1d + ((ProportionalTuneProfile) profile).getProportionalityFactor() * x)));
		//return Math.exp(-((ProportionalTuneProfile) profile).getProportionalityFactor() * x);
		double r = 1d;
		for (int i=0; i<((ProportionalTuneProfile) profile).getProportionalityFactor(); i++) r *= x;
		return 1d/r;
		/*
		*/
	}
}
