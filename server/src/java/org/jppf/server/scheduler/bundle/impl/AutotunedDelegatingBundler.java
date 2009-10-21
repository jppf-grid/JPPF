/*
 * JPPF.
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

package org.jppf.server.scheduler.bundle.impl;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.*;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.autotuned.AnnealingTuneProfile;

/**
 * Instances of this bundler delegate their operations to a singleton instance of a
 * {@link org.jppf.server.scheduler.bundle.impl.AbstractAutoTunedBundler AutoTunedBundler}.
 * @author Laurent Cohen
 */
public class AutotunedDelegatingBundler extends AbstractBundler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AutotunedDelegatingBundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The global bundler to which bundle size calculations are delegated. 
	 */
	private static AutoTunedBundler simpleBundler = null;
	/**
	 * Used to synchronize multiple threads when creating the simple bundler.
	 */
	private static ReentrantLock lock = new ReentrantLock();
	/**
	 * Parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
	 */
	protected AnnealingTuneProfile profile;

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * @param overriden true if the settings were overriden by the node, false otherwise.
	 * grouped as a performance analysis profile.
	 */
	public AutotunedDelegatingBundler(AnnealingTuneProfile profile, boolean overriden)
	{
		super(profile, overriden);
		log.info("Bundler#" + bundlerNumber + ": Using Auto-Tuned bundle size");
		//log.info("Bundler#" + bundlerNumber + ": The initial size is " + bundleSize);
		lock.lock();
		try
		{
			if (simpleBundler == null)
			{
				simpleBundler = new AutoTunedBundler(profile, overriden);
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Make a copy of this bundler
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	public Bundler copy()
	{
		return new AutotunedDelegatingBundler(profile, overriden);
	}

	/**
	 * Get the current size of bundle.
	 * @return  the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize()
	{
		return simpleBundler.getBundleSize();
	}

	/**
	 * This method delegates the bundle size calculation to the singleton instance of <code>SimpleBundler</code>.
	 * @param bundleSize the number of tasks executed.
	 * @param totalTime the time in milliseconds it took to execute the tasks.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#feedback(int, double)
	 */
	public void feedback(int bundleSize, double totalTime)
	{
		simpleBundler.feedback(bundleSize, totalTime);
	}

	/**
	 * Get the max bundle size that can be used for this bundler.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
	 */
	protected int maxSize()
	{
		int max = 0;
		synchronized(simpleBundler)
		{
			max = simpleBundler.maxSize();
		}
		return max;
	}
}
