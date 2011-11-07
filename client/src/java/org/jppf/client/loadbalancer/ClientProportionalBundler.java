/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.client.loadbalancer;

import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.proportional.AbstractProportionalBundler;
import org.slf4j.*;

/**
 * Implementation of the &quot;proportional&quot; load-balancing algorithm, dedicated to
 * balancing the load between local and remote execution, when local execution is enabled in the client.
 * @author Laurent Cohen
 */
public class ClientProportionalBundler extends AbstractProportionalBundler
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ClientProportionalBundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Maximum size returned by this bundler.
	 */
	private int maxBundleSize = 1;

	/**
	 * Creates a new instance with the initial size of bundle as the start size.
	 * @param profile the parameters of the auto-tuning algorithm,
	 * grouped as a performance analysis profile.
	 */
	public ClientProportionalBundler(final LoadBalancingProfile profile)
	{
		super(profile);
	}

	/**
	 * Make a copy of this bundler
	 * @return a <code>Bundler</code> instance.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	@Override
	public Bundler copy()
	{
		return new ClientProportionalBundler(profile);
	}

	/**
	 * Get the max bundle size that can be used for this bundler.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
	 */
	@Override
	protected int maxSize()
	{
		return maxBundleSize;
	}

	/**
	 * Get the maximum size returned by this bundler.
	 * @param maxBundleSize the size as an int.
	 */
	public void setMaxSize(final int maxBundleSize)
	{
		this.maxBundleSize = maxBundleSize;
	}
}
