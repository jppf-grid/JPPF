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
package org.jppf.server.scheduler.bundle;


/**
 * Default abstract implementation of the {@link AutoTuneProfile} interface,
 * providing an encapsulation of the parameters as Java properties.
 * @author Domingos Creado
 * @author Laurent Cohen
 */
public abstract class AbstractAutoTuneProfile implements AutoTuneProfile
{
	/**
	 * The minimum number of samples that must be collected before an analysis is triggered.
	 */
	protected long minSamplesToAnalyse = 0L;
	/**
	 * The minimum number of samples to be collected before checking if the performance profile has changed. 
	 */
	protected long minSamplesToCheckConvergence = 0L;
	/**
	 * The percentage of deviation of the current mean to the mean 
	 * when the system was considered stable. 
	 */
	protected double maxDeviation = 0d;
	/**
	 * The maximum number of guesses of number generated that were already tested
	 * for the algorithm to consider the current best solution stable.
	 */
	protected int maxGuessToStable = 0;

	/**
	 * Get the minimum number of samples that must be collected before an analysis is triggered.
	 * @return the number of samples as a long value.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#getMinSamplesToAnalyse()
	 */
	public long getMinSamplesToAnalyse()
	{
		return minSamplesToAnalyse;
	}

	/**
	 * Set the minimum number of samples that must be collected before an analysis is triggered.
	 * @param minSamplesToAnalyse the number of samples as a long value.
	 */
	public void setMinSamplesToAnalyse(long minSamplesToAnalyse)
	{
		this.minSamplesToAnalyse = minSamplesToAnalyse;
	}

	/**
	 * Get the the minimum number of samples to be collected before
	 * checking if the performance profile has changed. 
	 * @return the number of samples as a long value. 
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#getMinSamplesToCheckConvergence()
	 */
	public long getMinSamplesToCheckConvergence()
	{
		return minSamplesToCheckConvergence;
	}

	/**
	 * Set the the minimum number of samples to be collected before
	 * checking if the performance profile has changed. 
	 * @param minSamplesToCheckConvergence the number of samples as a long value.
	 */
	public void setMinSamplesToCheckConvergence(long minSamplesToCheckConvergence)
	{
		this.minSamplesToCheckConvergence = minSamplesToCheckConvergence;
	}

	/**
	 * Get the percentage of deviation of the current mean to the mean 
	 * when the system was considered stable. 
	 * @return the percentage of deviation as a double value.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#getMaxDeviation()
	 */
	public double getMaxDeviation()
	{
		return maxDeviation;
	}

	/**
	 * Set the percentage of deviation of the current mean to the mean 
	 * when the system was considered stable. 
	 * @param maxDeviation the percentage of deviation as a double value.
	 */
	public void setMaxDeviation(double maxDeviation)
	{
		this.maxDeviation = maxDeviation;
	}

	/**
	 * Get the maximum number of guesses of number generated that were already tested
	 * for the algorithm to consider the current best solution stable.
	 * @return the number of guesses as an int value.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#getMaxGuessToStable()
	 */
	public int getMaxGuessToStable()
	{
		return maxGuessToStable;
	}

	/**
	 * Set the maximum number of guesses of number generated that were already tested
	 * for the algorithm to consider the current best solution stable.
	 * @param maxGuessToStable the number of guesses as an int value.
	 */
	public void setMaxGuessToStable(int maxGuessToStable)
	{
		this.maxGuessToStable = maxGuessToStable;
	}
}
