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
}
