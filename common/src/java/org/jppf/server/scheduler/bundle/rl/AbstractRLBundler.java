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
	private static final int INCREASE_RANGE = 50;
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
	//private static Set<AbstractRLBundler> bundlers = new HashSet<AbstractRLBundler>();
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
	 * Pseudo-random number generator.
	 */
	protected Random rand = new Random(System.currentTimeMillis());

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * grouped as a performance analysis profile.
	 */
	public AbstractRLBundler(AutoTuneProfile profile, boolean override)
	{
		log.info("Bundler#" + bundlerNumber + ": Using Reinforcement Learning bundle size");
		this.override = override;
		this.profile = (RLProfile) profile;
		log.info("Bundler#" + bundlerNumber + ": The initial size is " + bundleSize +
			", performanceVariationThreshold = " + this.profile.getPerformanceVariationThreshold());
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
	 * set the current size of bundle.
	 * @param bundleSize - the bundle size as an int value.
	 */
	public void setBundleSize(int bundleSize)
	{
		this.bundleSize = bundleSize;
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
		double threshold = profile.getPerformanceVariationThreshold() * dataHolder.getPreviousMean();
		int n = bundleSize - prevBundleSize;
		prevBundleSize = bundleSize;
		if (d < -threshold)
		{
			action += (int) Math.signum(action) * STEP;
		}
		else if (d > threshold)
		{
			action = (int) -Math.signum(action) * Math.max(STEP, Math.abs(action/2));
		}
		//else action = (int) -Math.signum(d) * (int) Math.signum(action) * STEP;
		else action = 0;
		if (action > INCREASE_RANGE) action = INCREASE_RANGE;
		else if (action < -INCREASE_RANGE) action = -INCREASE_RANGE;
		bundleSize += action;
		//int max = Math.max(1, maxSize());
		int max = maxSize();
		if (bundleSize > max) bundleSize = max;
		if (bundleSize <= 0) bundleSize = 1;
		if (debugEnabled)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("bundler #").append(getBundlerNumber()).append(" : size=").append(getBundleSize());
			sb.append(", ").append(getDataHolder());
			log.debug(sb.toString());
		}
	}

	/**
	 * Perform context-independant initializations.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#setup()
	 */
	public void setup()
	{
	}
	
	/**
	 * Release the resources used by this bundler.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#dispose()
	 */
	public void dispose()
	{
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
}
