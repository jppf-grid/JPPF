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

package org.jppf.server;

import java.util.List;

import javax.management.MBeanServer;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.management.spi.*;
import org.jppf.security.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Handles various initializations for the driver.
 * @author Laurent Cohen
 */
public class DriverInitializer
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(JPPFDriver.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * 
	 */
	private JPPFDriver driver = null;

	/**
	 * Instantiate this initializer with the specified driver.
	 * @param driver the driver to initialize.
	 */
	public DriverInitializer(JPPFDriver driver)
	{
		this.driver = driver;
	}

	/**
	 * Register all MBeans defined through the service provider interface.
	 * @throws Exception if the registration failed.
	 */
	@SuppressWarnings("unchecked")
	void registerProviderMBeans() throws Exception
	{
  	MBeanServer server = driver.getJmxServer().getServer();
    JPPFMBeanProviderManager mgr = new JPPFMBeanProviderManager<JPPFDriverMBeanProvider>(JPPFDriverMBeanProvider.class, server);
		List<JPPFDriverMBeanProvider> list = mgr.getAllProviders();
		for (JPPFDriverMBeanProvider provider: list)
		{
			Object o = provider.createMBean();
			Class<?> inf = Class.forName(provider.getMBeanInterfaceName());
			boolean b = mgr.registerProviderMBean(o, inf, provider.getMBeanName());
			if (debugEnabled) log.debug("MBean registration " + (b ? "succeeded" : "failed") + " for [" + provider.getMBeanName() + "]");
		}
	}

	/**
	 * Read configuration for the host name and ports used to conenct to this driver.
	 * @return a <code>DriverConnectionInformation</code> instance.
	 */
	public JPPFConnectionInformation createConnectionInformation()
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		JPPFConnectionInformation info = new JPPFConnectionInformation();
		info.uuid = driver.getUuid();
		String s = props.getString("class.server.port", "11111");
		info.classServerPorts = StringUtils.parseIntValues(s);
		s = props.getString("app.server.port", "11112");
		info.applicationServerPorts = StringUtils.parseIntValues(s);
		s = props.getString("node.server.port", "11113");
		info.nodeServerPorts = StringUtils.parseIntValues(s);
		info.host = NetworkUtils.getManagementHost();
		if (props.getBoolean("jppf.management.enabled", true)) info.managementPort = props.getInt("jppf.management.port", 11198);
		return info;
	}

	/**
	 * Print a message to the console to signify that the initialization of a server was succesfull.
	 * @param ports the ports on which the server is listening.
	 * @param name the name to use for the server.
	 */
	void printInitializedMessage(int[] ports, String name)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(name).append(" initialized - listening on port");
		if (ports.length > 1) sb.append("s");
		for (int n: ports) sb.append(" ").append(n);
		System.out.println(sb.toString());
	}

	/**
	 * Initialize the security credentials associated with this JPPF driver.
	 * @return a {@link JPPFSecurityContext} instance.
	 */
	JPPFSecurityContext initCredentials()
	{
		StringBuilder sb = new StringBuilder("Driver:");
		sb.append(VersionUtils.getLocalIpAddress()).append(":");
		TypedProperties props = JPPFConfiguration.getProperties();
		sb.append(props.getInt("class.server.port", 11111)).append(":");
		sb.append(props.getInt("app.server.port", 11112)).append(":");
		sb.append(props.getInt("node.server.port", 11113));
		return new JPPFSecurityContext(driver.getUuid(), sb.toString(), new JPPFCredentials());
	}

}
