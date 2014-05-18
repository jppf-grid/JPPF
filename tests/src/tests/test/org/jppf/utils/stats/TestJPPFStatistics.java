/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package test.org.jppf.utils.stats;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.protocol.*;
import org.jppf.utils.ReflectionUtils;
import org.jppf.utils.stats.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for the {@link ScriptedTask} class.
 * @author Laurent Cohen
 */
public class TestJPPFStatistics extends Setup1D1N1C
{
  /**
   * Test that the latest queue size is zero, after a job has completed and during whose execution the node was restarted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testLatestQueueTaskCountUponNodeRestart() throws Exception {
    int nbTasks = 2;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, LifeCycleTask.class, 1000L);
    JMXDriverConnectionWrapper jmx = BaseSetup.getDriverManagementProxy();
    jmx.resetStatistics();
    JPPFNodeForwardingMBean nodeForwarder = jmx.getProxy(JPPFNodeForwardingMBean.MBEAN_NAME, JPPFNodeForwardingMBean.class);
    client.submitJob(job);
    Thread.sleep(1000L);
    nodeForwarder.restart(NodeSelector.ALL_NODES);
    List<Task<?>> results = ((JPPFResultCollector) job.getResultListener()).awaitResults();
    assertNotNull(results);
    JPPFStatistics stats = jmx.statistics();
    JPPFSnapshot snapshot = stats.getSnapshot(JPPFStatisticsHelper.TASK_QUEUE_COUNT);
    assertEquals((Double) 0d, (Double) snapshot.getLatest());
  }

  /**
   * Test that the latest queue size is zero, after a job has completed and whose tasks resubmit themselves once.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testLatestQueueTaskCountUponTaskResubmit() throws Exception {
    int nbTasks = 2;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, CustomTask.class, 100L);
    JMXDriverConnectionWrapper jmx = BaseSetup.getDriverManagementProxy();
    jmx.resetStatistics();
    job.getSLA().setMaxTaskResubmits(1);
    client.submitJob(job);
    List<Task<?>> results = ((JPPFResultCollector) job.getResultListener()).awaitResults();
    assertNotNull(results);
    JPPFStatistics stats = jmx.statistics();
    JPPFSnapshot snapshot = stats.getSnapshot(JPPFStatisticsHelper.TASK_QUEUE_COUNT);
    assertEquals((Double) 0d, (Double) snapshot.getLatest());
  }

  /**
   * A task that resubmits itself. Be careful to call job.getSLA().setMaxTaskResubmit(1) or with another appropriate value.
   */
  public static class CustomTask extends LifeCycleTask {
    /**
     * Initialize this task.
     * @param duration duration of the task in ms.
     */
    public CustomTask(final long duration) {
      super(duration);
    }

    @Override
    public void run() {
      super.run();
      this.setResubmit(true);
    }
  }
}
