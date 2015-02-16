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

package test.org.jppf.node.policy;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.ReflectionUtils;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D2N1C;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for the <code>Range</code> class.
 * @author Laurent Cohen
 */
public class TestPreferencePolicy extends Setup1D2N1C {
  /**
   * A valid XML representation of a preference policy, whose {@code accepts()} method returns {@code true}.
   */
  private String validTrueXML = new StringBuilder()
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
  private String validFalseXML = new StringBuilder()
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
  private String invalidXML = new StringBuilder()
  .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
  .append("  <Preference>\n")
  .append("  </Preference>\n")
  .append("</jppf:ExecutionPolicy>\n").toString();

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
    try {
      PolicyParser.validatePolicy(invalidXML);
      throw new IllegalStateException("the policy is invalid but passes the validation");
    } catch(Exception e) {
      assertTrue("e = " + e, e instanceof JPPFException);
    }
  }

  /**
   * Test that the preference for node 1, then node 2 is applied properly on 2 concurrent jobs, with a policy parsed from an XML document.
   * @throws Exception if any error occurs
   */
  @Test(timeout=15000)
  public void testTrueXMLPolicy() throws Exception {
    try {
      ExecutionPolicy policy = PolicyParser.parsePolicy(validTrueXML);
      assertTrue(policy instanceof Preference);
      testTruePolicy(ReflectionUtils.getCurrentClassAndMethod(), policy);
    } catch(Throwable t) {
      t.printStackTrace();
      if (t instanceof Exception) throw (Exception) t;
      else if (t instanceof Error) throw (Error) t;
      throw new RuntimeException(t);
    }
  }

  /**
   * Test that the preference for node 1, then node 2 is applied properly on 2 concurrent jobs, with a policy built using a Java constructor.
   * @throws Exception if any error occurs
   */
  @Test(timeout=15000)
  public void testTrueJavaPolicy() throws Exception {
    testTruePolicy(ReflectionUtils.getCurrentClassAndMethod(), new Preference(new Equal("jppf.node.uuid", true, "n1"), new Equal("jppf.node.uuid", true, "n2")));
  }

  /**
   * Test that the preference for node 1, then node 2 is applied properly on 2 concurrent jobs.
   * @param name the name prefix for the submitted jobs.
   * @param policy the execution policy to test.
   * @throws Exception if any error occurs
   */
  private void testTruePolicy(final String name, final ExecutionPolicy policy) throws Exception {
    JPPFConnectionPool pool = null;
    while ((pool = client.getConnectionPool()) == null) Thread.sleep(10L);
    try {
      pool.setMaxSize(2);
      pool.awaitActiveConnections(Operator.AT_LEAST, 2);
      int nbTasks = 1;
      JPPFJob job1 = BaseTestHelper.createJob(name + " 1", false, false, nbTasks, LifeCycleTask.class, 3000L);
      job1.getSLA().setExecutionPolicy(policy);
      JPPFJob job2 = BaseTestHelper.createJob(name + " 2", false, false, nbTasks, LifeCycleTask.class, 3000L);
      job2.getSLA().setExecutionPolicy(policy);
      // ensure job 2 is started by the server while job 1 is already executing
      job2.getSLA().setJobSchedule(new JPPFSchedule(1000L));
      client.submitJob(job1);
      client.submitJob(job2);
      List<Task<?>> results1 = job1.awaitResults();
      assertEquals(nbTasks, results1.size());
      LifeCycleTask task1 = (LifeCycleTask) results1.get(0);
      assertNotNull(task1.getResult());
      List<Task<?>> results2 = job2.awaitResults();
      assertEquals(nbTasks, results2.size());
      LifeCycleTask task2 = (LifeCycleTask) results2.get(0);
      assertNotNull(task2.getResult());
      assertFalse(task1.getNodeUuid().equals(task2.getNodeUuid()));
      //assertNotSame(task1.getNodeUuid(), task2.getNodeUuid());
    } finally {
      pool.setMaxSize(1);
    }
  }

  /**
   * Test that the given scripted policy return {@code false} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testFalseXMLPolicy() throws Exception {
    ExecutionPolicy policy = PolicyParser.parsePolicy(validFalseXML);
    assertTrue(policy instanceof Preference);
    testFalsePolicy(ReflectionUtils.getCurrentClassAndMethod(), policy);
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
  private void testFalsePolicy(final String name, final ExecutionPolicy policy) throws Exception {
    int nbTasks = 1;
    assertTrue(policy instanceof Preference);
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 10L);
    job.getSLA().setExecutionPolicy(policy);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(2000L));
    List<Task<?>> results = client.submitJob(job);
    assertEquals(nbTasks, results.size());
    LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertNull(task.getNodeUuid());
  }
}
