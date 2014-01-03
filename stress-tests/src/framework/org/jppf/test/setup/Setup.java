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

package org.jppf.test.setup;

import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.test.scenario.ScenarioConfiguration;
import org.jppf.utils.JPPFConfiguration;

/**
 * Helper object for setting up and cleaning the environment before and after testing.
 * @author Laurent Cohen
 */
public class Setup
{
  /**
   * The jppf client to use.
   */
  protected JPPFClient client = null;
  /**
   * The nodes to launch for the test.
   */
  protected RestartableNodeProcessLauncher[] nodes = null;
  /**
   * The drivers to launch for the test.
   */
  protected RestartableDriverProcessLauncher[] drivers = null;
  /**
   * Shutdown hook used to destroy the driver and node processes, in case the JVM terminates abnormally.
   */
  protected Thread shutdownHook = null;
  /**
   * The configuration of the scenario to run.
   */
  protected final ScenarioConfiguration config;
  /**
   * Manages the JMX connections to ddrivers and nodes.
   */
  private JMXHandler jmxHandler = null;
  /**
   * 
   */
  private AtomicInteger clientCount = new AtomicInteger(0);

  /**
   * Initialize this tests etup with the psecified scenario configuration.
   * @param config the configuration of the scenario to run.
   */
  public Setup(final ScenarioConfiguration config)
  {
    this.config = config;
  }

  /**
   * Get a proxy to the job management MBean.
   * @return an instance of <code>DriverJobManagementMBean</code>.
   * @throws Exception if the proxy could not be obtained.
   */
  public DriverJobManagementMBean getJobManagementProxy() throws Exception
  {
    JMXDriverConnectionWrapper driver = getDriverManagementProxy();
    return driver.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
  }

  /**
   * Get a proxy to the driver admin MBean.
   * @return an instance of <code>DriverJobManagementMBean</code>.
   * @throws Exception if the proxy could not be obtained.
   */
  public JMXDriverConnectionWrapper getDriverManagementProxy() throws Exception
  {
    JMXDriverConnectionWrapper driver = client.getClientConnection().getJmxConnection();
    while (!driver.isConnected()) driver.connectAndWait(10L);
    return driver;
  }

  /**
   * Launches a driver and node and start the client.
   * @param nbDrivers the number of drivers to launch.
   * @param nbNodes the number of nodes to launch.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public JPPFClient setup(final int nbDrivers, final int nbNodes) throws Exception
  {
    System.out.println("performing setup with " + nbDrivers + " drivers, " + nbNodes + " nodes and 1 client");
    createShutdownHook();
    drivers = new RestartableDriverProcessLauncher[nbDrivers];
    for (int i=0; i<nbDrivers; i++)
    {
      drivers[i] = new RestartableDriverProcessLauncher(i+1, config);
      new Thread(drivers[i], drivers[i].getName() + "process launcher").start(); 
    }
    nodes = new RestartableNodeProcessLauncher[nbNodes];
    for (int i=0; i<nbNodes; i++)
    {
      nodes[i] = new RestartableNodeProcessLauncher(i+1, config);
      new Thread(nodes[i], nodes[i].getName() + "process launcher").start(); 
    }
    if (config.isStartClient())
    {
      client = createClient("c" + clientCount.incrementAndGet(), true);
      if (config.getProperties().getBoolean("jppf.scenario.jmx.checks.enabled", true))
        jmxHandler.checkDriverAndNodesInitialized(nbDrivers, nbNodes);
    }
    return client;
  }

  /**
   * Create a client with the specified uuid.
   * @param uuid if null, let the client generate its uuid.
   * @param reset if <code>true</code>, the JPPF ocnfiguration is reloaded.
   * @return a <code>JPPFClient</code> instance.
   * @throws Exception if any error occurs.
   */
  public JPPFClient createClient(final String uuid, final boolean reset) throws Exception
  {
    if (reset) JPPFConfiguration.reset();
    client = (uuid == null) ? new JPPFClient() : new JPPFClient(uuid);
    //System.out.println("waiting for available client connection");
    while (!client.hasAvailableConnection()) Thread.sleep(10L);
    jmxHandler = new JMXHandler(client);
    return client;
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  public void cleanup() throws Exception
  {
    if (client != null)
    {
      client.close();
      client = null;
      jmxHandler = null;
      Thread.sleep(500L);
    }
    System.gc();
    stopProcesses();
    if (shutdownHook != null) Runtime.getRuntime().removeShutdownHook(shutdownHook);
  }

  /**
   * Stop driver and node processes.
   */
  protected void stopProcesses()
  {
    try
    {
      if (nodes != null) for (RestartableNodeProcessLauncher n: nodes) if (n != null) n.stopProcess();
      if (drivers != null) for (RestartableDriverProcessLauncher d: drivers) if (d != null) d.stopProcess();
    }
    catch(Throwable t)
    {
      t.printStackTrace();
    }
    finally
    {
      nodes = null;
      drivers = null;
    }
  }

  /**
   * Create the shutdown hook.
   */
  protected void createShutdownHook()
  {
    shutdownHook = new Thread()
    {
      @Override
      public void run()
      {
        stopProcesses();
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  /**
   * Get the jppf client to use.
   * @return a {@link JPPFClient} instance.
   */
  public JPPFClient getClient()
  {
    return client;
  }

  /**
   * Set the jppf client to use.
   * @param client a {@link JPPFClient} instance.
   */
  public void setClient(final JPPFClient client)
  {
    this.client = client;
  }

  /**
   * Get the nodes to launch for the test.
   * @return an array of <code>NodeProcessLauncher</code> instances.
   */
  public RestartableNodeProcessLauncher[] getNodes()
  {
    return nodes;
  }

  /**
   * Get the drivers to launch for the test.
   * @return an array of <code>DriverProcessLauncher</code> instances.
   */
  public RestartableDriverProcessLauncher[] getDrivers()
  {
    return drivers;
  }

  /**
   * Get the object which manages the JMX connections to ddrivers and nodes.
   * @return a {@link JMXHandler} instance.
   */
  public JMXHandler getJmxHandler()
  {
    return jmxHandler;
  }
}
