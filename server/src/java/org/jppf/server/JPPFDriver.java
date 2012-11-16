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

import java.util.Timer;

import org.jppf.JPPFException;
import org.jppf.classloader.LocalClassLoaderChannel;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.recovery.*;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.process.LauncherListener;
import org.jppf.queue.JPPFQueue;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.NioServer;
import org.jppf.server.nio.acceptor.AcceptorNioServer;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.server.nio.classloader.node.NodeClassNioServer;
import org.jppf.server.nio.client.ClientNioServer;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.JPPFNode;
import org.jppf.server.node.local.JPPFLocalNode;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.startup.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class serves as an initializer for the entire JPPF server. It follows the singleton pattern and provides access,
 * across the JVM, to the tasks execution queue.
 * <p>It also holds a server for incoming client connections, a server for incoming node connections, along with a class server
 * to handle requests to and from remote class loaders.
 * @author Laurent Cohen
 */
public class JPPFDriver
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
   * This listener gathers the statistics published through the management interface.
   */
  private final JPPFDriverStatsUpdater statsUpdater;
  /**
   * Generates the statistics events of which all related listeners are notified.
   */
  private final JPPFDriverStatsManager statsManager;
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
  protected JPPFDriver()
  {
    config = JPPFConfiguration.getProperties();
    uuid = config.getString("jppf.driver.uuid", new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString().toUpperCase());
    int pid = SystemUtils.getPID();
    if (pid > 0) System.out.println("driver process id: " + pid);
    // initialize the jmx logger
    new JmxMessageNotifier();
    String hrule = StringUtils.padRight("", '-', 80);
    log.info(hrule);
    log.info(VersionUtils.getVersionInformation());
    log.info("starting JPPF driver with PID=" + pid + " , uuid=" + uuid);
    log.info(hrule);
    systemInformation = new JPPFSystemInformation(uuid, false, true);
    statsUpdater = new JPPFDriverStatsUpdater();
    statsManager = new JPPFDriverStatsManager();
    statsManager.addListener(statsUpdater);
    jobManager = new JPPFJobManager();
    taskQueue = new JPPFPriorityQueue(statsManager, jobManager);
    initializer = new DriverInitializer(this, config);
  }

  /**
   * Initialize and start this driver.
   * @throws Exception if the initialization fails.
   * @exclude
   */
  @SuppressWarnings("unchecked")
  public void run() throws Exception
  {
    JPPFConnectionInformation info = initializer.getConnectionInformation();

    initializer.registerDebugMBean();
    initializer.initRecoveryServer();

    initializer.initJmxServer();
    if (isManagementEnabled(config)) initializer.registerProviderMBeans();
    new JPPFStartupLoader().load(JPPFDriverStartupSPI.class);
    initializer.getNodeConnectionEventHandler().loadListeners();

    RecoveryServer recoveryServer = initializer.getRecoveryServer();
    clientClassServer = startServer(recoveryServer, new ClientClassNioServer(this), null, null);
    nodeClassServer = startServer(recoveryServer, new NodeClassNioServer(this), null, null);
    clientNioServer = startServer(recoveryServer, new ClientNioServer(this), null, null);
    nodeNioServer = startServer(recoveryServer, new NodeNioServer(this, taskQueue), null, null);
    acceptorServer = startServer(recoveryServer, new AcceptorNioServer(info.serverPorts, info.sslServerPorts), info.serverPorts, info.sslServerPorts);

    if (config.getBoolean("jppf.local.node.enabled", false))
    {
      LocalClassLoaderChannel localClassChannel = new LocalClassLoaderChannel(new LocalClassContext());
      localClassChannel.getContext().setChannel(localClassChannel);
      LocalNodeChannel localNodeChannel = new LocalNodeChannel(new LocalNodeContext(nodeNioServer.getTransitionManager()));
      localNodeChannel.getContext().setChannel(localNodeChannel);
      localNode = new JPPFLocalNode(localNodeChannel, localClassChannel);
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
  public static JPPFDriver getInstance()
  {
    return instance;
  }

  /**
   * Get the queue that handles the tasks to execute.
   * @return a JPPFQueue instance.
   * @exclude
   */
  public static JPPFQueue<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode> getQueue()
  {
    return getInstance().taskQueue;
  }

  /**
   * Get the JPPF client server.
   * @return a <code>ClientNioServer</code> instance.
   * @exclude
   */
  public ClientNioServer getClientNioServer()
  {
    return clientNioServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   * @exclude
   */
  public ClassNioServer getClientClassServer()
  {
    return clientClassServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   * @exclude
   */
  public ClassNioServer getNodeClassServer()
  {
    return nodeClassServer;
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  public NodeNioServer getNodeNioServer()
  {
    return nodeNioServer;
  }

  /**
   * Get the server which handles the initial handshake and peer channel identification.
   * @return a {@link AcceptorNioServer} instance.
   * @exclude
   */
  public AcceptorNioServer getAcceptorServer()
  {
    return acceptorServer;
  }

  /**
   * Determines whether this server has initiated a shutdown, in which case it does not accept connections anymore.
   * @return true if a shutdown is initiated, false otherwise.
   */
  public boolean isShuttingDown()
  {
    return shuttingDown;
  }

  /**
   * Get this driver's unique identifier.
   * @return the uuid as a string.
   */
  public String getUuid()
  {
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
  public void initiateShutdownRestart(final long shutdownDelay, final boolean restart, final long restartDelay)
  {
    log.info("Scheduling server shutdown in " + shutdownDelay + " ms");
    shuttingDown = true;

    if (clientClassServer != null) clientClassServer.shutdown();
    if (nodeClassServer != null) nodeClassServer.shutdown();
    if (nodeNioServer != null) nodeNioServer.shutdown();
    if (clientNioServer != null) clientNioServer.shutdown();
    if (acceptorServer != null) acceptorServer.shutdown();

    Timer timer = new Timer();
    ShutdownRestartTask task = new ShutdownRestartTask(timer, restart, restartDelay, this);
    timer.schedule(task, (shutdownDelay <= 0L) ? 0L : shutdownDelay);
  }

  /**
   * Shutdown this server and all its components.
   * @exclude
   */
  public void shutdown()
  {
    log.info("Shutting down");
    initializer.stopBroadcaster();
    initializer.stopPeerDiscoveryThread();
    if (acceptorServer != null)
    {
      acceptorServer.end();
      acceptorServer = null;
    }
    if (clientClassServer != null)
    {
      clientClassServer.end();
      clientClassServer = null;
    }
    if (nodeClassServer != null)
    {
      nodeClassServer.end();
      nodeClassServer = null;
    }
    if (nodeNioServer != null)
    {
      nodeNioServer.end();
      nodeNioServer = null;
    }
    if (clientNioServer != null)
    {
      clientNioServer.end();
      clientNioServer = null;
    }
    initializer.stopJmxServer();
    jobManager.close();
    initializer.stopRecoveryServer();
  }

  /**
   * Get the listener that gathers the statistics published through the management interface.
   * @return a <code>JPPFStatsUpdater</code> instance.
   * @exclude
   */
  public JPPFDriverStatsUpdater getStatsUpdater()
  {
    return statsUpdater;
  }

  /**
   * Get a reference to the object that generates the statistics events of which all related listeners are notified.
   * @return a <code>JPPFDriverStatsManager</code> instance.
   * @exclude
   */
  public JPPFDriverStatsManager getStatsManager()
  {
    return statsManager;
  }

  /**
   * Get the object that manages and monitors the jobs throughout their processing within this driver.
   * @return an instance of <code>JPPFJobManager</code>.
   * @exclude
   */
  public JPPFJobManager getJobManager()
  {
    return jobManager;
  }

  /**
   * Get this driver's initializer.
   * @return a <code>DriverInitializer</code> instance.
   * @exclude
   */
  public DriverInitializer getInitializer()
  {
    return initializer;
  }

  /**
   * Start the JPPF server.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      if (debugEnabled) log.debug("starting the JPPF driver");
      if ((args == null) || (args.length <= 0))
        throw new JPPFException("The driver should be run with an argument representing a valid TCP port or 'noLauncher'");
      if (!"noLauncher".equals(args[0]))
      {
        int port = Integer.parseInt(args[0]);
        new LauncherListener(port).start();
      }
      instance = new JPPFDriver();
      instance.run();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Start server, register it to recovery server if requested and print initialization message.
   * @param recoveryServer Recovery server for nioServers that implements ReaperListener
   * @param nioServer starting nio server
   * @param ports ports for initialization message
   * @param sslPorts SSL ports for initialization message.
   * @param <T> the type of the server to start
   * @return started nioServer
   */
  private static <T extends NioServer> T startServer(final RecoveryServer recoveryServer, final T nioServer, final int[] ports, final int[] sslPorts) {
    if(nioServer == null) throw new IllegalArgumentException("nioServer is null");
    if(recoveryServer != null && nioServer instanceof ReaperListener) {
      Reaper reaper = recoveryServer.getReaper();
      reaper.addReaperListener((ReaperListener) nioServer);
    }
    nioServer.start();
    printInitializedMessage(ports, sslPorts, nioServer.getName());
    return nioServer;
  }

  /**
   * Print a message to the console to signify that the initialization of a server was successful.
   * @param ports the ports on which the server is listening.
   * @param sslPorts SSL ports for initialization message.
   * @param name the name to use for the server.
   */
  private static void printInitializedMessage(final int[] ports, final int[] sslPorts, final String name)
  {
    StringBuilder sb = new StringBuilder();
    if (name != null)
    {
      sb.append(name);
      sb.append(" initialized");
    }
    if (ports != null || sslPorts != null)
    {
      if ((ports != null) && (ports.length > 0))
      {
        sb.append("\n-  accepting plain connections on port");
        if (ports.length > 1) sb.append('s');
        for (int n: ports) sb.append(' ').append(n);
      }
      if ((sslPorts != null) && (sslPorts.length > 0))
      {
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
  private static boolean isManagementEnabled(final TypedProperties config)
  {
    return config.getBoolean("jppf.management.enabled", true) || config.getBoolean("jppf.management.ssl.enabled", false);
  }

  /**
   * Get the system ibnformation for this driver.
   * @return a {@link JPPFSystemInformation} instance.
   */
  public JPPFSystemInformation getSystemInformation()
  {
    return systemInformation;
  }
}
