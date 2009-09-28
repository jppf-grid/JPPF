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

package org.jppf.server.scheduler.bundle;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract implementation of the bundler interface.
 * @author Laurent Cohen
 */
public abstract class AbstractBundler implements Bundler
{
	/**
	 * Count of the bundlers used to generate a readable unique id.
	 */
	private static AtomicInteger bundlerCount = new AtomicInteger(0);
	/**
	 * The bundler number for this bundler.
	 */
	protected int bundlerNumber = incBundlerCount();
	/**
	 * The creation timestamp for this bundler.
	 */
	protected long timestamp = System.currentTimeMillis();
	/**
	 * The override indicator.
	 */
	protected boolean overriden = false;
	/**
	 * Parameters of the algorithm, grouped as a performance analysis profile.
	 */
	protected LoadBalancingProfile profile;

	/**
	 * Default constructor.
	 */
	private AbstractBundler()
	{
	}

	/**
	 * Creates a new instance with the specified parameters profile and overriden flag.
	 * @param profile the parameters of the load-balancing algorithm,
	 * @param overriden true if the settings were overriden by the node, false otherwise.
	 */
	public AbstractBundler(LoadBalancingProfile profile, boolean overriden)
	{
		this.profile = profile;
		this.overriden = overriden;
	}

	/**
	 * Increment the bundlers count by one.
	 * @return the new count as an int value.
	 */
	private static int incBundlerCount()
	{
		return bundlerCount.incrementAndGet();
	}

	/**
	 * Get the max bundle size that can be used for this bundler.
	 * @return the bundle size as an int.
	 */
	protected abstract int maxSize();

	/**
	 * This method does nothing and should be overriden in subclasses.
	 * @param bundleSize not used.
	 * @param totalTime not used.
	 * @see org.jppf.server.scheduler.bundle.Bundler#feedback(int, double)
	 */
	public void feedback(int bundleSize, double totalTime)
	{
	}

	/**
	 * Get the timestamp at which this bundler was created.
	 * This is used to enable node channels to know when the bundler settings have changed.
	 * @return the timestamp as a long value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getTimestamp()
	 */
	public long getTimestamp()
	{
		return timestamp;
	}

	/**
	 * Get the  override indicator.
	 * @return true if the settings were overriden by the node, false otherwise.
	 * @see org.jppf.server.scheduler.bundle.Bundler#isOverriden()
	 */
	public boolean isOverriden()
	{
		return overriden;
	}

	/**
	 * Set the  override indicator.
	 * @param override true if the settings were overriden by the node, false otherwise.
	 */
	public void setOverriden(boolean override)
	{
		this.overriden = override;
	}

	/**
	 * Get the bundler number for this bundler.
	 * @return the bundler number as an int.
	 */
	public int getBundlerNumber()
	{
		return bundlerNumber;
	}

	/**
	 * Perform context-independant initializations.
	 * @see org.jppf.server.scheduler.bundle.Bundler#setup()
	 */
	public void setup()
	{
	}

	/**
	 * Release the resources used by this bundler.
	 * @see org.jppf.server.scheduler.bundle.Bundler#dispose()
	 */
	public void dispose()
	{
	}

	/**
	 * Get the parameters of the algorithm, grouped as a performance analysis profile.
	 * @return an instance of <code>LoadBalancingProfile</code>.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getProfile()
	 */
	public LoadBalancingProfile getProfile()
	{
		return profile;
	}
}
