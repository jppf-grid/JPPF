/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.io.*;
import java.util.*;

import javax.management.remote.JMXServiceURL;

import org.jppf.client.*;
import org.jppf.client.event.ConnectionPoolListener;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.diagnostics.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;

import test.org.jppf.test.setup.common.TestUtils;


/**
 * Helper methods for setting up and cleaning the environment before and after testing.
 * @author Laurent Cohen
 */
public class BaseSetup {
  /**
   * The default configuratin used when none is specified.
   */
  public static final Configuration DEFAULT_CONFIG = createDefaultConfiguration();
  /**
   * The jppf client to use.
   */
  protected static JPPFClient client = null;
  /**
   * The node to lunch for the test.
   */
  protected static NodeProcessLauncher[] nodes = null;
  /**
   * The node to lunch for the test.
   */
  protected static DriverProcessLauncher[] drivers = null;
  /**
   * Shutdown hook used to destroy the driver and node processes, in case the JVM terminates abnormally.
   */
  protected static Thread shutdownHook = null;

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
  public static JPPFClient setup(final int nbDrivers, final int nbNodes, final boolean createClient, final Configuration config) throws Exception {
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
    final Configuration config, final ConnectionPoolListener... listeners) throws Exception {
    BaseTest.printOut("performing setup with %d drivers, %d nodes %s", nbDrivers, nbNodes, (createClient ? " and 1 client" : ""));
    ConfigSource.setClientConfig(config.clientConfig);
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    createShutdownHook();
    drivers = new DriverProcessLauncher[nbDrivers];
    for (int i=0; i<nbDrivers; i++) {
      drivers[i] = new DriverProcessLauncher(i+1, config.driverJppf, config.driverLog4j, config.driverClasspath, config.driverJvmOptions);
      new Thread(drivers[i], drivers[i].getName() + "process launcher").start();
    }
    nodes = new NodeProcessLauncher[nbNodes];
    for (int i=0; i<nbNodes; i++) {
      nodes[i] = new NodeProcessLauncher(i+1, config.nodeJppf, config.nodeLog4j, config.nodeClasspath, config.nodeJvmOptions);
      new Thread(nodes[i], nodes[i].getName() + "process launcher").start();
    }
    if (createClient) {
      client = createClient(null, true, config, listeners);
      if (checkDriversAndNodes) checkDriverAndNodesInitialized(nbDrivers, nbNodes);
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
  public static JPPFClient createClient(final String uuid, final boolean reset, final Configuration config, final ConnectionPoolListener... listeners) throws Exception {
    ConfigSource.setClientConfig(config.clientConfig);
    if (reset) JPPFConfiguration.reset();
    else SSLHelper.resetConfig();
    if ((listeners == null) || (listeners.length <= 0)) client = (uuid == null) ? new JPPFClient() : new JPPFClient(uuid);
    else client = (uuid == null) ? new JPPFClient(listeners) : new JPPFClient(uuid, listeners);
    while (!client.hasAvailableConnection()) Thread.sleep(10L);
    return client;
  }

  /**
   * Reset the client configuration to the defaults.
   * @throws Exception if any error occurs.
   */
  public static void resetClientConfig() throws Exception {
    ConfigSource.setClientConfig(DEFAULT_CONFIG.clientConfig);
    JPPFConfiguration.reset();
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  public static void cleanup() throws Exception {
    close();
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  private static void close() throws Exception {
    String text = TextThreadDumpWriter.printToString(new Diagnostics("client").threadDump(), "client thread dump");
    FileUtils.writeTextFile("client_thread_dump.log", text);
    if (client != null) {
      client.close();
      client = null;
      Thread.sleep(500L);
    }
    System.gc();
    stopProcesses();
    ConfigurationHelper.cleanup();
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
    if (client == null) throw new IllegalArgumentException("client cannot be null");
    Map<Integer, JPPFConnectionPool> connectionMap = new HashMap<>();
    boolean allConnected = false;
    while (!allConnected) {
      List<JPPFConnectionPool> list = client.getConnectionPools();
      if (list != null) {
        for (JPPFConnectionPool pool: list) {
          if (!connectionMap.containsKey(pool.getDriverPort())) connectionMap.put(pool.getDriverPort(), pool);
        }
      }
      if (connectionMap.size() < nbDrivers) Thread.sleep(10L);
      else allConnected = true;
    }
    Map<JMXServiceURL, JMXDriverConnectionWrapper> wrapperMap = new HashMap<>();
    for (Map.Entry<Integer, JPPFConnectionPool> entry: connectionMap.entrySet()) {
      JMXDriverConnectionWrapper wrapper = entry.getValue().awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
      if (!wrapperMap.containsKey(wrapper.getURL())) {
        wrapperMap.put(wrapper.getURL(), wrapper);
      }
    }
    int sum = 0;
    while (sum < nbNodes) {
      sum = 0;
      for (Map.Entry<JMXServiceURL, JMXDriverConnectionWrapper> entry: wrapperMap.entrySet()) {
        Integer n = entry.getValue().nbNodes();
        if (n != null) sum += n;
        else break;
      }
    }
    if (printEpilogue) TestUtils.printf("%d drivers and %d nodes successfully initialized", nbDrivers, nbNodes);
  }

  /**
   * Stop driver and node processes.
   */
  protected static void stopProcesses() {
    try {
      if (nodes != null)  for (NodeProcessLauncher n: nodes) n.stopProcess();
      if (drivers != null) for (DriverProcessLauncher d: drivers) d.stopProcess();
    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  /**
   * Create the shutdown hook.
   */
  protected static void createShutdownHook() {
    shutdownHook = new Thread() {
      @Override
      public void run() {
        try {
          close();
        } catch(@SuppressWarnings("unused") Exception ignore) {
        }
      }
    };
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
   * @return a {@link Configuration} instance.
   */
  public static Configuration createDefaultConfiguration() {
    Configuration config = new Configuration();
    List<String> commonCP = new ArrayList<>();
    commonCP.add("classes/addons");
    commonCP.add("classes/tests/config");
    commonCP.add("../node/classes");
    commonCP.add("../common/classes");
    commonCP.add("../jmxremote/classes");
    commonCP.add("../JPPF/lib/slf4j/slf4j-api-1.6.1.jar");
    commonCP.add("../JPPF/lib/slf4j/slf4j-log4j12-1.6.1.jar");
    commonCP.add("../JPPF/lib/log4j/log4j-1.2.15.jar");
    commonCP.add("../JPPF/lib/LZ4/lz4-1.3.0.jar");
    commonCP.add("../JPPF/lib/ApacheCommons/commons-io-2.4.jar");
    List<String> driverCP = new ArrayList<>(commonCP);
    driverCP.add("../server/classes");
    driverCP.add("../JPPF/lib/Groovy/groovy-all-1.6.5.jar");
    String dir = "classes/tests/config";
    config.driverJppf = dir + "/driver.template.properties";
    config.driverLog4j = "classes/tests/config/log4j-driver.template.properties";
    config.driverClasspath = driverCP;
    config.driverJvmOptions.add("-Xmx128m");
    config.driverJvmOptions.add("-Djava.util.logging.testConfig.file=classes/tests/config/logging-driver.properties");
    config.nodeJppf = dir + "/node.template.properties";
    config.nodeLog4j = "classes/tests/config/log4j-node.template.properties";
    config.nodeClasspath = commonCP;
    config.nodeJvmOptions.add("-Djava.util.logging.testConfig.file=classes/tests/config/logging-node1.properties");
    config.clientConfig = dir + "/client.properties";
    return config;
  }

  /** */
  public static class Configuration {
    /**
     * Path to the driver JPPF config
     */
    public String driverJppf = "";
    /**
     * Path to the driver log4j config
     */
    public String driverLog4j = "";
    /**
     * Driver classpath elements.
     */
    public List<String> driverClasspath = new ArrayList<>();
    /**
     * Driver JVM options.
     */
    public List<String> driverJvmOptions = new ArrayList<>();
    /**
     * Path to the node JPPF config
     */
    public String nodeJppf = "";
    /**
     * Path to the node log4j config
     */
    public String nodeLog4j = "";
    /**
     * Node classpath elements.
     */
    public List<String> nodeClasspath = new ArrayList<>();
    /**
     * Node JVM options.
     */
    public List<String> nodeJvmOptions = new ArrayList<>();
    /** */
    public String clientConfig = "classes/tests/config/client.properties";

    /**
     * Copy this configuration to a new instance.
     * @return a {@link Configuration} instance.
     */
    public Configuration copy() {
      Configuration copy = new Configuration();
      copy.driverJppf = driverJppf;
      copy.driverLog4j = driverLog4j;
      copy.driverClasspath = new ArrayList<>(driverClasspath);
      copy.driverJvmOptions = new ArrayList<>(driverJvmOptions);
      copy.nodeJppf = nodeJppf;
      copy.nodeLog4j = nodeLog4j;
      copy.nodeClasspath = new ArrayList<>(nodeClasspath);
      copy.nodeJvmOptions = new ArrayList<>(nodeJvmOptions);
      copy.clientConfig = clientConfig;
      return copy;
    }
  }

  /** */
  public static class ConfigSource implements JPPFConfiguration.ConfigurationSourceReader {
    /**
     * Path to the client configuration file.
     */
    private static String clientConfig = null;

    @Override
    public Reader getPropertyReader() throws IOException {
      if (clientConfig == null) return null;
      return FileUtils.getFileReader(clientConfig);
    }

    /**
     * Get the path to the client configuration file.
     * @return the path as a string.
     */
    public static String getClientConfig() {
      return clientConfig;
    }

    /**
     * Set the path to the client configuration file.
     * @param clientConfig the path as a string.
     */
    public static void setClientConfig(final String clientConfig) {
      ConfigSource.clientConfig = clientConfig;
    }
  }

  /**
   * Get the jppf client to use.
   * @return a {@link JPPFClient} instance.
   */
  public static JPPFClient getClient() {
    return client;
  }
}
