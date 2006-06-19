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

import java.io.Serializable;
import java.util.Random;

/**
 * Instances of this class serve as unique identifiers for messages sent to and from
 * remote execution services.
 * The identifier is generated as a string with the following elements:
 * <ul>
 * <li>sender host IP address</li>
 * <li>current system time in milliseconds</li>
 * <li>a random integer value between 0 and {@link java.lang.Integer#MAX_VALUE Integer.MAX_VALUE}</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFUuid implements Serializable
{
  /**
	 * Random number generator, static to ensure generated uuid are unique.
	 */
	private static Random rand = new Random(System.currentTimeMillis());
	/**
	 * The IP address of the host the JVM is running on.
	 */
	private static String ipAddress = obtainIpAddress();
	/**
	 * String holding a generated unique identifier.
	 */
	private String uuid = null;

	/**
	 * Instanciate this JPPFUuid with a generated unique identifier.
	 */
	public JPPFUuid()
	{
		uuid = generateUuid();
	}
	
	/**
	 * Generate a unique uuid.
	 * @return the uuid as a string.
	 */
	private static String generateUuid()
	{
		int n = 2;
		StringBuilder sb = new StringBuilder();
		sb.append(ipAddress);
		sb.append(StringUtils.padLeft("" + System.currentTimeMillis(), '0', 15));
		synchronized(rand)
		{
			for (int i=0; i<n; i++)
			{
				sb.append(StringUtils.padLeft("" + rand.nextInt(Integer.MAX_VALUE), '0', 10));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Get the IP address of the current host.<br>
	 * The address is formatted as <i>aaabbbcccddd</i>, where <i>aaa</i> is the first component of the address,
	 * formatted on 3 characters and padded with zeroes on the left if required, <i>bbb</i> is the second component, etc...
	 * @return the IP address as a string.
	 */
	private static String obtainIpAddress()
	{
		String ip = VersionUtils.getLocalIpAddress();
		if (ip == null) return null;
		String[] tokens = ip.split("\\.");
		StringBuilder sb = new StringBuilder();
		for (String token: tokens)
		{
			sb.append(StringUtils.padLeft(token, '0', 3));
		}
		return sb.toString();
	}

	/**
	 * Get a string representaiton of the generated unique identifier.
	 * @return a string containing the uuid.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return uuid;
	}
}
