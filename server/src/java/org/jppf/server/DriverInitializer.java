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

package org.jppf.server;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.*;

import org.jppf.comm.discovery.*;
import org.jppf.comm.recovery.RecoveryServer;
import org.jppf.management.*;
import org.jppf.management.spi.*;
import org.jppf.server.debug.*;
import org.jppf.server.peer.*;
import org.jppf.server.nio.classloader.ClassNioServer;
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
	 * The instance of the driver.
	 */
	private JPPFDriver driver = null;
	/**
	 * The thread that performs the peer servers discovery.
	 */
	private PeerDiscoveryThread peerDiscoveryThread = null;
	/**
	 * The thread that broadcasts the server connection information using UDP multicast.
	 */
	private JPPFBroadcaster broadcaster = null;
	/**
	 * The JPPF configuration.
	 */
	private TypedProperties config = null;
	/**
	 * Represents the connection information for this driver.
	 */
	private JPPFConnectionInformation connectionInfo = null;
	/**
	 * The jmx server used to manage and monitor this driver.
	 */
	private JMXServer jmxServer = null;
	/**
	 * The object that collects debug information.
	 */
	private ServerDebug serverDebug = null;
	/**
	 * The server used to detect that individual connections are broken due to hardware failures.
	 */
	private RecoveryServer recoveryServer = null;

	/**
	 * Instantiate this initializer with the specified driver.
	 * @param driver the driver to initialize.
	 */
	public DriverInitializer(JPPFDriver driver)
	{
		this.driver = driver;
		config = JPPFConfiguration.getProperties();
	}

	/**
	 * Register the MBean that collects debug/troubleshooting information.
	 */
	void registerDebugMBean()
	{
		if (JPPFDriver.JPPF_DEBUG)
		{
			try
			{
				MBeanServer server = ManagementFactory.getPlatformMBeanServer();
				serverDebug = new ServerDebug();
				StandardMBean mbean = new StandardMBean(serverDebug, ServerDebugMBean.class);
				server.registerMBean(mbean, new ObjectName("org.jppf:name=debug,type=driver"));
			}
			catch (Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Register all MBeans defined through the service provider interface.
	 * @throws Exception if the registration failed.
	 */
	@SuppressWarnings("unchecked")
	void registerProviderMBeans() throws Exception
	{
  	MBeanServer server = getJmxServer().getServer();
    JPPFMBeanProviderManager mgr = new JPPFMBeanProviderManager<JPPFDriverMBeanProvider>(JPPFDriverMBeanProvider.class, server);
		List<JPPFDriverMBeanProvider> list = mgr.getAllProviders();
		for (JPPFDriverMBeanProvider provider: list)
		{
			Object o = provider.createMBean();
			Class<?> inf = Class.forName(provider.getMBeanInterfaceName());
			boolean b = mgr.registerProviderMBean(o, inf, provider.getMBeanName());
			if (debugEnabled) log.debug("MBean registration " + (b ? "succeeded" : "failed") + " for [" + provider.getMBeanName() + ']');
		}
	}

	/**
	 * Read configuration for the host name and ports used to conenct to this driver.
	 * @return a <code>DriverConnectionInformation</code> instance.
	 */
	public JPPFConnectionInformation getConnectionInformation()
	{
		if (connectionInfo == null)
		{
			connectionInfo = new JPPFConnectionInformation();
			connectionInfo.uuid = driver.getUuid();
			String s = config.getString("jppf.server.port", "11111");
			connectionInfo.serverPorts = StringUtils.parseIntValues(s);
			connectionInfo.host = NetworkUtils.getManagementHost();
			if (config.getBoolean("jppf.management.enabled", true)) connectionInfo.managementPort = config.getInt("jppf.management.port", 11198);
			boolean recoveryEnabled = config.getBoolean("jppf.recovery.enabled", false);
			if (recoveryEnabled) connectionInfo.recoveryPort = config.getInt("jppf.recovery.server.port", 22222);
		}
		return connectionInfo;
	}
	/*
	public JPPFConnectionInformation getConnectionInformation()
	{
		if (connectionInfo == null)
		{
			connectionInfo = new JPPFConnectionInformation();
			connectionInfo.uuid = driver.getUuid();
			String s = config.getString("class.server.port", "11111");
			connectionInfo.classServerPorts = StringUtils.parseIntValues(s);
			s = config.getString("app.server.port", "11112");
			connectionInfo.applicationServerPorts = StringUtils.parseIntValues(s);
			s = config.getString("node.server.port", "11113");
			connectionInfo.nodeServerPorts = StringUtils.parseIntValues(s);
			connectionInfo.host = NetworkUtils.getManagementHost();
			if (config.getBoolean("jppf.management.enabled", true)) connectionInfo.managementPort = config.getInt("jppf.management.port", 11198);
			boolean recoveryEnabled = config.getBoolean("jppf.recovery.enabled", false);
			if (recoveryEnabled) connectionInfo.recoveryPort = config.getInt("jppf.recovery.server.port", 22222);
		}
		return connectionInfo;
	}
	*/

    /**
	 * Initialize and start the discovery service.
	 */
	void initBroadcaster()
	{
		if (config.getBoolean("jppf.discovery.enabled", true))
		{
			broadcaster = new JPPFBroadcaster(getConnectionInformation());
			new Thread(broadcaster, "JPPF Broadcaster").start();
		}
	}

	/**
	 * Stop the discovery service if it is running.
	 */
	void stopBroadcaster()
	{
		if (broadcaster != null)
		{
			broadcaster.setStopped(true);
			broadcaster = null;
		}
	}

	/**
	 * Initialize this driver's peers.
     * @param classServer JPPF class server
     */
	void initPeers(final ClassNioServer classServer)
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		if (props.getBoolean("jppf.peer.discovery.enabled", false))
		{
			if (debugEnabled) log.debug("starting peers discovery");
			peerDiscoveryThread = new PeerDiscoveryThread(getConnectionInformation(), classServer);
			new Thread(peerDiscoveryThread, "PeerDiscoveryThread").start();
		}
		else
		{
			String peerNames = props.getString("jppf.peers");
			if ((peerNames == null) || "".equals(peerNames.trim())) return;
			if (debugEnabled) log.debug("found peers in the configuration");
			String[] names = peerNames.split("\\s");
			for (String peerName: names) {
                JPPFConnectionInformation connectionInfo = new JPPFConnectionInformation();
                connectionInfo.host = props.getString(String.format("jppf.peer.%s.server.host", peerName), "localhost");
                connectionInfo.serverPorts = new int[] { props.getInt(String.format("jppf.peer.%s.server.port", peerName), 11111) };
                new JPPFPeerInitializer(peerName, connectionInfo, classServer).start();
            }
		}
	}

	/**
	 * Get the thread that performs the peer servers discovery.
	 * @return a <code>PeerDiscoveryThread</code> instance.
	 */
	public PeerDiscoveryThread getPeerDiscoveryThread()
	{
		return peerDiscoveryThread;
	}

	/**
	 * Stop the peer discovery thread if it is running.
	 */
	void stopPeerDiscoveryThread()
	{
		if (peerDiscoveryThread != null)
		{
			peerDiscoveryThread.setStopped(true);
			peerDiscoveryThread = null;
		}
	}

	/**
	 * Get the jmx server used to manage and monitor this driver.
	 * @return a <code>JMXServerImpl</code> instance.
	 */
	public synchronized JMXServer getJmxServer()
	{
		return jmxServer;
	}

	/**
	 * Initialize the JMX server.
	 */
	void initJmxServer()
	{
		JPPFConnectionInformation info = getConnectionInformation();
		try
		{
			if (config.getBoolean("jppf.management.enabled", true))
			{
				//jmxServer = new JMXServerImpl(JPPFAdminMBean.DRIVER_SUFFIX, driver.getUuid());
				//jmxServer = new JMXMPServer(driver.getUuid());
				jmxServer = JMXServerFactory.createServer(driver.getUuid(), JPPFAdminMBean.DRIVER_SUFFIX);
				jmxServer.start(getClass().getClassLoader());
				info.managementPort = JPPFConfiguration.getProperties().getInt("jppf.management.port", 11198);
				registerProviderMBeans();
				System.out.println("JPPF Driver management initialized");
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
			config.setProperty("jppf.management.enabled", "false");
			String s = e.getMessage();
			s = (s == null) ? "<none>" : s.replace("\t", "  ").replace("\n", " - ");
			System.out.println("JPPF Driver management failed to initialize, with error message: '" + s + '\'');
			System.out.println("Management features are disabled. Please consult the driver's log file for more information");
		}
	}

	/**
	 * Stop the JMX server.
	 */
	void stopJmxServer()
	{
		try
		{
			if (jmxServer != null) jmxServer.stop();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * The server used to detect that individual connections are broken due to hardware failures.
	 * @return a {@link RecoveryServer} instance.
	 */
	public RecoveryServer getRecoveryServer()
	{
		return recoveryServer;
	}

	/**
	 * Initialize the JMX server.
	 */
	void initRecoveryServer()
	{
		if (config.getBoolean("jppf.recovery.enabled", true))
		{
			recoveryServer = new RecoveryServer();
			new Thread(recoveryServer, "RecoveryServer thread").start();
		}
	}

	/**
	 * Stop the JMX server.
	 */
	void stopRecoveryServer()
	{
		if (recoveryServer != null) recoveryServer.close();
	}

	/**
	 * Get the object that collects debug information.
	 * @return a {@link ServerDebug} instance.
	 */
	public ServerDebug getServerDebug()
	{
		return serverDebug;
	}
}
