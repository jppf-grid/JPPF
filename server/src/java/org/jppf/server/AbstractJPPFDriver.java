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

import static org.jppf.utils.stats.JPPFStatisticsHelper.createServerStatistics;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.JPPFInitializer;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.management.*;
import org.jppf.nio.NioServer;
import org.jppf.nio.acceptor.AcceptorNioServer;
import org.jppf.node.initialization.OutputRedirectHook;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.classloader.client.AsyncClientClassNioServer;
import org.jppf.server.nio.classloader.node.AsyncNodeClassNioServer;
import org.jppf.server.nio.client.AsyncClientNioServer;
import org.jppf.server.nio.heartbeat.HeartbeatNioServer;
import org.jppf.server.nio.nodeserver.async.AsyncNodeNioServer;
import org.jppf.server.node.JPPFNode;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.*;
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
 * @exclude
 */
abstract class AbstractJPPFDriver {
  // this static block must be the first thing executed when this class is loaded
  static {
    JPPFInitializer.init();
  }
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractJPPFDriver.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Used for serialization / deserialization.
   */
  private final ObjectSerializer serializer = new ObjectSerializerImpl();
  /**
   * Reference to the local node if it is enabled.
   */
  JPPFNode localNode;
  /**
   * The queue that handles the tasks to execute. Objects are added to, and removed from, this queue, asynchronously and by multiple threads.
   */
  JPPFPriorityQueue taskQueue;
  /**
   * Serves the execution requests coming from client applications.
   */
  AsyncClientNioServer asyncClientNioServer;
  /**
   * Serves the JPPF nodes.
   */
  AsyncNodeNioServer asyncNodeNioServer;
  /**
   * Serves class loading requests from the JPPF nodes.
   */
  AsyncClientClassNioServer asyncClientClassServer;
  /**
   * Serves class loading requests from the JPPF nodes.
   */
  AsyncNodeClassNioServer asyncNodeClassServer;
  /**
   * Handles the initial handshake and peer channel identification.
   */
  AcceptorNioServer acceptorServer;
  /**
   * Handles the heartbeat messages with the nodes.
   */
  HeartbeatNioServer nodeHeartbeatServer;
  /**
   * Handles the heartbeat messages with the clients.
   */
  HeartbeatNioServer clientHeartbeatServer;
  /**
   * Determines whether this server has scheduled a shutdown.
   */
  final AtomicBoolean shutdownSchduled = new AtomicBoolean(false);
  /**
   * Determines whether this server has initiated a shutdown, in which case it does not accept connections anymore.
   */
  final AtomicBoolean shuttingDown = new AtomicBoolean(false);
  /**
   * Holds the statistics monitors.
   */
  final JPPFStatistics statistics;
  /**
   * Manages and monitors the jobs throughout their processing within this driver.
   */
  JPPFJobManager jobManager;
  /**
   * Uuid for this driver.
   */
  final String uuid;
  /**
   * Performs initialization of the driver's components.
   */
  DriverInitializer initializer;
  /**
   * Configuration for this driver.
   */
  final TypedProperties configuration;
  /**
   * System ibnformation for this driver.
   */
  JPPFSystemInformation systemInformation;
  /**
   * MBean handling changes in number of nodes/processing threads.
   */
  PeerDriver peerDriver;
  /**
   * Whether JPPF debug mode is enabled.
   */
  final boolean jppfDebugEnabled;

  /**
   * Initialize this JPPFDriver.
   * @param configuration this driver's configuration.
   */
  public AbstractJPPFDriver(final TypedProperties configuration) {
    this.configuration = configuration;
    final String s;
    this.uuid = (s = configuration.getString("jppf.driver.uuid", null)) == null ? JPPFUuid.normalUUID() : s;
    new JmxMessageNotifier(); // initialize the jmx logger
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    new OutputRedirectHook().initializing(configuration);
    VersionUtils.logVersionInformation("driver", uuid);
    SystemUtils.printPidAndUuid("driver", uuid);
    statistics = createServerStatistics();
    systemInformation = new JPPFSystemInformation(configuration, uuid, false, true, statistics);
    statistics.addListener(new StatsSystemInformationUpdater(systemInformation));
    jppfDebugEnabled = configuration.get(JPPFProperties.DEBUG_ENABLED);
  }

  /**
   * Get the queue that handles the tasks to execute.
   * @return a JPPFQueue instance.
   * @exclude
   */
  public JPPFPriorityQueue getQueue() {
    return taskQueue;
  }

  /**
   * Get the JPPF client server.
   * @return a {@link AsyncClientNioServer} instance.
   * @exclude
   */
  public AsyncClientNioServer getAsyncClientNioServer() {
    return asyncClientNioServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   * @exclude
   */
  public AsyncClientClassNioServer getAsyncClientClassServer() {
    return asyncClientClassServer;
  }

  /**
   * Get the JPPF class server.
   * @return a <code>ClassNioServer</code> instance.
   * @exclude
   */
  public AsyncNodeClassNioServer getAsyncNodeClassServer() {
    return asyncNodeClassServer;
  }

  /**
   * Get the JPPF nodes server.
   * @return a <code>NodeNioServer</code> instance.
   * @exclude
   */
  public AsyncNodeNioServer getAsyncNodeNioServer() {
    return asyncNodeNioServer;
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
   * Start server, register it to recovery server if requested and print initialization message.
   * @param <T> the type of the server to start.
   * @param nioServer the nio server to start.
   * @return started nioServer
   */
  static <T extends NioServer> T startServer(final T nioServer) {
    if (nioServer == null) throw new IllegalArgumentException("nioServer is null");
    if (debugEnabled) log.debug("starting nio server {}", nioServer);
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
    final StringBuilder sb = new StringBuilder();
    if (name != null) {
      sb.append(name);
      sb.append(" initialized");
    }
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
    System.out.println(sb.toString());
    if (debugEnabled) log.debug(sb.toString());
  }

  /**
   * Determine whether management is enabled and if there is an active remote connector server.
   * @return <code>true</code> if management is enabled, <code>false</code> otherwise.
   * @param config the configuration to test whether management is enabled.
   */
  static boolean isManagementEnabled(final TypedProperties config) {
    return config.get(JPPFProperties.MANAGEMENT_ENABLED);
  }

  /**
   * Extract only th valid ports from the input array.
   * @param ports the array of port numbers to check.
   * @return an array, possibly of length 0, containing all the valid port numbers in the input array.
   */
  static int[] extractValidPorts(final int[] ports) {
    if ((ports == null) || (ports.length == 0)) return ports;
    final List<Integer> list = new ArrayList<>();
    for (int port: ports) {
      if (port >= 0) list.add(port);
    }
    final int[] result = new int[list.size()];
    for (int i=0; i<result.length; i++) result[i] = list.get(i);
    return result;
  }

  /**
   * @return an object Used for serialization / deserialization.
   * @exclude
   */
  public ObjectSerializer getSerializer() {
    return serializer;
  }

  /**
   * @return the MBean handling changes in number of nodes/processing threads.
   * @exclude
   */
  public PeerDriver getPeerDriver() {
    return peerDriver;
  }

  /**
   * 
   * @param peerDriver the MBean handling changes in number of nodes/processing threads.
   * @exclude
   */
  public void setPeerDriver(final PeerDriver peerDriver) {
    this.peerDriver = peerDriver;
  }

  /**
   * @return whether JPPF debug mode is enabled.
   */
  public boolean isJppfDebugEnabled() {
    return jppfDebugEnabled;
  }
}
