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


/**
 * Default abstract implementation of the {@link AutoTuneProfile} interface,
 * providing an encapsulation of the parameters as Java properties.
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
