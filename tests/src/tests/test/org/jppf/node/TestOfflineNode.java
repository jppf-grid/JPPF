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

package test.org.jppf.node;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.SetupOfflineNode1D2N1C;
import test.org.jppf.test.setup.common.*;

/**
 * Unit test for nodes in offline mode.
 * @author Laurent Cohen
 */
public class TestOfflineNode extends SetupOfflineNode1D2N1C
{
  /**
   * Test that a simple job triggers a deserialization error in the node (because the task class is not found in the classpath),
   * and that this error is handled properly.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testSimpleJobDeserializationError() throws Exception
  {
    int nbTasks = 5;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 10L);
    job.getSLA().getClassPath().setForceClassLoaderReset(true);
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (JPPFTask task: results)
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
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 10L);
    Location loc = new MemoryLocation(new FileLocation("build/jppf-test-framework.jar").toByteArray());
    job.getSLA().getClassPath().add("jppf-test-framework.jar", loc);
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (JPPFTask task: results)
    {
      assertTrue("task = " + task, task instanceof LifeCycleTask);
      Throwable t = task.getThrowable();
      assertNull("throwable for task '" + task.getId() + "' : " + ExceptionUtils.getStackTrace(t), t);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }
}
