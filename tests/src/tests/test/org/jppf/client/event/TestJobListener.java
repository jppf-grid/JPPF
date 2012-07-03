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

package test.org.jppf.client.event;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.LifeCycleTask;

/**
 * Unit tests for <code>JPPFClient</code> using multiple connections to the same server
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
  //@Test(timeout=5000)
  //@Test
  public void testJobListenerSingleConnection() throws Exception
  {
    doTestJobListener();
  }

  /**
   * Test the <code>JobListener</code> notifications with <code>jppf.pool.size = 2</code>.
   * @throws Exception if any error occurs
   */
  //@Test(timeout=5000)
  public void testJobListenerMultipleConnections() throws Exception
  {
    configure();
    doTestJobListener();
  }

  /**
   * Test the <code>JobListener</code> notifications.
   * @throws Exception if any error occurs
   */
  public void doTestJobListener() throws Exception
  {
    try
    {
      client = BaseSetup.createClient(null, false);
      int nbTasks = 100;
      JPPFJob job = BaseSetup.createJob("TestSubmit", true, false, nbTasks, LifeCycleTask.class, 0L);
      final AtomicInteger startedCount = new AtomicInteger(0);
      final AtomicInteger endedCount = new AtomicInteger(0);
      job.addJobListener(new JobListener()
      {
        @Override
        public void jobStarted(final JobEvent event)
        {
          startedCount.incrementAndGet();
        }

        @Override
        public void jobEnded(final JobEvent event)
        {
          endedCount.incrementAndGet();
        }
      });
      List<JPPFTask> results = client.submit(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      assertEquals(1, startedCount.get());
      assertEquals(1, endedCount.get());
    }
    finally
    {
      reset();
    }
  }

  /**
   * Configure the client for a connection pool.
   */
  private void configure()
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.load.balancing.algorithm", "proportional");
    config.setProperty("jppf.load.balancing.strategy", "test");
    config.setProperty("strategy.test.initialSize", "10");
    config.setProperty("jppf.pool.size", "2");
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
