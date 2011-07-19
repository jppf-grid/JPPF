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

package org.jppf.management;

import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXServerFactory
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JMXServerFactory.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determine whether we should use the RMI or JMXMP connector.
	 */
	private static boolean usingRMIConnector = JPPFConfiguration.getProperties().getString("jppf.management.connector", "jmxmp").equalsIgnoreCase("rmi");

	/**
	 * Create a JMXServer instance based on the specified parameters.
	 * @param uuid the server's unique identifier.
	 * @param suffix the suffix to use int he JMX service URL, only used with the RMI connector
	 * @return an instance of {@link JMXServer}.
	 * @throws Exception if the server could not be created.
	 */
	public static JMXServer createServer(String uuid, String suffix) throws Exception
	{
		JMXServer server = !usingRMIConnector && isJMXMPPresent() ? new JMXMPServer(uuid) : new JMXServerImpl(suffix, uuid);
		if (debugEnabled) log.debug("created JMX server: " + server);
		return server;
	}

	/**
	 * Determine whether JMXMP classes are in the classpath.
	 * @return true if JMXMP is in the classpath, false otherwise.
	 */
	public static boolean isJMXMPPresent()
	{
		try
		{
			Class c = JMXServerFactory.class.getClassLoader().loadClass("javax.management.remote.jmxmp.JMXMPConnectorServer");
			if (debugEnabled) log.debug("jmxmp classes are present in the classpath");
			return true;
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug("jmxmp classes are not present in the classpath");
		}
		return false;
	}

	/**
	 * Determine whether we should use the RMI connector.
	 * @return true if the RMI connector should be used, false otherwise.
	 */
	public static boolean isUsingRMIConnector()
	{
		return usingRMIConnector;
	}
}
