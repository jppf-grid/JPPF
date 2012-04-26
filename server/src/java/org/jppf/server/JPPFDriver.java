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
import org.jppf.process.LauncherListener;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.NioServer;
import org.jppf.server.nio.acceptor.AcceptorNioServer;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.client.ClientNioServer;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.JPPFNode;
import org.jppf.server.node.local.JPPFLocalNode;
import org.jppf.server.queue.*;
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
  private static JPPFNode localNode = null;
  /**
   * The queue that handles the tasks to execute. Objects are added to, and removed from, this queue, asynchronously and by multiple threads.
   */
  private JPPFQueue taskQueue = null;
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
  private ClassNioServer classServer = null;
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
  private JPPFDriverStatsUpdater statsUpdater = new JPPFDriverStatsUpdater();
  /**
   * Generates the statistics events of which all related listeners are notified.
   */
  private JPPFDriverStatsManager statsManager = new JPPFDriverStatsManager();
  /**
   * Manages and monitors the jobs throughout their processing within this driver.
   */
  private JPPFJobManager jobManager = null;
  /**
   * Uuid for this driver.
   */
  private String uuid = JPPFConfiguration.getProperties().getString("jppf.driver.uuid", new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString().toUpperCase());
  /**
   * Performs initialization of the driver's components.
   */
  private DriverInitializer initializer = null;
  /**
   * Manages information about the nodes.
   */
  private NodeInformationHandler nodeHandler = null;

  /**
   * Initialize this JPPFDriver.
   */
  protected JPPFDriver()
  {
    int pid = SystemUtils.getPID();
    if (pid > 0) System.out.println("driver process id: " + pid);
    // initialize the jmx logger
    new JmxMessageNotifier();
    nodeHandler = new NodeInformationHandler();
    statsManager.addListener(statsUpdater);
    initializer = new DriverInitializer(this);
    if (debugEnabled) log.debug("instantiating JPPF driver with uuid=" + uuid);
  }

  /**
   * Initialize and start this driver.
   * @throws Exception if the initialization fails.
   */
  @SuppressWarnings("unchecked")
  public void run() throws Exception
  {
    jobManager = new JPPFJobManager();
    taskQueue = new JPPFPriorityQueue();
    ((JPPFPriorityQueue) taskQueue).addQueueListener(jobManager);
    JPPFConnectionInformation info = initializer.getConnectionInformation();
    TypedProperties config = JPPFConfiguration.getProperties();

    initializer.registerDebugMBean();
    initializer.initRecoveryServer();

    initializer.initJmxServer();
    new JPPFStartupLoader().load(JPPFDriverStartupSPI.class);
    initializer.getNodeConnectionEventHandler().loadListeners();

    RecoveryServer recoveryServer = initializer.getRecoveryServer();
    classServer = startServer(recoveryServer, new ClassNioServer(null), null);
    clientNioServer = startServer(recoveryServer, new ClientNioServer(null), null);
    nodeNioServer = startServer(recoveryServer, new NodeNioServer(null), null);
    acceptorServer = startServer(recoveryServer, new AcceptorNioServer(info.serverPorts), info.serverPorts);

    if (config.getBoolean("jppf.local.node.enabled", false))
    {
      LocalClassLoaderChannel localClassChannel = new LocalClassLoaderChannel(new LocalClassContext());
      localClassChannel.getContext().setChannel(localClassChannel);
      LocalNodeChannel localNodeChannel = new LocalNodeChannel(new LocalNodeContext());
      localNodeChannel.getContext().setChannel(localNodeChannel);
      localNode = new JPPFLocalNode(localNodeChannel, localClassChannel);
      classServer.initLocalChannel(localClassChannel);
      nodeNioServer.initLocalChannel(localNodeChannel);
      new Thread(localNode, "Local node").start();
    }

    initializer.initBroadcaster();
    initializer.initPeers(classServer);
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
   */
  public static JPPFQueue getQueue()
  {
    return getInstance().taskQueue;
  }

  /**
   * Get the JPPF client server.
   * @return a <code>ClientNioServer</code> instance.
   */
  public ClientNioServer getClientNioServer()
  {
    return clientNioServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   */
  public ClassNioServer getClassServer()
  {
    return classServer;
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   */
  public NodeNioServer getNodeNioServer()
  {
    return nodeNioServer;
  }

  /**
   * Get the server which handles the initial handshake and peer channel identification.
   * @return a {@link AcceptorNioServer} instance.
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
   */
  public void initiateShutdownRestart(final long shutdownDelay, final boolean restart, final long restartDelay)
  {
    log.info("Scheduling server shutdown in " + shutdownDelay + " ms");
    shuttingDown = true;

    if(classServer != null) classServer.shutdown();
    if(nodeNioServer != null) nodeNioServer.shutdown();
    if(clientNioServer != null) clientNioServer.shutdown();
    if(acceptorServer != null) acceptorServer.shutdown();

    Timer timer = new Timer();
    ShutdownRestartTask task = new ShutdownRestartTask(timer, restart, restartDelay);
    timer.schedule(task, (shutdownDelay <= 0L) ? 0L : shutdownDelay);
  }

  /**
   * Shutdown this server and all its components.
   */
  public void shutdown()
  {
    log.info("Shutting down");
    initializer.stopBroadcaster();
    initializer.stopPeerDiscoveryThread();
    if(classServer != null) {
      classServer.end();
      classServer = null;
    }
    if (nodeNioServer != null)
    {
      nodeNioServer.end();
      nodeNioServer = null;
    }
    if (acceptorServer != null)
    {
      acceptorServer.end();
      acceptorServer = null;
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
   */
  public JPPFDriverStatsUpdater getStatsUpdater()
  {
    return statsUpdater;
  }

  /**
   * Get a reference to the object that generates the statistics events of which all related listeners are notified.
   * @return a <code>JPPFDriverStatsManager</code> instance.
   */
  public JPPFDriverStatsManager getStatsManager()
  {
    return statsManager;
  }

  /**
   * Get the object that manages and monitors the jobs throughout their processing within this driver.
   * @return an instance of <code>JPPFJobManager</code>.
   */
  public JPPFJobManager getJobManager()
  {
    return jobManager;
  }

  /**
   * Get this driver's initializer.
   * @return a <code>DriverInitializer</code> instance.
   */
  public DriverInitializer getInitializer()
  {
    return initializer;
  }

  /**
   * Get the object that manages information about the nodes.
   * @return a {@link NodeInformationHandler} instance.
   */
  public NodeInformationHandler getNodeHandler()
  {
    return nodeHandler;
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
      {
        throw new JPPFException("The driver should be run with an argument representing a valid TCP port or 'noLauncher'");
      }
      if (!"noLauncher".equals(args[0]))
      {
        int port = Integer.parseInt(args[0]);
        new LauncherListener(port).start();
      }

      instance = new JPPFDriver();
      instance.run();
      //JPPFDriver driver = getInstance();
      //driver.run();
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
   * @param <T> the type of the server to start
   * @return started nioServer
   */
  protected static <T extends NioServer> T startServer(final RecoveryServer recoveryServer, final T nioServer, final int[] ports) {
    if(nioServer == null) throw new IllegalArgumentException("nioServer is null");
    if(recoveryServer != null && nioServer instanceof ReaperListener) {
      Reaper reaper = recoveryServer.getReaper();
      reaper.addReaperListener((ReaperListener) nioServer);
    }
    nioServer.start();
    printInitializedMessage(ports, nioServer.getName());
    return nioServer;
  }

  /**
   * Print a message to the console to signify that the initialization of a server was successful.
   * @param ports the ports on which the server is listening.
   * @param name the name to use for the server.
   */
  protected static void printInitializedMessage(final int[] ports, final String name)
  {
    StringBuilder sb = new StringBuilder();
    if (name != null)
    {
      sb.append(name);
      sb.append(" initialized");
    }
    if (ports != null)
    {
      if (name != null) sb.append(" - ");
      sb.append("accepting connections on port");
      if (ports.length > 1) sb.append('s');
      for (int n: ports) sb.append(' ').append(n);
    }
    System.out.println(sb.toString());
  }

  /**
   * Get a reference to the local node if it is enabled.
   * @return a {@link JPPFNode} instance, or <code>null</code> if local node is disabled.
   */
  public static JPPFNode getLocalNode()
  {
    return localNode;
  }
}
