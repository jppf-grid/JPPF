/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.scheduler.bundle.rl;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.server.JPPFStatsUpdater;
import org.jppf.server.scheduler.bundle.*;

/**
 * Bundler based on a reinforcement learning algorithm.
 * @author Laurent Cohen
 */
public abstract class AbstractRLBundler extends AbstractBundler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractRLBundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The range of actions, expressed in terms of a percentage of increase/decrease of the bundle size.
	 */
	private static final int INCREASE_RANGE = 20;
	/**
	 * The incrementation step of the action.
	 */
	private static final int STEP = 1;
	/**
	 * The number of possible actions.
	 */
	private static final int NB_ACTIONS = 2 * INCREASE_RANGE + 1;
	/**
	 * List of all currently active bundlers. Should always be used within a <code>synchronized(bundlers)</code> statement.
	 */
	private static Set<AbstractRLBundler> bundlers = new HashSet<AbstractRLBundler>();
	//private static Set<BundleDataHolder> allDataHolders = new HashSet<BundleDataHolder>();
	/**
	 * Action to take.
	 */
	protected int action = INCREASE_RANGE;
	/**
	 * Index of the actrion taken.
	 */
	protected int actionIndex = -1;
	/**
	 * Parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
	 */
	protected RLProfile profile = null;
	/**
	 * Bounded memory of the past performance updates.
	 */
	protected BundleDataHolder dataHolder = null;
	/**
	 * The current bundle size.
	 */
	protected int bundleSize = 1;
	/**
	 * The previous bundle size.
	 */
	protected int prevBundleSize = 1;
	/**
	 * Value of the utility function.
	 */
	protected double[] utilities = initUtilities();
	//private double[] utilities = { 2.0, 1.0, 1.0 };
	/**
	 * Value of the max expected utility function.
	 */
	protected double[] maxUtilities = initMaxUtilities();
	//private double[] maxUtilities = { 1.0, 1.0, 1.0 };
	/**
	 * Pseudo-random number generator.
	 */
	protected Random rand = new Random(System.currentTimeMillis());
	/**
	 * 
	 */
	protected BundlerStats stats[] = { new BundlerStats(), new BundlerStats() };

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * grouped as a performance analysis profile.
	 */
	public AbstractRLBundler(AutoTuneProfile profile, boolean override)
	{
		log.info("Bundler#" + bundlerNumber + ": Using Auto-Tuned bundle size");
		this.override = override;
		int bundleSize = JPPFStatsUpdater.getStaticBundleSize();
		if (bundleSize < 1) bundleSize = 1;
		log.info("Bundler#" + bundlerNumber + ": The initial size is " + bundleSize);
		this.profile = (RLProfile) profile;
		this.dataHolder = new BundleDataHolder(this.profile.getPerformanceCacheSize());
	}

	/**
	 * Get the current size of bundle.
	 * @return  the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize()
	{
		return bundleSize;
	}

	/**
	 * This method computes the bundle size based on the new state of the server.
	 * @param size the number of tasks executed.
	 * @param totalTime the time in milliseconds it took to execute the tasks.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#feedback(int, double)
	 */
	public void feedback(int size, double totalTime)
	{
		if (size <= 0) return;
		BundlePerformanceSample sample = new BundlePerformanceSample((double) totalTime / (double) size, size);
		dataHolder.addSample(sample);

		double d = dataHolder.getPreviousMean() - dataHolder.getMean();
		int n = bundleSize - prevBundleSize;
		prevBundleSize = bundleSize;
		if (d < 0)
		{
			action += Math.signum(action) * STEP;
		}
		else if (d > 0)
		{
			action = (int) -Math.signum(action) * Math.max(STEP, Math.abs(action/2));
		}
		if (action > INCREASE_RANGE) action = INCREASE_RANGE;
		else if (action < -INCREASE_RANGE) action = -INCREASE_RANGE;
		bundleSize += action;
		int max = maxSize();
		if (bundleSize <= 0) bundleSize = 1;
		else if (bundleSize > max) bundleSize = max;
		if (debugEnabled)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("bundler #").append(getBundlerNumber()).append(" : size=").append(getBundleSize());
			sb.append(", ").append(getDataHolder());
			log.debug(sb.toString());
		}
	}

	/**
	 * This method computes the value of the reward based on the current and previous bundleSize and mean execution time.
	 * @param size the number of tasks executed.
	 * @param totalTime the time in milliseconds it took to execute the tasks.
	 * @return the reward to use in the reinforcment learning algorithm equations.
	 */
	private double computeReward(int size, double totalTime)
	{
		int n = maxSize();
		synchronized(bundlers)
		{
			getAllBundleStats(stats[0]);
			dataHolder.addSample(new BundlePerformanceSample(totalTime, size));
			getAllBundleStats(stats[1]);
		}
		// assign a positive reward if the global mean has decreased, negative otherwise
		double r = stats[1].sum - stats[0].sum < 0 ? 1d : -1d;
		double r2 = 0d;
		// encourage bundler size to be smaller than the max size of the bundles in the queue
		int sizeDiff = stats[1].sizeSum - stats[0].sizeSum;
		if (stats[1].sizeSum > n)
		{
			if (sizeDiff > 0) r2 = -2d;
			else
			{
				//if ((bundleSize >= n) && (sizeDiff < 0)) r2 = 0.5;
			}
			if (bundleSize >= n)
			{
				// bias towards decreasing the bundler size
				if (bundleSize < prevBundleSize) r2 += 0.5d;
				else r2 -= 10d;
			}
		}
		else if (stats[1].sizeSum == n) r2 = 1d;
		else r2 = 0.5d;
		//if (bundleSize >= n) r2 -= 1d;
		/*
		if (bundleSize < n) r2 = 1d;
		else
		{
			// bias towards decreasing the bundler size
			if (bundleSize < prevBundleSize) r2 = 0.5d;
			else r2 = -10d;
		}
		*/
		/*
		if (bundleSize >= n)
		{
			// bias towards decreasing the bundler size
			if (bundleSize < prevBundleSize) r2 = 0.5d;
			else r2 = -10d;
		}
		*/
		return r + r2;
	}

	/**
	 * Perform context-independant initializations.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#setup()
	 */
	public void setup()
	{
		synchronized(bundlers)
		{
			//allDataHolders.add(this.dataHolder);
			bundlers.add(this);
		}
	}
	
	/**
	 * Release the resources used by this bundler.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#dispose()
	 */
	public void dispose()
	{
		synchronized(bundlers)
		{
			//allDataHolders.remove(this.dataHolder);
			bundlers.remove(this);
		}
		dataHolder = null;
	}

	/**
	 * Get a string representation of an array of double values.
	 * @param array the array to convert top a string.
	 * @return a string representation of the array.
	 */
	private String dumpArray(double[] array)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i=0; i<array.length; i++)
		{
			if (i> 0) sb.append(", ");
			sb.append(array[i]);
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Get the bounded memory of the past performance updates.
	 * @return a BundleDataHolder instance.
	 */
	public BundleDataHolder getDataHolder()
	{
		return dataHolder;
	}

	/**
	 * Gather statistics on all current bundles.
	 * @return an <code>AllBundleStats</code> instance.
	 */
	private BundlerStats getAllBundleStats()
	{
		return getAllBundleStats(new BundlerStats());
	}

	/**
	 * Gather statistics on all current bundles.
	 * @param stats the object to fill with computed statistics.
	 * @return an <code>AllBundleStats</code> instance.
	 */
	private BundlerStats getAllBundleStats(BundlerStats stats)
	{
		stats.sum = 0;
		stats.sizeSum = 0;
		/*
		for (BundleDataHolder holder: allDataHolders)
		{
			stats.sum += holder.getMean();
		}
		*/
		for (AbstractRLBundler bundler: bundlers)
		{
			BundleDataHolder holder = bundler.getDataHolder();
			stats.sum += holder.getMean();
			stats.sizeSum += bundler.getBundleSize();
		}
		return stats;
	}

	/**
	 * Structure holding consolidated statistics for all current bundles.
	 */
	private static class BundlerStats
	{
		/**
		 * Sum of all mean values.
		 */
		public double sum = 0d;
		/**
		 * Sum of all bundler sizes.
		 */
		public int sizeSum = 0;
	}

	/**
	 * Initialize the array of utility values.
	 * @return an array of double values.
	 */
	private double[] initUtilities()
	{
		double[] result = new double[NB_ACTIONS];
		for (int i=0; i<NB_ACTIONS; i++)
		{
			//result[i] = (i == NB_ACTIONS - 1) ? 2.0 : 1.0;
			result[i] = i - INCREASE_RANGE;
		}
		return result;
	}

	/**
	 * Initialize the array of utility values.
	 * @return an array of double values.
	 */
	private double[] initMaxUtilities()
	{
		double[] result = new double[NB_ACTIONS];
		for (int i=0; i<NB_ACTIONS; i++) result[i] = 1.0;
		return result;
	}
}
