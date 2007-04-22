/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

import org.apache.log4j.Logger;
import org.jppf.server.*;

/**
 * 
 * @author Laurent Cohen
 */
public class CopyOfResilientBundler extends AbstractBundler
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(CopyOfResilientBundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * A map of performance samples, aorted by increasing bundle size.
	 */
	protected Map<Integer, BundlePerformanceSample> samplesMap = new Hashtable<Integer, BundlePerformanceSample>();
	/**
	 * Parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
	 */
	protected AutoTuneProfile profile;
	/**
	 * Search direction, the value is either -1 or +1.
	 */
	protected int direction = 1;
	/**
	 * Difference to add to the current bundle size.
	 */
	protected int diff = 1;
	/**
	 * The currrent bundle size.
	 */
	protected int currentSize = 1;
	/**
	 * The currrent bundle size.
	 */
	protected double currentMean = 1d;
	/**
	 * Determines whether the bundler is currently analysing the performance or it has a stable size value.
	 */
	private boolean analysing = false;
	/**
	 * Number of guesses performed so far.
	 */
	private int guessCount = 0; 
	/**
	 * Used to compute a pseudo-random increment to the bundle size, as part of a Monte Carlo random walk
	 * towards a good solution.
	 */
	protected Random rnd = new Random(System.currentTimeMillis());

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * grouped as a performance analysis profile.
	 */
	public CopyOfResilientBundler(AutoTuneProfile profile)
	{
		this(profile, false);
	}

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * grouped as a performance analysis profile.
	 */
	public CopyOfResilientBundler(AutoTuneProfile profile, boolean override)
	{
		log.info("Bundler#" + bundlerNumber + ": Using resilient bundle size");
		this.override = override;
		currentSize = JPPFStatsUpdater.getStaticBundleSize();
		if (currentSize < 1) currentSize = 1;
		log.info("Bundler#" + bundlerNumber + ": The initial size is " + currentSize);
		this.profile = profile;
	}

	/**
	 * This method performs the actual bundle size computation, based on current and past
	 * performance data.<br>
	 * @param bundleSize the size of the sample.
	 * @param totalTime the time taken for the bundle's round-trip form the server to the node.
	 * @see org.jppf.server.scheduler.bundle.Bundler#feedback(int, double)
	 */
	public void feedback(int bundleSize, double totalTime)
	{
		BundlePerformanceSample sample = samplesMap.get(bundleSize);
		if (sample == null)
		{
			sample = new BundlePerformanceSample();
			samplesMap.put(bundleSize, sample);
		}
		long nbSamples = sample.samples + bundleSize;
		double sampleMean = (sample.mean * sample.samples + totalTime) / nbSamples;
		if (nbSamples > profile.getMinSamplesToAnalyse())
		{
			double deviation = sampleMean/currentMean - 1d;
			if (!analysing)
			{
				if (Math.abs(deviation) > profile.getMaxDeviation())
				{
					analysing = true;
					if (debugEnabled) log.debug("Bundler#" + bundlerNumber + ": starting analysis");
					guessCount = 0;
					samplesMap.clear();
				}
				else samplesMap.remove(bundleSize);

				sample.samples = bundleSize;
				sample.mean = totalTime/bundleSize;
				samplesMap.put(bundleSize, sample);
			}
			if (analysing)
			{
				int max = (int) Math.round(JPPFDriver.getQueue().getMaxBundleSize() * 0.5);
				if (guessCount >= profile.getMaxGuessToStable())
				{
					analysing = false;
					currentSize = findBestSize(-1);
					currentMean = samplesMap.get(currentSize).mean;
					if (debugEnabled) log.debug("Bundler#" + bundlerNumber + ": ending analysis, new best size = "+ currentSize);
				}
				else
				{
					int bestSize = findBestSize(-1);
					if (bestSize <= 0) bestSize = currentSize;
					int n = createDiff(deviation);
					currentSize = bestSize + n;
					currentMean = sampleMean;

					/*
					int cnt = 0; 
					do
					{
						diff = profile.createDiff(bestSize, samplesMap.size(), rnd);
						direction = rnd.nextBoolean() ? 1 : -1; 
						diff *= direction;
						cnt++;
					}
					while (((diff + bestSize <= 0) || (diff + bestSize > max)) && (cnt < 10));
					currentSize = bestSize + diff;
					*/
					if (currentSize < 1) currentSize = 1;
					else if (currentSize > max) currentSize = max;
					if (debugEnabled) log.debug("Bundler#" + bundlerNumber + ": found currentSize="+currentSize);
					sample = samplesMap.get(bestSize);
					if (sampleMean < sample.mean) guessCount = 0;
					else guessCount++;
				}
			}
		}
		else
		{
			sample.samples = nbSamples;
			sample.mean = sampleMean;
		}
	}

	/**
	 * Find the size in the samples map with the lowest mean execution time.
	 * @param exclude a size to exclude from the search.
	 * @return the known bundle size with the lowest mean.
	 */
	private int findBestSize(int exclude)
	{
		double min = Double.MAX_VALUE;
		int bestSize = -1;
		for (Integer size: samplesMap.keySet())
		{
			if (size == exclude) continue;
			BundlePerformanceSample sample = samplesMap.get(size);
			if (sample.mean < min)
			{
				min = sample.mean;
				bestSize = size;
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
		CopyOfResilientBundler b = new CopyOfResilientBundler(profile.copy());
		return b;
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
	 * @param deviation the deviation from the currentMean.
	 * @return the difference as a double value.
	 */
	public int createDiff(double deviation)
	{
		if ((deviation > 0) && (direction > 0))
		{
			diff = (int) (currentSize * 0.5d);
			direction = -1;
		}
		else if ((deviation < 0) && (direction > 0))
		{
			diff = (int) (currentSize * 0.2d);
			direction = 1;
		}
		else if ((deviation > 0) && (direction < 0))
		{
			diff = (int) (currentSize * 0.2d);
			direction = 1;
		}
		else if ((deviation < 0) && (direction < 0))
		{
			diff = (int) (currentSize * 0.5d);
			direction = -1;
		}
		return diff*direction;
	}
}
