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
	 * Prefix for execution services configuration properties names.
	 */
	public static final String CONFIG_PROPERTY = "jppf.config";
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
		String filename = System.getProperty(CONFIG_PROPERTY, "node.properties");
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
