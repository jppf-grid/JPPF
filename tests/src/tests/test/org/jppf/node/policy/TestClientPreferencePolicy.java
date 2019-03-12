/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package test.org.jppf.node.policy;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for the <code>Range</code> class.
 * @author Laurent Cohen
 */
public class TestClientPreferencePolicy extends AbstractNonStandardSetup {
  /**
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = createConfig("preference");
    config.clientConfig = "classes/tests/config/preference/client2.properties";
    config.node.jppf = "classes/tests/config/preference/node2.properties";
    client = BaseSetup.setup(2, 2, true, true, config);
    final List<JPPFConnectionPool> pools = client.awaitConnectionPools(Operator.EQUAL, 2, Operator.EQUAL, 1, 5000L, JPPFClientConnectionStatus.ACTIVE);
    assertEquals(2, pools.size());
    client.setLocalExecutionEnabled(true);
    client.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 1));
  }

  /**
   * Test various preference policies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testTrueJavaPolicy() throws Exception {
    testTruePolicy(ReflectionUtils.getCurrentMethodName() + 1, new Preference(new Equal("jppf.channel.local", false)), null, "n1", "n2");
    testTruePolicy(ReflectionUtils.getCurrentMethodName() + 2, new Preference(new Equal("jppf.channel.local", true)), null, "local_client");
    testTruePolicy(ReflectionUtils.getCurrentMethodName() + 3, new Preference(new LessThan("id", 2)), null, "local_client", "n1");
    testTruePolicy(ReflectionUtils.getCurrentMethodName() + 4, new Preference(new MoreThan("id", 1), new LessThan("id", 1)), null, "local_client", "n2");
  }

  /**
   * Test that the nodes allowed by a preference policy are further restricted by the "regular" execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testRestrictingExecutionPolicy() throws Exception {
    testTruePolicy(ReflectionUtils.getCurrentClassAndMethod(), new Preference(new MoreThan("id", 0)), new MoreThan("id", 1), "n2");
  }

  /**
   * Test that the preference for node 1, then node 2 is applied properly on 2 concurrent jobs.
   * @param name the name prefix for the submitted jobs.
   * @param preference the execution policy to test.
   * @param policy an optional (may be {@code null}) "regular" execution policy.
   * @param expectedNodes the uuids that are expected to be eligible for the preferenc epolicy.
   * @throws Exception if any error occurs.
   */
  private static void testTruePolicy(final String name, final Preference preference, final ExecutionPolicy policy, final String...expectedNodes) throws Exception {
    final int nbTasks = 10;
    final JPPFJob job = BaseTestHelper.createJob(name, false, nbTasks, LifeCycleTask.class, 10L);
    job.getClientSLA().setPreferencePolicy(preference);
    job.getClientSLA().setExecutionPolicy(policy);
    final List<Task<?>> results = client.submit(job);
    assertEquals(nbTasks, results.size());
    for (final Task<?> tsk: results) {
      assertTrue(tsk instanceof LifeCycleTask);
      final LifeCycleTask task = (LifeCycleTask) tsk;
      assertNotNull(task.getResult());
      assertTrue(String.format("%s was executed on %s, expected one of %s", task.getId(), task.getNodeUuid(), Arrays.toString(expectedNodes)), StringUtils.isOneOf(task.getNodeUuid(), false, expectedNodes));
    }
  }

  /**
   * Test that a preference policy that should allow at least one node is contracdicted by the "regular" execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testContradictoryExecutionPolicy() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, LifeCycleTask.class, 20L);
    job.getClientSLA().setPreferencePolicy(new Preference(new LessThan("id", 2)));
    job.getClientSLA().setExecutionPolicy(new AtLeast("id", 2));
    job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(2000L));
    final List<Task<?>> results = client.submit(job);
    assertEquals(1, results.size());
    final LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertNull(task.getNodeUuid());
  }

  /**
   * Test that the given scripted policy return {@code false} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testFalseJavaPolicy() throws Exception {
    testFalsePolicy(ReflectionUtils.getCurrentClassAndMethod(), new Preference(new Equal("jppf.driver.uuid", true, "some value never seen")));
  }

  /**
   * Test that the given scripted policy return {@code false} when its {@code accepts()} method is called.
   * @param name the name of the submitted job.
   * @param policy the execution policy to test.
   * @throws Exception if any error occurs.
   */
  private static void testFalsePolicy(final String name, final Preference policy) throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(name, false, nbTasks, LifeCycleTask.class, 20L);
    job.getClientSLA().setPreferencePolicy(policy);
    job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(2000L));
    final List<Task<?>> results = client.submit(job);
    assertEquals(nbTasks, results.size());
    final LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertNull(task.getNodeUuid());
  }
}
