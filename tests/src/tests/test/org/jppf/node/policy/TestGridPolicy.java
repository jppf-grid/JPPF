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
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D2N1C;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for the {@link NodesMatching} class.
 * @author Laurent Cohen
 */
public class TestGridPolicy extends Setup1D2N1C {
  /**
   * Folder where the XML policy files are located.
   */
  private static final String RESOURCES_DIR = TestGridPolicy.class.getPackage().getName().replace(".", "/");
  /**
   * Job expiration timeout.
   */
  private static final long JOB_TIMEOUT = 3_000L;
  /**
   * Test timeout.
   */
  private static final long TEST_TIMEOUT = 20_000L;

  /**
   * Test that an XML representation of a scripted policy is valid according to the ExecutionPolicy schema.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void testValidXML() throws Exception {
    final String validTrueXML = FileUtils.readTextFile(RESOURCES_DIR + "/ValidTrueGridPolicy.xml");
    final String validFalseXML = FileUtils.readTextFile(RESOURCES_DIR + "/ValidFalseGridPolicy.xml");
    PolicyParser.validatePolicy(validTrueXML);
    PolicyParser.validatePolicy(validFalseXML);
  }

  /**
   * Test that an XML representation of a scripted policy is valid according to the ExecutionPolicy.xsd schema.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void testInvalidXML() throws Exception {
    try {
      final String invalidXML = FileUtils.readTextFile(RESOURCES_DIR + "/InvalidGridPolicy.xml");
      PolicyParser.validatePolicy(invalidXML);
      throw new IllegalStateException("the policy is invalid but passes the validation");
    } catch(final Exception e) {
      assertTrue("e = " + e, e instanceof JPPFException);
    }
  }

  /**
   * Test the results of an XML grid policy expected to let the job execute.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void testTrueXmlPolicy() throws Exception {
    final int nbTasks = 5;
    final String xml = FileUtils.readTextFile(RESOURCES_DIR + "/ValidTrueGridPolicy.xml");
    final String name = ReflectionUtils.getCurrentClassAndMethod();
    final ExecutionPolicy p = PolicyParser.parsePolicy(xml);
    printOut("%s() grid policy:%n%s%n", name, p);
    final JPPFJob job = BaseTestHelper.createJob(name, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      final Task<?> task = results.get(i);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test the results of an XML grid policy, where the number of expected nodes is definedas an scripted expression with property substitutions.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void tesGridPolicyWithExpression() throws Exception {
    final int nbTasks = 5;
    final String xml = FileUtils.readTextFile(RESOURCES_DIR + "/GridPolicyWithExpression.xml");
    final String name = ReflectionUtils.getCurrentClassAndMethod();
    final ExecutionPolicy p = PolicyParser.parsePolicy(xml);
    printOut("%s() grid policy:%n%s%n", name, p);
    final JPPFJob job = BaseTestHelper.createJob(name, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      final Task<?> task = results.get(i);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test the results of an XML grid policy expected to prevent the job from executing.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void testFalseXmlPolicy() throws Exception {
    final int nbTasks = 5;
    final String xml = FileUtils.readTextFile(RESOURCES_DIR + "/ValidFalseGridPolicy.xml");
    final ExecutionPolicy p = PolicyParser.parsePolicy(xml);
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      final Task<?> task = results.get(i);
      assertNull(task.getResult());
    }
  }

  /**
   * Test the results of a grid policy expected to let the job execute.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void testTrueJavaPolicy() throws Exception {
    final int nbTasks = 5;
    final String name = ReflectionUtils.getCurrentClassAndMethod();
    // more than 1 node with at least 1 processing thread
    final ExecutionPolicy p = new NodesMatching(Operator.MORE_THAN, 1, new AtLeast(JPPFProperties.PROCESSING_THREADS.getName(), 1));
    printOut("%s() grid policy:%n%s%n", name, p);
    final JPPFJob job = BaseTestHelper.createJob(name, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      final Task<?> task = results.get(i);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test the results of a grid policy expected to prevent the job from executing.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void testFalseJavaPolicy() throws Exception {
    final int nbTasks = 5;
    // at least 4 nodes with at least 1 processing thread
    final ExecutionPolicy p = new NodesMatching(Operator.AT_LEAST, 4, new AtLeast(JPPFProperties.PROCESSING_THREADS.getName(), 1));
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      final Task<?> task = results.get(i);
      assertNull(task.getResult());
    }
  }

  /**
   * Test a global policy expected to prevent the job from executing in the grid's global state.
   * Then, two additional nodes are started after the job is submitted, making the global policy a working one.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void testWorkingFalseJavaPolicy() throws Exception {
    final NodeSelector masterNodeSelector = new ExecutionPolicySelector(new IsMasterNode());
    final int nbTasks = 5;
    final JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    // more than 4 nodes with at least 1 processing thread
    final ExecutionPolicy p = new NodesMatching(Operator.AT_LEAST, 4, new AtLeast(JPPFProperties.PROCESSING_THREADS.getName(), 1));
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(3*JOB_TIMEOUT)); // to avoid the job being stuck
    BaseTestHelper.printToAll(client, true, true, true, true, false, "submitting job %s", job.getName());
    client.submitAsync(job);
    try {
      Thread.sleep(1000L);
      // a static node uuid is already assigned in the master's config file and must be overriden
      final TypedProperties overrides = new TypedProperties().setString("jppf.node.uuid", "$script{ java.util.UUID.randomUUID().toString(); }$");
      // start 1 slave node for each master
      BaseTestHelper.printToAll(client, true, true, true, true, false, "provisioning slave nodes");
      jmx.getForwarder().provisionSlaveNodes(masterNodeSelector, 1, overrides);
      BaseTestHelper.printToAll(client, true, true, true, true, false, "awaiting job results");
      final List<Task<?>> results = job.awaitResults();
      BaseTestHelper.printToAll(client, true, true, true, true, false, "got job results");
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      for (int i=0; i<nbTasks; i++) {
        final Task<?> task = results.get(i);
        assertNotNull("result of task #" + i + " is null", task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    } finally {
      // terminate the slave nodes
      BaseTestHelper.printToAll(client, true, true, true, true, false, "stopping slave nodes");
      jmx.getForwarder().provisionSlaveNodes(masterNodeSelector, 0);
      print(false, false, "slave nodes shutdown requested");
      ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmx.nbNodes() == 2, 5000L, 250L, true);
      print(false, false, "all slave nodes have stopped");
    }
  }
}
