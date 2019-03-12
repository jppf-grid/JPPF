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

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
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
public class TestPreferencePolicy extends AbstractNonStandardSetup {
  /**
   * A valid XML representation of a preference policy, whose {@code accepts()} method returns {@code true}.
   */
  private static final String validTrueXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Preference>\n")
    .append("    <Equal ignoreCase='true'>\n")
    .append("      <Property>jppf.node.uuid</Property>\n")
    .append("      <Value>n1</Value>\n")
    .append("    </Equal>\n")
    .append("    <Equal ignoreCase='true'>\n")
    .append("      <Property>jppf.node.uuid</Property>\n")
    .append("      <Value>n2</Value>\n")
    .append("    </Equal>\n")
    .append("  </Preference>\n")
    .append("</jppf:ExecutionPolicy>\n").toString();
  /**
   * A valid XML representation of a preference policy, whose {@code accepts()} method returns {@code false}.
   */
  private static final String validFalseXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Preference>\n")
    .append("    <Equal ignoreCase='true'>\n")
    .append("      <Property>jppf.node.uuid</Property>\n")
    .append("      <Value>some value that will never be seen</Value>\n")
    .append("    </Equal>\n")
    .append("  </Preference>\n")
    .append("</jppf:ExecutionPolicy>\n").toString();
  /**
   * An invalid XML representation of a preference policy, which does not contain any policy in its list.
   */
  private static final String invalidXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Preference>\n")
    .append("  </Preference>\n")
    .append("</jppf:ExecutionPolicy>\n").toString();

  /**
   * Launches 3 drivers connected to each other,  with 1 node attached to each and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = createConfig("preference");
    client = BaseSetup.setup(1, 3, true, true, config);
    client.setLocalExecutionEnabled(false);
  }

  /**
   * Test that an XML representation of a scripted policy is valid according to the ExecutionPolicy schema.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testValidXML() throws Exception {
    PolicyParser.validatePolicy(validTrueXML);
    PolicyParser.validatePolicy(validFalseXML);
  }

  /**
   * Test that an XML representation of a scripted policy is valid according to the ExecutionPolicy.xsd schema.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testInvalidXML() throws Exception {
    assertThrows(JPPFException.class, () -> PolicyParser.validatePolicy(invalidXML));
  }

  /**
   * Test that the preference for node 1, then node 2 is applied properly on 2 concurrent jobs, with a policy parsed from an XML document.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testTrueXMLPolicy() throws Exception {
    final ExecutionPolicy policy = PolicyParser.parsePolicy(validTrueXML);
    assertTrue(policy instanceof Preference);
    testTruePolicy(ReflectionUtils.getCurrentClassAndMethod(), (Preference) policy, null, "n1", "n2");
  }

  /**
   * Test various preference policies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testTrueJavaPolicy() throws Exception {
    testTruePolicy(ReflectionUtils.getCurrentClassAndMethod() + 1, new Preference(new Equal("jppf.node.uuid", true, "n1"), new Equal("jppf.node.uuid", true, "n2")), null, "n1", "n2");
    testTruePolicy(ReflectionUtils.getCurrentClassAndMethod() + 2, new Preference(new MoreThan("id", 2)), null, "n3");
    testTruePolicy(ReflectionUtils.getCurrentClassAndMethod() + 3, new Preference(new LessThan("id", 2)), null, "n1");
    testTruePolicy(ReflectionUtils.getCurrentClassAndMethod() + 4, new Preference(new MoreThan("id", 2), new LessThan("id", 2)), null, "n1", "n3");
  }

  /**
   * Test that the nodes allowed by a preference policy are further restricted by the "regular" execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testRestrictingExecutionPolicy() throws Exception {
    testTruePolicy(ReflectionUtils.getCurrentClassAndMethod(), new Preference(new MoreThan("id", 1)), new MoreThan("id", 2), "n3");
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
    job.getSLA().setPreferencePolicy(preference);
    job.getSLA().setExecutionPolicy(policy);
    final List<Task<?>> results = client.submit(job);
    assertEquals(nbTasks, results.size());
    for (final Task<?> tsk: results) {
      assertTrue(tsk instanceof LifeCycleTask);
      final LifeCycleTask task = (LifeCycleTask) tsk;
      assertNotNull(task.getResult());
      assertTrue(StringUtils.isOneOf(task.getUuidFromNode(), false, expectedNodes));
    }
  }

  /**
   * Test that a preference policy that should allow at least one node is contracdicted by the "regular" execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testContradictoryExecutionPolicy() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, LifeCycleTask.class, 20L);
    job.getSLA().setPreferencePolicy(new Preference(new LessThan("id", 3)));
    job.getSLA().setExecutionPolicy(new AtLeast("id", 3));
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(2000L));
    final List<Task<?>> results = client.submit(job);
    assertEquals(1, results.size());
    final LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertNull(task.getUuidFromNode());
  }

  /**
   * Test that the given scripted policy return {@code false} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testFalseXMLPolicy() throws Exception {
    final ExecutionPolicy policy = PolicyParser.parsePolicy(validFalseXML);
    assertTrue(policy instanceof Preference);
    testFalsePolicy(ReflectionUtils.getCurrentClassAndMethod(), (Preference) policy);
  }

  /**
   * Test that the given scripted policy return {@code false} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testFalseJavaPolicy() throws Exception {
    testFalsePolicy(ReflectionUtils.getCurrentClassAndMethod(), new Preference(new Equal("jppf.node.uuid", true, "some value never seen")));
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
    job.getSLA().setPreferencePolicy(policy);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(2000L));
    final List<Task<?>> results = client.submit(job);
    assertEquals(nbTasks, results.size());
    final LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertNull(task.getUuidFromNode());
  }
}
