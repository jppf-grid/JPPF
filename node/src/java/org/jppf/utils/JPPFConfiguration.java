/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import org.slf4j.*;

/**
 * Utility class for loading and accessing the JPPF configuration properties.
 * <p>The configuration file path is set through the system property {@link org.jppf.utils#CONFIG_PROPERTY CONFIG_PROPERTY},
 * whose value is &quot;jppf.config&quot;.<br>
 * As an example, it can be configured by adding the JVM argument &quot;<i>-Djppf.config=jppf-config.properties</i>&quot;.
 * @author Laurent Cohen
 * @author Jonathan Newbrough
 * <br>Modified to allow users to get configuration properties from an alternate source. Any user-provided class that
 * implements {@link ConfigurationSource} and returns a stream with the same configuration values provided in the properties file.
 */
public class JPPFConfiguration
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFConfiguration.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Name of the system property holding the location of the JPPF configuration file.
	 */
	public static final String CONFIG_PROPERTY = "jppf.config";
	/**
	 * The name of the system property used to specified an alternate JPPF configuration source.
	 */
	public static final String CONFIG_PLUGIN_PROPERTY = "jppf.config.plugin";
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
	 * Reset and reload the JPPF configuration.
	 * This allows reloading the configuration from a different source or file
	 * (after changing the values of the related system properties for instance). 
	 */
	public static void reset()
	{
		loadProperties();
	}

	/**
	 * Load the JPPF configuration properties from a file.
	 */
	private static void loadProperties()
	{
		props = new TypedProperties();
		try
		{
			InputStream is = getStream();
			if (is != null) props.load(is);
		}
		catch(Exception e)
		{
			log.error("error reading the configuration", e);
		}
	}

	/**
	 * Get an input stream from which to read the configuration properties.
	 * @return an {@link InputStream} instance.
	 * @throws Exception if any error occurs while trying to obtain the stream.
	 */
	private static InputStream getStream() throws Exception
	{
		String altSource = System.getProperty(CONFIG_PLUGIN_PROPERTY);
		if (altSource == null)
		{
			String filename = System.getProperty(CONFIG_PROPERTY, DEFAULT_FILE);
			if (log.isDebugEnabled()) log.debug("reading JPPF configuration file: " + filename);
			InputStream is = null;
			File file = new File(filename);
			if (file.exists()) is = new BufferedInputStream(new FileInputStream(filename));
			if (is == null) is = JPPFConfiguration.class.getClassLoader().getResourceAsStream(filename);
			return is;
		}
		else
		{
			if (log.isDebugEnabled()) log.debug("reading JPPF configuration from alternate source: " + altSource);
			ConfigurationSource source = (ConfigurationSource) Class.forName(altSource).newInstance();
			return source.getPropertyStream();
		}
	}

	/**
	 * Implement this interface to provide an alternate configuration source.
	 * <p>WARNING: not shown in the interface but also required:
	 * implementations must have a public no-arg constructor.
	 */
	public static interface ConfigurationSource
	{
		/**
		 * Obtain the JPPF configuration properties from an input stream.
		 * The returned stream content must conform to the properties file's specifications
		 * (i.e. it must be usable as the argument to <code>Properties.load(InputStream)</code>).
		 * @return an {@link InputStream} instance.
		 * @throws IOException if the stream cannot be created.
		 */
		InputStream getPropertyStream() throws IOException;
	}
}
