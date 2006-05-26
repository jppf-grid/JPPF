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
 * This class implements a smoothly changing profile.
 * @author Domingos Creado
 */
public class SmoothProfile extends AnnealingTuneProfile {

	/**
	 * This parameter defines the multiplicity used to define the range available to
	 * random generator, as the maximum.
	 * @return the value 1.5f.
	 * @see org.jppf.server.scheduler.bundle.AnnealingTuneProfile#getSizeRatioDevitation()
	 */
	float getSizeRatioDevitation() {
		return 1.5f; 
	}

	/**
	 * This parameter define how fast the bundler will stop generating random numbers. 
	 * @return the value 0.2f.
	 * @see org.jppf.server.scheduler.bundle.AnnealingTuneProfile#getDecreaseRatio()
	 */
	float getDecreaseRatio() {
		return 0.2f; //will make it 
	}

	/**
	 * Get the minimum number of samples that must be collected before an 
	 * analysis is triggered.
	 * @return the value 500.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#getMinSamplesToAnalyse()
	 */
	public long getMinSamplesToAnalyse() {
		return 500;
	}

	/**
	 * Get the the minimum number of samples to be collected before
	 * checking if the performance profile has changed. 
	 * @return the value 300.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#getMinSamplesToCheckConvergence()
	 */
	public long getMinSamplesToCheckConvergence() {
		return 300;
	}

	/**
	 * Get the percentage of deviation of the current mean to the mean 
	 * when the system was considered stable. 
	 * @return the value 0.2.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#getMaxDevtation()
	 */
	public double getMaxDevtation() {
		return 0.2;
	}

	/**
	 * Get the maximum number of guesses of number generated that were already tested
	 * for the algorithm to consider the best solution stable.
	 * @return the value 10.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#getMaxGuessToStable()
	 */
	public int getMaxGuessToStable() {
		return 10;
	}
}
