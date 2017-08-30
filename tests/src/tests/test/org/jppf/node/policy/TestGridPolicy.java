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

package test.org.jppf.node.policy;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
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
   * Test that an XML representation of a scripted policy is valid according to the ExecutionPolicy schema.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testValidXML() throws Exception {
    String validTrueXML = FileUtils.readTextFile(RESOURCES_DIR + "/ValidTrueGridPolicy.xml");
    String validFalseXML = FileUtils.readTextFile(RESOURCES_DIR + "/ValidFalseGridPolicy.xml");
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
      String invalidXML = FileUtils.readTextFile(RESOURCES_DIR + "/InvalidGridPolicy.xml");
      PolicyParser.validatePolicy(invalidXML);
      throw new IllegalStateException("the policy is invalid but passes the validation");
    } catch(Exception e) {
      assertTrue("e = " + e, e instanceof JPPFException);
    }
  }

  /**
   * Test the results of an XML grid policy expected to let the job execute.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testTrueXmlPolicy() throws Exception {
    int nbTasks = 5;
    String xml = FileUtils.readTextFile(RESOURCES_DIR + "/ValidTrueGridPolicy.xml");
    String name = ReflectionUtils.getCurrentClassAndMethod();
    ExecutionPolicy p = PolicyParser.parsePolicy(xml);
    printOut("%s() grid policy:%n%s%n", name, p);
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      Task<?> task = results.get(i);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test the results of an XML grid policy, where the number of expected nodes is definedas an scripted expression with property substitutions.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void tesGridPolicyWithExpression() throws Exception {
    int nbTasks = 5;
    String xml = FileUtils.readTextFile(RESOURCES_DIR + "/GridPolicyWithExpression.xml");
    String name = ReflectionUtils.getCurrentClassAndMethod();
    ExecutionPolicy p = PolicyParser.parsePolicy(xml);
    printOut("%s() grid policy:%n%s%n", name, p);
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      Task<?> task = results.get(i);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test the results of an XML grid policy expected to prevent the job from executing.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testFalseXmlPolicy() throws Exception {
    int nbTasks = 5;
    String xml = FileUtils.readTextFile(RESOURCES_DIR + "/ValidFalseGridPolicy.xml");
    ExecutionPolicy p = PolicyParser.parsePolicy(xml);
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      Task<?> task = results.get(i);
      assertNull(task.getResult());
    }
  }

  /**
   * Test the results of a grid policy expected to let the job execute.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testTrueJavaPolicy() throws Exception {
    int nbTasks = 5;
    String name = ReflectionUtils.getCurrentClassAndMethod();
    // more than 1 node with at least 1 processing thread
    ExecutionPolicy p = new NodesMatching(Operator.MORE_THAN, 1, new AtLeast(JPPFProperties.PROCESSING_THREADS.getName(), 1));
    printOut("%s() grid policy:%n%s%n", name, p);
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      Task<?> task = results.get(i);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }

  /**
   * Test the results of a grid policy expected to prevent the job from executing.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testFalseJavaPolicy() throws Exception {
    int nbTasks = 5;
    // at least 4 nodes with at least 1 processing thread
    ExecutionPolicy p = new NodesMatching(Operator.AT_LEAST, 4, new AtLeast(JPPFProperties.PROCESSING_THREADS.getName(), 1));
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(JOB_TIMEOUT)); // to avoid the job being stuck
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      Task<?> task = results.get(i);
      assertNull(task.getResult());
    }
  }

  /**
   * Test a global policy expected to prevent the job from executing in the grid's global state.
   * Then, two additional nodes are started after the job is submitted, making the global policy a working one.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testWorkingFalseJavaPolicy() throws Exception {
    int nbTasks = 5;
    JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    // more than 4 nodes with at least 1 processing thread
    ExecutionPolicy p = new NodesMatching(Operator.AT_LEAST, 4, new AtLeast(JPPFProperties.PROCESSING_THREADS.getName(), 1));
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, LifeCycleTask.class, 0L);
    job.getSLA().setGridExecutionPolicy(p);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(3*JOB_TIMEOUT)); // to avoid the job being stuck
    client.submitJob(job);
    try {
      Thread.sleep(1000L);
      // a static node uuid is already assigned in the master's config file and must be overriden
      TypedProperties overrides = new TypedProperties();
      overrides.setString("jppf.node.uuid", "$script{ java.util.UUID.randomUUID().toString(); }$");
      // start 1 slave node for each master
      jmx.getNodeForwarder().provisionSlaveNodes(NodeSelector.ALL_NODES, 1, overrides);
      List<Task<?>> results = job.awaitResults();
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      for (int i=0; i<nbTasks; i++) {
        Task<?> task = results.get(i);
        assertNotNull("result of task #" + i + " is null", task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    } finally {
      // terminate the slave nodes
      jmx.getNodeForwarder().provisionSlaveNodes(NodeSelector.ALL_NODES, 0);
    }
  }
}
