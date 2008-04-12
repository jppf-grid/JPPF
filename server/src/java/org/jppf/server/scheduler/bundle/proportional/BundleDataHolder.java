/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.server.scheduler.bundle.proportional;

import java.util.*;

import org.jppf.server.scheduler.bundle.BundlePerformanceSample;

/**
 * Each instance of this class acts as a container for the performance data related to a node,
 * and is used for the global computations of the
 * {@link org.jppf.server.scheduler.bundle.proportional.ProportionalBundler ProportionalBundler}. 
 * @author Laurent Cohen
 */
public class BundleDataHolder
{
	/**
	 * Holds the samples required for calculating the moving average.
	 */
	private LinkedList<BundlePerformanceSample> samples = new LinkedList<BundlePerformanceSample>();
	/**
	 * Current value of the moving average.
	 */
	private double mean = 0d;
	/**
	 * Current value of the moving average.
	 */
	private double totalTime = 0d;
	/**
	 * Current number of samples.
	 */
	private long nbSamples = 0L;
	/**
	 * Number of samples required to compute the moving average.
	 */
	private int maLength = 0;
	/**
	 * The bundle size associated with the bundler.
	 */
	private int bundleSize = 1;

	/**
	 * Initialize this data holder with the specified number of samples required to compute the moving average.
	 * @param maLength the number of samples as an int.
	 */
	public BundleDataHolder(int maLength)
	{
		this.maLength = maLength;
	}

	/**
	 * Add the specified sample to the list of samples.
	 * @param sample the sample to add.
	 * @return true if the new performance sample triggers a new analysis, false otherwise.
	 */
	public boolean addSample(BundlePerformanceSample sample)
	{
		boolean b = (sample.samples + nbSamples > maLength) || samples.isEmpty();
		if (b)
		{
			while ((sample.samples + nbSamples > maLength) && !samples.isEmpty())
			{
				removeHeadSample();
			}
		}
		samples.add(sample);
		totalTime += sample.samples * sample.mean;
		nbSamples += sample.samples;

		computeMean();
		return b;
	}

	/**
	 * Add the least recent sample from the list of samples.
	 */
	private void removeHeadSample()
	{
		BundlePerformanceSample sample = samples.removeFirst();
		nbSamples -= sample.samples;
		totalTime -= sample.samples * sample.mean;
	}

	/**
	 * Compute the mean time.
	 */
	private void computeMean()
	{
		if (nbSamples > 0) mean = totalTime / nbSamples;
	}

	/**
	 * Get the bundle size associated with the bundler.
	 * @return the bundle size as an int. 
	 */
	public int getBundleSize()
	{
		return bundleSize;
	}

	/**
	 * Set the bundle size associated with the bundler.
	 * @param bundleSize the bundle size as an int.
	 */
	public void setBundleSize(int bundleSize)
	{
		this.bundleSize = bundleSize;
	}

	/**
	 * Get the computed mean execution time for the corresponding node.
	 * @return  the mean value as a double.
	 */
	public double getMean()
	{
		return mean;
	}

	/**
	 * Get the number of samples required to compute the moving average.
	 * @return the number of samples as an int.
	 */
	public int getMaLength()
	{
		return maLength;
	}

	/**
	 * Set the number of samples required to compute the moving average.
	 * @param maLength the number of samples as an int.
	 */
	public void setMaLength(int maLength)
	{
		this.maLength = maLength;
	}
}
