/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package test.org.jppf.server.peer;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.Condition;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Test a multi-server topology with 2 servers, 1 node attached to the first server only (2nd server is an orphan) and 1 client.
 * This is to test that no job will be disptached to the server without odes and hang there.
 * @author Laurent Cohen
 */
public class TestMultiServerWithOrphan extends AbstractNonStandardSetup {
  /**
   * Launches 2 drivers with 1 node attached to the first server only and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass()
  public static void setup() throws Exception {
    printOut("checking topology");
    client = BaseSetup.setup(2, 1, true, createConfig("p2p_orphan"));
    printOut("topology checked OK");
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSimpleJob() throws Exception {
    final List<JPPFConnectionPool> pools = client.awaitConnectionPools(Operator.AT_LEAST, 2, Operator.AT_LEAST, 1, 5000L, JPPFClientConnectionStatus.workingStatuses());
    for (final JPPFConnectionPool pool: pools) {
      final int expectedNodes = "driver1".equals(pool.getName()) ? 1 : 0;
      final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
      ConcurrentUtils.awaitCondition(new Condition() {
        @Override
        public boolean evaluate() {
          try {
            return jmx.nbIdleNodes() == expectedNodes;
          } catch (@SuppressWarnings("unused") final Exception e) {
            return false;
          }
        }
      }, 5000L, true);
    }
    final int nbTasks = 20;
    final String name = ReflectionUtils.getCurrentClassAndMethod();
    final JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 1L);
    // execute only on 1st server
    job.getClientSLA().setExecutionPolicy(new Equal("jppf.server.port", 11101));
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> t: results) {
      assertTrue("task = " + t, t instanceof LifeCycleTask);
      final Throwable throwable = t.getThrowable();
      assertNull("throwable for task '" + t.getId() + "' : " + ExceptionUtils.getStackTrace(throwable), throwable);
      assertNotNull(t.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, t.getResult());
    }
  }
}
