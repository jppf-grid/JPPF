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

import java.io.InputStream;
import java.net.*;
import org.apache.log4j.Logger;

/**
 * This class provides a utility method to determine the JPPF build number available in the class path.<br>
 * It is used for the nodes to determine when their code is outdated, in which case they will automatically reload
 * their own code.
 * @author Laurent Cohen
 */
public final class VersionUtils
{
  /**
   * Log4j logger for this class.
   */
  private static Logger log = Logger.getLogger(VersionUtils.class);
	/**
	 * The current JPPF build number.
	 */
	private static int buildNumber = -1;
	/**
	 * IP addresse of the current host.
	 */
	private static String ip = getLocalIpAddress();

	/**
	 * Instantiation of this class is not permitted.
	 */
	private VersionUtils()
	{
	}

	/**
	 * Determine the current JPPF build number.
	 * @return the number found in the classpath, or 0 if it is not found.
	 */
	public static int getBuildNumber()
	{
		if (buildNumber < 0)
		{
			try
			{
				InputStream is = VersionUtils.class.getClassLoader().getResourceAsStream("build.number");
				TypedProperties props = new TypedProperties();
				props.load(is);
				buildNumber = props.getInt("build.number");
			}
			catch(Exception e)
			{
			}
		}
		return buildNumber;
	}

	/**
	 * Set the current JPPF build number.
	 * @param buildNumber the build number to set.
	 */
	public static void setBuildNumber(int buildNumber)
	{
		VersionUtils.buildNumber = buildNumber;
	}

	/**
	 * Get the IP address of the current host.<br>
	 * @return the IP address as a string in the format &quot;a.b.c.d&quot;.
	 */
	public static String getLocalIpAddress()
	{
		if (ip != null) return ip;
		try
		{
			InetAddress ip = InetAddress.getLocalHost();
			return ip.getHostAddress();
		}
		catch(UnknownHostException e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}
}
