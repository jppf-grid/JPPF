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

package org.jppf.process;

import java.util.Properties;

/**
 * Utility class used to generate the configuration properties of a JPPF client,
 * to be used when executing a JPPF client process. 
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
