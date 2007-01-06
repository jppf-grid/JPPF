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
