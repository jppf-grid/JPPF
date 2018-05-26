/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import static org.jppf.utils.configuration.JPPFProperties.*;

import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.*;

import javax.management.*;

import org.jppf.comm.discovery.*;
import org.jppf.comm.recovery.RecoveryServer;
import org.jppf.discovery.*;
import org.jppf.jmx.JMXHelper;
import org.jppf.load.balancer.ChannelAwareness;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;
import org.jppf.management.spi.*;
import org.jppf.persistence.*;
import org.jppf.server.debug.*;
import org.jppf.server.event.NodeConnectionEventHandler;
import org.jppf.server.nio.classloader.ClassCache;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.server.peer.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Handles various initializations for the driver.
 * @author Laurent Cohen
 * @exclude
 */
public class DriverInitializer {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(DriverInitializer.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
   * Supports built-in and custom discovery mechanisms.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  final DriverDiscoveryHandler<DriverConnectionInfo> discoveryHandler = new DriverDiscoveryHandler(PeerDriverDiscovery.class);
  /**
   * Listens to new connection notifications from {@link PeerDriverDiscovery} instances.
   */
  private PeerDriverDiscoveryListener discoveryListener;
  /**
   * Handles the pools of connections to remote peer drivers.
   */
  final PeerConnectionPoolHandler peerConnectionPoolHandler = new PeerConnectionPoolHandler();

  /**
   * Instantiate this initializer with the specified driver.
   * @param driver the driver to initialize.
   * @param config the driver's configuration.
   */
  public DriverInitializer(final JPPFDriver driver, final TypedProperties config) {
    this.driver = driver;
    this.config = config;
  }

  /**
   * Register the MBean that collects debug/troubleshooting information.
   */
  void handleDebugActions() {
    if (JPPFDriver.JPPF_DEBUG) {
      if (debugEnabled) log.debug("registering deadlock detector");
      DeadlockDetector.setup("driver");
      if (debugEnabled) log.debug("registering debug mbean");
      try {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        serverDebug = new ServerDebug();
        final StandardMBean mbean = new StandardMBean(serverDebug, ServerDebugMBean.class);
        server.registerMBean(mbean, ObjectNameCache.getObjectName(ServerDebugMBean.MBEAN_NAME));
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Register all MBeans defined through the service provider interface.
   * @throws Exception if the registration failed.
   */
  void registerProviderMBeans() throws Exception {
    final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    new JPPFMBeanProviderManager<>(JPPFDriverMBeanProvider.class, null, server);
    registerNodeConfigListener();
  }

  /**
   * Read configuration for the host name and ports used to connect to this driver.
   * @return a <code>DriverConnectionInformation</code> instance.
   */
  public JPPFConnectionInformation getConnectionInformation() {
    if (connectionInfo == null) {
      connectionInfo = new JPPFConnectionInformation();
      connectionInfo.uuid = driver.getUuid();
      String s = config.getString("jppf.server.port", "11111");
      connectionInfo.serverPorts = parsePorts(s, 11111);
      s = config.getString("jppf.ssl.server.port", null);
      connectionInfo.sslServerPorts = s != null ? parsePorts(s, -1) : null;
      try {
        connectionInfo.host = InetAddress.getLocalHost().getHostName();
      } catch(@SuppressWarnings("unused") final UnknownHostException e) {
        connectionInfo.host = "localhost";
      }
      if (config.get(RECOVERY_ENABLED)) connectionInfo.recoveryPort = config.get(RECOVERY_SERVER_PORT);
    }
    return connectionInfo;
  }

  /**
   * Initialize and start the discovery service.
   */
  public void initBroadcaster() {
    if (config.get(DISCOVERY_ENABLED)) {
      if (debugEnabled) log.debug("initializing broadcaster");
      broadcaster = new JPPFBroadcaster(getConnectionInformation());
      ThreadUtils.startThread(broadcaster, "JPPF Broadcaster");
    }
  }

  /**
   * Stop the discovery service if it is running.
   */
  public void stopBroadcaster() {
    if (broadcaster != null) {
      if (debugEnabled) log.debug("stopping broadcaster");
      broadcaster.close();
      broadcaster = null;
    }
  }

  /**
   * Determine whether broadcasting is active.
   * @return {@code true} if broadcast is active, {@code false} otherwise.
   * @since 4.2
   */
  public boolean isBroadcasting() {
    return (broadcaster != null) && !broadcaster.isStopped();
  }

  /**
   * Initialize this driver's peers.
   * @param classServer JPPF class server
   */
  void initPeers(final ClientClassNioServer classServer) {
    boolean initPeers;
    final TypedProperties props = JPPFConfiguration.getProperties();
    final boolean ssl = props.get(PEER_SSL_ENABLED);
    final boolean enabled = props.get(PEER_DISCOVERY_ENABLED);
    if (debugEnabled) log.debug("{} = {}", PEER_DISCOVERY_ENABLED.getName(), enabled);
    if (enabled) {
      if (debugEnabled) log.debug("starting peers discovery");
      peerDiscoveryThread = new PeerDiscoveryThread(new PeerDiscoveryThread.ConnectionHandler() {
        @Override
        public void onNewConnection(final String name, final JPPFConnectionInformation info) {
          peerDiscoveryThread.addConnectionInformation(info);
          peerConnectionPoolHandler.newPool(name, JPPFConfiguration.get(PEER_POOL_SIZE), info, ssl, false);
        }
      }, new IPFilter(props, true), getConnectionInformation());
      initPeers = false;
    } else {
      peerDiscoveryThread = null;
      initPeers = true;
    }
    final String discoveryNames = props.get(PEERS);
    if (debugEnabled) log.debug("discoveryNames = {}", discoveryNames);
    if ((discoveryNames != null) && !discoveryNames.trim().isEmpty()) {
      if (debugEnabled) log.debug("found peers in the configuration");
      final String[] names = RegexUtils.SPACES_PATTERN.split(discoveryNames);
      for (String name : names) initPeers |= VALUE_JPPF_DISCOVERY.equals(name);
      if (initPeers) {
        for (final String name : names) {
          if (!VALUE_JPPF_DISCOVERY.equals(name)) {
            final JPPFConnectionInformation info = new JPPFConnectionInformation();
            info.host = props.get(PARAM_PEER_SERVER_HOST, name);
            final int[] ports = { props.get(PARAM_PEER_SERVER_PORT, name) };
            boolean peerSSL = ssl;
            if (props.containsKey(PARAM_PEER_SSL_ENABLED.resolveName(new String[] {name}))) peerSSL = props.get(PARAM_PEER_SSL_ENABLED, name);
            if (peerSSL) info.sslServerPorts = ports;
            else info.serverPorts = ports;
            final int size = props.get(PARAM_PEER_POOL_SIZE, name);
            if (peerDiscoveryThread != null) peerDiscoveryThread.addConnectionInformation(info);
            if (debugEnabled) log.debug(String.format("read peer configuration: name=%s, size=%d, secure=%b, info=%s", name, size, peerSSL, info));
            peerConnectionPoolHandler.newPool(name, size, info, peerSSL, false);
          }
        }
      }
    }
    if (peerDiscoveryThread != null) ThreadUtils.startThread(peerDiscoveryThread, "PeerDiscovery");
    discoveryListener = new PeerDriverDiscoveryListener();
    discoveryHandler.register(discoveryListener.open()).start();
  }

  /**
   * Get the thread that performs the peer servers discovery.
   * @return a <code>PeerDiscoveryThread</code> instance.
   */
  public PeerDiscoveryThread getPeerDiscoveryThread() {
    return peerDiscoveryThread;
  }

  /**
   * Stop the peer discovery thread if it is running.
   */
  void stopPeerDiscoveryThread() {
    if (peerDiscoveryThread != null) {
      peerDiscoveryThread.setStopped(true);
      peerDiscoveryThread = null;
    }
  }

  /**
   * Get the jmx server used to manage and monitor this driver.
   * @param ssl specifies whether to get the ssl-based connector server. 
   * @return a <code>JMXServerImpl</code> instance.
   */
  public synchronized JMXServer getJmxServer(final boolean ssl) {
    return ssl ? sslJmxServer : jmxServer;
  }

  /**
   * Initialize the JMX server.
   */
  void initJmxServer() {
    jmxServer = createJMXServer(false);
    sslJmxServer = createJMXServer(true);
  }

  /**
   * Create a JMX connector server.
   * @param ssl specifies whether JMX communication should be done via SSL/TLS.
   * @return a new {@link JMXServer} instance, or null if the server could not be created.
   */
  private JMXServer createJMXServer(final boolean ssl) {
    JMXServer server = null;
    final JPPFProperty<Boolean> prop = ssl ? MANAGEMENT_SSL_ENABLED : MANAGEMENT_ENABLED;
    final String tmp = ssl ? "secure " : "";
    try {
      // default is false for ssl, true for plain connection
      if (config.get(prop)) {
        if (debugEnabled) log.debug("initializing {}management", tmp);
        final String protocol = JPPFConfiguration.get(JMX_REMOTE_PROTOCOL);
        JPPFProperty<Integer> jmxProp = null;
        if (JMXHelper.JPPF_JMX_PROTOCOL.equals(protocol)) jmxProp = ssl ? SERVER_SSL_PORT : SERVER_PORT;
        else jmxProp = ssl ? MANAGEMENT_SSL_PORT : MANAGEMENT_PORT;
        server = JMXServerFactory.createServer(driver.getUuid(), ssl, jmxProp);
        server.start(getClass().getClassLoader());
        final String msg = String.format("%smanagement initialized and listening on port %s", tmp, server.getManagementPort());
        System.out.println(msg);
        if (debugEnabled) log.debug(msg);
      }
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      config.set(prop, false);
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
  void stopJmxServer() {
    try {
      if (debugEnabled) log.debug("stopping JMX server");
      if (jmxServer != null) jmxServer.stop();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * The server used to detect that individual connections are broken due to hardware failures.
   * @return a {@link RecoveryServer} instance.
   */
  public RecoveryServer getRecoveryServer() {
    return recoveryServer;
  }

  /**
   * Initialize the recovery server.
   */
  public void initRecoveryServer() {
    if (config.get(RECOVERY_ENABLED)) {
      if (debugEnabled) log.debug("initializing recovery server");
      recoveryServer = new RecoveryServer();
      ThreadUtils.startThread(recoveryServer, "RecoveryServer thread");
    }
  }

  /**
   * Stop the recovery server.
   */
  public void stopRecoveryServer() {
    if (recoveryServer != null) recoveryServer.close();
  }

  /**
   * Get the object that collects debug information.
   * @return a {@link ServerDebug} instance.
   */
  public ServerDebug getServerDebug() {
    return serverDebug;
  }

  /**
   * Get the object that handles listeners to node connection events.
   * @return a {@link NodeConnectionEventHandler} instance.
   */
  public NodeConnectionEventHandler getNodeConnectionEventHandler() {
    return nodeConnectionEventHandler;
  }

  /**
   * Get the soft cache of classes downloaded form the clients r from this driver's classpath.
   * @return an instance of {@link ClassCache}.
   */
  public ClassCache getClassCache() {
    return classCache;
  }

  /**
   *
   */
  void registerNodeConfigListener() {
    if (debugEnabled) log.debug("registering NodeConfigListener");
    try (JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper()) {
      jmx.connect();
      final NotificationListener listener = new NotificationListener() {
        @Override
        public void handleNotification(final Notification notification, final Object handback) {
          final Notification notif = ((JPPFNodeForwardingNotification) notification).getNotification();
          final String nodeUuid = (String) notif.getSource();
          final TypedProperties nodeConfig = (TypedProperties) notif.getUserData();
          if (debugEnabled) log.debug("received notification for node {}, nb threads={}", nodeUuid, nodeConfig.get(JPPFProperties.PROCESSING_THREADS));
          final AbstractNodeContext node = driver.getNodeNioServer().getConnection(nodeUuid);
          if (node == null) return;
          synchronized(node.getMonitor()) {
            final TypedProperties oldConfig = node.getSystemInformation().getJppf();
            oldConfig.clear();
            oldConfig.putAll(nodeConfig);
            if (node.getBundler() instanceof ChannelAwareness) ((ChannelAwareness) node.getBundler()).setChannelConfiguration(node.getSystemInformation());
          }
        }
      };
      jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, NodeConfigNotifierMBean.MBEAN_NAME, listener, null, null);
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Parse an array of port numbers from a string containing a list of space-separated port numbers.
   * @param s list of space-separated port numbers
   * @param def the default port number to use if none is specified or valid.
   * @return an array of int port numbers.
   */
  private static int[] parsePorts(final String s, final int def) {
    final String[] strPorts = RegexUtils.SPACES_PATTERN.split(s);
    final List<Integer> portsList = new ArrayList<>(strPorts.length);
    for (int i=0; i<strPorts.length; i++) {
      try {
        final int n = Integer.valueOf(strPorts[i].trim());
        portsList.add(n);
      } catch(@SuppressWarnings("unused") final NumberFormatException e) {
        if (debugEnabled) log.debug("invalid port number value '" + strPorts[i] + "'");
      }
    }
    if (portsList.isEmpty() && (def > 0)) portsList.add(def);
    final int[] ports = new int[portsList.size()];
    for (int i=0; i<ports.length; i++) ports[i] = portsList.get(i);
    return ports;
  }

  /**
   * Get the discovered peers connection information.
   * @return a set of {@link DriverConnectionInfo} instances.
   * @exclude
   */
  public Set<DriverConnectionInfo> getDiscoveredPeers() {
    return discoveryListener.getDiscoveredPools();
  }

  /**
   * Create and initialize the datasources found inthe configurarion.
   */
  void initDatasources() {
    final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
    final TypedProperties config = JPPFConfiguration.getProperties();
    factory.configure(config, JPPFDatasourceFactory.Scope.LOCAL);
    //factory.configure(config, JPPFDatasourceFactory.Scope.ANY);
  }
}
