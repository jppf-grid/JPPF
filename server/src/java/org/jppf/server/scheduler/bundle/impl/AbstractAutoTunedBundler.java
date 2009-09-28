/*
 * Java Parallel Processing Framework.
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

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.autotuned.AnnealingTuneProfile;

/**
 * This class implements a self tuned bundle size algorithm. It starts using the
 * bundle size defined in property file and starts changing it to find a better
 * performance. The algorithm starts making The algorithm waits for some
 * execution to get a mean execution time, and them make a change in bundle size
 * Each time the change is done, it is done over a smaller range randomly
 * selected (like Monte Carlo algorithm).
 * 
 * @author Domingos Creado
 * 
 */
public abstract class AbstractAutoTunedBundler extends AbstractBundler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractAutoTunedBundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The currrent bundle size.
	 */
	protected int currentSize = 1;
	/**
	 * Used to compute a pseudo-random increment to the bundle size, as part of a Monte Carlo random walk
	 * towards a good solution.
	 */
	protected Random rnd = new Random(System.currentTimeMillis());
	/**
	 * A map of performance samples, aorted by increasing bundle size.
	 */
	protected Map<Integer, BundlePerformanceSample> samplesMap = new HashMap<Integer, BundlePerformanceSample>();

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * grouped as a performance analysis profile.
	 */
	public AbstractAutoTunedBundler(AnnealingTuneProfile profile)
	{
		this(profile, false);
	}

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * @param overriden true if the settings were overriden by the node, false otherwise.
	 * grouped as a performance analysis profile.
	 */
	public AbstractAutoTunedBundler(AnnealingTuneProfile profile, boolean overriden)
	{
		super(profile, overriden);
		log.info("Bundler#" + bundlerNumber + ": Using Auto-Tuned bundle size");
		if (currentSize < 1) currentSize = 1;
		log.info("Bundler#" + bundlerNumber + ": The initial size is " + currentSize);
	}

	/**
	 * Get the latest bundle size computed by this bundler.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize()
	{
		return currentSize;
	}

	/**
	 * This method performs the actual bundle size computation, based on current and past
	 * performance data.<br>
	 * Depending on the the performance samples and profile parameters, the following actions
	 * may be triggered in this method:
	 * <ul>
	 * <li>samples collection (unconditional)</li>
	 * <li>detection of performance profile changes, if not currently being done</li>
	 * <li>when a performance profile change is detected, recompute the bundle size.</li>
	 * </ul>
	 * @param bundleSize bundle size of the new performance sample.
	 * @param time total execution time of the new sample.
	 * @see org.jppf.server.scheduler.bundle.Bundler#feedback(int, double)
	 */
	public void feedback(int bundleSize, double time)
	{
		assert bundleSize > 0;
		if (debugEnabled)
		{
			log.debug("Bundler#" + bundlerNumber + ": Got another sample with bundleSize="
				+ bundleSize + " and totalTime=" + time);
		}

		// retrieving the record of the bundle size
		BundlePerformanceSample bundleSample;
		synchronized (samplesMap)
		{
			bundleSample = samplesMap.get(bundleSize);
			if (bundleSample == null)
			{
				bundleSample = new BundlePerformanceSample();
				samplesMap.put(bundleSize, bundleSample);
			}
		}

		long samples = bundleSample.samples + bundleSize;
		synchronized (bundleSample)
		{
			bundleSample.mean = (time + bundleSample.samples * bundleSample.mean) / samples;
			bundleSample.samples = samples;
		}
		if (samples > ((AnnealingTuneProfile) profile).getMinSamplesToAnalyse())
		{
			performAnalysis();
			if (debugEnabled) log.debug("Bundler#" + bundlerNumber + ": bundle size = " + currentSize);
		}
	}

	/**
	 * Recompute the bundle size after a performance profile change has been detected. 
	 */
	private void performAnalysis()
	{
		double stableMean = 0;
		synchronized (samplesMap)
		{
			int bestSize = searchBestSize();
			int max = maxSize();
			if ((max > 0) && (bestSize > max)) bestSize = max;
			int counter = 0;
			while (counter < ((AnnealingTuneProfile) profile).getMaxGuessToStable())
			{
				int diff = ((AnnealingTuneProfile) profile).createDiff(bestSize, samplesMap.size(), rnd);
				if (diff < bestSize)
				{
					// the second part is there to ensure the size is > 0
					if (rnd.nextBoolean()) diff = -diff;
				}
				currentSize = bestSize + diff;
				if (samplesMap.get(currentSize) == null)
				{
					if (debugEnabled)
					{
						log.debug("Bundler#" + bundlerNumber + ": The next bundle size that will be used is " + currentSize);
					}
					return;
				}
				counter++;
			}

			currentSize = Math.max(1, bestSize);
			BundlePerformanceSample sample = samplesMap.get(currentSize);
			if (sample != null)
			{
				stableMean = sample.mean;
				samplesMap.clear();
				samplesMap.put(currentSize, sample);
			}
		}
		log.info("Bundler#" + bundlerNumber + ": The bundle size converged to " + currentSize
				+ " with the mean execution of " + stableMean);
	}

	/**
	 * Lookup the best bundle size in the current samples map.
	 * @return the best bundle size as an int value.
	 */
	private int searchBestSize()
	{
		int bestSize = 0;
		double minorMean = Double.POSITIVE_INFINITY;
		for (Integer size: samplesMap.keySet())
		{
			BundlePerformanceSample sample = samplesMap.get(size);
			if (sample.mean < minorMean)
			{
				bestSize = size;
				minorMean = sample.mean;
			}
		}
		if (debugEnabled)
		{
			log.debug("Bundler#" + bundlerNumber + ": best size found = " + bestSize);
		}
		return bestSize;
	}
}
