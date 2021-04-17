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

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.management.remote.JMXServiceURL;

import org.jppf.JPPFTimeoutException;
import org.jppf.client.*;
import org.jppf.client.event.ConnectionPoolListener;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.NodeRunner;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.junit.Assert;

import test.org.jppf.test.setup.common.*;


/**
 * Helper methods for setting up and cleaning the environment before and after testing.
 * @author Laurent Cohen
 */
public class BaseSetup {
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
  public static final TestConfiguration DEFAULT_CONFIG = TestConfiguration.newDefault();
  static {
    TestConfigSource.setClientConfig(DEFAULT_CONFIG.clientConfig);
    JPPFConfiguration.reset();
    if (detectorStarted.compareAndSet(false, true)) DeadlockDetector.setup("client");
  }
  /**
   * The jppf client to use.
   */
  private static JPPFClient client;
  /**
   * The node to lunch for the test.
   */
  private static NodeProcessLauncher[] nodes;
  /**
   * The node to lunch for the test.
   */
  private static DriverProcessLauncher[] drivers;
  /**
   * Shutdown hook used to destroy the driver and node processes, in case the JVM terminates abnormally.
   */
  private static Thread shutdownHook;
  /**
   * Sequence number to build client uuids.
   */
  private static final AtomicInteger clientUuidSequence = new AtomicInteger(0);
  /**
   * Whether to run the tests with an embedded grid, where applicable.
   */
  private static final boolean testWithEmbeddedGrid = Boolean.valueOf(TestUtils.getEnv("JPPF_TEST_EMBEDDED_GRID", "false"));
  /**
   * The node runners.
   */
  private static NodeRunner[] embeddedNodes;
  /**
   * The drivers.
   */
  private static JPPFDriver[] embeddedDrivers;

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
    BaseTest.print(false, false, "performing setup with %d drivers, %d nodes %s", nbDrivers, nbNodes, (createClient ? " and 1 client" : ""));
    TestConfigSource.setClientConfig(config.clientConfig);
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    createShutdownHook();
    final Map<String, Object> bindings = new HashMap<>();
    bindings.put("$nbDrivers", nbDrivers);
    bindings.put("$nbNodes", nbNodes);
    if (testWithEmbeddedGrid) {
      embeddedDrivers = new JPPFDriver[nbDrivers];
      for (int i=0; i<nbDrivers; i++) {
        BaseTest.print(false, false, ">>> starting the JPPF driver " + (i + 1));
        bindings.put("$n", i + 1);
        final String path = config.driver.jppf;
        final TypedProperties driverConfig = ConfigurationHelper.createConfigFromTemplate(path, bindings).set(BaseTest.DEADLOCK_DETECTOR_ENABLED, false);
        embeddedDrivers[i] = new JPPFDriver(driverConfig).start();
      }
      embeddedNodes = new NodeRunner[nbNodes];
      final String path = config.node.jppf;
      Assert.assertTrue(new File(path).exists());
      for (int i=0; i<nbNodes; i++) {
        BaseTest.print(false, false, ">>> starting the JPPF node " +  (i + 1));
        bindings.put("$n", i + 1);
        final TypedProperties nodeConfig = ConfigurationHelper.createConfigFromTemplate(path, bindings).set(BaseTest.DEADLOCK_DETECTOR_ENABLED, false);
        embeddedNodes[i] = new NodeRunner(nodeConfig);
        final NodeRunner runner = embeddedNodes[i];
        new Thread(() -> runner.start(), String.format("[node-%03d]", i + 1)).start();
      }
    } else {
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
    }
    if (createClient) {
      client = createClient("client-" + clientUuidSequence.incrementAndGet(), true, config, listeners);
      if (checkDriversAndNodes) checkDriverAndNodesInitialized(client, nbDrivers, nbNodes, false);
    } else {
      JPPFConfiguration.reset();
    }
    return client;
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
    //else SSLHelper.resetConfig();
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
      BaseTestHelper.generateClientThreadDump();
      if ((client != null) && !client.isClosed()) {
        if (!isTestWithEmbeddedGrid()) BaseTestHelper.generateDriverThreadDump(client);
        client.close();
        client = null;
      }
      if (isTestWithEmbeddedGrid()) stopEmbeddedGrid();
      else stopProcesses();
      ConfigurationHelper.cleanup();
    } catch (final Exception e) {
      e.printStackTrace();
    }
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
   * Stop driver and node processes.
   */
  protected static void stopEmbeddedGrid() {
    try {
      if (embeddedNodes != null) {
        int i = 0;
        for (final NodeRunner runner: embeddedNodes) {
          if (runner == null) BaseTest.print("<<< node runner %d is null!", ++i);
          else {
            BaseTest.print(false, false, "<<< stoping the JPPF node " + (++i));
            runner.shutdown();
          }
        }
      }
      if (embeddedDrivers != null) {
        int i = 0;
        for (final JPPFDriver driver: embeddedDrivers) {
          if (driver == null) BaseTest.print("<<< driver%d is null!", ++i);
          else {
            BaseTest.print(false, false, "<<< shutting down driver" + (++i));
            driver.shutdown();
          }
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
    if (isTestWithEmbeddedGrid()) return (embeddedDrivers == null) ? 0: embeddedDrivers.length;
    return (drivers == null) ? 0 : drivers.length;
  }

  /**
   * Get the number of nodes in the test setup.
   * @return the number of nodes as an int.
   */
  public static int nbNodes() {
    if (isTestWithEmbeddedGrid()) return (embeddedNodes == null) ? 0: embeddedNodes.length;
    return (nodes == null) ? 0 : nodes.length;
  }

  /**
   * Get the jppf client to use.
   * @return a {@link JPPFClient} instance.
   */
  public static JPPFClient getClient() {
    return client;
  }

  /**
   * Determine whether to run the tests with an embedded grid, where applicable.
   * @return {@code true} if the tests should be run with an embedded grid, {@code false} otherwise.
   */
  public static boolean isTestWithEmbeddedGrid() {
    return testWithEmbeddedGrid;
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
    final long timeout = DEFAULT_GRID_CHECK_TIMEOUT;
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
}
