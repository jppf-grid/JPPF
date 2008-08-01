/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
package org.jppf.server.scheduler.bundle;

import org.jppf.server.JPPFStatsUpdater;


/**
 * This class provide a used defined bundle size strategy.
 * It uses the size defined by admin in property file or the 
 * size defined by admin application.
 * 
 * @author Domingos Creado
 */
public class FixedSizedBundler extends AbstractBundler
{
	/**
	 * The fixed bundle size such as specified by the node.
	 * If the node doesn't specifiy a size, the size provided by the driver is used.
	 */
	private int overrideSize = -1;

	/**
	 * Initialize this bundler.
	 */
	public FixedSizedBundler(){
		LOG.info("Using user-defined bundle size");
	}

	/**
	 * Initialize this bundler.
	 * @param overrideSize the node-defined (override) size.
	 */
	public FixedSizedBundler(int overrideSize){
		this.overrideSize = overrideSize;
		override = true;
		LOG.info("Using node-overriden bundle size: " + overrideSize);
	}

	/**
	 * This method always returns a statically assigned bundle size.
	 * @return the bundle size defined in the JPPF driver configuration.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getBundleSize()
	 */
	public int getBundleSize() {
		if (override) return overrideSize;
		return JPPFStatsUpdater.getStaticBundleSize();
	}

	/**
	 * Make a copy of this bundler.
	 * @return a reference to this bundler, no copy is actually made.
	 * @see org.jppf.server.scheduler.bundle.Bundler#copy()
	 */
	public Bundler copy()
	{
		return this;
	}

	/**
	 * Get the max bundle size that can be used for this bundler.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.scheduler.bundle.AbstractBundler#maxSize()
	 */
	protected int maxSize()
	{
		return -1;
	}
}
