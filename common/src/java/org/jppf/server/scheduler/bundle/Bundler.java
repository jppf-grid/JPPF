/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import org.apache.commons.logging.*;

/**
 * This is the interface of all strategies for defining bundle task size.
 * A Bundler define the current bundle size using different ways, 
 * can it be fixed or auto-tunned.
 * 
 * @author Domingos Creado
 */
public interface Bundler {
	
	/**
	 * Logger for this class.
	 */
	Log LOG = LogFactory.getLog(Bundler.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	boolean DEBUG_ENABLED = LOG.isDebugEnabled();

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
	void feedback(int bundleSize, double totalTime) ;

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
	/**
	 * Get the  override indicator.
	 * @return true if the settings were overriden by the node, false otherwise.
	 */
	boolean isOverride();
	/**
	 * Release the resources used by this bundler.
	 */
	void dispose();
	/**
	 * Perform context-independant initializations.
	 */
	void setup();
}
