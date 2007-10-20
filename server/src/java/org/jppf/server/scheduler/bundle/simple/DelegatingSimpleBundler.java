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

package org.jppf.server.scheduler.bundle.simple;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.*;
import org.jppf.server.JPPFStatsUpdater;
import org.jppf.server.scheduler.bundle.*;

/**
 * 
 * @author Laurent Cohen
 */
public class DelegatingSimpleBundler extends AbstractBundler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(DelegatingSimpleBundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The global bundler to which bundle size calculations are delegated. 
	 */
	private static SimpleBundler simpleBundler = null;
	/**
	 * Used to synchronize multiple threads when creating the simple bundler.
	 */
	private static ReentrantLock lock = new ReentrantLock();
	/**
	 * Parameters of the auto-tuning algorithm, grouped as a performance analysis profile.
	 */
	protected AutoTuneProfile profile;
	/**
	 * The current bunlde size.
	 */
	private int bundleSize = 1;

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * @param override true if the settings were overriden by the node, false otherwise.
	 * grouped as a performance analysis profile.
	 */
	public DelegatingSimpleBundler(AutoTuneProfile profile, boolean override)
	{
		log.info("Bundler#" + bundlerNumber + ": Using Auto-Tuned bundle size");
		this.override = override;
		int bundleSize = JPPFStatsUpdater.getStaticBundleSize();
		if (bundleSize < 1)
		{
			bundleSize = 1;
		}
		log.info("Bundler#" + bundlerNumber + ": The initial size is " + bundleSize);
		this.profile = profile;
		lock.lock();
		try
		{
			if (simpleBundler == null)
			{
				simpleBundler = new SimpleBundler(profile, override);
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
		return new DelegatingSimpleBundler(profile, override);
	}

	/**
	 * Get the current size of bundle.
	 * @return  the bundle size as an int value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize()
	{
		return simpleBundler.getBundleSize(this);
	}

	/**
	 * This method delegates the bundle size calculation to the singleton instance of <code>SimpleBundler</code>.
	 * @param bundleSize the number of tasks executed.
	 * @param totalTime the time in milliseconds it took to execute the tasks.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#feedback(int, double)
	 */
	public void feedback(int bundleSize, double totalTime)
	{
		simpleBundler.feedback(this, new BundlePerformanceSample(totalTime, bundleSize));
	}

	/**
	 * Release the resources used by this bundler.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#dispose()
	 */
	public void dispose()
	{
		simpleBundler.removeBundler(this);
	}
}
