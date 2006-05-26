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

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

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
	public static Bundler createBundler(){
		TypedProperties props = JPPFConfiguration.getProperties();
		if("autotuned".equalsIgnoreCase(props.getProperty("task.bundle.strategy"))){
			AutoTuneProfile profile;
			if("smooth".equalsIgnoreCase(props.getProperty("task.bundle.autotuned.strategy"))){
				profile = new SmoothProfile();
			} else {
				//until there is not other profile
				profile = new SmoothProfile();
			}
			return new AutoTunedBundler(profile);
		} 
		return new FixedSizedBundler();
	}
}
