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

package org.jppf.process;

import java.util.Properties;

/**
 * 
 * @author Laurent Cohen
 */
public class ClientPropertiesBuilder
{
	/**
	 * Driver connection configuration 1.
	 */
	public static final Properties CONNECTION_1 = buildDriverConnection("driver1", "localhost", 11111, 11112, 10);
	/**
	 * Driver connection configuration 2.
	 */
	public static final Properties CONNECTION_2 = buildDriverConnection("driver2", "localhost", 11121, 11122, 10);
	/**
	 * Base client configuration.
	 */
	public static final Properties BASE_CLIENT =
		buildClientConfig("driver1 driver2", new Properties[] {CONNECTION_1, CONNECTION_2}); 

	/**
	 * Generate the configuration properties for the JPPF client.
	 * @param driverNames space-separated list of driver connection names.
	 * @param drivers a list of client connection configurations.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildClientConfig(String driverNames, Properties[] drivers)
	{
		Properties props = new Properties();
		props.setProperty("jppf.drivers", driverNames);
		for (Properties driver: drivers) props.putAll(driver);
		props.setProperty("reconnect.initial.delay", "1");
		props.setProperty("reconnect.max.time", "10");
		props.setProperty("reconnect.interval", "1");

		return props;
	}

	/**
	 * Generate the configuration for a driver connection.
	 * @param name the connection name.
	 * @param host the host on which the driver is running.
	 * @param classPort the class server port.
	 * @param appPort the application server port.
	 * @param priority the connection priority.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildDriverConnection(String name, String host, int classPort, int appPort, int priority)
	{
		Properties props = new Properties();
		props.setProperty(name + ".jppf.server.host", host);
		props.setProperty("driver1.class.server.port", "" + classPort);
		props.setProperty("driver1.app.server.port", "" + appPort);
		props.setProperty("driver1.priority", "" + priority);
		return props;
	}
}
