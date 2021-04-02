/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.io.IOException;
import java.util.Timer;

import org.jppf.JPPFException;
import org.jppf.classloader.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.discovery.PeerDriverDiscovery;
import org.jppf.job.JobTasksListenerManager;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.nio.*;
import org.jppf.nio.acceptor.AcceptorNioServer;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.process.LauncherListener;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.classloader.client.AsyncClientClassNioServer;
import org.jppf.server.nio.classloader.node.*;
import org.jppf.server.nio.client.AsyncClientNioServer;
import org.jppf.server.nio.heartbeat.HeartbeatNioServer;
import org.jppf.server.nio.nodeserver.async.*;
import org.jppf.server.node.JPPFNode;
import org.jppf.server.node.local.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.hooks.HookFactory;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * This class serves as an initializer for the entire JPPF driver.
 * <p>It also holds a server for incoming client connections, a server for incoming node connections, along with a class server to handle requests to and from remote class loaders.
 * @author Laurent Cohen
 * @author Lane Schwartz (dynamically allocated server port) 
 */
public class JPPFDriver extends AbstractJPPFDriver {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFDriver.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Whether the driver was started via the {@link #main(String[]) main()} method.
   */
  boolean startedfromMain = false;

  /**
   * Initialize this JPPF driver with the specified configuration.
   * @param configuration this driver's configuration.
   */
  public JPPFDriver(final TypedProperties configuration) {
    super(configuration);
  }

  /**
   * Initialize and start this driver.
   * @return this driver.
   * @throws Exception if the initialization fails.
   */
  public JPPFDriver start() throws Exception {
    if (debugEnabled) log.debug("starting JPPF driver");
    shutdownScheduled.set(false);
    shuttingDown.set(false);
    systemInformation = new JPPFSystemInformation(configuration, uuid, false, true, statistics);
    statistics.addListener(new StatsSystemInformationUpdater(systemInformation));
    initializer = new DriverInitializer(this, configuration);
    initializer.initDatasources();
    jobManager = new JPPFJobManager(this);
    taskQueue = new JPPFPriorityQueue(this, jobManager);
    if (log.isTraceEnabled()) {
      log.trace("JPPF Driver system properties: {}", SystemUtils.printSystemProperties());
      log.trace("JPPF Driver configuration:\n{}", configuration.asString());
    }

    final JPPFConnectionInformation info = initializer.getConnectionInformation();

    nioHelper = new NioHelper();
    final int[] serverPorts = extractValidPorts(info.serverPorts);
    final int[] sslPorts = extractValidPorts(info.sslServerPorts);
    for (final int[] ports: new int[][] { serverPorts, sslPorts }) {
      if ((ports != null) && (ports.length > 0)) {
        for (final int port: ports) NioHelper.putNioHelper(port, nioHelper);
      }
    }
    final boolean useSSL = (sslPorts != null) && (sslPorts.length > 0);
    if (debugEnabled) log.debug("starting nio servers");
    if (configuration.get(JPPFProperties.RECOVERY_ENABLED)) {
      nodeHeartbeatServer = getOrCreateServer(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL, () -> new HeartbeatNioServer(this, JPPFIdentifiers.NODE_HEARTBEAT_CHANNEL, useSSL));
      clientHeartbeatServer = getOrCreateServer(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL, () -> new HeartbeatNioServer(this, JPPFIdentifiers.CLIENT_HEARTBEAT_CHANNEL, useSSL));
    }
    asyncClientClassServer = getOrCreateServer(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL, () -> new AsyncClientClassNioServer(this, JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL, useSSL));
    asyncNodeClassServer = getOrCreateServer(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL, () -> new AsyncNodeClassNioServer(this, JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL, useSSL));
    asyncClientNioServer  = getOrCreateServer(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL, () -> new AsyncClientNioServer(this, JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL, useSSL));
    asyncNodeNioServer = getOrCreateServer(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL, () -> new AsyncNodeNioServer(this, JPPFIdentifiers.NODE_JOB_DATA_CHANNEL, useSSL));

    if (isManagementEnabled(configuration)) initializer.registerProviderMBeans();

    final boolean startAcceptor;
    synchronized(NioHelper.class) {
      startAcceptor = nioHelper.getAcceptorServer(false) == null;
      if (startAcceptor) {
        NioHelper.setAcceptorServer(acceptorServer = new AcceptorNioServer(serverPorts, sslPorts, statistics, configuration));
      }
    }
    jobManager.loadTaskReturnListeners();
    initializer.initJmxServer();
    initializer.handleDebugActions();
    initializer.initStartups();
    initializer.getNodeConnectionEventHandler().loadListeners();

    if (startAcceptor) startServer(acceptorServer);
    initializer.registerNodeConfigListener();

    if (configuration.get(JPPFProperties.LOCAL_NODE_ENABLED)) initLocalNodes();
    initializer.initBroadcaster();
    initializer.initPeers();
    taskQueue.getPersistenceHandler().loadPersistedJobs();
    if (debugEnabled) log.debug("JPPF Driver initialization complete");
    System.out.println("JPPF Driver initialization complete");
    return this;
  }

  /**
   * Get this driver's unique identifier.
   * @return the uuid as a string.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Get a server-side representation of a job from its uuid.
   * @param uuid the uuid of the job to lookup.
   * @return a {@link JPPFDistributedJob} instance, or {@code null} if there is no job with the specified uuid.
   */
  public JPPFDistributedJob getJob(final String uuid) {
    return this.getQueue().getJob(uuid);
  }

  /**
   * Initialize this task with the specified parameters.<br>
   * The shutdown is initiated after the specified shutdown delay has expired.<br>
   * If the restart parameter is set to false then the JVM exits after the shutdown is complete.
   * @param shutdownDelay delay, in milliseconds, after which the driver shutdown is initiated. A value of 0 or less
   * means an immediate shutdown.
   * @param restart determines whether the driver should restart after shutdown is complete.
   * If set to false, then the JVM will exit.
   * @param restartDelay delay, starting from shutdown completion, after which the driver is restarted.
   * A value of 0 or less means the driver is restarted immediately after the shutdown is complete.
   * @exclude
   */
  public void initiateShutdownRestart(final long shutdownDelay, final boolean restart, final long restartDelay) {
    if (shutdownScheduled.compareAndSet(false, true)) {
      log.info("Scheduling server shutdown in " + shutdownDelay + " ms");
      final Timer timer = new Timer("JPPF driver shutdown timer");
      final ShutdownRestartTask task = new ShutdownRestartTask(restart, restartDelay, this);
      timer.schedule(task, (shutdownDelay <= 0L) ? 0L : shutdownDelay);
    } else {
      log.info("shutdown/restart request ignored because a previous request is already scheduled");
    }
  }

  /**
   * Get the object which manages the registration and unregistration of job
   * dispatch listeners and notifies these listeners of job dispatch events.
   * @return an instance of {@link JobTasksListenerManager}.
   */
  public JobTasksListenerManager getJobTasksListenerManager() {
    return jobManager;
  }

  /**
   * Start the JPPF driver.
   * @param args not used.
   * @exclude
   */
  public static void main(final String...args) {
    try {
      if (debugEnabled) log.debug("starting the JPPF driver");
      if ((args == null) || (args.length <= 0)) throw new JPPFException("The driver should be run with an argument representing a valid TCP port or 'noLauncher'");
      final JPPFDriver driver = new JPPFDriver(JPPFConfiguration.getProperties());
      driver.startedfromMain = true;
      if (!"noLauncher".equals(args[0])) new LauncherListener(Integer.parseInt(args[0])).start();
      driver.start();
      final Object lock = new Object();
      synchronized(lock) {
        try {
          while(true) lock.wait();
        } catch (@SuppressWarnings("unused") final Exception e) {
        }
      }
    } catch(final Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      if (JPPFConfiguration.get(JPPFProperties.SERVER_EXIT_ON_SHUTDOWN)) System.exit(1);
    }
  }

  /**
   * Get the system ibnformation for this driver.
   * @return a {@link JPPFSystemInformation} instance.
   */
  public JPPFSystemInformation getSystemInformation() {
    return systemInformation;
  }

  /**
   * Get the object holding the statistics monitors.
   * @return a {@link JPPFStatistics} instance.
   */
  public JPPFStatistics getStatistics() {
    return statistics;
  }

  /**
   * Add a custom peer driver discovery mechanism to those already registered, if any.
   * @param discovery the driver discovery to add.
   */
  public void addDriverDiscovery(final PeerDriverDiscovery discovery) {
    initializer.discoveryHandler.addDiscovery(discovery);
  }

  /**
   * Remove a custom peer driver discovery mechanism from those already registered.
   * @param discovery the driver discovery to remove.
   */
  public void removeDriverDiscovery(final PeerDriverDiscovery discovery) {
    initializer.discoveryHandler.removeDiscovery(discovery);
  }

  /**
   * Determine whether this driver has initiated a shutdown, in which case it does not accept connections anymore.
   * @return {@code true} if a shutdown is initiated, {@code false} otherwise.
   */
  public boolean isShuttingDown() {
    return shuttingDown.get();
  }

  /**
   * Get this driver's configuration.
   * @return the configuration for this driver as a {@link TypedProperties} instance.
   */
  public TypedProperties getConfiguration() {
    return configuration;
  }

  /**
   * Shutdown this driver and all its components.
   */
  public void shutdown() {
    if (shuttingDown.compareAndSet(false, true)) {
      shutdownNow();
    } else log.info("already Shutting down");
  }

  /**
   * Shutdown this driver and all its components.
   */
  void shutdownNow() {
    log.info("Shutting down JPPF driver");
    if (!localNodes.isEmpty()) {
      if (debugEnabled) log.debug("Shutting down local nodes");
      for (JPPFNode node: localNodes) node.shutdown(false);
      localNodes.clear();
    }
    if (debugEnabled) log.debug("resetting hook factory");
    hookFactory.reset();
    if (debugEnabled) log.debug("closing acceptor");
    if (acceptorServer != null) {
      if (startedfromMain) acceptorServer.end();
      else {
        for (final Integer port: nioHelper.getPorts()) {
          NioHelper.removeNioHelper(port);
          nioHelper.removePort(port);
          try {
            acceptorServer.removeServer(port);
          } catch (final IOException e) {
            log.error("error closing acceptor server on port {}", port, e); 
          }
        }
      }
    }
    closeServer(asyncClientClassServer, "client class server");
    closeServer(asyncClientNioServer, "client job server");
    closeServer(asyncNodeNioServer, "node job server");
    closeServer(asyncNodeClassServer, "node class server");
    closeServer(clientHeartbeatServer, "client heartbeat server");
    closeServer(nodeHeartbeatServer, "node heartbeat server");
    if (debugEnabled) log.debug("closing job queue");
    taskQueue.close();
    if (debugEnabled) log.debug("closing broadcaster");
    initializer.stopBroadcaster();
    if (debugEnabled) log.debug("stopping peer discovery");
    initializer.stopPeerDiscoveryThread();
    if (debugEnabled) log.debug("closing job manager");
    jobManager.close();
    if (debugEnabled) log.debug("resetting statistics");
    statistics.clearListeners();
    statistics.reset();
    if (debugEnabled) log.debug("closing JMX server");
    initializer.stopJmxServer();
    if (debugEnabled) log.debug("shutdown complete");
  }

  /**
   * shutdwon the specified nio server, logging its specified name.
   * @param server the nio server to close.
   * @param serverName the name of the server.
   */
  private static void closeServer(final NioServer server, final String serverName) {
    if (server != null) {
      if (debugEnabled) log.debug("closing {}", serverName);
      server.shutdown();
    }
  }

  /**
   * Initialize the local node.
   * @throws Exception if any error occurs.
   */
  private void initLocalNodes() throws Exception {
    int nbNodes = configuration.get(JPPFProperties.LOCAL_NODES);
    if (nbNodes < 0) nbNodes = 0;
    if (debugEnabled) log.debug("starting {} local nodes", nbNodes);
    if (nbNodes > 0) {
      for (int i=0; i<nbNodes; i++) initLocalNode();
    }
  }

  /**
   * Initialize the local node.
   * @throws Exception if any error occurs.
   */
  private void initLocalNode() throws Exception {
    AbstractClassLoaderConnection<?> classLoaderConnection = null;
    final TypedProperties nodeConfig = new TypedProperties(configuration);
    final int n = localNodes.size() + 1;
    String uuid = nodeConfig.getString("jppf.node.uuid", null);
    if (uuid == null) uuid = JPPFUuid.normalUUID();
    else uuid += "-" + n;
    final boolean secure = nodeConfig.get(JPPFProperties.SSL_ENABLED);
    nodeConfig.set(JPPFProperties.MANAGEMENT_PORT_NODE, nodeConfig.get(secure ? JPPFProperties.SERVER_SSL_PORT : JPPFProperties.SERVER_PORT));
    final AsyncNodeClassContext context = new AsyncNodeClassContext(asyncNodeClassServer, null);
    context.setLocal(true);
    classLoaderConnection = new AsyncLocalClassLoaderConnection(uuid, context);
    asyncNodeClassServer.addNodeConnection(uuid, context);

    final AsyncNodeContext ctx = new AsyncNodeContext(asyncNodeNioServer, null, true);
    ctx.setNodeInfo(getSystemInformation(), false);
    final JPPFNode localNode = new JPPFLocalNode(configuration, new AsyncLocalNodeConnection(ctx), classLoaderConnection, HookFactory.newInstance());
    localNodes.add(localNode);
    ThreadUtils.startDaemonThread(localNode, "Local node " + n);
    asyncNodeNioServer.getMessageHandler().sendHandshakeBundle(ctx, asyncNodeNioServer.getHandshakeBundle());
    getStatistics().addValue(JPPFStatisticsHelper.NODES, 1);
  }
}
