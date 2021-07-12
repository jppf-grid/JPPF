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

package test.org.jppf.test.setup;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Level;
import org.jppf.JPPFTimeoutException;
import org.jppf.client.*;
import org.jppf.client.event.ConnectionPoolListener;
import org.jppf.management.*;
import org.jppf.management.diagnostics.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

import test.org.jppf.test.setup.common.TestUtils;


/**
 * Helper methods for setting up and cleaning the environment before and after testing.
 * @author Laurent Cohen
 */
public class BaseSetup {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(BaseSetup.class);
  /** */
  private static final AtomicBoolean detectorStarted = new AtomicBoolean(false);
  /**
   * Current version of SLF4J.
   */
  public static final String SLF4J_VERSION = "1.7.25";
  /**
   * Default timeout when checking that the grid is up.
   */
  private static final long DEFAULT_GRID_CHECK_TIMEOUT = 15_000L;
  /**
   * The default configuratin used when none is specified.
   */
  public static final TestConfiguration DEFAULT_CONFIG = createDefaultConfiguration();
  static {
    TestConfigSource.setClientConfig(DEFAULT_CONFIG.clientConfig);
    JPPFConfiguration.reset();
    if (detectorStarted.compareAndSet(false, true)) DeadlockDetector.setup("client");
  }
  /**
   * The jppf client to use.
   */
  protected static JPPFClient client;
  /**
   * The node to lunch for the test.
   */
  protected static NodeProcessLauncher[] nodes;
  /**
   * The node to lunch for the test.
   */
  protected static DriverProcessLauncher[] drivers;
  /**
   * Shutdown hook used to destroy the driver and node processes, in case the JVM terminates abnormally.
   */
  protected static Thread shutdownHook;

  /**
   * Get a proxy ot the job management MBean.
   * @param client the JPPF client from which to get the proxy.
   * @return an instance of <code>DriverJobManagementMBean</code>.
   * @throws Exception if the proxy could not be obtained.
   */
  public static DriverJobManagementMBean getJobManagementProxy(final JPPFClient client) throws Exception {
    return getJMXConnection(client).getJobManager();
  }

  /**
   * Get a JMX connection from the specified client.
   * @param client the JPPF client from which to get the proxy.
   * @return a JMXDriverConnectionWrapper instance.
   * @throws Exception if a JMX connection could not be obtained.
   */
  public static JMXDriverConnectionWrapper getJMXConnection(final JPPFClient client) throws Exception {
    return client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
  }

  /**
   * Get a JMX connection from the default client.
   * @return a JMXDriverConnectionWrapper instance.
   * @throws Exception if a JMX connection could not be obtained.
   */
  public static JMXDriverConnectionWrapper getJMXConnection() throws Exception {
    return getJMXConnection(client);
  }

  /**
   * Launches a driver and the specified number of node and start the client.
   * @param nbNodes the number of nodes to launch.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public static JPPFClient setup(final int nbNodes) throws Exception {
    return setup(1, nbNodes, true, DEFAULT_CONFIG);
  }

  /**
   * Launches a driver and node and start the client.
   * @param nbDrivers the number of drivers to launch.
   * @param nbNodes the number of nodes to launch.
   * @param createClient if true then start a client.
   * @param config the driver and node configuration to use.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public static JPPFClient setup(final int nbDrivers, final int nbNodes, final boolean createClient, final TestConfiguration config) throws Exception {
    return setup(nbDrivers, nbNodes, createClient, true, config);
  }

  /**
   * Launches a driver and node and start the client.
   * @param nbDrivers the number of drivers to launch.
   * @param nbNodes the number of nodes to launch.
   * @param createClient if true then start a client.
   * @param checkDriversAndNodes if true then check that all drivers and nodes are connected before returning.
   * @param config the driver and node configuration to use.
   * @param listeners the listeners to add to the JPPF client to receive notifications of new connections.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public static JPPFClient setup(final int nbDrivers, final int nbNodes, final boolean createClient, final boolean checkDriversAndNodes,
    final TestConfiguration config, final ConnectionPoolListener... listeners) throws Exception {
    BaseTest.printOut("performing setup with %d drivers, %d nodes %s", nbDrivers, nbNodes, (createClient ? " and 1 client" : ""));
    TestConfigSource.setClientConfig(config.clientConfig);
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    createShutdownHook();
    final Map<String, Object> bindings = new HashMap<>();
    bindings.put("$nbDrivers", nbDrivers);
    bindings.put("$nbNodes", nbNodes);
    drivers = new DriverProcessLauncher[nbDrivers];
    for (int i=0; i<nbDrivers; i++) {
      drivers[i] = new DriverProcessLauncher(i + 1, config.driver, new HashMap<>(bindings));
      BaseTest.print(true, false, "starting %s", drivers[i].getName());
      ThreadUtils.startDaemonThread(drivers[i], drivers[i].getName().trim() + "-Launcher");
    }
    nodes = new NodeProcessLauncher[nbNodes];
    for (int i=0; i<nbNodes; i++) {
      nodes[i] = new NodeProcessLauncher(i + 1, config.node, new HashMap<>(bindings));
      BaseTest.print(true, false, "starting %s", nodes[i].getName());
      ThreadUtils.startDaemonThread(nodes[i], nodes[i].getName().trim() + "-Launcher");
    }
    if (createClient) {
      client = createClient(null, true, config, listeners);
      if (checkDriversAndNodes) checkDriverAndNodesInitialized(nbDrivers, nbNodes);
    } else {
      JPPFConfiguration.reset();
    }
    return client;
  }

  /**
   * Create a client with the specified uuid.
   * @param uuid if null, let the client generate its uuid.
   * @return a <code>JPPFClient</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFClient createClient(final String uuid) throws Exception {
    return createClient(uuid, true, DEFAULT_CONFIG);
  }

  /**
   * Create a client with the specified uuid.
   * @param uuid if null, let the client generate its uuid.
   * @param reset if <code>true</code>, the JPPF ocnfiguration is reloaded.
   * @return a <code>JPPFClient</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFClient createClient(final String uuid, final boolean reset) throws Exception {
    return createClient(uuid, reset, DEFAULT_CONFIG);
  }

  /**
   * Create a client with the specified uuid.
   * @param uuid if null, let the client generate its uuid.
   * @param reset if <code>true</code>, the JPPF configuration is reloaded.
   * @param config the configuration to use.
   * @param listeners the listeners to add to the JPPF client to receive notifications of new connections.
   * @return a <code>JPPFClient</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFClient createClient(final String uuid, final boolean reset, final TestConfiguration config, final ConnectionPoolListener... listeners) throws Exception {
    TestConfigSource.setClientConfig(config.clientConfig);
    if (reset) JPPFConfiguration.reset();
    else SSLHelper.resetConfig();
    if ((listeners == null) || (listeners.length <= 0)) client = (uuid == null) ? new JPPFClient() : new JPPFClient(uuid);
    else client = (uuid == null) ? new JPPFClient(listeners) : new JPPFClient(uuid, listeners);
    while (!client.hasAvailableConnection()) Thread.sleep(10L);
    return client;
  }

  /**
   * Reset the client configuration to the defaults.
   * @return the new JPPF configuration.
   * @throws Exception if any error occurs.
   */
  public static TypedProperties resetClientConfig() throws Exception {
    TestConfigSource.setClientConfig(DEFAULT_CONFIG.clientConfig);
    JPPFConfiguration.reset();
    return JPPFConfiguration.getProperties();
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  public static void cleanup() throws Exception {
    close();
    if (shutdownHook != null) Runtime.getRuntime().removeShutdownHook(shutdownHook);
  }

  /**
   * Stops the driver and node and close the client.
   */
  private static void close() {
    try {
      generateClientThreadDump();
      if ((client != null) && !client.isClosed()) {
        generateDriverThreadDump(client);
        client.close();
        client = null;
      }
      stopProcesses();
      ConfigurationHelper.cleanup();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Generates a thread dump of the local JVM.
   * @throws Exception if any error occurs.
   */
  public static void generateClientThreadDump() throws Exception {
    BaseTest.print(false, false, "generating client thread dump");
    try (final Diagnostics diag = new Diagnostics("client")) {
      final String text = TextThreadDumpWriter.printToString(diag.threadDump(), "client thread dump");
      FileUtils.writeTextFile("client_thread_dump.log", text);
    }
  }

  /**
   * Generates a thread dump for each of the drivers the specified client is connected to.
   * @param client the JPPF client.
   * @throws Exception if any error occurs.
   */
  public static void generateDriverThreadDump(final JPPFClient client) throws Exception {
    if (client == null) return;
    BaseTest.print(false, false, "generating driver and nodes thread dumps");
    final List<JPPFConnectionPool> pools = client.awaitWorkingConnectionPools(1000L);
    BaseTest.print(false, false, "found %d pools", pools.size());
    final JMXDriverConnectionWrapper[] jmxArray = new JMXDriverConnectionWrapper[pools.size()];
    for (int i=0; i<pools.size(); i++) {
      final JPPFConnectionPool pool = pools.get(i);
      BaseTest.print(false, false, "getting JMX connection for %s", pool);
      jmxArray[i] = pool.awaitWorkingJMXConnection();
    }
    generateDriverThreadDump(jmxArray);
  }

  /**
   * Generates a thread dump for each of the specified drivers, and for each of the nodes connected to them.
   * @param jmxConnections JMX connections to the drivers.
   * @throws Exception if any error occurs.
   */
  public static void generateDriverThreadDump(final JMXDriverConnectionWrapper... jmxConnections) throws Exception {
    BaseTest.print(false, false, "generating thread dumps for %d drivers", jmxConnections.length);
    for (final JMXDriverConnectionWrapper jmx: jmxConnections) {
      BaseTest.print(false, false, "generating driver thread dump for %s", jmx);
      if ((jmx != null) && jmx.isConnected()) {
        try {
          final DiagnosticsMBean proxy = jmx.getDiagnosticsProxy();
          final String text = TextThreadDumpWriter.printToString(proxy.threadDump(), "driver thread dump for " + jmx);
          FileUtils.writeTextFile("driver_thread_dump_" + jmx.getPort() + ".log", text);
        } catch (final Exception e) {
          log.error("failed to generate driver thread dump for {} : {}", jmx, ExceptionUtils.getStackTrace(e));
        }
        try {
          final String dump = (String) jmx.invoke("org.jppf:name=debug,type=driver", "all");
          FileUtils.writeTextFile("server_debug_" + jmx.getPort() + ".log", dump);
          BaseTest.print(false, false, "wrote driver thread dump for %s", jmx);
        } catch (@SuppressWarnings("unused") final Exception e) {
          log.error("failed to get debug dump for {} : {}", jmx, ExceptionUtils.getStackTrace(e));
        }
      }
      try {
        BaseTest.print(false, false, "generating nodes thread dumps for %s", jmx);
        final Collection<JPPFManagementInfo> infos = jmx.nodesInformation();
        final Map<String, JPPFManagementInfo> infoMap = new HashMap<>(infos.size());
        for (final JPPFManagementInfo info: infos) infoMap.put(info.getUuid(), info);
        final ResultsMap<String, ThreadDump> dumpsMap = jmx.getForwarder().threadDump(NodeSelector.ALL_NODES);
        for (final Map.Entry<String, InvocationResult<ThreadDump>> entry: dumpsMap.entrySet()) {
          final String uuid = entry.getKey();
          if (entry.getValue().isException()) {
            log.error("error getting thread dump for node {}", uuid, entry.getValue());
          } else {
            final ThreadDump dump = entry.getValue().result();
            final JPPFManagementInfo info = infoMap.get(uuid);
            final String text = TextThreadDumpWriter.printToString(dump, "node thread dump for " + (info == null ? uuid : info.getHost() + ":" + info.getPort()));
            FileUtils.writeTextFile("node_thread_dump_" + (info == null ? uuid : info.getPort()) + ".log", text);
            BaseTest.print(false, false, "wrote node thread dump for %s", info);
          }
        }
      } catch (final Exception e) {
        log.error("failed to generate the node thread dumps for driver {}", jmx, e);
      }
    }
  }

  /**
   * Check that the driver and all nodes have been started and are accessible.
   * @param nbDrivers the number of drivers that were started.
   * @param nbNodes the number of nodes that were started.
   * @throws Exception if any error occurs.
   */
  public static void checkDriverAndNodesInitialized(final int nbDrivers, final int nbNodes) throws Exception {
    checkDriverAndNodesInitialized(client, nbDrivers, nbNodes, false);
  }

  /**
   * Check that the driver and all nodes have been started and are accessible.
   * @param client the JPPF client to use for the checks.
   * @param nbDrivers the number of drivers that were started.
   * @param nbNodes the number of nodes that were started.
   * @throws Exception if any error occurs.
   */
  public static void checkDriverAndNodesInitialized(final JPPFClient client, final int nbDrivers, final int nbNodes) throws Exception {
    checkDriverAndNodesInitialized(client, nbDrivers, nbNodes, false);
  }

  /**
   * Check that the driver and all nodes have been started and are accessible.
   * @param client the JPPF client to use for the checks.
   * @param nbDrivers the number of drivers that were started.
   * @param nbNodes the number of nodes that were started.
   * @param printEpilogue whether to print a message once the initialization is confirmed.
   * @throws Exception if any error occurs.
   */
  public static void checkDriverAndNodesInitialized(final JPPFClient client, final int nbDrivers, final int nbNodes, final boolean printEpilogue) throws Exception {
    checkDriverAndNodesInitialized(client, nbDrivers, nbNodes, printEpilogue, DEFAULT_GRID_CHECK_TIMEOUT);
  }

  /**
   * Check that the driver and all nodes have been started and are accessible.
   * @param client the JPPF client to use for the checks.
   * @param nbDrivers the number of drivers that were started.
   * @param nbNodes the number of nodes that were started.
   * @param printEpilogue whether to print a message once the initialization is confirmed.
   * @param timeout the maximlum time in millis during to check for the gridd state.
   * @throws Exception if any error occurs.
   */
  public static void checkDriverAndNodesInitialized(final JPPFClient client, final int nbDrivers, final int nbNodes, final boolean printEpilogue, final long timeout) throws Exception {
    if (client == null) throw new IllegalArgumentException("client cannot be null");
    final Map<Integer, JPPFConnectionPool> connectionMap = new HashMap<>();
    boolean allConnected = false;
    final TimeMarker time = new TimeMarker().start();
    while (!allConnected && (time.markTime().getLastElapsedMillis() < timeout)) {
      final List<JPPFConnectionPool> list = client.getConnectionPools();
      if (list != null) {
        for (final JPPFConnectionPool pool: list) {
          if (!connectionMap.containsKey(pool.getDriverPort())) connectionMap.put(pool.getDriverPort(), pool);
        }
      }
      if (connectionMap.size() < nbDrivers) Thread.sleep(100L);
      else allConnected = true;
    }
    if (!allConnected) throw new JPPFTimeoutException(String.format("exceeded tiemeout of %,d ms", timeout));
    final Map<JMXServiceURL, JMXDriverConnectionWrapper> wrapperMap = new HashMap<>();
    for (final Map.Entry<Integer, JPPFConnectionPool> entry: connectionMap.entrySet()) {
      final long remainingTime = timeout - time.markTime().getLastElapsedMillis();
      if (remainingTime <= 0L) throw new JPPFTimeoutException(String.format("exceeded timeout of %,d ms", timeout));
      final List<JMXDriverConnectionWrapper> jmxConnections = entry.getValue().awaitJMXConnections(Operator.AT_LEAST, 1, remainingTime, true);
      if (!jmxConnections.isEmpty()) {
        final JMXDriverConnectionWrapper wrapper = jmxConnections.get(0);
        if (!wrapperMap.containsKey(wrapper.getURL())) wrapperMap.put(wrapper.getURL(), wrapper);
      } else throw new JPPFTimeoutException(String.format("exceeded timeout of %,d ms", timeout));
    }
    int sum = 0;
    while ((sum < nbNodes) && (time.markTime().getLastElapsedMillis() < timeout)) {
      sum = 0;
      for (final Map.Entry<JMXServiceURL, JMXDriverConnectionWrapper> entry: wrapperMap.entrySet()) {
        final Integer n = entry.getValue().nbNodes();
        if (n != null) sum += n;
        else break;
      }
    }
    if (sum < nbNodes) throw new JPPFTimeoutException(String.format("exceeded timeout of %,d ms", timeout));
    if (printEpilogue) TestUtils.printf("%d drivers and %d nodes successfully initialized", nbDrivers, nbNodes);
  }

  /**
   * Stop driver and node processes.
   */
  protected static void stopProcesses() {
    try {
      if (nodes != null) {
        for (final NodeProcessLauncher n: nodes) {
          BaseTest.print(true, false, "stopping %s", n.getName());
          n.stopProcess();
        }
      }
      if (drivers != null) {
        for (final DriverProcessLauncher d: drivers) {
          BaseTest.print(true, false, "stopping %s", d.getName());
          d.stopProcess();
        }
      }
    } catch(final Throwable t) {
      t.printStackTrace();
    }
  }

  /**
   * Create the shutdown hook.
   */
  protected static void createShutdownHook() {
    shutdownHook = new Thread(() -> close());
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  /**
   * Get the number of drivers in the test setup.
   * @return the number of drivers as an int.
   */
  public static int nbDrivers() {
    return drivers == null ? 0 : drivers.length;
  }

  /**
   * Get the number of nodes in the test setup.
   * @return the number of nodes as an int.
   */
  public static int nbNodes() {
    return nodes == null ? 0 : nodes.length;
  }

  /**
   * Create the default configuratin used when none is specified.
   * @return a {@link TestConfiguration} instance.
   */
  public static TestConfiguration createDefaultConfiguration() {
    final TestConfiguration config = new TestConfiguration();
    final List<String> commonCP = new ArrayList<>();
    final String dir = "classes/tests/config";
    commonCP.add("classes/addons");
    commonCP.add(dir);
    commonCP.add("../node/classes");
    commonCP.add("../common/classes");
    commonCP.add("../jmxremote-nio/classes");
    commonCP.add("../JPPF/lib/slf4j/slf4j-api-" + SLF4J_VERSION + ".jar");
    commonCP.add("../JPPF/lib/slf4j/slf4j-log4j12-" + SLF4J_VERSION + ".jar");
    commonCP.add("../JPPF/lib/log4j/*");
    commonCP.add("../JPPF/lib/LZ4/*");
    commonCP.add("../JPPF/lib/ApacheCommons/*");
    commonCP.add("../JPPF/lib/JNA/*");
    commonCP.add("../JPPF/lib/oshi/*");
    commonCP.add("lib/xstream.jar");
    commonCP.add("lib/xpp3_min.jar");
    commonCP.add("lib/xmlpull.jar");

    final List<String> driverCP = new ArrayList<>(commonCP);
    driverCP.add("../server/classes");
    driverCP.add("../JPPF/lib/Groovy/*");
    config.driver.jppf = dir + "/driver.template.properties";
    config.driver.log4j = dir + "/log4j-driver.template.properties";
    config.driver.classpath = driverCP;
    config.node.jppf = dir + "/node.template.properties";
    config.node.log4j = dir + "/log4j-node.template.properties";
    config.node.classpath = commonCP;
    config.clientConfig = dir + "/client.properties";
    return config;
  }

  /**
   * Get the jppf client to use.
   * @return a {@link JPPFClient} instance.
   */
  public static JPPFClient getClient() {
    return client;
  }

  /**
   * Set the specified Log4j logger to the specified level.
   * @param level the level to set.
   * @param names the names of the log4j loggers to configure.
   */
  public static void setLoggerLevel(final Level level, final String...names) {
    for (final String name: names) {
      final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(name);
      if (logger != null) logger.setLevel(level);
    }
  }
}
