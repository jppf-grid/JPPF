/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.org.jppf.node;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.location.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.BaseSetup.Configuration;
import test.org.jppf.test.setup.common.*;

/**
 * Unit test for nodes in offline mode.
 * @author Laurent Cohen
 */
public class TestOfflineNode extends AbstractSetupOfflineNode
{
  /**
   * Launches a driver and 2 nodes and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception
  {
    Configuration testConfig = createConfig();
    client = BaseSetup.setup(1, 2, true, false, testConfig);
  }

  /**
   * Test that a simple job triggers a deserialization error in the node (because the task class is not found in the classpath),
   * and that this error is handled properly.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testSimpleJobDeserializationError() throws Exception
  {
    int nbTasks = 5;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 10L);
    job.getSLA().getClassPath().setForceClassLoaderReset(true);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results)
    {
      assertTrue("task = " + task, task instanceof LifeCycleTask);
      Throwable t = task.getThrowable();
      assertNotNull("throwable for task '" + task.getId() + "' is null", t);
      assertNull(task.getResult());
    }
  }

  /**
   * Test that a simple job is normally executed.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testSimpleJob() throws Exception
  {
    int nbTasks = 5;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 10L);
    Location loc = new MemoryLocation(new FileLocation("build/jppf-test-framework.jar").toByteArray());
    job.getSLA().getClassPath().add("jppf-test-framework.jar", loc);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results)
    {
      assertTrue("task = " + task, task instanceof LifeCycleTask);
      Throwable t = task.getThrowable();
      assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test that a simple job expires and is cancelled upon first dispatch.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testJobDispatchExpiration() throws Exception
  {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 5000L);
    Location loc = new MemoryLocation(new FileLocation("build/jppf-test-framework.jar").toByteArray());
    job.getSLA().getClassPath().add("jppf-test-framework.jar", loc);
    job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(2000L));
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results)
    {
      assertTrue("task = " + task, task instanceof LifeCycleTask);
      Throwable t = task.getThrowable();
      assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
      assertNull(task.getResult());
    }
  }
}
