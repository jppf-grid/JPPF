/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import javax.management.remote.JMXServiceURL;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.JPPFConfiguration;


/**
 * Helper methods for setting up and cleaning the environment before and after testing.
 * @author Laurent Cohen
 */
public class BaseSetup
{
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
  public static DriverJobManagementMBean getJobManagementProxy(final JPPFClient client) throws Exception
  {
    JMXDriverConnectionWrapper driver = client.getClientConnection().getJmxConnection();
    while (!driver.isConnected()) driver.connectAndWait(10L);
    return driver.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
  }

  /**
   * Get a proxy to the driver admin MBean.
   * @param client the JPPF client from which to get the proxy.
   * @return an instance of <code>DriverJobManagementMBean</code>.
   * @throws Exception if the proxy could not be obtained.
   */
  public static JMXDriverConnectionWrapper getDriverManagementProxy(final JPPFClient client) throws Exception
  {
    JMXDriverConnectionWrapper driver = client.getClientConnection().getJmxConnection();
    while (!driver.isConnected()) driver.connectAndWait(10L);
    return driver;
  }

  /**
   * Launches a driver and the specified number of node and start the client.
   * @param nbNodes the number of nodes to launch.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public static JPPFClient setup(final int nbNodes) throws Exception
  {
    return setup(1, nbNodes, true, null);
  }

  /**
   * Launches a driver and node and start the client.
   * @param nbNodes the number of nodes to launch.
   * @param initClient if true then start a client.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public static JPPFClient setup(final int nbNodes, final boolean initClient) throws Exception
  {
    return setup(1, nbNodes, initClient, null);
  }

  /**
   * Launches a driver and node and start the client.
   * @param nbDrivers the number of drivers to launch.
   * @param nbNodes the number of nodes to launch.
   * @param initClient if true then start a client.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public static JPPFClient setup(final int nbDrivers, final int nbNodes, final boolean initClient) throws Exception
  {
    return setup(nbDrivers, nbNodes, initClient, null);
  }

  /**
   * Launches a driver and node and start the client.
   * @param nbDrivers the number of drivers to launch.
   * @param nbNodes the number of nodes to launch.
   * @param initClient if true then start a client.
   * @param config the driver and node configuration to use.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public static JPPFClient setup(final int nbDrivers, final int nbNodes, final boolean initClient, final Configuration config) throws Exception
  {
    System.out.println("performing setup with " + nbDrivers + " drivers, " + nbNodes + " nodes" + (initClient ? " and 1 client" : ""));
    createShutdownHook();
    drivers = new DriverProcessLauncher[nbDrivers];
    for (int i=0; i<nbDrivers; i++)
    {
      if (config == null) drivers[i] = new DriverProcessLauncher(i+1);
      else drivers[i] = new DriverProcessLauncher(i+1, config.driverJppf, config.driverLog4j, config.driverClasspath, config.driverJvmOptions);
      new Thread(drivers[i], drivers[i].getName() + "process launcher").start();
    }
    nodes = new NodeProcessLauncher[nbNodes];
    for (int i=0; i<nbNodes; i++)
    {
      if (config == null) nodes[i] = new NodeProcessLauncher(i+1);
      else nodes[i] = new NodeProcessLauncher(i+1, config.nodeJppf, config.nodeLog4j, config.nodeClasspath, config.nodeJvmOptions);
      new Thread(nodes[i], nodes[i].getName() + "process launcher").start();
    }
    if (initClient)
    {
      client = createClient(null);
      checkDriverAndNodesInitialized(nbDrivers, nbNodes);
    }
    return client;
  }

  /**
   * Create a client with the specified uuid.
   * @param uuid if null, let the client generate its uuid.
   * @return a <code>JPPFClient</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFClient createClient(final String uuid) throws Exception
  {
    return createClient(uuid, true);
  }

  /**
   * Create a client with the specified uuid.
   * @param uuid if null, let the client generate its uuid.
   * @param reset if <code>true</code>, the JPPF ocnfiguration is reloaded.
   * @return a <code>JPPFClient</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFClient createClient(final String uuid, final boolean reset) throws Exception
  {
    if (reset) JPPFConfiguration.reset();
    JPPFClient jppfClient = (uuid == null) ? new JPPFClient() : new JPPFClient(uuid);
    //System.out.println("waiting for available client connection");
    while (!jppfClient.hasAvailableConnection()) Thread.sleep(10L);
    return jppfClient;
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  public static void cleanup() throws Exception
  {
    close();
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  private static void close() throws Exception
  {
    if (client != null)
    {
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
  public static void checkDriverAndNodesInitialized(final int nbDrivers, final int nbNodes) throws Exception
  {
    checkDriverAndNodesInitialized(client, nbDrivers, nbNodes);
  }

  /**
   * Check that the driver and all nodes have been started and are accessible.
   * @param client the JPPF client to use for the checks.
   * @param nbDrivers the number of drivers that were started.
   * @param nbNodes the number of nodes that were started.
   * @throws Exception if any error occurs.
   */
  public static void checkDriverAndNodesInitialized(final JPPFClient client, final int nbDrivers, final int nbNodes) throws Exception
  {
    if (client == null) throw new IllegalArgumentException("client cannot be null");
    Map<Integer, JPPFClientConnection> connectionMap = new HashMap<>();
    boolean allConnected = false;
    while (!allConnected)
    {
      List<JPPFClientConnection> list = client.getAllConnections();
      if (list != null)
      {
        for (JPPFClientConnection c: list)
        {
          if (!connectionMap.containsKey(c.getPort())) connectionMap.put(c.getPort(), c);
        }
      }
      if (connectionMap.size() < nbDrivers) Thread.sleep(10L);
      else allConnected = true;
    }
    Map<JMXServiceURL, JMXDriverConnectionWrapper> wrapperMap = new HashMap<>();
    for (Map.Entry<Integer, JPPFClientConnection> entry: connectionMap.entrySet())
    {
      JMXDriverConnectionWrapper wrapper = entry.getValue().getJmxConnection();
      if (!wrapperMap.containsKey(wrapper.getURL()))
      {
        while (!wrapper.isConnected()) wrapper.connectAndWait(10L);
        wrapperMap.put(wrapper.getURL(), wrapper);
      }
    }
    int sum = 0;
    while (sum < nbNodes)
    {
      sum = 0;
      for (Map.Entry<JMXServiceURL, JMXDriverConnectionWrapper> entry: wrapperMap.entrySet())
      {
        Integer n = entry.getValue().nbNodes();
        if (n != null) sum += n;
        else break;
      }
    }
  }

  /**
   * Stop driver and node processes.
   */
  protected static void stopProcesses()
  {
    try
    {
      if (nodes != null)  for (NodeProcessLauncher n: nodes) n.stopProcess();
      if (drivers != null) for (DriverProcessLauncher d: drivers) d.stopProcess();
    }
    catch(Throwable t)
    {
      t.printStackTrace();
    }
  }

  /**
   * Create the shutdown hook.
   */
  protected static void createShutdownHook()
  {
    shutdownHook = new Thread()
    {
      @Override
      public void run()
      {
        try
        {
          close();
        }
        catch(Exception ignore)
        {
        }
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  /**
   * 
   */
  public static class Configuration
  {
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
  }
}
