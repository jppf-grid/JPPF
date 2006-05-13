package org.jppf.server.scheduler.bundle;

import java.util.Random;

public interface AutoTuneProfile {

	/**
	 * 
	 * @return the minimum samples that must be collected before an 
	 * analysis be done
	 */
	long getMinSamplesToAnalyse();

	/**
	 * 
	 * @return the minimum number of samples to be collected before
	 * check if the profile has changed. 
	 */
	long getMinSamplesToCheckConvergency();
	
	/**
	 * 
	 * @return the percentage of deviation of the current mean to the mean 
	 * when the system was considered stable. 
	 */
	double getMaxDevitation();

	/**
	 * @return the number of max guess of number generated that was already tested
	 * to the algorithm consider the best solution stable.
	 */
	int getMaxGuessToStable();

	
	/**
	 * Generate a difference to be applied to the best bundle size already known. 
	 * @param bestSize the known best size of bundle
	 * @param collectedSamples the number of samples that were already collected
	 * @param rnd a Random generator
	 * @return an always positive diff to be applied to bundle size
	 */
	int createDiff(int bestSize, int collectedSamples, Random rnd);
}
