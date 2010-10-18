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

package org.jppf.management;

import java.io.Serializable;

import org.jppf.utils.*;

/**
 * This class encapsulates the system information for a node.<br>
 * It includes:
 * <ul>
 * <li>System properties, including -D flags</li>
 * <li>Runtime information such as available processors and memory usage</li>
 * <li>Environment variables</li>
 * <li>JPPF configuration properties</li>
 * <li>IPV4 and IPV6 addresses assigned to the JVM host</li>
 * <li>Disk space information (JDK 1.6 or later only)</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFSystemInformation implements Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Map holding the system properties.
	 */
	private TypedProperties system = null;
	/**
	 * Map holding the runtime information
	 */
	private TypedProperties runtime = null;
	/**
	 * Map holding the environment variables.
	 */
	private TypedProperties env = null;
	/**
	 * Map holding the JPPF configuration properties.
	 */
	private TypedProperties jppf = null;
	/**
	 * A map of the network configuration.
	 */
	private TypedProperties network = null;
	/**
	 * A map of the available storage information.
	 */
	private TypedProperties storage = null;

	/**
	 * Get the map holding the system properties.
	 * @return a <code>TypedProperties</code> instance.
	 * @see org.jppf.utils.SystemUtils#getSystemProperties()
	 */
	public TypedProperties getSystem()
	{
		return system;
	}

	/**
	 * Get the map holding the runtime information.
	 * <p>The resulting map will contain the following properties:
	 * <ul>
	 * <li>availableProcessors = <i>number of processors available to the JVM</i></li>
	 * <li>freeMemory = <i>current free heap size in bytes</i></li>
	 * <li>totalMemory = <i>current total heap size in bytes</i></li>
	 * <li>maxMemory = <i>maximum heap size in bytes (i.e. as specified by -Xmx JVM option)</i></li>
	 * </ul> 
	 * <p>Some or all of these properties may be missing if a security manager is installed
	 * that does not grant access to the related {@link java.lang.Runtime} APIs.
	 * @return a <code>TypedProperties</code> instance.
	 * @see org.jppf.utils.SystemUtils#getRuntimeInformation()
	 */
	public TypedProperties getRuntime()
	{
		return runtime;
	}

	/**
	 * Get the map holding the environment variables.
	 * @return a <code>TypedProperties</code> instance.
	 * @see org.jppf.utils.SystemUtils#getEnvironment()
	 */
	public TypedProperties getEnv()
	{
		return env;
	}

	/**
	 * Get the  map of the network configuration.
	 * <p>The resulting map will contain the following properties:
	 * <ul>
	 * <li>ipv4.addresses = <i>hostname_1</i>|<i>ipv4_address_1</i> ... <i>hostname_n</i>|<i>ipv4_address_n</i></li>
	 * <li>ipv6.addresses = <i>hostname_1</i>|<i>ipv6_address_1</i> ... <i>hostname_p</i>|<i>ipv6_address_p</i></li>
	 * </ul>
	 * <p>Each property is a space-separated list of <i>hostname</i>|<i>ip_address</i> pairs,
	 * the hostname and ip address being separated by a pipe symbol &quot;|&quot;.
	 * @return a <code>TypedProperties</code> instance.
	 * @see org.jppf.utils.SystemUtils#getNetwork()
	 */
	public TypedProperties getNetwork()
	{
		return network;
	}

	/**
	 * Get the map holding the JPPF configuration properties.
	 * @return a <code>TypedProperties</code> instance.
	 * @see org.jppf.utils.JPPFConfiguration
	 */
	public TypedProperties getJppf()
	{
		return jppf;
	}

	/**
	 * Get the map holding the host storage information.
	 * <p>The map will contain the following information:
	 * <ul>
	 * <li>host.roots.names = <i>root_name_0</i> ... <i>root_name_n-1</i> : the names of all accessible file system roots</li>
	 * <li>host.roots.number = <i>n</i> : the number of accessible file system roots</li>
	 * <li><b>For each root <i>i</i>:</b></li>
	 * <li>root.<i>i</i>.name = <i>root_name</i> : for instance &quot;C:\&quot; on Windows or &quot;/&quot; on Unix</li>
	 * <li>root.<i>i</i>.space.free = <i>space_in_bytes</i> : current free space for the root</li>
	 * <li>root.<i>i</i>.space.total = <i>space_in_bytes</i> : total space for the root</li>
	 * <li>root.<i>i</i>.space.usable = <i>space_in_bytes</i> : space available to the user the JVM is running under</li>
	 * </ul>
	 * If the JVM version is prior to 1.6, the space information will not be available.
	 * @return a <code>TypedProperties</code> instance.
	 * @see org.jppf.utils.SystemUtils#getStorageInformation()
	 */
	public TypedProperties getStorage()
	{
		return storage;
	}

	/**
	 * Populate this node information object.
	 */
	public void populate()
	{
		system = SystemUtils.getSystemProperties();
		runtime = SystemUtils.getRuntimeInformation();
		env = SystemUtils.getEnvironment();
		jppf = JPPFConfiguration.getProperties();
		network = SystemUtils.getNetwork();
		storage = SystemUtils.getStorageInformation();
	}

	/**
	 * Parse the list of IP v4 addresses contained in this JPPFSystemInformation instance.<br>
	 * This method is provided as a convenience so developers don't have to do the parsing themselves.
	 * @return an array on <code>HostIP</code> instances.
	 */
	private HostIP[] parseIPV4Addresses()
	{
		String s = getNetwork().getString("ipv4.adresses");
		if ((s == null) || "".equals(s.trim())) return null;
		return parseAddresses(s);
	}

	/**
	 * Parse the list of IP v6 addresses contained in this JPPFSystemInformation instance.<br>
	 * This method is provided as a convenience so developers don't have to do the parsing themselves.
	 * @return an array on <code>HostIP</code> instances.
	 */
	private HostIP[] parseIPV6Addresses()
	{
		String s = getNetwork().getString("ipv6.adresses");
		if ((s == null) || "".equals(s.trim())) return null;
		return parseAddresses(s);
	}

	/**
	 * Parse a list of addresses.
	 * @param addresses a string containing a space-separated list of host_name|ip_address pairs. 
	 * @return an array on <code>HostIP</code> instances.
	 */
	private HostIP[] parseAddresses(String addresses)
	{
		String[] pairs = addresses.split("\\s");
		if ((pairs == null) || (pairs.length <= 0)) return null;
		HostIP[] result = new HostIP[pairs.length];
		int count = 0;
		for (String pair: pairs)
		{
			String[] comps = pair.split("|");
			if ((comps[0] != null) && "".equals(comps[0].trim())) comps[0] = null;
			result[count++] = new HostIP(comps[0], comps[1]);
		}
		return result;
	}

	/**
	 * Instances of this class represent a hostname / ip address pair. 
	 */
	public static class HostIP extends Pair<String, String>
	{
		/**
		 * Initialize this HostIP object with the specified host name and IP addresse.
		 * @param first the host name.
		 * @param second the corresponnding IP address.
		 */
		public HostIP(String first, String second)
		{
			super(first, second);
		}

		/**
		 * Get the host name.
		 * @return the name as a string.
		 */
		public String hostName()
		{
			return first();
		}

		/**
		 * Get the ip address.
		 * @return the ip address as a string.
		 */
		public String ipAddress()
		{
			return second();
		}
	}
}
