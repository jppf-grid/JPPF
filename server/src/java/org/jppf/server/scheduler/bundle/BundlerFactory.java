package org.jppf.server.scheduler.bundle;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

public final class BundlerFactory {

	
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
