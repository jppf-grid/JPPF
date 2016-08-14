/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
import org.jppf.utils.stats.*;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D2N1C;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for the <code>ScriptedPolicy</code> class.
 * @author Laurent Cohen
 */
public class TestScriptedPolicy extends Setup1D2N1C {
  /**
   * A valid XML representation of a scripted policy, whose {@code accepts()} method returns {@code true}.
   */
  private String validTrueXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Script language='javascript'>true</Script>\n")
    .append("</jppf:ExecutionPolicy>\n").toString();

  /**
   * A valid XML representation of a scripted policy, whose {@code accepts()} method returns {@code false}.
   */
  private String validFalseXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Script language='groovy'>return false</Script>\n")
    .append("</jppf:ExecutionPolicy>\n").toString();

  /**
   * An invalid XML representation of a scripted policy.<br/>
   * 'unknownAttribute' attribute is not permitted, and 'unknownElement' element isn't permitted.
   */
  private String invalidXML = new StringBuilder()
    .append("<jppf:ExecutionPolicy xmlns:jppf='http://www.jppf.org/schemas/ExecutionPolicy.xsd'>\n")
    .append("  <Script language='groovy' unknownAttribute='whatever'>return true\n")
    .append("    <unknownElement>some text</unknownElement>\n")
    .append("  </Script>\n")
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
   * Test that the given scripted policy return {@code true} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs
   * 
   */
  @Test(timeout=5000)
  public void testSimpleTruePolicy() throws Exception {
    ExecutionPolicy p = PolicyParser.parsePolicy(validTrueXML);
    assertTrue(p instanceof ScriptedPolicy);
    assertTrue(p.accepts(null));
  }

  /**
   * Test that the given scripted policy return {@code false} when its {@code accepts()} method is called.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSimpleFalsePolicy() throws Exception {
    ExecutionPolicy p = PolicyParser.parsePolicy(validFalseXML);
    assertTrue(p instanceof ScriptedPolicy);
    assertFalse(p.accepts(null));
  }

  /**
   * Test the results of a scripted policy based on a non-trivial Groovy script.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testComplexPolicyGroovy() throws Exception {
    String script = FileUtils.readTextFile(getClass().getPackage().getName().replace('.', '/') + "/TestScriptedPolicy.groovy");
    ScriptedPolicy p = new ScriptedPolicy("groovy", script);
    printOut("the policy is: %s", p);
    JPPFStatistics stats = new JPPFStatistics();
    JPPFSnapshot sn = stats.createSnapshot(true, "nodes");
    sn.addValues(10, 10);
    JPPFJob job = new JPPFJob();
    job.getSLA().setPriority(7);
    p.setContext(job.getSLA(), job.getClientSLA(), null, 3, stats);
    assertTrue(p.accepts(null));
    p.setContext(job.getSLA(), job.getClientSLA(), null, 7, stats);
    assertFalse(p.accepts(null));
  }

  /**
   * Test the results of a scripted policy based on a non-trivial Groovy script.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testComplexPolicyJavascript() throws Exception {
    String script = FileUtils.readTextFile(getClass().getPackage().getName().replace('.', '/') + "/TestScriptedPolicy.js");
    ScriptedPolicy p = new ScriptedPolicy("javascript", script);
    printOut("the policy is: %s", p);
    JPPFStatistics stats = new JPPFStatistics();
    JPPFSnapshot sn = stats.createSnapshot(true, "nodes");
    sn.addValues(10, 10);
    JPPFJob job = new JPPFJob();
    job.getSLA().setPriority(7);
    p.setContext(job.getSLA(), job.getClientSLA(), null, 3, stats);
    assertTrue(p.accepts(null));
    p.setContext(job.getSLA(), job.getClientSLA(), null, 7, stats);
    assertFalse(p.accepts(null));
  }

  /**
   * Test the results of a server-side scripted policy based on a Groovy script.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testInServerGroovy() throws Exception {
    ScriptedPolicy p = new ScriptedPolicy("groovy", "jppfSystemInfo.getJppf().getString('jppf.node.uuid') == 'n2'");
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, LifeCycleTask.class, 0L);
    job.getSLA().setExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(4000L)); // to avoid the job being stuck
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    Task<?> task = results.get(0);
    assertNotNull(task.getResult());
    assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    assertEquals("n2", ((LifeCycleTask) task).getNodeUuid());
  }

  /**
   * Test the results of a server-side scripted policy based on a Javascript script.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testInServerJavascript() throws Exception {
    ScriptedPolicy p = new ScriptedPolicy("javascript", "jppfSystemInfo.getJppf().getString('jppf.node.uuid') == 'n2'");
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, LifeCycleTask.class, 0L);
    job.getSLA().setExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(4000L)); // to avoid the job being stuck
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    Task<?> task = results.get(0);
    assertNotNull(task.getResult());
    assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    assertEquals("n2", ((LifeCycleTask) task).getNodeUuid());
  }

  /**
   * Test the results of a client-side scripted policy based on a Groovy script.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testInClient() throws Exception {
    try {
      client.setLocalExecutionEnabled(true);
      ExecutionPolicy p = new Equal("jppf.channel.local", true).and(new ScriptedPolicy("groovy", "true"));
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, LifeCycleTask.class, 0L);
      job.getClientSLA().setExecutionPolicy(p);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(4000L)); // to avoid the job being stuck
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      Task<?> task = results.get(0);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      assertEquals("local_client", ((LifeCycleTask) task).getNodeUuid());
    } finally {
      client.setLocalExecutionEnabled(false);
    }
  }
}
