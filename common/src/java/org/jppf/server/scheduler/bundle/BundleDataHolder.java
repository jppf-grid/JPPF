/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.scheduler.bundle;

import java.util.*;


/**
 * Each instance of this class acts as a container for the performance data related to a node.
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
	 * Previous value of the moving average, generally before a new sample was added.
	 */
	private double previousMean = 0d;
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
	private int performanceCacheSize = 0;

	/**
	 * Initialize this data holder with the maximum number of samples in the performance cache.
	 * @param performanceCacheSize - the number of samples as an int.
	 */
	public BundleDataHolder(int performanceCacheSize)
	{
		this.performanceCacheSize = performanceCacheSize;
	}

	/**
	 * Add the specified performance sample to the list of samples.
	 * @param sample - the performance sample to add.
	 */
	public void addSample(BundlePerformanceSample sample)
	{
		boolean b = (sample.samples + nbSamples > performanceCacheSize) || samples.isEmpty();
		if (b)
		{
			while ((sample.samples + nbSamples > performanceCacheSize) && !samples.isEmpty())
			{
				removeHeadSample();
			}
		}
		samples.add(sample);
		totalTime += sample.samples * sample.mean;
		nbSamples += sample.samples;

		computeMean();
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
		if (nbSamples > 0)
		{
			previousMean = mean;
			mean = totalTime / nbSamples;
		}
	}

	/**
	 * Get the computed mean execution time for the corresponding node.
	 * @return the mean value as a double.
	 */
	public double getMean()
	{
		return mean;
	}

	/**
	 * Get the computed mean execution time for the corresponding node.
	 * @param mean - the mean value as a double.
	 */
	public void setMean(double mean)
	{
		this.mean = mean;
	}

	/**
	 * Get the previously computed mean execution time for the corresponding node.
	 * @return the previous mean value as a double.
	 */
	public double getPreviousMean()
	{
		return previousMean;
	}

	/**
	 * Get the number of samples required to compute the moving average.
	 * @return the number of samples as an int.
	 */
	public int getPerformanceCacheSize()
	{
		return performanceCacheSize;
	}

	/**
	 * Set the number of samples required to compute the moving average.
	 * @param performanceCacheSize - the number of samples as an int.
	 */
	public void setPerformanceCacheSize(int performanceCacheSize)
	{
		this.performanceCacheSize = performanceCacheSize;
	}

	/**
	 * Get a string representation of this bundler data holder.
	 * @return a string representing the state of this object.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("mean = ").append(mean).append(", previousMean = ").append(previousMean);
		sb.append(", totalTime = ").append(totalTime).append(", nbSamples = ").append(nbSamples);
		sb.append(", maLength = ").append(performanceCacheSize).append(", samples.size() = ").append(samples.size());
		return sb.toString();
	}

	/**
	 * Get the current number of samples.
	 * @return the number of samples as an int.
	 */
	public long getNbSamples()
	{
		return nbSamples;
	}
}
