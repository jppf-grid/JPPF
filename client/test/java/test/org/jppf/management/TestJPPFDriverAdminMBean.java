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

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.util.Collection;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.server.protocol.JPPFTask;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D2N1C;

/**
 * Unit tests for {@link JPPFDriverAdminMBean}.
 * In this class, we test that the functionality of the DriverJobManagementMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFDriverAdminMBean extends Setup1D2N1C
{
  /**
   * We test a job with 1 task, and attempt to cancel it after it has completed.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testNodesMatchingExecutionPolicy() throws Exception
  {
    JMXNodeConnectionWrapper[] nodes = null;
    JMXDriverConnectionWrapper driver = null;
    try
    {
      driver = getDriverProxy();
      assertNotNull(driver);
      Collection<JPPFManagementInfo> coll = driver.nodesInformation();
      assertNotNull(coll);
      assertTrue(coll.size() == 2);
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
      assertTrue("n is " + n + " but should be 2", n == 2);
      nodes[1].updateThreadPoolSize(2);
      Thread.sleep(500L);
      client.submit(createJob("broadcast2"));
      Thread.sleep(500L);
      n = driver.matchingNodes(policy);
      assertTrue("n is " + n + " but should be 1", n == 1);
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
