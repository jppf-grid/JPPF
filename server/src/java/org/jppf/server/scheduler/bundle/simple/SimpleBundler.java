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
 * 
 * @author Laurent Cohen
 */
public class SimpleBundler extends AbstractBundler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SimpleBundler.class);
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
	protected Map<DelegatingSimpleBundler, BundleDataHolder> map = 
		new HashMap<DelegatingSimpleBundler, BundleDataHolder>();

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * grouped as a performance analysis profile.
	 */
	public SimpleBundler(AutoTuneProfile profile, boolean override)
	{
		log.info("Bundler#" + bundlerNumber + ": Using Auto-Tuned bundle size");
		this.override = override;
		int currentSize = JPPFStatsUpdater.getStaticBundleSize();
		if (currentSize < 1)
		{
			currentSize = 1;
		}
		log.info("Bundler#" + bundlerNumber + ": The initial size is " + currentSize);
		this.profile = profile;
	}

	/**
	 * Process a new performance sample for the specified bundler.
	 * @param bundler the bundler the sample applies to.
	 * @param sample the performance sample to process.
	 */
	public synchronized void feedback(DelegatingSimpleBundler bundler, BundlePerformanceSample sample)
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
		//for (BundleDataHolder h: map.values()) diffSum += 1d + maxMean - h.getMean();
		for (BundleDataHolder h: map.values()) diffSum += maxMean / h.getMean();
		int max = JPPFDriver.getQueue().getMaxBundleSize();
		for (DelegatingSimpleBundler b: map.keySet())
		{
			BundleDataHolder h = map.get(b);
			double m = h.getMean();
			//double diff = 1d + maxMean - m;
			double diff = maxMean / m;
			int size = (int) (max * (diff / diffSum));
			h.setBundleSize(size);
			if (debugEnabled) log.debug("bundler #"+b.getBundlerNumber()+" new size="+size);
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
	public synchronized int getBundleSize(DelegatingSimpleBundler bundler)
	{
		return getDataHolder(bundler).getBundleSize();
	}

	/**
	 * Get, and create if necessary the data holder associated with a bundler.
	 * @param bundler the bundler for which to get the data holder.
	 * @return a <code>BundleDataHolder</code> instance.
	 */
	private BundleDataHolder getDataHolder(DelegatingSimpleBundler bundler)
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
	public synchronized void removeBundler(DelegatingSimpleBundler bundler)
	{
		map.remove(bundler);
	}
}
