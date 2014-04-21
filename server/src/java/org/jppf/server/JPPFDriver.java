/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.recovery.*;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.nio.NioServer;
import org.jppf.node.initialization.InitializationHook;
import org.jppf.process.LauncherListener;
import org.jppf.queue.JPPFQueue;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.acceptor.AcceptorNioServer;
import org.jppf.server.nio.classloader.LocalClassContext;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.server.nio.classloader.node.NodeClassNioServer;
import org.jppf.server.nio.client.ClientNioServer;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.JPPFNode;
import org.jppf.server.node.local.*;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.startup.JPPFDriverStartupSPI;
import org.jppf.utils.*;
import org.jppf.utils.hooks.HookFactory;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * This class serves as an initializer for the entire JPPF server. It follows the singleton pattern and provides access,
 * across the JVM, to the tasks execution queue.
 * <p>It also holds a server for incoming client connections, a server for incoming node connections, along with a class server
 * to handle requests to and from remote class loaders.
 * @author Laurent Cohen
 * @author Lane Schwartz (dynamically allocated server port) 
 */
public class JPPFDriver {
  // this static block must be the first thing executed when this class is loaded
  static {
    JPPFInitializer.init();
  }
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JPPFDriver.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Flag indicating whether collection of debug information is available via JMX.
   */
  public static final boolean JPPF_DEBUG = JPPFConfiguration.getProperties().getBoolean("jppf.debug.enabled", false);
  /**
   * Singleton instance of the JPPFDriver.
   */
  private static JPPFDriver instance = null;
  /**
   * Reference to the local node if it is enabled.
   */
  private JPPFNode localNode = null;
  /**
   * The queue that handles the tasks to execute. Objects are added to, and removed from, this queue, asynchronously and by multiple threads.
   */
  private final JPPFPriorityQueue taskQueue;
  /**
   * Serves the execution requests coming from client applications.
   */
  private ClientNioServer clientNioServer = null;
  /**
   * Serves the JPPF nodes.
   */
  private NodeNioServer nodeNioServer = null;
  /**
   * Serves class loading requests from the JPPF nodes.
   */
  private ClientClassNioServer clientClassServer = null;
  /**
   * Serves class loading requests from the JPPF nodes.
   */
  private NodeClassNioServer nodeClassServer = null;
  /**
   * Handles the initial handshake and peer channel identification.
   */
  private AcceptorNioServer acceptorServer = null;
  /**
   * Determines whether this server has initiated a shutdown, in which case it does not accept connections anymore.
   */
  private boolean shuttingDown = false;
  /**
   * Holds the statistics monitors.
   */
  private final JPPFStatistics statistics = createServerStatistics();
  /**
   * Manages and monitors the jobs throughout their processing within this driver.
   */
  private JPPFJobManager jobManager = null;
  /**
   * Uuid for this driver.
   */
  private final String uuid;
  /**
   * Performs initialization of the driver's components.
   */
  private DriverInitializer initializer = null;
  /**
   * Configuration for this driver.
   */
  private final TypedProperties config;
  /**
   * System ibnformation for this driver.
   */
  private JPPFSystemInformation systemInformation = null;

  /**
   * Initialize this JPPFDriver.
   * @exclude
   */
  protected JPPFDriver() {
    instance = this;
    config = JPPFConfiguration.getProperties();
    String s;
    this.uuid = (s = config.getString("jppf.driver.uuid", null)) == null ? JPPFUuid.normalUUID() : s;
    new JmxMessageNotifier(); // initialize the jmx logger
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    HookFactory.registerSPIMultipleHook(InitializationHook.class, null, null);
    HookFactory.invokeHook(InitializationHook.class, "initializing", new UnmodifiableTypedProperties(config));
    VersionUtils.logVersionInformation("driver", uuid);
    SystemUtils.printPidAndUuid("driver", uuid);
    systemInformation = new JPPFSystemInformation(uuid, false, true);
    jobManager = new JPPFJobManager();
    taskQueue = new JPPFPriorityQueue(this, jobManager);
    initializer = new DriverInitializer(this, config);
  }

  /**
   * Initialize and start this driver.
   * @throws Exception if the initialization fails.
   * @exclude
   */
  @SuppressWarnings("unchecked")
  public void run() throws Exception {
    JPPFConnectionInformation info = initializer.getConnectionInformation();

    initializer.registerDebugMBean();
    initializer.initRecoveryServer();

    initializer.initJmxServer();
    if (isManagementEnabled(config)) initializer.registerProviderMBeans();
    HookFactory.registerSPIMultipleHook(JPPFDriverStartupSPI.class, null, null).invoke("run");
    initializer.getNodeConnectionEventHandler().loadListeners();

    RecoveryServer recoveryServer = initializer.getRecoveryServer();
    int[] sslPorts = extractValidPorts(info.sslServerPorts);
    boolean useSSL = (sslPorts != null) && (sslPorts.length > 0);
    clientClassServer = startServer(recoveryServer, new ClientClassNioServer(this, useSSL));
    nodeClassServer = startServer(recoveryServer, new NodeClassNioServer(this, useSSL));
    clientNioServer = startServer(recoveryServer, new ClientNioServer(this, useSSL));
    nodeNioServer = startServer(recoveryServer, new NodeNioServer(this, taskQueue, useSSL));
    acceptorServer = startServer(recoveryServer, new AcceptorNioServer(extractValidPorts(info.serverPorts), sslPorts));

    if (config.getBoolean("jppf.local.node.enabled", false)) {
      LocalClassLoaderChannel localClassChannel = new LocalClassLoaderChannel(new LocalClassContext());
      localClassChannel.getContext().setChannel(localClassChannel);
      LocalNodeChannel localNodeChannel = new LocalNodeChannel(new LocalNodeContext(nodeNioServer.getTransitionManager()));
      localNodeChannel.getContext().setChannel(localNodeChannel);
      final boolean offline = JPPFConfiguration.getProperties().getBoolean("jppf.node.offline", false);
      localNode = new JPPFLocalNode(new LocalNodeConnection(localNodeChannel), offline  ? null : new LocalClassLoaderConnection(localClassChannel));
      nodeClassServer.initLocalChannel(localClassChannel);
      nodeNioServer.initLocalChannel(localNodeChannel);
      new Thread(localNode, "Local node").start();
    }

    initializer.initBroadcaster();
    //initializer.initPeers(nodeClassServer);
    initializer.initPeers(clientClassServer);
    System.out.println("JPPF Driver initialization complete");
  }

  /**
   * Get the singleton instance of the JPPFDriver.
   * @return a <code>JPPFDriver</code> instance.
   */
  public static JPPFDriver getInstance() {
    return instance;
  }

  /**
   * Get the queue that handles the tasks to execute.
   * @return a JPPFQueue instance.
   * @exclude
   */
  public static JPPFQueue<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode> getQueue() {
    return getInstance().taskQueue;
  }

  /**
   * Get the JPPF client server.
   * @return a <code>ClientNioServer</code> instance.
   * @exclude
   */
  public ClientNioServer getClientNioServer() {
    return clientNioServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   * @exclude
   */
  public ClientClassNioServer getClientClassServer() {
    return clientClassServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   * @exclude
   */
  public NodeClassNioServer getNodeClassServer() {
    return nodeClassServer;
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  public NodeNioServer getNodeNioServer() {
    return nodeNioServer;
  }

  /**
   * Get the server which handles the initial handshake and peer channel identification.
   * @return a {@link AcceptorNioServer} instance.
   * @exclude
   */
  public AcceptorNioServer getAcceptorServer() {
    return acceptorServer;
  }

  /**
   * Determines whether this server has initiated a shutdown, in which case it does not accept connections anymore.
   * @return true if a shutdown is initiated, false otherwise.
   */
  public boolean isShuttingDown() {
    return shuttingDown;
  }

  /**
   * Get this driver's unique identifier.
   * @return the uuid as a string.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Initialize this task with the specified parameters.<br>
   * The shutdown is initiated after the specified shutdown delay has expired.<br>
   * If the restart parameter is set to false then the JVM exits after the shutdown is complete.
   * @param shutdownDelay delay, in milliseconds, after which the server shutdown is initiated. A value of 0 or less
   * means an immediate shutdown.
   * @param restart determines whether the server should restart after shutdown is complete.
   * If set to false, then the JVM will exit.
   * @param restartDelay delay, starting from shutdown completion, after which the server is restarted.
   * A value of 0 or less means the server is restarted immediately after the shutdown is complete.
   * @exclude
   */
  public void initiateShutdownRestart(final long shutdownDelay, final boolean restart, final long restartDelay) {
    log.info("Scheduling server shutdown in " + shutdownDelay + " ms");
    shuttingDown = true;

    if (acceptorServer != null) acceptorServer.shutdown();
    if (clientClassServer != null) clientClassServer.shutdown();
    if (nodeClassServer != null) nodeClassServer.shutdown();
    if (nodeNioServer != null) nodeNioServer.shutdown();
    if (clientNioServer != null) clientNioServer.shutdown();

    Timer timer = new Timer();
    ShutdownRestartTask task = new ShutdownRestartTask(timer, restart, restartDelay, this);
    timer.schedule(task, (shutdownDelay <= 0L) ? 0L : shutdownDelay);
  }

  /**
   * Shutdown this server and all its components.
   * @exclude
   */
  public void shutdown() {
    log.info("Shutting down");
    initializer.stopBroadcaster();
    initializer.stopPeerDiscoveryThread();
    initializer.stopJmxServer();
    jobManager.close();
    initializer.stopRecoveryServer();
  }

  /**
   * Get the object that manages and monitors the jobs throughout their processing within this driver.
   * @return an instance of <code>JPPFJobManager</code>.
   * @exclude
   */
  public JPPFJobManager getJobManager() {
    return jobManager;
  }

  /**
   * Get this driver's initializer.
   * @return a <code>DriverInitializer</code> instance.
   * @exclude
   */
  public DriverInitializer getInitializer() {
    return initializer;
  }

  /**
   * Start the JPPF server.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      if (debugEnabled) log.debug("starting the JPPF driver");
      if ((args == null) || (args.length <= 0))
        throw new JPPFException("The driver should be run with an argument representing a valid TCP port or 'noLauncher'");
      if (!"noLauncher".equals(args[0])) {
        int port = Integer.parseInt(args[0]);
        new LauncherListener(port).start();
      }
      new JPPFDriver().run();
    } catch(Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Start server, register it to recovery server if requested and print initialization message.
   * @param recoveryServer Recovery server for nioServers that implements ReaperListener
   * @param nioServer starting nio server
   * @param <T> the type of the server to start
   * @return started nioServer
   */
  private static <T extends NioServer> T startServer(final RecoveryServer recoveryServer, final T nioServer) {
    if(nioServer == null) throw new IllegalArgumentException("nioServer is null");
    if(recoveryServer != null && nioServer instanceof ReaperListener) {
      Reaper reaper = recoveryServer.getReaper();
      reaper.addReaperListener((ReaperListener) nioServer);
    }
    nioServer.start();
    printInitializedMessage(nioServer.getPorts(), nioServer.getSSLPorts(), nioServer.getName());
    return nioServer;
  }

  /**
   * Print a message to the console to signify that the initialization of a server was successful.
   * @param ports the ports on which the server is listening.
   * @param sslPorts SSL ports for initialization message.
   * @param name the name to use for the server.
   */
  private static void printInitializedMessage(final int[] ports, final int[] sslPorts, final String name) {
    StringBuilder sb = new StringBuilder();
    if (name != null) {
      sb.append(name);
      sb.append(" initialized");
    }
    if (ports != null || sslPorts != null) {
      if ((ports != null) && (ports.length > 0)) {
        sb.append("\n-  accepting plain connections on port");
        if (ports.length > 1) sb.append('s');
        for (int n: ports) sb.append(' ').append(n);
      }
      if ((sslPorts != null) && (sslPorts.length > 0)) {
        sb.append("\n- accepting secure connections on port");
        if (sslPorts.length > 1) sb.append('s');
        for (int n: sslPorts) sb.append(' ').append(n);
      }
    }
    System.out.println(sb.toString());
  }

  /**
   * Determine whether management is enabled and if there is an active remote connector server.
   * @return <code>true</code> if management is enabled, <code>false</code> otherwise.
   * @param config the configuration to test whether management is enabled.
   */
  private static boolean isManagementEnabled(final TypedProperties config) {
    return config.getBoolean("jppf.management.enabled", true) || config.getBoolean("jppf.management.ssl.enabled", false);
  }

  /**
   * Get the system ibnformation for this driver.
   * @return a {@link JPPFSystemInformation} instance.
   */
  public JPPFSystemInformation getSystemInformation() {
    return systemInformation;
  }

  /**
   * Extract only th valid ports from the input array.
   * @param ports the array of port numbers to check.
   * @return an array, possibly of length 0, containing all the valid port numbers in the input array.
   */
  private int[] extractValidPorts(final int[] ports) {
    if ((ports == null) || (ports.length == 0)) return ports;
    List<Integer> list = new ArrayList<>();
    for (int port: ports) {
      if (port >= 0) list.add(port);
    }
    int[] result = new int[list.size()];
    for (int i=0; i<result.length; i++) result[i] = list.get(i);
    return result;
  }

  /**
   * Get the object holding the statitics monitors.
   * @return a {@link JPPFStatistics} instance.
   */
  public JPPFStatistics getStatistics() {
    return statistics;
  }
}
