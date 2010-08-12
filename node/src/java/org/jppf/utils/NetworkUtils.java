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

import java.net.*;
import java.util.*;

import org.apache.commons.logging.*;

/**
 * Utility class that provides method to discover the network configuration of the current machine.
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
	 * Contains a set of all possible loopback addresses.
	 * These are all the IPs in the 127.0.0.0/8 range.
	 */
	private static final Set<String> LOOPBACK_ADDRESSES = createLoopbackAddresses();

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
		List<Inet4Address> allAddresses = getNonLocalIPV4Addresses();
		return allAddresses.isEmpty() ? null : allAddresses.get(0).getHostAddress();
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
			if (debugEnabled) log.debug("Getting all network interfaces"); 
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (debugEnabled) log.debug("got all network interfaces"); 
			while (interfaces.hasMoreElements())
			{
				NetworkInterface ni = interfaces.nextElement();
				if (debugEnabled) log.debug("interface " + ni.getName() + " : " + ni.getDisplayName());
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while (addresses.hasMoreElements())
				{
					InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet4Address) list.add((Inet4Address) addr);
					if (debugEnabled) log.debug("  " + addr.getHostName() + " : " + addr.getHostAddress()); 
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
			if (LOOPBACK_ADDRESSES.contains(host) || "localhost".equals(host)) it.remove();
		}
		return addresses;
	}

	/**
	 * Create a set of IPV4 addresses that map to the loopback address.
	 * These are all the IPs in the 127.0.0.0/8 range.
	 * @return a set of IP addresses as strings.
	 */
	private static Set<String> createLoopbackAddresses()
	{
		Set<String> addresses = new HashSet<String>();
		String s = "127.0.0.";
		for (int i=0; i<=8; i++) addresses.add(s + i);
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
	 * Get a list of all known non-local IP v4 addresses for the current host.
	 * @return a List of <code>Inet6Address</code> instances, may be empty but never null.
	 */
	public static List<Inet6Address> getNonLocalIPV6Addresses()
	{
		List<Inet6Address> addresses = getIPV6Addresses();
		Iterator<Inet6Address> it = addresses.iterator();
		while (it.hasNext())
		{
			Inet6Address ad = it.next();
			if (ad.isLoopbackAddress() || "localhost".equals(ad.getHostName())) it.remove();
		}
		return addresses;
	}

	/**
	 * Get a list of all known non-local IP v4  and v6 addresses for the current host.
	 * @return a List of <code>InetAddress</code> instances, may be empty but never null.
	 */
	public static List<InetAddress> getNonLocalIPAddresses()
	{
		List<InetAddress> addresses = new ArrayList<InetAddress>();
		addresses.addAll(getIPV4Addresses());
		addresses.addAll(getIPV6Addresses());
		return addresses;
	}

	/**
	 * Get the management host specified in the configuration file.
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
	 * Attempt to resolve an IP address into a host name.
	 * @param ip the ip address to resolve.
	 * @return the corresponding host name, or its IP if the name could not be resolved.
	 */
	public static String getHostName(String ip)
	{
		InetSocketAddress addr = new InetSocketAddress(ip, 0);
		String s = addr.getHostName();
		return s == null ? ip : s;
	}

	/**
	 * Main entry point.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		System.out.println("This host's ip addresses: " + getNonLocalHostAddress());
	}
}
