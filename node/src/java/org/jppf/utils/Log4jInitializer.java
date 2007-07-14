/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
package org.jppf.utils;

import java.net.URL;
import org.apache.log4j.*;

/**
 * This class provides a way to configure log4j when the ocnfiguration file is not on the local file system.
 * @author Laurent Cohen
 */
public class Log4jInitializer
{
	/**
	 * Set to true once initialization is complete.
	 */
	private static boolean initialized = false;

	/**
	 * Configure log4j from a configuration file located in the classpath.
	 * @param path the path to the configuration file.
	 */
	public static void configureFromClasspath(String path)
	{
		if (initialized) return;
		LogManager.resetConfiguration();
		URL url = Log4jInitializer.class.getClassLoader().getResource(path);
		PropertyConfigurator.configure(url);
		initialized = true;
	}
}
