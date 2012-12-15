/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import java.util.*;

import javax.management.*;

import org.jppf.comm.discovery.*;
import org.jppf.comm.recovery.RecoveryServer;
import org.jppf.management.*;
import org.jppf.management.spi.*;
import org.jppf.server.debug.*;
import org.jppf.server.event.NodeConnectionEventHandler;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.peer.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Handles various initializations for the driver.
 * @author Laurent Cohen
 * @exclude
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
   * Constant for JPPF automatic connection discovery
   */
  protected static final String VALUE_JPPF_DISCOVERY = "jppf_discovery";
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
   * The jmx server used to manage and monitor this driver over a secure connection.
   */
  private JMXServer sslJmxServer = null;
  /**
   * The object that collects debug information.
   */
  private ServerDebug serverDebug = null;
  /**
   * The server used to detect that individual connections are broken due to hardware failures.
   */
  private RecoveryServer recoveryServer = null;
  /**
   * Handles listeners to node connection events.
   */
  private final NodeConnectionEventHandler nodeConnectionEventHandler = new NodeConnectionEventHandler();
  /**
   * Holds the soft cache of classes downlaoded form the clients r from this driver's classpath.
   */
  private final ClassCache classCache = new ClassCache();

  /**
   * Instantiate this initializer with the specified driver.
   * @param driver the driver to initialize.
   */
  public DriverInitializer(final JPPFDriver driver)
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
        server.registerMBean(mbean, new ObjectName(ServerDebugMBean.MBEAN_NAME));
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
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
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
   * Read configuration for the host name and ports used to connect to this driver.
   * @return a <code>DriverConnectionInformation</code> instance.
   */
  public JPPFConnectionInformation getConnectionInformation()
  {
    if (connectionInfo == null)
    {
      connectionInfo = new JPPFConnectionInformation();
      connectionInfo.uuid = driver.getUuid();
      String s = config.getAndReplaceString("jppf.server.port", "class.server.port", "11111", false);
      connectionInfo.serverPorts = parsePorts(s, 11111);
      s = config.getString("jppf.ssl.server.port", null);
      connectionInfo.sslServerPorts = s != null ? parsePorts(s, -1) : null;
      connectionInfo.host = NetworkUtils.getManagementHost();
      if (config.getBoolean("jppf.management.enabled", true))
        connectionInfo.managementPort = config.getInt("jppf.management.port", 11198);
      if (config.getBoolean("jppf.management.ssl.enabled", false))
        connectionInfo.sslManagementPort = config.getInt("jppf.management.ssl.port", 11193);
      boolean recoveryEnabled = config.getBoolean("jppf.recovery.enabled", false);
      if (recoveryEnabled) connectionInfo.recoveryPort = config.getInt("jppf.recovery.server.port", 22222);
    }
    return connectionInfo;
  }

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
    boolean initPeers;
    TypedProperties props = JPPFConfiguration.getProperties();
    boolean ssl = props.getBoolean("jppf.peer.ssl.enabled", false);
    if (props.getBoolean("jppf.peer.discovery.enabled", false))
    {
      if (debugEnabled) log.debug("starting peers discovery");
      peerDiscoveryThread = new PeerDiscoveryThread(new PeerDiscoveryThread.ConnectionHandler() {
        @Override
        public void onNewConnection(final String name, final JPPFConnectionInformation info) {
          new JPPFPeerInitializer(name, info, classServer).start();
        }
      }, null, getConnectionInformation());
      initPeers = false;
    }
    else
    {
      peerDiscoveryThread = null;
      initPeers = true;
    }

    String discoveryNames = props.getString("jppf.peers");
    if ((discoveryNames != null) && !"".equals(discoveryNames.trim()))
    {
      if (debugEnabled) log.debug("found peers in the configuration");
      String[] names = discoveryNames.split("\\s");
      for (String name : names) {
        initPeers |= VALUE_JPPF_DISCOVERY.equals(name);
      }

      if(initPeers)
      {
        for (String name : names)
        {
          if (!VALUE_JPPF_DISCOVERY.equals(name))
          {
            JPPFConnectionInformation info = new JPPFConnectionInformation();
            info.host = props.getString(String.format("jppf.peer.%s.server.host", name), "localhost");
            // to keep backward compatibility with v2.x configurations
            String s = props.getAndReplaceString(String.format("jppf.peer.%s.server.port", name), String.format("class.peer.%s.server.port", name), "11111", false);
            if (ssl) info.sslServerPorts = StringUtils.parseIntValues(s);
            else info.serverPorts = StringUtils.parseIntValues(s);
            if(peerDiscoveryThread != null) peerDiscoveryThread.addConnectionInformation(info);
            new JPPFPeerInitializer(name, info, classServer).start();
          }
        }
      }
    }

    if(peerDiscoveryThread != null)
    {
      new Thread(peerDiscoveryThread, "PeerDiscovery").start();
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
   * @param ssl specifies whether to get the ssl-based connector server. 
   * @return a <code>JMXServerImpl</code> instance.
   */
  public synchronized JMXServer getJmxServer(final boolean ssl)
  {
    return ssl ? sslJmxServer : jmxServer;
  }

  /**
   * Initialize the JMX server.
   */
  void initJmxServer()
  {
    jmxServer = createJMXServer(false);
    sslJmxServer = createJMXServer(true);
  }

  /**
   * Create a JMX connector server.
   * @param ssl specifies whether JMX communication should be done via SSL/TLS.
   * @return a new {@link JMXServer} instance, or null if the server could not be created.
   */
  private JMXServer createJMXServer(final boolean ssl)
  {
    JMXServer server = null;
    JPPFConnectionInformation info = getConnectionInformation();
    String prop = ssl ? "jppf.management.ssl.enabled" : "jppf.management.enabled";
    String tmp = ssl ? "secure " : "";
    try
    {
      // default is false for ssl, true for plain connection
      if (config.getBoolean(prop, !ssl))
      {
        server = JMXServerFactory.createServer(driver.getUuid(), ssl);
        server.start(getClass().getClassLoader());
        if (!ssl) info.managementPort = server.getManagementPort();
        else info.sslManagementPort = server.getManagementPort();
        System.out.println(tmp + "management initialized and listening on port " + server.getManagementPort());
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      config.setProperty(prop, "false");
      String s = e.getMessage();
      s = (s == null) ? "<none>" : s.replace("\t", "  ").replace("\n", " - ");
      System.out.println(tmp + "management failed to initialize, with error message: '" + s + '\'');
      System.out.println(tmp + "management features are disabled. Please consult the driver's log file for more information");
    }
    return server;
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
   * Initialize the recovery server.
   */
  void initRecoveryServer()
  {
    if (config.getBoolean("jppf.recovery.enabled", false))
    {
      recoveryServer = new RecoveryServer();
      new Thread(recoveryServer, "RecoveryServer thread").start();
    }
  }

  /**
   * Stop the recovery server.
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

  /**
   * Get the object that handles listeners to node connection events.
   * @return a {@link NodeConnectionEventHandler} instance.
   */
  public NodeConnectionEventHandler getNodeConnectionEventHandler()
  {
    return nodeConnectionEventHandler;
  }

  /**
   * Get the soft cache of classes downloaded form the clients r from this driver's classpath.
   * @return an instance of {@link ClassCache}.
   */
  public ClassCache getClassCache()
  {
    return classCache;
  }

  /**
   * Parse an array of port numbers from a string containing a list of space-separated port numbers.
   * @param s list of space-separated port numbers
   * @param def the default port number to use if none is specified or valid.
   * @return an array of int port numbers.
   */
  private int[] parsePorts(final String s, final int def)
  {
    String[] strPorts = s.split("\\s");
    List<Integer> portsList = new ArrayList<Integer>(strPorts.length);
    for (int i=0; i<strPorts.length; i++)
    {
      try
      {
        int n = Integer.valueOf(strPorts[i].trim());
        portsList.add(n);
      }
      catch(NumberFormatException e)
      {
        if (debugEnabled) log.debug("invalid port number value '" + strPorts[i] + "'");
      }
    }
    if (portsList.isEmpty() && (def > 0)) portsList.add(def);
    int[] ports = new int[portsList.size()];
    for (int i=0; i<ports.length; i++) ports[i] = portsList.get(i);
    return ports;
  }
}
