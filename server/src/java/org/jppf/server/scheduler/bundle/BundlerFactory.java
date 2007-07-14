/*
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
package org.jppf.server.scheduler.bundle;

import static org.jppf.server.protocol.BundleParameter.*;

import java.util.Map;

import org.jppf.server.*;
import org.jppf.server.protocol.BundleParameter;
import org.jppf.utils.*;

/**
 * Instances of this class implement the Factory pattern for creating
 * <code>Bundler</code> instances.
 * @author Domingos Creado
 */
public final class BundlerFactory {
	/**
	 * Instantiate a bundler, based on theJPPF driver configuration properties.
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createBundler()
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String algorithm = props.getProperty("task.bundle.strategy");
		if(!"manual".equalsIgnoreCase(algorithm))
		{
			String profile = props.getProperty("task.bundle.autotuned.strategy");
			return createBundler(new AnnealingTuneProfile(profile), algorithm);
		} 
		return new FixedSizedBundler();
	}

	/**
	 * Instantiate a fixed size bundler, based on a user-defined bundle size.
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createFixedSizeBundler() {
		return new FixedSizedBundler();
	}

	/**
	 * Instantiate a fixed size bundler, based on a node-defined bundle size.
	 * @param overrideSize the node-defined (override) size.
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createFixedSizeBundler(int overrideSize) {
		return new FixedSizedBundler(overrideSize);
	}

	/**
	 * Instantiate a bundler, based on an annealing profile.
	 * @param profile a <code>AnnealingTuneProfile</code> instance.
	 * @param algorithm a <code>AnnealingTuneProfile</code> instance.
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createBundler(AnnealingTuneProfile profile, String algorithm) {
		return createBundler(profile, false, algorithm);
	}

	/**
	 * Instantiate a bundler, based on an annealing profile.
	 * @param profile a <code>AnnealingTuneProfile</code> instance.
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * @param algorithm a <code>AnnealingTuneProfile</code> instance.
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createBundler(AnnealingTuneProfile profile, boolean override, String algorithm) {
		if ("resilient".equalsIgnoreCase(algorithm)) return new ResilientBundler(profile);
		return new AutoTunedBundler(profile);
	}

	/**
	 * Instantiate a bundler, based on an annealing profile.
	 * @param map a set of properties defining the bundler's parameters.
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createBundler(Map<BundleParameter, Object> map, boolean override)
	{
		Bundler bundler = null;
		String algorithm = (String) map.get(BUNDLE_TUNING_TYPE_PARAM);
		boolean manual = "manual".equalsIgnoreCase(algorithm);
		if (manual)
		{
			Number n = (Number) map.get(BUNDLE_SIZE_PARAM);
			if (n == null) n = JPPFStatsUpdater.getStaticBundleSize();
			bundler = createFixedSizeBundler(n.intValue());
		}
		else
		{
			AnnealingTuneProfile prof = new AnnealingTuneProfile();
			Number n = (Number) map.get(MIN_SAMPLES_TO_ANALYSE);
			prof.setMinSamplesToAnalyse(n.longValue());
			n = (Number) map.get(MIN_SAMPLES_TO_CHECK_CONVERGENCE);
			prof.setMinSamplesToCheckConvergence(n.longValue());
			n = (Number) map.get(MAX_DEVIATION);
			prof.setMaxDeviation(n.doubleValue());
			n = (Number) map.get(MAX_GUESS_TO_STABLE);
			prof.setMaxGuessToStable(n.intValue());
			n = (Number) map.get(SIZE_RATIO_DEVIATION);
			prof.setSizeRatioDeviation(n.floatValue());
			n = (Number) map.get(DECREASE_RATIO);
			prof.setDecreaseRatio(n.floatValue());
			bundler = createBundler(prof, override, algorithm);
			JPPFDriver.getInstance().getNodeNioServer().setBundler(bundler);
		}
		return bundler;
	}
}
