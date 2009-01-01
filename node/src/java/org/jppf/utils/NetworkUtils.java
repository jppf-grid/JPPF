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

package org.jppf.utils;

import java.net.*;
import java.util.*;

import org.apache.commons.logging.*;

/**
 * 
 * @author Laurent Cohen
 */
public final class NetworkUtils
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
	 * Instantiation opf this class is not permitted.
	 */
	private NetworkUtils()
	{
	}

	/**
	 * Get the non local (meaning neither localhost or loopback) address of the current host.
	 * @return the ipv4 address as a string.
	 */
	public static String getNonLocalHostAddress()
	{
		try
		{
			List<Inet4Address> allAddresses = getIPV4Addresses();
			for (Inet4Address addr: allAddresses)
			{
				String host = addr.getHostAddress();
				if (!"127.0.0.1".equals(host)) return host;
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Get a list of all known IP v4 addresses for the current host.
	 * @return a List of <code>Inet4Address</code> instances, may be empty but never null.
	 */
	public static List<Inet4Address> getIPV4Addresses()
	{
		List<Inet4Address> list = new ArrayList<Inet4Address>();
		try
		{
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements())
			{
				NetworkInterface ni = interfaces.nextElement();
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while (addresses.hasMoreElements())
				{
					InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet4Address) list.add((Inet4Address) addr);
				}
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return list;
	}

	/**
	 * Get a list of all known non-local IP v4 addresses for the current host.
	 * @return a List of <code>Inet4Address</code> instances, may be empty but never null.
	 */
	public static List<Inet4Address> getNonLocalIPV4Addresses()
	{
		List<Inet4Address> addresses = getIPV4Addresses();
		Iterator<Inet4Address> it = addresses.iterator();
		while (it.hasNext())
		{
			Inet4Address ad = it.next();
			String host = ad.getHostAddress();
			if ("127.0.0.1".equals(host) || "localhost".equals(host)) it.remove();
		}
		return addresses;
	}

	/**
	 * Get a list of all known IP v6 addresses for the current host.
	 * @return a List of <code>Inet6Address</code> instances, may be empty but never null.
	 */
	public static List<Inet6Address> getIPV6Addresses()
	{
		List<Inet6Address> list = new ArrayList<Inet6Address>();
		try
		{
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements())
			{
				NetworkInterface ni = interfaces.nextElement();
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while (addresses.hasMoreElements())
				{
					InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet6Address) list.add((Inet6Address) addr);
				}
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return list;
	}

	/**
	 * Get the management host specifieed in the configuration file.
	 * @return the host as a string.
	 */
	public static String getManagementHost()
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = NetworkUtils.getNonLocalHostAddress();
		if (debugEnabled) log.debug("JMX host from NetworkUtils: "+host);
		if (host == null) host = "localhost";
		host = props.getString("jppf.management.host", host);
		if (debugEnabled) log.debug("computed JMX host: "+host);
		return host;
	}

	/**
	 * Main entry point.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		System.out.println("This host's ip address: " + getNonLocalHostAddress());
	}
}
