package org.jppf.server.scheduler.bundle;

import org.jppf.server.JPPFStatsUpdater;


/**
 * This class provide a used defined bundle size strategy.
 * It uses the size defined by admin in property file or the 
 * size defined by admin application.
 * 
 * @author Domingos Creado
 */
public class FixedSizedBundler implements Bundler {

	public FixedSizedBundler(){
		log.info("Using user-defined bundle size");
	}
	public int getBundleSize() {
		return JPPFStatsUpdater.getStaticBundleSize();
	}
	public void feedback(int bundleSize, long totalTime) {
		//just ignored
	}

}
