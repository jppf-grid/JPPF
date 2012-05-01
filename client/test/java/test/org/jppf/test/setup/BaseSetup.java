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

package test.org.jppf.test.setup;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;

/**
 * Helper methods for setting up and cleaning the environment before and after testing.
 * @author Laurent Cohen
 */
public class BaseSetup
{
  /**
   * Message used for successful task execution.
   */
  public static final String EXECUTION_SUCCESSFUL_MESSAGE = "execution successful";
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
  protected static DriverProcessLauncher driver = null;
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
    JMXDriverConnectionWrapper driver = ((JPPFClientConnectionImpl) client.getClientConnection()).getJmxConnection();
    while (!driver.isConnected()) driver.connectAndWait(100L);
    return driver.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
  }

  /**
   * Launches a driver and node and start the client.
   * @param nbNodes the number of nodes to launch.
   * @return an instance of <code>JPPFClient</code>.
   * @throws Exception if a process could not be started.
   */
  public static JPPFClient setup(final int nbNodes) throws Exception
  {
    return setup(nbNodes, true);
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
    System.out.println("performing setup with 1 driver, " + nbNodes + " nodes" + (initClient ? " and 1 client" : ""));
    createShutdownHook();
    (driver = new DriverProcessLauncher()).startProcess();
    nodes = new NodeProcessLauncher[nbNodes];
    for (int i=0; i<nodes.length; i++)
    {
      // to avoid driver and node producing the same UUID
      Thread.sleep(51L);
      nodes[i] = new NodeProcessLauncher(i+1);
      nodes[i].startProcess();
      Thread.sleep(500L);
    }
    if (initClient)
    {
      client = new JPPFClient();
      System.out.println("waiting for available client connection");
      while (!client.hasAvailableConnection()) Thread.sleep(100L);
    }
    return client;
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  public static void cleanup() throws Exception
  {
    if (client != null) client.close();
    Thread.sleep(1000L);
    stopProcesses();
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
  }

  /**
   * Stop driver and node processes.
   */
  protected static void stopProcesses()
  {
    try
    {
      if (nodes != null)  for (NodeProcessLauncher n: nodes) n.stopProcess();
      if (driver != null) driver.stopProcess();
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
        stopProcesses();
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

}
