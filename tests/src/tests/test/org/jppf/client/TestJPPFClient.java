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

package test.org.jppf.client;

import static org.junit.Assert.*;

import java.io.NotSerializableException;
import java.lang.management.*;
import java.util.*;
import java.util.regex.Pattern;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFClient</code>.
 * @author Laurent Cohen
 */
public class TestJPPFClient extends Setup1D1N
{
  /**
   * Invocation of the <code>JPPFClient()</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testDefaultConstructor() throws Exception
  {
    JPPFClient client = new JPPFClient();
    try
    {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
    }
    finally
    {
      client.close();
    }
  }

  /**
   * Invocation of the <code>JPPFClient(String uuid)</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testConstructorWithUuid() throws Exception
  {
    JPPFClient client = new JPPFClient("some_uuid");
    try
    {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
    }
    finally
    {
      client.close();
    }
  }

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testSubmit() throws Exception
  {
    JPPFClient client = BaseSetup.createClient(null);
    try
    {
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob("TestSubmit", true, false, nbTasks, LifeCycleTask.class, 0L);
      int i = 0;
      for (JPPFTask task: job.getTasks()) task.setId("" + i++);
      List<JPPFTask> results = client.submit(job);
      assertNotNull(results);
      assertTrue("results size should be " + nbTasks + " but is " + results.size(), results.size() == nbTasks);
      String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      for (i=0; i<nbTasks; i++)
      {
        JPPFTask t = results.get(i);
        Exception e = t.getException();
        assertNull("task " + i +" has an exception " + e, e);
        assertEquals("result of task " + i + " should be " + msg + " but is " + t.getResult(), msg, t.getResult());
      }
    }
    finally
    {
      client.close();
    }
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testCancelJob() throws Exception
  {
    JPPFClient client = BaseSetup.createClient(null);
    try
    {
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob("TestJPPFClientCancelJob", false, false, nbTasks, LifeCycleTask.class, 5000L);
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      int i = 0;
      for (JPPFTask task: job.getTasks()) task.setId("" + i++);
      client.submit(job);
      Thread.sleep(1500L);
      client.cancelJob(job.getUuid());
      List<JPPFTask> results = collector.waitForResults();
      assertNotNull(results);
      assertTrue("results size should be " + nbTasks + " but is " + results.size(), results.size() == nbTasks);
      int count = 0;
      for (JPPFTask t: results)
      {
        if (t.getResult() == null) count++;
      }
      assertTrue(count > 0);
    }
    finally
    {
      client.close();
    }
  }

  /**
   * Test that the number of threads for local execution is the configured one.
   * See bug <a href="http://sourceforge.net/tracker/?func=detail&aid=3539111&group_id=135654&atid=733518">3539111 - Local execution does not use configured number of threads</a>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testLocalExecutionNbThreads() throws Exception
  {
    int nbThreads = 2;
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.local.execution.enabled", "true");
    config.setProperty("jppf.local.execution.threads", "" + nbThreads);
    JPPFClient client = new JPPFClient();
    try
    {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);

      // submit a job to ensure all local execution threads are created
      int nbTasks = 100;
      JPPFJob job = BaseTestHelper.createJob("TestSubmit", true, false, nbTasks, LifeCycleTask.class, 0L);
      int i = 0;
      for (JPPFTask task: job.getTasks()) task.setId("" + i++);
      List<JPPFTask> results = client.submit(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      for (JPPFTask t: results)
      {
        Exception e = t.getException();
        assertNull(e);
        assertEquals(msg, t.getResult());
      }

      ThreadMXBean mxbean = ManagementFactory.getThreadMXBean();
      long[] ids = mxbean.getAllThreadIds();
      ThreadInfo[] allInfo = mxbean.getThreadInfo(ids);
      int count = 0;
      for (ThreadInfo ti: allInfo)
      {
        if (ti == null) continue;
        String name = ti.getThreadName();
        if (name == null) continue;
        if (name.startsWith("node processing")) count++;
      }
      assertEquals(nbThreads, count);
    }
    finally
    {
      client.close();
      JPPFConfiguration.reset();
    }
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when submitting a job to a driver is properly handled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testNotSerializableExceptionFromClient() throws Exception
  {
    JPPFClient client = new JPPFClient();
    try
    {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, NotSerializableTask.class, true);
      List<JPPFTask> results = client.submit(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      JPPFTask task = results.get(0);
      assertNotNull(task.getException());
      assertTrue(task.getException() instanceof NotSerializableException);
    }
    finally
    {
      client.close();
    }
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when a node returns execution results is properly handled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testNotSerializableExceptionFromNode() throws Exception
  {
    JPPFClient client = new JPPFClient();
    try
    {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, NotSerializableTask.class, false);
      List<JPPFTask> results = client.submit(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      JPPFTask task = results.get(0);
      assertTrue(task instanceof JPPFExceptionResult);
      assertNotNull(task.getException());
      assertTrue(task.getException() instanceof NotSerializableException);
    }
    finally
    {
      client.close();
    }
  }

  /**
   * Test that JMX connection threads are properly terminated when the JPPF connection fails.
   * This relates to the bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-131">JPPF-131 JPPF client does not release JMX thread upon connection failure</a>
   * @throws Exception if any error occurs
   */
  @Test(timeout=15000)
  public void testNoJMXConnectionThreadsLeak() throws Exception
  {
    String name = Thread.currentThread().getName();
    MyClient client = null;
    TypedProperties config = JPPFConfiguration.getProperties();
    try {
      Thread.currentThread().setName("JPPF-test");
      int poolSize = 2;
      int maxReconnect = 3;
      config.setProperty("reconnect.max.time", Integer.toString(maxReconnect));
      config.setProperty("jppf.discovery.enabled", "false");
      config.setProperty("jppf.pool.size", Integer.toString(poolSize));
      config.setProperty("jppf.drivers", "driver1");
      config.setProperty("driver1.jppf.server.host", "localhost");
      config.setProperty("driver1.jppf.server.port", "11101");
      config.setProperty("driver1.jppf.pool.size", Integer.toString(poolSize));
      config.setProperty("driver1.jppf.management.port", "11201");
      client = new MyClient();
      waitForNbConnections(client, poolSize, JPPFClientConnectionStatus.ACTIVE);
      restartDriver(client, poolSize, 1000L * maxReconnect + 1500L);
      String[] threads = threadNames("^JMX connection .*");
      assertEquals(poolSize, threads.length);
    } finally {
      System.out.println("connections: " + client.getAllConnections());
      if (client != null) client.close();
      JPPFConfiguration.reset();
      Thread.currentThread().setName(name);
    }
  }

  /**
   * Restart the driver.
   * @param client the JPPF client.
   * @param poolSize the number of expected connections.
   * @param restartDelay the driver restart delay in milliseconds.
   * @throws Exception if any error occurs.
   */
  private void restartDriver(final MyClient client, final int poolSize, final long restartDelay) throws Exception {
    JMXDriverConnectionWrapper jmx = getJmxConnection(client);
    jmx.restartShutdown(100L, restartDelay);
    waitForNbConnections(client, 0, null);
    client.initRemotePools(JPPFConfiguration.getProperties());
    waitForNbConnections(client, poolSize, JPPFClientConnectionStatus.ACTIVE);
  }

  /**
   * Wait until the client has the specified number of connections.
   * @param client the JPPF client.
   * @param status the expected status of each connection.
   * @param nbConnections the number of connections to reach.
   * @throws Exception if any error occurs.
   */
  private void waitForNbConnections(final JPPFClient client, final int nbConnections, final JPPFClientConnectionStatus status) throws Exception {
    int count = -1;
    while (count != nbConnections) {
      try {
        count = 0;
        Thread.sleep(10L);
        List<JPPFClientConnection> list = client.getAllConnections();
        for (JPPFClientConnection conn: list) {
          if ((status == null) || (conn.getStatus() == status)) count++;
        }
      } catch (Exception ignore) {
        throw ignore;
      }
    }
    if (nbConnections > 0) getJmxConnection(client);
  }

  /**
   * Get a connected JMX connection for the psecified client.
   * @param client the JPPF client.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    JMXDriverConnectionWrapper jmx = null;
    while (jmx == null) {
      try {
        jmx = client.getClientConnection().getJmxConnection();
        while (!jmx.isConnected()) Thread.sleep(10L);
      } catch (Exception e) {
        Thread.sleep(10L);
      }
    }
    return jmx;
  }

  /**
   * Get the names of all threads in this JVM matching the specified regex pattern.
   * @param pattern the pattern to match against.
   * @return an array of thread names.
   */
  private String[] threadNames(final String pattern)
  {
    Pattern p = pattern == null ? null : Pattern.compile(pattern);
    ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
    long[] ids = threadsBean.getAllThreadIds();
    ThreadInfo[] infos = threadsBean.getThreadInfo(ids, 0);
    List<String> result = new ArrayList<String>();
    for (int i=0; i<infos.length; i++)
    {
      if ((p == null) || p.matcher(infos[i].getThreadName()).matches())
        result.add(infos[i].getThreadName());
    }
    return result.toArray(new String[result.size()]);
  }

  /**
   * A task which holds a non-serializable object.
   */
  public static class NotSerializableTask extends JPPFTask
  {
    /**
     * A non-serializable object.
     */
    private NotSerializableObject nso = null;
    /**
     *  <code>true</code> if the non-serializable object should be created in the constructor, <code>false</code> if it should be created in the client.
     */
    private final boolean instantiateInClient;

    /**
     * Initialize with the specified flag.
     * @param instantiateInClient <code>true</code> if the non-serializable object should be created in the constructor (client side),
     * <code>false</code> if it should be created in the <code>run()</code> method (node side).
     */
    public NotSerializableTask(final boolean instantiateInClient)
    {
      this.instantiateInClient = instantiateInClient;
      if (instantiateInClient) nso = new NotSerializableObject();
    }

    @Override
    public void run()
    {
      if (!instantiateInClient) nso = new NotSerializableObject();
    }
  }

  /**
   * A task which holds a non-serializable object.
   */
  public static class NotSerializableObject
  {
    /**
     * Any attribute will do.
     */
    public String name = "NotSerializableObject";
  }

  /**
   * 
   */
  public static class MyClient extends JPPFClient
  {
    @Override
    public void initRemotePools(final TypedProperties props) {
      super.initRemotePools(props);
    }
  }
}
