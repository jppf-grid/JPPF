/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.utils;

import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;

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
	public static TypedProperties getBundleTunningParameters()
	{
		TypedProperties cfg = JPPFConfiguration.getProperties();
		String s = cfg.getString("task.bundle.strategy");
		if (s == null) return null;
		
		String profile = cfg.getString("task.bundle.autotuned.strategy", "smooth");
		TypedProperties params = new JPPFBundlerFactory().convertJPPFConfiguration(profile, cfg);
		params.put("strategy", s);
		return params;
	}
}
