/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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

import org.apache.log4j.Logger;

/**
 * This is the interface of all strategies for defining bundle task size.
 * A Bundler define the current bundle size using different ways, 
 * can it be fixed or auto-tunned.
 * 
 * @author Domingos Creado
 */
public interface Bundler {
	
	/**
	 * Log4j logger for this class.
	 */
	Logger LOG = Logger.getLogger(Bundler.class);
	
	/**
	 * Get the current size of bundle.
	 * @return  the bundle size as an int value.
	 */
	int getBundleSize();
	
	/**
	 * feedback the bundler with the result of using the bundle
	 * with the specified size.
	 * 
	 * @param bundleSize the bundle size used
	 * @param totalTime the total time considering the transmission and execution.
	 */
	void feedback(int bundleSize, long totalTime) ;

	/**
	 * Make a copy of this bundler.
	 * Wich parts are actually copied depends on the implementation.
	 * @return a new <code>Bundler</code> instance.
	 */
	Bundler copy();
	/**
	 * Get the timestamp at which this bundler was created.
	 * This is used to enable node channels to know when the bundler settings have changed.
	 * @return the timestamp as a long value.
	 */
	long getTimestamp();
}
