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

import java.lang.reflect.Constructor;
import java.util.Collection;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFConfiguration;

import test.org.jppf.test.setup.common.BaseTestHelper;

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
    JMXDriverConnectionWrapper driver = ((JPPFClientConnectionImpl) client.getClientConnection()).getJmxConnection();
    while (!driver.isConnected()) driver.connectAndWait(10L);
    return driver;
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
      if (i > 0) Thread.sleep(511L);
      nodes[i] = new NodeProcessLauncher(i+1);
      nodes[i].startProcess();
    }
    if (initClient) client = createClient(null);
    checkDriverAndNodesInitialized(nbNodes);
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
    if (client != null)
    {
      client.close();
      client = null;
      Thread.sleep(500L);
      System.gc();
    }
    stopProcesses();
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
  }

  /**
   * Check that the driver and all nodes have been started and are accessible.
   * @param nbNodes the number of nodes that were started.
   * @throws Exception if any error occurs.
   */
  public static void checkDriverAndNodesInitialized(final int nbNodes) throws Exception
  {
    JMXDriverConnectionWrapper wrapper = null;
    try
    {
      wrapper = new JMXDriverConnectionWrapper("localhost", 11198);
      while (!wrapper.isConnected()) wrapper.connectAndWait(10L);
      while (true)
      {
        Collection<JPPFManagementInfo> coll = wrapper.nodesInformation();
        if ((coll == null) || (coll.size() < nbNodes)) Thread.sleep(10L);
        else break;
      }
    }
    finally
    {
      if (wrapper != null) wrapper.close();
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

  /**
   * Create a job with the specified parameters.
   * The type of the tasks is specified via their class, and the constructor to
   * use is specified based on the number of parameters.
   * @param name the job's name.
   * @param blocking specifies whether the job is blocking.
   * @param broadcast specifies whether the job is a broadcast job.
   * @param nbTasks the number of tasks to add to the job.
   * @param taskClass the class of the tasks to add to the job.
   * @param params the parameters for the tasks constructor.
   * @return a <code>JPPFJob</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFJob createJob(final String name, final boolean blocking, final boolean broadcast, final int nbTasks, final Class<?> taskClass, final Object...params) throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    int nbArgs = (params == null) ? 0 : params.length;
    Constructor constructor = BaseTestHelper.findConstructor(taskClass, nbArgs);
    for (int i=1; i<=nbTasks; i++)
    {
      Object o = constructor.newInstance(params);
      if (o instanceof JPPFTask) ((JPPFTask) o).setId(job.getName() + " - task " + i);
      job.addTask(o);
    }
    job.setBlocking(blocking);
    job.getSLA().setBroadcastJob(broadcast);
    if (!blocking) job.setResultListener(new JPPFResultCollector(job));
    return job;
  }

  /**
   * Create a task with the specified parameters.
   * The type of the task is specified via its class, and the constructor to
   * use is specified based on the number of parameters.
   * @param id the task id.
   * @param taskClass the class of the tasks to add to the job.
   * @param params the parameters for the tasks constructor.
   * @return an <code>Object</code> representing a task.
   * @throws Exception if any error occurs.
   */
  public static Object createTask(final String id, final Class<?> taskClass, final Object...params) throws Exception
  {
    int nbArgs = (params == null) ? 0 : params.length;
    Constructor constructor = findConstructor(taskClass, nbArgs);
    Object o = constructor.newInstance(params);
    if (o instanceof JPPFTask) ((JPPFTask) o).setId(id);
    return o;
  }

  /**
   * Find a constructor with the specified number of parameters for the specified class.
   * @param taskClass the class of the tasks to add to the job.
   * @param nbParams the number of parameters for the tasks constructor.
   * @return a <code>constructor</code> instance.
   * @throws Exception if any error occurs if a constructor could not be found.
   */
  public static Constructor findConstructor(final Class<?> taskClass, final int nbParams) throws Exception
  {
    Constructor[] constructors = taskClass.getConstructors();
    Constructor constructor = null;
    for (Constructor c: constructors)
    {
      if (c.getParameterTypes().length == nbParams)
      {
        constructor = c;
        break;
      }
    }
    if (constructor == null) throw new IllegalArgumentException("couldn't find a constructor for class " + taskClass.getName() + " with " + nbParams + " arguments");
    return constructor;
  }
}
