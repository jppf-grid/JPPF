/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.management;

import java.io.Serializable;

import org.jppf.utils.*;

/**
 * This class encapsulates the system information for a node.<br>
 * It includes:
 * <ul>
 * <li>System properties, including -X flags</li>
 * <li>Runtime information such as available processors and memory usage</li>
 * <li>Environment variables</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFSystemInformation implements Serializable
{
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
	 * Get the map holding the system properties.
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getSystem()
	{
		return system;
	}

	/**
	 * Get the map holding the runtime information
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getRuntime()
	{
		return runtime;
	}

	/**
	 * Get the map holding the environment variables.
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getEnv()
	{
		return env;
	}

	/**
	 * Get the  map of the network configuration.
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getNetwork()
	{
		return network;
	}

	/**
	 * Get the nap holding the JPPF configuration properties.
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getJppf()
	{
		return jppf;
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
