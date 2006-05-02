package org.jppf.server.scheduler.bundle;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jppf.server.JPPFStatsUpdater;

/**
 * This class implements a self tuned bundle size algorithm. It starts using the
 * bundle size defined in property file and starts changing it to find a better
 * performance. The algorithm starts making The algorithm waits for some
 * execution to get a mean execution time, and them make a change in bundle size
 * Each time the change is done, it is done over a smaller range randomicaly
 * selected (like Monte Carlo algorithm),
 * 
 * @author Domingos Creado
 * 
 */
public class AutoTunedBundler implements Bundler {

	private int currentSize;

	private boolean stable = false;

	private double stableMean;

	private Random rnd = new Random();

	private Map<Integer, BundlePerformanceSample> samplesMap = new TreeMap<Integer, BundlePerformanceSample>();
	
	private AutoTuneProfile profile;

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 */
	public AutoTunedBundler(AutoTuneProfile profile) {
		log.info("Using Auto-Tuned bundle size");
		currentSize = JPPFStatsUpdater.getStaticBundleSize();
		if (currentSize < 1) {
			currentSize = 1;
		}
		log.info("The initial size is " + currentSize);
		this.profile = profile;
	}

	public int getBundleSize() {
		return currentSize;
	}

	public void feedback(int bundleSize, long totalTime) {
		assert bundleSize > 0;

		boolean isDebugEnable = log.isDebugEnabled();
		if (isDebugEnable) {
			log.debug("Got another sample with bundleSize=" + bundleSize
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
			bundleSample.mean = (totalTime + bundleSample.samples
					* bundleSample.mean)
					/ (bundleSample.samples + bundleSize);
			bundleSample.samples = bundleSample.samples + bundleSize;
			samples = bundleSample.samples;
			mean = bundleSample.mean;
		}

		boolean makeAnalysis = false;

		if (bundleSize == currentSize) {
			if (stable) {
				if (samples > profile.getMinSamplesToCheckConvergency()
						&& (Math.abs(stableMean - mean) / stableMean > profile.getMaxDevitation())) {
					log.info("Detected a change in tasks profile... restarting the discovering process");
					makeAnalysis = true;
					stable = false;
				}
				if (samples > 2 * profile.getMinSamplesToCheckConvergency()) {
					bundleSample.mean = 0;
					bundleSample.samples = 0;
				}
			} else if (samples > profile.getMinSamplesToAnalyse()) {
				makeAnalysis = true;
			}
		}

		if (makeAnalysis) {
			synchronized (samplesMap) {
				int bestSize = searchBestSize();
				int counter = 0;
				while (counter < profile.getMaxGuessToStable()) {
					
					int diff = profile.createDiff(bestSize,samplesMap.size(),rnd);
					if (rnd.nextBoolean() && diff < bestSize) {
						// the second part is there to not let the size be
						// negative or zero
						diff = -diff;
					}
					currentSize = bestSize + diff;
					if (samplesMap.get(currentSize) == null) {
						log.info("The next bundle sized that will be used is "
								+ currentSize);
						return;
					}
					counter++;
				}
				stable = true;
				currentSize = bestSize;
				stableMean = samplesMap.get(currentSize).mean;
				samplesMap.clear();
				log.info("The bundle was converged to size " + currentSize
						+ " with the mean execution of " + stableMean);
			}
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
	 * this is a utility class to be used to store the pair of mean and the
	 * number of samples does this mean was based.
	 */
	private class BundlePerformanceSample {

		public double mean;

		public long samples;
	}
}
