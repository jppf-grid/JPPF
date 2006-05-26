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

import java.util.Random;

/**
 * This interface defines the set of parameters used by the auto-compute algorithm.
 * @author Domingos Creado
 */
public interface AutoTuneProfile {

	/**
	 * Get the minimum number of samples that must be collected before an 
	 * analysis is triggered.
	 * @return the number of samples as a long value.
	 */
	long getMinSamplesToAnalyse();

	/**
	 * Get the the minimum number of samples to be collected before
	 * checking if the performance profile has changed. 
	 * @return the number of samples as a long value. 
	 */
	long getMinSamplesToCheckConvergence();
	
	/**
	 * Get the percentage of deviation of the current mean to the mean 
	 * when the system was considered stable. 
	 * @return the percentage of deviation as a double value.
	 */
	double getMaxDeviation();

	/**
	 * Get the maximum number of guesses of number generated that were already tested
	 * for the algorithm to consider the best solution stable.
	 * @return the number guesses as an int value.
	 */
	int getMaxGuessToStable();
	
	/**
	 * Generate a difference to be applied to the best known bundle size.
	 * @param bestSize the known best size of bundle.
	 * @param collectedSamples the number of samples that were already collected.
	 * @param rnd a pseudo-random number generator.
	 * @return an always positive diff to be applied to bundle size
	 */
	int createDiff(int bestSize, int collectedSamples, Random rnd);
}
