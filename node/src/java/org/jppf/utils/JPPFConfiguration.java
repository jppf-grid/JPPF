/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
package org.jppf.utils;

import java.io.*;

/**
 * Utility class for loading and accessing the JPPF configuration properties.
 * <p>The configuration file path is set through the system property {@link org.jppf.utils#CONFIG_PROPERTY CONFIG_PROPERTY},
 * whose value is &quot;jppf.config&quot;.<br>
 * As an example, it can be configured by adding the JVM argument &quot;<i>-Djppf.config=jppf-config.properties</i>&quot;.
 * @author Laurent Cohen
 */
public class JPPFConfiguration
{
	/**
	 * Name of the system property holding the location of the JPPF configuration file.
	 */
	public static final String CONFIG_PROPERTY = "jppf.config";
	/**
	 * Default location of the JPPF configuration file.
	 */
	public static final String DEFAULT_FILE = "jppf.properties";
	/**
	 * Holds the JPPF configuration properties.
	 */
	private static TypedProperties props = null;

	/**
	 * Get the configuration properties.
	 * @return a TypedProperties instance.
	 */
	public static TypedProperties getProperties()
	{
		if (props == null) loadProperties();
		return props;
	}

	/**
	 * Load the JPPF configuration properties from a file.
	 */
	private static void loadProperties()
	{
		String filename = System.getProperty(CONFIG_PROPERTY, DEFAULT_FILE);
		props = new TypedProperties();
		try
		{
			InputStream is = null;
			File file = new File(filename);
			if (file.exists()) is = new BufferedInputStream(new FileInputStream(filename));
			if (is == null) is = JPPFConfiguration.class.getClassLoader().getResourceAsStream(filename);
			if (is != null) props.load(is);
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
