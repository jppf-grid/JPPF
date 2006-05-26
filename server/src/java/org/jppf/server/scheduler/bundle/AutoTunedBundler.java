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
import org.jppf.server.JPPFStatsUpdater;

/**
 * This class implements a self tuned bundle size algorithm. It starts using the
 * bundle size defined in property file and starts changing it to find a better
 * performance. The algorithm starts making The algorithm waits for some
 * execution to get a mean execution time, and them make a change in bundle size
 * Each time the change is done, it is done over a smaller range randomicaly
 * selected (like Monte Carlo algorithm).
 * 
 * @author Domingos Creado
 * 
 */
public class AutoTunedBundler implements Bundler {

	/**
	 * The currrent bundle size.
	 */
	private int currentSize;

	/**
	 * True if a performance profile analysis is currently being done, false otherwise. 
	 */
	private boolean stable = false;

	/**
	 * The mean performance value associated with the current bundle size.
	 */
	private double stableMean;

	/**
	 * Used to compute a pseudo-random increment to the bundle size, as part of a Monte Carlo random walk
	 * towards a good solution.
	 */
	private Random rnd = new Random(System.currentTimeMillis());

	/**
	 * A map of performance samples, aorted by increasing bundle size.
	 */
	private Map<Integer, BundlePerformanceSample> samplesMap = new TreeMap<Integer, BundlePerformanceSample>();
	
	/**
	 * Parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
	 */
	private AutoTuneProfile profile;

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * grouped as a performance analysis profile.
	 */
	public AutoTunedBundler(AutoTuneProfile profile) {
		LOG.info("Using Auto-Tuned bundle size");
		currentSize = JPPFStatsUpdater.getStaticBundleSize();
		if (currentSize < 1) {
			currentSize = 1;
		}
		LOG.info("The initial size is " + currentSize);
		this.profile = profile;
	}

	/**
	 * Get the latest bundle size computed by this bundler.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize() {
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
	 * @param totalTime total execution time of the new sample.
	 * @see org.jppf.server.scheduler.bundle.Bundler#feedback(int, long)
	 */
	public void feedback(int bundleSize, long totalTime) {
		assert bundleSize > 0;

		boolean isDebugEnable = LOG.isDebugEnabled();
		if (isDebugEnable) {
			LOG.debug("Got another sample with bundleSize=" + bundleSize
					+ " and totalTime=" + totalTime);
		}

		// retrieving the record of the bundle size
		BundlePerformanceSample bundleSample;
		synchronized (samplesMap) {
			bundleSample = samplesMap.get(bundleSize);
			if (bundleSample == null) {
				bundleSample = new BundlePerformanceSample();
				samplesMap.put(bundleSize, bundleSample);
			}
		}

		long samples;
		double mean;
		synchronized (bundleSample) {
			bundleSample.mean = 
				(totalTime + bundleSample.samples * bundleSample.mean)
				/ (bundleSample.samples + bundleSize);
			bundleSample.samples = bundleSample.samples + bundleSize;
			samples = bundleSample.samples;
			mean = bundleSample.mean;
		}

		boolean makeAnalysis = false;
		if (bundleSize == currentSize) {
			if (stable) {
				if (samples > profile.getMinSamplesToCheckConvergence()
						&& (Math.abs(stableMean - mean) / stableMean > profile.getMaxDeviation())) {
					LOG.info("Detected a change in tasks profile... restarting the discovering process");
					makeAnalysis = true;
					stable = false;
				}
				if (samples > 2 * profile.getMinSamplesToCheckConvergence()) {
					bundleSample.mean = 0;
					bundleSample.samples = 0;
				}
			} else if (samples > profile.getMinSamplesToAnalyse()) {
				makeAnalysis = true;
			}
		}
		if (makeAnalysis) performAnalysis();
	}

	/**
	 * Recompute the bundle size after a performance profile change has been detected. 
	 */
	private void performAnalysis()
	{
		synchronized (samplesMap) {
			int bestSize = searchBestSize();
			int counter = 0;
			while (counter < profile.getMaxGuessToStable()) {
				
				int diff = profile.createDiff(bestSize,samplesMap.size(),rnd);
				if (rnd.nextBoolean() && diff < bestSize) {
					// the second part is there to ensure the size is > 0
					diff = -diff;
				}
				currentSize = bestSize + diff;
				if (samplesMap.get(currentSize) == null) {
					LOG.info("The next bundle sized that will be used is "
							+ currentSize);
					return;
				}
				counter++;
			}
			stable = true;
			currentSize = bestSize;
			stableMean = samplesMap.get(currentSize).mean;
			samplesMap.clear();
			LOG.info("The bundle was converged to size " + currentSize
					+ " with the mean execution of " + stableMean);
		}
	}

	/**
	 * 
	 * @return the best bundle size
	 */
	private int searchBestSize() {
		int bestSize = 0;
		double minorMean = Double.POSITIVE_INFINITY;
		for (Map.Entry<Integer, BundlePerformanceSample> sample : samplesMap
				.entrySet()) {
			if (sample.getValue().mean < minorMean) {
				bestSize = sample.getKey();
				minorMean = sample.getValue().mean;
			}
		}
		return bestSize;
	}

	

	/**
	 * This is a utility class to be used to store the pair of mean and the
	 * number of samples this mean is based on.
	 */
	private class BundlePerformanceSample {

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
