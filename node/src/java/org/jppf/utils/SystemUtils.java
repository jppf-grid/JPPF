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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.*;
import java.util.*;

import org.slf4j.*;

/**
 * Collection of utility methods used as a convenience for retrieving
 * JVM-level or System-level information. 
 * @author Laurent Cohen
 */
public final class SystemUtils
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(NetworkUtils.class);
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
	 * A map of the network configuration.
	 */
	private static TypedProperties network = null;
	/**
	 * Windows OS.
	 */
	private static final int WINDOWS_OS = 1;
	/**
	 * X11 based OS.
	 */
	private static final int X11_OS = 2;
	/**
	 * The MAC based OS.
	 */
	private static final int MAC_OS = 3;
	/**
	 * Unknown or unsupported OS.
	 */
	private static final int UNKNOWN_OS = -1;
	/**
	 * The type of this host's OS.
	 */
	private static final int OS_TYPE = determineOSType();

	/**
	 * Instantiation of this class is not permitted.
	 */
	private SystemUtils()
	{
	}

	/**
	 * Return a set of properties guaranteed to always be part of those returned by 
	 * {@link java.lang.System#getProperties() System.getProperties()}.
	 * @return the properties as a <code>TypedProperties</code> instance.
	 */
	public static synchronized TypedProperties getSystemProperties()
	{
		if (systemProps == null)
		{
			TypedProperties props = new TypedProperties();
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
		try
		{
			// run as priviledged so we don't have to set write access on all propeorties
			// in the security policy file. 
			Properties sysProps = AccessController.doPrivileged(new PrivilegedAction<Properties>()
			{
				@Override
                public Properties run()
				{
					return System.getProperties();
				}
			});
			Enumeration en = sysProps.propertyNames();
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
					else log.info(e.getMessage());
				}
			}
		}
		catch(SecurityException e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.info(e.getMessage());
		}
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
			s = String.valueOf(Runtime.getRuntime().availableProcessors());
			props.setProperty("availableProcessors", s);
			s = String.valueOf(Runtime.getRuntime().freeMemory());
			props.setProperty("freeMemory", s);
			s = String.valueOf(Runtime.getRuntime().totalMemory());
			props.setProperty("totalMemory", s);
			s = String.valueOf(Runtime.getRuntime().maxMemory());
			props.setProperty("maxMemory", s);
		}
		catch(SecurityException e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.info(e.getMessage());
		}

		return props;
	}

	/**
	 * Get storage information about each file system root available on the current host.
	 * This method first checks that the Java version is 1.6 or later, then populates
	 * a <code>TypedProperties</code> object with root name, free space, total space and usable space
	 * information for each root.
	 * <p>If the Java version is before 1.6, only the root name is retrieved.
	 * An example root name would be &quot;C:\&quot; for a Windows system and &quot;/&quot; for a Unix system.
	 * @return TypedProperties object with storage information.
	 */
	public static synchronized TypedProperties getStorageInformation()
	{
		TypedProperties props = new TypedProperties();
		File[] roots = File.listRoots();
		if ((roots == null) || (roots.length <= 0)) return props;
		boolean atLeastJdk16 = true;
		Method usableSpaceMethod = null;
		Method freeSpaceMethod = null;
		Method totalSpaceMethod = null;
		try
		{
			usableSpaceMethod = File.class.getMethod("getUsableSpace");
			freeSpaceMethod = File.class.getMethod("getFreeSpace");
			totalSpaceMethod = File.class.getMethod("getTotalSpace");
		}
		catch (Exception e)
		{
			atLeastJdk16 = false;
		}
		props.setProperty("host.roots.number", String.valueOf(roots.length));
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<roots.length; i++)
		{
			try
			{
				if (i > 0) sb.append(' ');
				String s = roots[i].getCanonicalPath();
				sb.append(s);
				String prefix = "root." + i;
				props.setProperty(prefix + ".name", s);
				if (!atLeastJdk16) continue;
				long space = (Long) totalSpaceMethod.invoke(roots[i]);
                props.setProperty(prefix + ".space.total", Long.toString(space));
				space = (Long) freeSpaceMethod.invoke(roots[i]);
				props.setProperty(prefix + ".space.free", Long.toString(space));
				space = (Long) usableSpaceMethod.invoke(roots[i]);
				props.setProperty(prefix + ".space.usable", Long.toString(space));
			}
			catch(Exception e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
				else log.info(e.getMessage());
			}
		}
		props.setProperty("host.roots.names", sb.toString());		
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
				else log.info(e.getMessage());
			}
		}
		return env;
	}

	/**
	 * Get a map of the environment variables.
	 * @return a mapping of environment variables to their value.
	 */
	public static synchronized TypedProperties getNetwork()
	{
		if (network == null)
		{
			network = new TypedProperties();
			try
			{
				network.setProperty("ipv4.addresses", formatAddresses(NetworkUtils.getIPV4Addresses()));
				network.setProperty("ipv6.addresses", formatAddresses(NetworkUtils.getIPV6Addresses()));
			}
			catch(SecurityException e)
			{
				if (debugEnabled) log.debug(e.getMessage(), e);
				else log.info(e.getMessage());
			}
		}
		return network;
	}

	/**
	 * Format a list of InetAddress.
	 * @param addresses a List of <code>InetAddress</code> instances.
	 * @return a string containing a space-separated list of <i>hostname</i>|<i>ip_address</i> pairs.
	 */
	private static String formatAddresses(List<? extends InetAddress> addresses)
	{
        StringBuilder sb = new StringBuilder();
		for (InetAddress addr: addresses)
		{
			String name = addr.getHostName();
			String ip = addr.getHostAddress();
			if (sb.length() > 0) sb.append(' ');
			sb.append(name).append('|').append(ip);
		}
		return sb.toString();
	}

	/**
	 * Compute the maximum memory currently available for the Java heap. 
	 * @return the maximum number of free bytes in the heap.
	 */
	public static long maxFreeHeap()
	{
		Runtime rt = Runtime.getRuntime();
		return rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
	}

	/**
	 * Determine the type of this host's operating system, based on the value
	 * of the system property &quot;os.name&quot;.
	 * @return an int value indentifying the type of OS.
	 */
	private static int determineOSType()
  {
    String name = System.getProperty("os.name");
    if (StringUtils.startsWithOneOf(name, true, "Linux", "SunOS", "Solaris", "FreeBSD", "OpenBSD")) return X11_OS; 
    else if (StringUtils.startsWithOneOf(name, true, "Mac", "Darwin")) return MAC_OS;
    else if (name.startsWith("Windows") && !name.startsWith("Windows CE")) return WINDOWS_OS;
    return UNKNOWN_OS;
  }

	/**
	 * Determine whether the current OS is Windows.
	 * @return true if the system is Windows, false otherwise.
	 */
	public static boolean isWindows()
	{
		return OS_TYPE == WINDOWS_OS;
	}

	/**
	 * Determine whether the current OS is X11-based.
	 * @return true if the system is X11, false otherwise.
	 */
	public static boolean isX11()
	{
		return OS_TYPE == X11_OS;
	}

	/**
	 * Determine whether the current OS is Mac-based.
	 * @return true if the system is Mac-based, false otherwise.
	 */
	public static boolean isMac()
	{
		return OS_TYPE == MAC_OS;
	}

	/**
	 * Return the current process ID.
	 * @return the pid as an int, or -1 if the pid could not be obtained.
	 */
	public static int getPID()
	{
		int pid = -1;
		// we expect the name to be in '<pid>@hostname' format - this is JVM dependent
		String name = ManagementFactory.getRuntimeMXBean().getName();
		int idx = name.indexOf('@');
		if (idx >= 0)
		{
			String sub = name.substring(0, idx);
			try
			{
				pid = Integer.valueOf(sub);
				if (debugEnabled) log.debug("process name=" + name + ", pid=" + pid);
			}
			catch (Exception e)
			{
				String msg = "could not parse '" + sub +"' into a valid integer pid";
				if (debugEnabled) log.debug(msg, e);
				else log.warn(msg);
			}
		}
		return pid;
	}
}
