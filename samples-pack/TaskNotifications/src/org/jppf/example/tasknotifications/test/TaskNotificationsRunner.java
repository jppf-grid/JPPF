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
package org.jppf.example.tasknotifications.test;

import java.util.*;
import java.util.concurrent.*;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.example.tasknotifications.mbean.TaskNotificationsMBean;
import org.jppf.management.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a starting point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class TaskNotificationsRunner implements NotificationListener
{
  /**
   * The JPPF client, handles all communications with the server.
   * It is recommended to only use one JPPF client per JVM, so it
   * should generally be created and used as a singleton.
   */
  private static JPPFClient jppfClient =  null;
  /**
   * Proxies to the MBean server of each node.
   */
  private List<JMXNodeConnectionWrapper> nodeConnections = new ArrayList<JMXNodeConnectionWrapper>();
  /**
   * Used to sequentialize the processing of notifications from multiple nodes.
   */
  private ExecutorService executor = Executors.newSingleThreadExecutor();

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments,
   * however nothing prevents us from using them if need be.
   */
  public static void main(final String...args)
  {
    try
    {
      // create the JPPFClient. This constructor call causes JPPF to read the configuration file
      // and connect with one or multiple JPPF drivers.
      jppfClient = new JPPFClient();

      // create a runner instance.
      TaskNotificationsRunner runner = new TaskNotificationsRunner();
      try
      {
        // subscribe to the notifications from all nodes
        runner.registerToMBeans();

        // Create a job
        JPPFJob job = runner.createJob();

        // execute a blocking job
        runner.executeBlockingJob(job);
      }
      finally
      {
        runner.close();
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Create a JPPF job that can be submitted for execution.
   * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public JPPFJob createJob() throws Exception
  {
    // create a JPPF job
    JPPFJob job = new JPPFJob();

    // give this job a readable unique id that we can use to monitor and manage it.
    job.setName("Task Notification Job");

    int nbTasks = 1;
    for (int i=1; i<=nbTasks; i++)
    {
      // add a task to the job.
      job.addTask(new NotifyingTask("" + i));
    }

    // there is no guarantee on the order of execution of the tasks,
    // however the results are guaranteed to be returned in the same order as the tasks.
    return job;
  }

  /**
   * Execute a job in blocking mode. The application will be blocked until the job
   * execution is complete.
   * @param job the JPPF job to execute.
   * @throws Exception if an error occurs while executing the job.
   */
  public void executeBlockingJob(final JPPFJob job) throws Exception
  {
    // set the job in blocking mode.
    job.setBlocking(true);

    // Submit the job and wait until the results are returned.
    // The results are returned as a list of JPPFTask instances,
    // in the same order as the one in which the tasks where initially added the job.
    List<JPPFTask> results = jppfClient.submit(job);

    // process the results
    for (JPPFTask task: results)
    {
      // if the task execution resulted in an exception
      if (task.getException() != null)
      {
        System.out.println("Task " + task.getId() + " in error: " + task.getException().getMessage());
      }
      else
      {
        System.out.println("Task " + task.getId() + " successful: " + task.getResult());
      }
    }
  }

  /**
   * Subscribe to notifications from all the nodes.
   * @throws Exception if any error occurs.
   */
  public void registerToMBeans() throws Exception
  {
    // obtain the driver connection object
    JPPFClientConnectionImpl connection = null;
    do
    {
      Thread.sleep(100L);
      connection = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
    }
    while (connection == null);
    while ((connection.getJmxConnection() == null) || !connection.getJmxConnection().isConnected()) Thread.sleep(100L);
    // get its jmx connection to the driver MBean server
    JMXDriverConnectionWrapper jmxDriver = connection.getJmxConnection();
    // collect the information to connect to the nodes' mbean servers
    Collection<JPPFManagementInfo> nodes = jmxDriver.nodesInformation();
    ObjectName objectName = new ObjectName(TaskNotificationsMBean.MBEAN_NAME);
    for (JPPFManagementInfo node: nodes)
    {
      // get a jmx connection to the node MBean server
      JMXNodeConnectionWrapper jmxNode = new JMXNodeConnectionWrapper(node.getHost(), node.getPort());
      jmxNode.connectAndWait(5000L);

      // obtain a proxy to the task notifications MBean
      MBeanServerConnection mbsc = jmxNode.getMbeanConnection();
      TaskNotificationsMBean proxy = MBeanServerInvocationHandler.newProxyInstance(mbsc, objectName, TaskNotificationsMBean.class, true);

      // subscribe to all notifications from the MBean
      proxy.addNotificationListener(this, null, null);
      nodeConnections.add(jmxNode);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleNotification(final Notification notification, final Object handback)
  {
    // to smoothe the throughput of notifications processing,
    // we submit each notification to a queue instead of handling it directly
    final String message = notification.getMessage();
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        // process the notification
        // here we simply display the message
        System.out.println("received notification: " + message);
      }
    };
    executor.submit(r);
  }

  /**
   * Close the connections to all nodes.
   */
  public void close()
  {
    for (JMXNodeConnectionWrapper jmxNode: nodeConnections)
    {
      try
      {
        jmxNode.close();
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
    if (executor != null) executor.shutdown();
  }
}
