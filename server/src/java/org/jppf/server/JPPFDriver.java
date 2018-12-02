/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.discovery.PeerDriverDiscovery;
import org.jppf.job.JobTasksListenerManager;
import org.jppf.management.*;
import org.jppf.nio.NioHelper;
import org.jppf.nio.acceptor.AcceptorNioServer;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.process.LauncherListener;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.classloader.LocalClassContext;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.server.nio.classloader.node.NodeClassNioServer;
import org.jppf.server.nio.client.AsyncClientNioServer;
import org.jppf.server.nio.heartbeat.HeartbeatNioServer;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.local.*;
import org.jppf.server.protocol.ServerJob;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.jppf.utils.configuration.JPPFProperties;
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
public class JPPFDriver extends AbstractJPPFDriver {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFDriver.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this JPPFDriver.
   * @param configuration this driver's configuration.
   */
  public JPPFDriver(final TypedProperties configuration) {
    super(configuration);
    initializer = new DriverInitializer(this, configuration);
    initializer.initDatasources();
    jobManager = new JPPFJobManager(this);
    taskQueue = new JPPFPriorityQueue(this, jobManager);
    if (debugEnabled) {
      log.debug("JPPF Driver system properties: {}", SystemUtils.printSystemProperties());
      log.debug("JPPF Driver configuration:\n{}", configuration.asString());
    }
  }

  /**
   * Initialize and start this driver.
   * @return this driver.
   * @throws Exception if the initialization fails.
   */
  public JPPFDriver start() throws Exception {
    if (debugEnabled) log.debug("starting JPPF driver");
    final JPPFConnectionInformation info = initializer.getConnectionInformation();
    initializer.handleDebugActions();

    final int[] sslPorts = extractValidPorts(info.sslServerPorts);
    final boolean useSSL = (sslPorts != null) && (sslPorts.length > 0);
    if (debugEnabled) log.debug("starting nio servers");
    if (configuration.get(JPPFProperties.RECOVERY_ENABLED)) {
      nodeHeartbeatServer = initHeartbeatServer(JPPFIdentifiers.NODE_HEARTBEAT_CHANNEL, useSSL);
      clientHeartbeatServer = initHeartbeatServer(JPPFIdentifiers.CLIENT_HEARTBEAT_CHANNEL, useSSL);
    }
    NioHelper.putServer(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL, clientClassServer = startServer(new ClientClassNioServer(this, useSSL)));
    NioHelper.putServer(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL, nodeClassServer = startServer(new NodeClassNioServer(this, useSSL)));
    NioHelper.putServer(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL, asyncClientNioServer = startServer(new AsyncClientNioServer(this, JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL, useSSL)));
    NioHelper.putServer(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL, nodeNioServer = startServer(new NodeNioServer(this, taskQueue, useSSL)));
    NioHelper.putServer(JPPFIdentifiers.ACCEPTOR_CHANNEL, acceptorServer = new AcceptorNioServer(extractValidPorts(info.serverPorts), sslPorts, statistics));
    jobManager.loadTaskReturnListeners();
    if (isManagementEnabled(configuration)) initializer.registerProviderMBeans();
    initializer.initJmxServer();
    initializer.initStartups();
    initializer.getNodeConnectionEventHandler().loadListeners();

    startServer(acceptorServer);

    if (configuration.get(JPPFProperties.LOCAL_NODE_ENABLED)) {
      final LocalClassLoaderChannel localClassChannel = new LocalClassLoaderChannel(new LocalClassContext(this));
      localClassChannel.getContext().setChannel(localClassChannel);
      final LocalNodeChannel localNodeChannel = new LocalNodeChannel(new LocalNodeContext(nodeNioServer));
      localNodeChannel.getContext().setChannel(localNodeChannel);
      final boolean offline = configuration.get(JPPFProperties.NODE_OFFLINE);
      final String uuid = configuration.getString("jppf.node.uuid", JPPFUuid.normalUUID());
      localNode = new JPPFLocalNode(configuration, new LocalNodeConnection(localNodeChannel), offline  ? null : new LocalClassLoaderConnection(uuid, localClassChannel));
      nodeClassServer.initLocalChannel(localClassChannel);
      nodeNioServer.initLocalChannel(localNodeChannel);
      ThreadUtils.startDaemonThread(localNode, "Local node");
    }
    initializer.initBroadcaster();
    initializer.initPeers(clientClassServer);
    taskQueue.getPersistenceHandler().loadPersistedJobs();
    if (debugEnabled) log.debug("JPPF Driver initialization complete");
    System.out.println("JPPF Driver initialization complete");
    return this;
  }

  /**
   * Initialize and start this driver.
   * @return this driver.
   * @throws Exception if the initialization fails.
   * @deprecated use {@link #start()} instead.
   */
  @Deprecated
  public JPPFDriver run() throws Exception {
    return start();
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
    final ServerJob serverJob = this.getQueue().getJob(uuid);
    return (serverJob == null) ? null : serverJob.getJob();
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
    if (shutdownSchduled.compareAndSet(false, true)) {
      log.info("Scheduling server shutdown in " + shutdownDelay + " ms");
      final Timer timer = new Timer();
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
   * Start the JPPF server.
   * @param args not used.
   * @exclude
   */
  public static void main(final String...args) {
    try {
      if (debugEnabled) log.debug("starting the JPPF driver");
      if ((args == null) || (args.length <= 0)) throw new JPPFException("The driver should be run with an argument representing a valid TCP port or 'noLauncher'");
      if (!"noLauncher".equals(args[0])) new LauncherListener(Integer.parseInt(args[0])).start();
      final JPPFDriver driver = new JPPFDriver(JPPFConfiguration.getProperties());
      driver.run();
    } catch(final Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      if (JPPFConfiguration.get(JPPFProperties.SERVER_EXIT_ON_SHUTDOWN)) System.exit(1);
    }
  }

  /**
   * Start a heartbeat server with the specified channel identifier.
   * @param identifier the channel identifier for the server connections.
   * @param useSSL whether to use SSL connectivity.
   * @return the created server.
   * @throws Exception if any error occurs.
   */
  private HeartbeatNioServer initHeartbeatServer(final int identifier, final boolean useSSL) throws Exception {
    final HeartbeatNioServer server = startServer(new HeartbeatNioServer(this, identifier, useSSL));
    NioHelper.putServer(identifier, server);
    return server;
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
   * Get the jmx server used to manage and monitor this driver.
   * @param secure specifies whether to get the ssl-based connector server. 
   * @return a {@link JMXServer} instance.
   */
  public JMXServer getJMXServer(final boolean secure) {
    return initializer.getJmxServer(secure);
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
   * Determine whether this server has initiated a shutdown, in which case it does not accept connections anymore.
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
   * Shutdown this server and all its components.
   */
  public void shutdown() {
    if (shuttingDown.compareAndSet(false, true)) {
      shutdownNow();
    } else log.info("already Shutting down");
  }

  /**
   * Shutdown this server and all its components.
   */
  void shutdownNow() {
    log.info("Shutting down");
    if (debugEnabled) log.debug("closing acceptor");
    if (acceptorServer != null) acceptorServer.shutdown();
    if (debugEnabled) log.debug("closing node heartbeat server");
    if (nodeHeartbeatServer != null) nodeHeartbeatServer.shutdown();
    if (debugEnabled) log.debug("client heartbeat server");
    if (clientHeartbeatServer != null) clientHeartbeatServer.shutdown();
    if (debugEnabled) log.debug("closing client class server");
    if (clientClassServer != null) clientClassServer.shutdown();
    if (debugEnabled) log.debug("closing node class server");
    if (nodeClassServer != null) nodeClassServer.shutdown();
    if (debugEnabled) log.debug("closing node job server");
    if (nodeNioServer != null) nodeNioServer.shutdown();
    if (debugEnabled) log.debug("closing client job server");
    if (asyncClientNioServer != null) asyncClientNioServer.shutdown();
    if (debugEnabled) log.debug("closing global executor");
    NioHelper.shutdown(true);
    if (debugEnabled) log.debug("closing broadcaster");
    initializer.stopBroadcaster();
    if (debugEnabled) log.debug("stopping peer discovery");
    initializer.stopPeerDiscoveryThread();
    if (debugEnabled) log.debug("closing JMX server");
    initializer.stopJmxServer();
    if (debugEnabled) log.debug("closing job manager");
    jobManager.close();
    if (debugEnabled) log.debug("shutdown complete");
  }
}
