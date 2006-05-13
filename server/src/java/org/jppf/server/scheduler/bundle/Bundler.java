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
	 * @return the current size of bundle
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
	
}
