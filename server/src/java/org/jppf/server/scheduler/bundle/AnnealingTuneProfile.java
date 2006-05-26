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
 * This class implements the basis of a profile based on simulated annealing 
 * strategy. The possible move from the best known solution get smaller each
 * time it make a move.
 * This strategy let the algorithm explore the universe of bundle size with 
 * a almost known end. Check method getDecreaseRatio about the maximum number
 * of changes.
 *  
 * @author Domingos Creado
 */
public abstract class AnnealingTuneProfile implements AutoTuneProfile{
	
	/**
	 * This parameter defines the multiplicity used to define the range available to
	 * random generator, as the maximum.
	 * @return the multiplicity as a float value.
	 */
	abstract float getSizeRatioDevitation() ;
	
	/**
	 * This parameter defines how fast does it will stop generating random numbers. 
	 * This is essential to define what is the size of the universe will be explored. 
	 * Greater numbers make the algorithm stop sooner.
	 * Just as example, if the best solution is between 0-100, the following might
	 * occur:
	 *   1    => 5 max guesses
	 *   2    => 2 max guesses
	 *   0.5  => 9 max guesses
	 *   0.1  => 46 max guesses
	 *   0.05 => 96 max guesses
	 *   
	 * This expected number of guesses might not occur if the number of getMaxGuessToStable()
	 * is short.
	 * @return the decrease rate as a float value.
	 */
	abstract float getDecreaseRatio() ;
	
	/**
	 * Generate a difference to be applied to the best known bundle size.
	 * @param bestSize the known best size of bundle.
	 * @param collectedSamples the number of samples that were already collected.
	 * @param rnd a pseudo-random number generator.
	 * @return an always positive diff to be applied to bundle size
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#createDiff(int, int, java.util.Random)
	 */
	public int createDiff(int bestSize, int collectedSamples, Random rnd) {
		long max = rnd.nextInt(Math.max(Math.round(bestSize * getSizeRatioDevitation()), 1));
		return (int) expDist(max, collectedSamples);
	}
	
	/**
	 * This method implements the always decreasing policy of the algorithm.
	 * The ratio define how fast this instance will stop generating random
	 * numbers.
	 * The calculation is performed as max * exp(-x * getDecreaseRatio()).
	 * 
	 * @param max the maximum value this algorithm will generate.
	 * @param x a randomly generated bundle size increment.
	 * @return an int value.
	 */
	private double expDist(long max, long x) {
		return max * Math.pow(Math.E, -x * getDecreaseRatio());
	}

	

}
