/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

import java.util.*;

import org.apache.commons.logging.*;

/**
 * Collection of utility methods used as a convenience for retrieving
 * JVM-level or SYstem-level information. 
 * @author Laurent Cohen
 */
public final class SystemUtils
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NetworkUtils.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Singleton holding the unchanging system properties.
	 */
	private static TypedProperties systemProps = null;
	/**
	 * A map of the environment properties.
	 */
	private static TypedProperties env = null;

	/**
	 * Instantiation of this class is not permitted.
	 */
	private SystemUtils()
	{
	}

	/**
	 * Return a set of properties guaranteed to always be part of those returned by 
	 * {@link java.lang.System.getProperties() System.getProperties()}.
	 * @return the properties as a <code>TypedProperties</code> instance.
	 */
	public synchronized static TypedProperties getSystemProperties()
	{
		if (systemProps == null)
		{
			TypedProperties props = new TypedProperties();
			addSystemProperty("java.version", props);
			addSystemProperty("java.vendor", props);
			addSystemProperty("java.vendor.url", props);
			addSystemProperty("java.home", props);
			addSystemProperty("java.vm.specification.version", props);
			addSystemProperty("java.vm.specification.vendor", props);
			addSystemProperty("java.vm.specification.name", props);
			addSystemProperty("java.vm.version", props);
			addSystemProperty("java.vm.vendor", props);
			addSystemProperty("java.vm.name", props);
			addSystemProperty("java.specification.version", props);
			addSystemProperty("java.specification.vendor", props);
			addSystemProperty("java.specification.name", props);
			addSystemProperty("java.class.version", props);
			addSystemProperty("java.class.path", props);
			addSystemProperty("java.library.path", props);
			addSystemProperty("java.io.tmpdir", props);
			addSystemProperty("java.compiler", props);
			addSystemProperty("java.ext.dirs", props);
			addSystemProperty("os.name", props);
			addSystemProperty("os.arch", props);
			addSystemProperty("os.version", props);
			addSystemProperty("file.separator", props);
			addSystemProperty("path.separator", props);
			addSystemProperty("line.separator", props);
			addSystemProperty("user.name", props);
			addSystemProperty("user.home", props);
			addSystemProperty("user.dir", props);
			addSystemProperty("jppf.config", props);
			addSystemProperty("log4j.configuration", props);
			addOtherSystemProperties(props);
			systemProps = props;
		}
		return systemProps;
	}

	/**
	 * Add system properties whose name is not known in advance.
	 * @param props the TypedProperties instance to add the system properties to.
	 */
	private static void addOtherSystemProperties(TypedProperties props)
	{
		Properties systemProps = null;
		try
		{
			systemProps = System.getProperties();
			Enumeration en = systemProps.propertyNames();
			while (en.hasMoreElements())
			{
				String name = (String) en.nextElement();
				try
				{
					if (!props.contains(name)) props.setProperty(name, System.getProperty(name));
				}
				catch(SecurityException e)
				{
					if (debugEnabled) log.debug(e.getMessage(), e);
				}
			}
		}
		catch(SecurityException e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
	}

	/**
	 * Add a system property to a set of properties.
	 * @param name name of the system property to add.
	 * @param props properties set to add to.
	 */
	private static void addSystemProperty(String name, TypedProperties props)
	{
		String s = null;
		try
		{
			s = System.getProperty(name);
		}
		catch(SecurityException e)
		{
			s = e.getMessage();
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
		if (s != null) props.setProperty(name, s);
	}

	/**
	 * Get information about the number of processors available to the JVM and the JVM memory usage.
	 * @return a <code>TypedProperties</code> instance holding the requested information.
	 */
	public static TypedProperties getRuntimeInformation()
	{
		TypedProperties props = new TypedProperties();
		String s = null;
		try
		{
			s = "" + Runtime.getRuntime().availableProcessors();
		}
		catch(SecurityException e)
		{
			s = e.getMessage();
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
		props.setProperty("availableProcessors", s);
		try
		{
			s = "" + Runtime.getRuntime().freeMemory();
		}
		catch(SecurityException e)
		{
			s = e.getMessage();
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
		props.setProperty("freeMemory", s);
		try
		{
			s = "" + Runtime.getRuntime().totalMemory();
		}
		catch(SecurityException e)
		{
			s = e.getMessage();
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
		props.setProperty("totalMemory", s);
		try
		{
			s = "" + Runtime.getRuntime().maxMemory();
		}
		catch(SecurityException e)
		{
			s = e.getMessage();
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
		props.setProperty("maxMemory", s);
		return props;
	}

	/**
	 * Get a map of the environment variables.
	 * @return a mapping of environment variables to their value.
	 */
	public static synchronized TypedProperties getEnvironment()
	{
		if (env == null)
		{
			env = new TypedProperties();
			try
			{
				Map<String, String> props = System.getenv();
				for (Map.Entry<String, String> entry: props.entrySet())
				{
					env.setProperty(entry.getKey(), entry.getValue());
				}
			}
			catch(SecurityException e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
			}
		}
		return env;
	}
}
