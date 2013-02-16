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

package test.org.jppf.client.event;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JobListener</code> using multiple connections to the same server
 * (connection pool size > 1).
 * @author Laurent Cohen
 */
public class TestJobListener extends Setup1D1N
{
  /**
   * The JPPF client.
   */
  private JPPFClient client = null;

  /**
   * Test the <code>JobListener</code> notifications with <code>jppf.pool.size = 1</code>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testJobListenerSingleLocalConnection() throws Exception
  {
    try
    {
      configure(false, true, 1);
      CountingJobListener listener = new CountingJobListener();
      int nbTasks = 20;
      List<JPPFTask> results = runJob(ReflectionUtils.getCurrentMethodName(), listener, nbTasks);
      assertEquals(1, listener.startedCount.get());
      assertEquals(1, listener.endedCount.get());
      assertEquals(4, listener.dispatchedCount.get());
      assertEquals(4, listener.returnedCount.get());
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test the <code>JobListener</code> notifications with <code>jppf.pool.size = 2</code>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testJobListenerMultipleRemoteConnections() throws Exception
  {
    try
    {
      configure(true, false, 2);
      CountingJobListener listener = new CountingJobListener();
      int nbTasks = 20;
      List<JPPFTask> results = runJob(ReflectionUtils.getCurrentMethodName(), listener, nbTasks);
      assertEquals(1, listener.startedCount.get());
      assertEquals(1, listener.endedCount.get());
      assertEquals(4, listener.dispatchedCount.get());
      assertEquals(4, listener.returnedCount.get());
    }
    finally
    {
      reset();
    }
  }

  /**
   * submit the job with the specified listener and number of tasks.
   * @param name the name of the job to run.
   * @param listener the listener to use for the test.
   * @param nbTasks the number of tasks
   * @return the execution results.
   * @throws Exception if any error occurs
   */
  public List<JPPFTask> runJob(final String name, final CountingJobListener listener, final int nbTasks) throws Exception
  {
    client = BaseSetup.createClient(null, false);
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 0L);
    if (listener != null) job.addJobListener(listener);
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    Thread.sleep(250L);
    return results;
  }

  /**
   * Configure the client for a connection pool.
   * @param remoteEnabled specifies whether remote execution is enabled.
   * @param localEnabled specifies whether local execution is enabled.
   * @param poolSize the size of the connection pool.
   */
  private void configure(final boolean remoteEnabled, final boolean localEnabled, final int poolSize)
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.remote.execution.enabled", String.valueOf(remoteEnabled));
    config.setProperty("jppf.local.execution.enabled", String.valueOf(localEnabled));
    config.setProperty("jppf.local.execution.threads", "4");
    config.setProperty("jppf.load.balancing.algorithm", "manual");
    config.setProperty("jppf.load.balancing.strategy", "manual");
    config.setProperty("strategy.manual.size", "5");
    config.setProperty("jppf.pool.size", String.valueOf(poolSize));
  }

  /**
   * Reset the confiugration.
   */
  private void reset()
  {
    if (client != null)
    {
      client.close();
      client = null;
    }
    JPPFConfiguration.reset();
  }
}
