/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

import static org.jppf.server.protocol.AdminRequestConstants.*;

import java.util.Map;

import org.jppf.server.*;
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
		if("autotuned".equalsIgnoreCase(props.getProperty("task.bundle.strategy")))
		{
			String profile = props.getProperty("task.bundle.autotuned.strategy");
			return createBundler(new AnnealingTuneProfile(profile));
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
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createBundler(AnnealingTuneProfile profile) {
		return createBundler(profile, false);
	}

	/**
	 * Instantiate a bundler, based on an annealing profile.
	 * @param profile a <code>AnnealingTuneProfile</code> instance.
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createBundler(AnnealingTuneProfile profile, boolean override) {
		return new AutoTunedBundler(profile);
	}

	/**
	 * Instantiate a bundler, based on an annealing profile.
	 * @param map a set of properties defining the bundler's parameters.
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler
	 */
	public static Bundler createBundler(Map<String, Object> map, boolean override)
	{
		Bundler bundler = null;
		boolean manual = "manual".equalsIgnoreCase((String) map.get(BUNDLE_TUNING_TYPE_PARAM));
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
			bundler = BundlerFactory.createBundler(prof, override);
			JPPFDriver.getInstance().getNodeNioServer().setBundler(bundler);
		}
		return bundler;
	}
}
