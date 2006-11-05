/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server.scheduler.bundle;

import java.util.*;
import org.jppf.server.*;

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
public class AutoTunedBundler implements Bundler
{
	/**
	 * Count of the bundlers used to generate a readable unique id.
	 */
	private static int bundlerCount = 0;
	/**
	 * Increment the bundlers count by one.
	 * @return the new count as an int value.
	 */
	private static synchronized int incBundlerCount()
	{
		return ++bundlerCount;
	}
	/**
	 * The bundler number for this bundler.
	 */
	protected int bundlerNumber = incBundlerCount();
	/**
	 * The currrent bundle size.
	 */
	protected int currentSize;

	/**
	 * The mean performance value associated with the current bundle size.
	 */
	protected double stableMean;

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
	 * Parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
	 */
	protected AutoTuneProfile profile;
	/**
	 * The creation timestamp for this bundler.
	 */
	private long timestamp = System.currentTimeMillis();

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * grouped as a performance analysis profile.
	 */
	public AutoTunedBundler(AutoTuneProfile profile)
	{
		LOG.info("Bundler#" + bundlerNumber + ": Using Auto-Tuned bundle size");
		currentSize = JPPFStatsUpdater.getStaticBundleSize();
		if (currentSize < 1)
		{
			currentSize = 1;
		}
		LOG.info("Bundler#" + bundlerNumber + ": The initial size is " + currentSize);
		this.profile = profile;
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
	 * @see org.jppf.server.scheduler.bundle.Bundler#feedback(int, long)
	 */
	public void feedback(int bundleSize, double time)
	{
		assert bundleSize > 0;
		if (DEBUG_ENABLED)
		{
			LOG.debug("Bundler#" + bundlerNumber + ": Got another sample with bundleSize="
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
		if (samples > profile.getMinSamplesToAnalyse()) performAnalysis();
	}

	/**
	 * Recompute the bundle size after a performance profile change has been detected. 
	 */
	private void performAnalysis()
	{
		synchronized (samplesMap)
		{
			int bestSize = searchBestSize();
			int max = JPPFDriver.getQueue().getMaxBundleSize()/2;
			if ((max > 0) && (bestSize > max)) bestSize = max;
			int counter = 0;
			while (counter < profile.getMaxGuessToStable())
			{
				int diff = profile.createDiff(bestSize, samplesMap.size(), rnd);
				if (diff < bestSize)
				{
					// the second part is there to ensure the size is > 0
					if (rnd.nextBoolean()) diff = -diff;
				}
				currentSize = bestSize + diff;
				if (samplesMap.get(currentSize) == null)
				{
					LOG.info("Bundler#" + bundlerNumber + ": The next bundle size that will be used is " + currentSize);
					return;
				}
				counter++;
			}

			currentSize = bestSize;
			if (samplesMap.get(currentSize) != null)
			{
				stableMean = samplesMap.get(currentSize).mean;
				samplesMap.clear();
			}
		}
		LOG.info("Bundler#" + bundlerNumber + ": The bundle size converged to " + currentSize
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
		return bestSize;
	}

	/**
	 * Make a copy of this bundler.
	 * @return a new <code>AutoTunedBundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	public Bundler copy()
	{
		AutoTunedBundler b = new AutoTunedBundler(profile.copy());
		return b;
	}

	/**
	 * Get the timestamp at which this bundler was created.
	 * This is used to enable node channels to know when the bundler settings have changed.
	 * @return the timestamp as a long value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getTimestamp()
	 */
	public long getTimestamp()
	{
		return timestamp;
	}

	/**
	 * This is a utility class to be used to store the pair of mean and the
	 * number of samples this mean is based on.
	 */
	private class BundlePerformanceSample
	{
		/**
		 * Mean compute time for server to node round trip.
		 */
		public double mean;

		/**
		 * Number of samples used to compute the mean value.
		 */
		public long samples;
	}
}
