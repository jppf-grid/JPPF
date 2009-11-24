/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.scheduler.bundle.rl;

import java.util.concurrent.atomic.AtomicReference;

import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.autotuned.AbstractAutoTuneProfile;
import org.jppf.utils.*;

/**
 * Paremeters profile for a proportional bundler. 
 * @author Laurent Cohen
 */
public class RLProfile extends AbstractAutoTuneProfile
{
	/**
	 * A default profile with default parameter values.
	 */
	private static AtomicReference<RLProfile> defaultProfile = new AtomicReference<RLProfile>(new RLProfile());
	/**
	 * The maximum szie of the performance samples cache.
	 */
	private int performanceCacheSize = 2000;
	/**
	 * The bundle size increase rate.
	 */
	private double increaseRate = 0.1;
	/**
	 * The utility function's discount factor.
	 */
	private double discountFactor = 0.9;
	/**
	 * The utility function's rate of change.
	 */
	private double rateOfChange = 0.9;

	/**
	 * Initialize this profile with default parameters.
	 */
	public RLProfile()
	{
	}

	/**
	 * Initialize this profile with values read from the configuration file.
	 * @param profileName name of the profile in the configuration file.
	 */
	public RLProfile(String profileName)
	{
		String prefix = "strategy." + profileName + ".";
		TypedProperties props = JPPFConfiguration.getProperties();
		performanceCacheSize = props.getInt(prefix + "performanceCacheSize", 2000);
		increaseRate = props.getDouble(prefix + "increaseRate", 0.1);
		discountFactor = props.getDouble(prefix + "discountFactor", 0.9);
		rateOfChange = props.getDouble(prefix + "rateOfChange", 0.9);
	}

	/**
	 * Make a copy of this profile.
	 * @return a new <code>AutoTuneProfile</code> instance.
	 * @see org.jppf.server.scheduler.bundle.AutoTuneProfile#copy()
	 */
	public AutoTuneProfile copy()
	{
		RLProfile other = new RLProfile();
		other.setPerformanceCacheSize(performanceCacheSize);
		other.setIncreaseRate(increaseRate);
		return other;
	}

	/**
	 * Get the maximum size of the performance samples cache.
	 * @return the cache size as an int.
	 */
	public int getPerformanceCacheSize()
	{
		return performanceCacheSize;
	}

	/**
	 * Set the maximum size of the performance samples cache.
	 * @param performanceCacheSize the cache size as an int.
	 */
	public void setPerformanceCacheSize(int performanceCacheSize)
	{
		this.performanceCacheSize = performanceCacheSize;
	}

	/**
	 * Get the proportionality factor.
	 * @return the factor as a double.
	 */
	public double getIncreaseRate()
	{
		return increaseRate;
	}

	/**
	 * Set the increase rate.
	 * @param increaseRate the factor as a double.
	 */
	public void setIncreaseRate(double increaseRate)
	{
		this.increaseRate = increaseRate;
	}

	/**
	 * Get the default profile with default parameter values.
	 * @return a <code>ProportionalTuneProfile</code> singleton instance.
	 */
	public static RLProfile getDefaultProfile()
	{
		return defaultProfile.get();
	}

	/**
	 * Get the utility function's discount factor.
	 * @return the discount factor as a double.
	 */
	public double getDiscountFactor()
	{
		return discountFactor;
	}

	/**
	 * Set the utility function's discount factor.
	 * @param discountFactor the discount factor as a double.
	 */
	public void setDiscountFactor(double discountFactor)
	{
		this.discountFactor = discountFactor;
	}

	/**
	 * Get the utility function's rate of change.
	 * @return the rate of change as a double.
	 */
	public double getRateOfChange()
	{
		return rateOfChange;
	}

	/**
	 * Set the utility function's rate of change.
	 * @param rateOfChange the rate of change as a double.
	 */
	public void setRateOfChange(double rateOfChange)
	{
		this.rateOfChange = rateOfChange;
	}
}
