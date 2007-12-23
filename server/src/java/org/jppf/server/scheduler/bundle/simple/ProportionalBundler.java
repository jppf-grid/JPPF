/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.scheduler.bundle.simple;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.server.*;
import org.jppf.server.scheduler.bundle.*;

/**
 * This bundler implementation computes bundle sizes propertional to the mean execution
 * time for each node.<br>
 * The scope of this bundler is all nodes, which means that it computes the size for all nodes,
 * unless an override is specified by the nodes.<br>
 * The mean execution time is computed as a moving average over a number of tasks, specified in the bundling
 * algorithm profile configuration as &quot;minSamplesToAnalyse&quot;<br>
 * This algorithm is well suited for relatively small networks (a few dozen nodes at most). It generates an overhead
 * everytime the performance data for a node is updated. In the case of a small network, this overhead is not
 * large enough to impact the overall performance significantly.
 * @author Laurent Cohen
 */
public class ProportionalBundler extends AbstractBundler
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
	 * Parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
	 */
	protected AutoTuneProfile profile;
	/**
	 * Mapping of individual bundler to corresponding performance data.
	 */
	protected Map<DelegatingBundler, BundleDataHolder> map = 
		new HashMap<DelegatingBundler, BundleDataHolder>();

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * grouped as a performance analysis profile.
	 */
	public ProportionalBundler(AutoTuneProfile profile, boolean override)
	{
		this.override = override;
		int currentSize = JPPFStatsUpdater.getStaticBundleSize();
		if (currentSize < 1)
		{
			currentSize = 1;
		}
		log.info("Bundler #" + bundlerNumber + ": The initial size is " + currentSize);
		this.profile = profile;
	}

	/**
	 * Process a new performance sample for the specified bundler.
	 * @param bundler the bundler the sample applies to.
	 * @param sample the performance sample to process.
	 */
	public synchronized void feedback(DelegatingBundler bundler, BundlePerformanceSample sample)
	{
		BundleDataHolder holder = getDataHolder(bundler);
		holder.addSample(sample);
		double maxMean = Double.NEGATIVE_INFINITY;
		double minMean = Double.POSITIVE_INFINITY;
		BundleDataHolder minHolder = null;
		for (BundleDataHolder h: map.values())
		{
			double m = h.getMean();
			if (m > maxMean) maxMean = m;
			if (m < minMean)
			{
				minMean = m;
				minHolder = h;
			}
		}
		double diffSum = 0d;
		for (BundleDataHolder h: map.values())
		{
			double diff = maxMean / h.getMean();
			diffSum += diff*diff;
		}
		int max = JPPFDriver.getQueue().getMaxBundleSize();
		int sum = 0;
		for (BundleDataHolder h: map.values())
		{
			double diff = maxMean / h.getMean();
			int size = Math.max(1, (int) (max * (diff*diff / diffSum)));
			h.setBundleSize(size);
			sum += size;
			//h.setMaLength((int) (BundleDataHolder.INITIAL_MA_LENGTH/diff));
		}
		if (sum < max)
		{
			int size = minHolder.getBundleSize();
			minHolder.setBundleSize(size + (max - sum));
		}
		if (debugEnabled)
		{
			for (DelegatingBundler b: map.keySet())
			{
				BundleDataHolder h = map.get(b);
				log.debug("bundler #"+b.getBundlerNumber()+" new size="+h.getBundleSize()+", maLength="+h.getMaLength());
			}
		}
	}

	/**
	 * Process a new performance sample for the specified bundler.
	 * @param bundler the bundler the sample applies to.
	 * @param sample the performance sample to process.
	 */
	public synchronized void feedback2(DelegatingBundler bundler, BundlePerformanceSample sample)
	{
		BundleDataHolder holder = getDataHolder(bundler);
		holder.addSample(sample);
		double maxMean = 0d;
		for (BundleDataHolder h: map.values())
		{
			double m = h.getMean();
			if (m > maxMean) maxMean = m;
		}
		double diffSum = 0d;
		for (BundleDataHolder h: map.values()) diffSum += maxMean / h.getMean();
		int max = JPPFDriver.getQueue().getMaxBundleSize();
		for (DelegatingBundler b: map.keySet())
		{
			BundleDataHolder h = map.get(b);
			double diff = maxMean / h.getMean();
			int size = Math.max(1, (int) (max * (diff / diffSum)));
			h.setBundleSize(size);
			if (debugEnabled)
			{
				log.debug("bundler #"+b.getBundlerNumber()+" new size="+size+", maLength="+h.getMaLength());
			}
		}
	}

	/**
	 * Make a copy of this bundler
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	public Bundler copy()
	{
		return null;
	}

	/**
	 * Get the current size of the bundle.
	 * @return  the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize()
	{
		return 0;
	}

	/**
	 * Get the current size of the specified bundler.
	 * @param bundler the bundler for which to get the bundle size.
	 * @return  the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public synchronized int getBundleSize(DelegatingBundler bundler)
	{
		return getDataHolder(bundler).getBundleSize();
	}

	/**
	 * Get, and create if necessary the data holder associated with a bundler.
	 * @param bundler the bundler for which to get the data holder.
	 * @return a <code>BundleDataHolder</code> instance.
	 */
	private BundleDataHolder getDataHolder(DelegatingBundler bundler)
	{
		BundleDataHolder holder = map.get(bundler);
		if (holder == null)
		{
			holder = new BundleDataHolder((int) profile.getMinSamplesToAnalyse());
			map.put(bundler, holder);
		}
		return holder;
	}

	/**
	 * Remove the specified bundler from the list of bundler in this object.
	 * @param bundler the bundler to remove.
	 */
	public synchronized void removeBundler(DelegatingBundler bundler)
	{
		map.remove(bundler);
	}
}
