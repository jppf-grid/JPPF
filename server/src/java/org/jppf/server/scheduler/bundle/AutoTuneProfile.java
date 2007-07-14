/*
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
package org.jppf.server.scheduler.bundle;

import java.io.Serializable;
import java.util.Random;

/**
 * This interface defines the set of parameters used by the auto-compute algorithm.
 * @author Domingos Creado
 */
public interface AutoTuneProfile extends Serializable {

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
	 * for the algorithm to consider the current best solution stable.
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
	/**
	 * Make a copy of this profile.
	 * @return a newly created <code>AutoTuneProfile</code> instance.
	 */
	AutoTuneProfile copy();
}
