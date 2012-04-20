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

package sample.test.job.management;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class JobManagementTestRunner
{
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;

  /**
   * Run the first test.
   * @throws Exception if any error occurs.
   */
  public void runTest1() throws Exception
  {
    TypedProperties props = JPPFConfiguration.getProperties();
    int nbTasks = props.getInt("job.management.nbTasks", 2);
    long duration = props.getLong("job.management.duration", 1000L);
    String jobId = "test1";
    JPPFJob job = new JPPFJob();
    job.setName(jobId);
    job.setBlocking(false);
    for (int i=0; i<nbTasks; i++)
    {
      job.addTask(new LongTask(duration));
    }
    JPPFResultCollector collector = new JPPFResultCollector(job);
    job.setResultListener(collector);
    client.submit(job);
    JMXDriverConnectionWrapper driver = new JMXDriverConnectionWrapper("localhost", 11198);
    driver.connect();
    while (!driver.isConnected()) Thread.sleep(10);
    // wait to ensure the job has been dispatched to the nodes
    Thread.sleep(1000);
    driver.cancelJob(jobId);
    driver.close();
    List<JPPFTask> results = collector.waitForResults();
    System.out.println("Test ended");
  }

  /**
   * Run the first test.
   * @throws Exception if any error occurs.
   */
  public void runTest2() throws Exception
  {
    JMXNodeConnectionWrapper[] nodes = null;
    JMXDriverConnectionWrapper driver = null;
    try
    {
      driver = getDriverProxy();
      Collection<JPPFManagementInfo> coll = driver.nodesInformation();
      nodes = new JMXNodeConnectionWrapper[2];
      int count = 0;
      for (JPPFManagementInfo info: coll)
      {
        JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
        node.connectAndWait(0L);
        nodes[count++] = node;
      }
      for (JMXNodeConnectionWrapper node: nodes) node.updateThreadPoolSize(4);
      Thread.sleep(500L);
      client.submit(createJob("broadcast1"));
      Thread.sleep(500L);
      ExecutionPolicy policy = new AtLeast("processing.threads", 4);
      int n = driver.matchingNodes(policy);
      System.out.println("found " + n + " nodes, expected = 2");
      nodes[1].updateThreadPoolSize(2);
      Thread.sleep(500L);
      client.submit(createJob("broadcast2"));
      Thread.sleep(500L);
      n = driver.matchingNodes(policy);
      System.out.println("found " + n + " nodes, expected = 1");
    }
    finally
    {
      if (driver != null) driver.close();
      if (nodes != null) for (JMXNodeConnectionWrapper node: nodes) node.close();
    }
  }

  /**
   * Get a proxy to the driver admin MBean.
   * @return an instance of <code>DriverJobManagementMBean</code>.
   * @throws Exception if the proxy could not be obtained.
   */
  protected JMXDriverConnectionWrapper getDriverProxy() throws Exception
  {
    JPPFClientConnectionImpl c = null;
    while ((c = (JPPFClientConnectionImpl) client.getClientConnection()) == null) Thread.sleep(100L); 
    JMXDriverConnectionWrapper wrapper = c.getJmxConnection();
    wrapper.connectAndWait(0);
    return wrapper;
  }

  /**
   * Create a broadcast job.
   * @param id the job id.
   * @return a <code>JPPFJob</code> instance.
   * @throws Exception if any error occurs.
   */
  protected JPPFJob createJob(final String id) throws Exception
  {
    JPPFJob job = new JPPFJob(id);
    job.setName(id);
    job.addTask(new MyBroadcastTask());
    job.getSLA().setBroadcastJob(true);
    return job;
  }

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      client = new JPPFClient();
      JobManagementTestRunner runner = new JobManagementTestRunner();
      runner.runTest2();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (client != null) client.close();
    }
  }

  /**
   * A simple task.
   */
  public static class MyBroadcastTask extends JPPFTask
  {
    @Override
    public void run()
    {
      System.out.println("broadcast of " + getClass().getName());
    }
  }
}
