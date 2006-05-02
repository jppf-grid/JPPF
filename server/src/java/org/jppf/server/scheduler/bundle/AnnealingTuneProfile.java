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
	 * @return
	 */
	abstract float getSizeRatioDevitation() ;
	
	/**
	 * This parameter define how fast does it will stop generating random numbers. 
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
	 * @return
	 */
	abstract float getDecreaseRatio() ;

	
	public int createDiff(int bestSize, int collectedSamples, Random rnd) {
		return (int) expDist(rnd.nextInt( Math.max(Math.round(bestSize * getSizeRatioDevitation()),1)),
				collectedSamples);
	}
	
	
	/**
	 * This method implement the always decreasing policy of the algorithm.
	 * The ratio define how fast does this instance will stop generating random
	 * numbers.
	 * 
	 * @param max the maximum that this algorithm will generate
	 * @param x 
	 * @return
	 */
	private double expDist(long max, long x) {
		return max * Math.pow(Math.E, -x * getDecreaseRatio());
	}

	

}
