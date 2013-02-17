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

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.BaseSetup;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFJob</code>.
 * @author Laurent Cohen
 */
public class TestJPPFJob
{
  /**
   * Test that {@link JPPFTask#getTaskObject()} always returns the expected object.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testGetTaskObject() throws Exception
  {
    JPPFJob job = new JPPFJob();
    JPPFTask task = job.addTask(new SimpleRunnable());
    assertNotNull(task);
    assertNotNull(task.getTaskObject());
    JPPFTask task2 = job.addTask(new SimpleTask());
    assertNotNull(task2);
    assertNotNull(task2.getTaskObject());
  }

  /**
   * Test that the expected number of {@link JobListener} notifications are received int he expected order.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testJobListener() throws Exception
  {
    int nbTasks = 10;
    TypedProperties props = JPPFConfiguration.getProperties();
    props.setProperty("jppf.remote.execution.enabled", "false");
    props.setProperty("jppf.local.execution.enabled", "true");
    props.setProperty("jppf.local.execution.threads", "4");
    props.setProperty("jppf.load.balancing.algorithm", "manual");
    props.setProperty("jppf.load.balancing.profile", "manual");
    props.setProperty("jppf.load.balancing.profile.manual.size", "5");

    JPPFClient client = null;
    try
    {
      client = BaseSetup.createClient(null, false);
      JPPFJob job = BaseTestHelper.createJob("TestSubmit", true, false, nbTasks, LifeCycleTask.class, 50L);
      CountingJobListener listener = new CountingJobListener();
      job.addJobListener(listener);
      client.submit(job);
      assertEquals(1, listener.startedCount.get());
      assertEquals(1, listener.endedCount.get());
    }
    finally
    {
      if (client != null) client.close();
    }
  }
}
