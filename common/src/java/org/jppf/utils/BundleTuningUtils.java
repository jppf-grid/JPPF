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

package org.jppf.utils;

import static org.jppf.server.protocol.BundleParameter.*;

import java.util.*;

import org.jppf.server.protocol.BundleParameter;

/**
 * This class provides a set of utility methods for accessing and operating on bundle tuning. 
 * @author Laurent Cohen
 */
public final class BundleTuningUtils
{
	/**
	 * Instantiation of this class is not permitted.
	 */
	private BundleTuningUtils()
	{
	}

	/**
	 * Get a configured bundle size tuning profile form the configuration file.
	 * @return an <code>AnnealingTuneProfile</code> instance, or null if no profile was configured.
	 */
	public static Map<BundleParameter, Object> getBundleTunningParameters()
	{
		TypedProperties cfg = JPPFConfiguration.getProperties();
		String s = cfg.getString("task.bundle.strategy");
		if (s == null) return null;
		
		Map<BundleParameter, Object> params = new HashMap<BundleParameter, Object>();
		params.put(BUNDLE_TUNING_TYPE_PARAM, s);
		params.put(BUNDLE_SIZE_PARAM, cfg.getInt("task.bundle.size", 10));
		if ("autotuned".equalsIgnoreCase(s))
		{
			String profile = cfg.getString("task.bundle.autotuned.strategy", "smooth");
			String prefix = "strategy." + profile + ".";
			params.put(MIN_SAMPLES_TO_ANALYSE, cfg.getInt(prefix + "minSamplesToAnalyse", 500));
			params.put(MIN_SAMPLES_TO_CHECK_CONVERGENCE, cfg.getInt(prefix + "minSamplesToCheckConvergence", 300));
			params.put(MAX_DEVIATION, cfg.getDouble(prefix + "maxDeviation", 0.2d));
			params.put(MAX_GUESS_TO_STABLE, cfg.getInt(prefix + "maxGuessToStable", 10));
			params.put(SIZE_RATIO_DEVIATION, cfg.getFloat(prefix + "sizeRatioDeviation", 1.5f));
			params.put(DECREASE_RATIO, cfg.getFloat(prefix + "decreaseRatio", 0.2f));
		}
		return params;
	}
}
