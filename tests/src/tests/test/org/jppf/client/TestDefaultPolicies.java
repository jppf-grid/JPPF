/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package test.org.jppf.client;

import static org.jppf.utils.configuration.JPPFProperties.*;
import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.Setup1D2N;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@code JPPFClient}.
 * @author Laurent Cohen
 */
public class TestDefaultPolicies extends Setup1D2N {
  /** */
  private static final long TEST_TIMEOUT = 10_000L;
  /** */
  private static final String CLIENT_SCRIPT = "$s{ new org.jppf.node.policy.Equal(\"jppf.channel.local\", true).toXML(); }$";
  /** */
  private static final String SERVER_SCRIPT = "$s{ new org.jppf.node.policy.Equal(\"jppf.node.uuid\", false, \"n2\").toXML(); }$";
  /** */
  private static final String CLIENT_XML = new StringBuilder()
    .append("<jppf:ExecutionPolicy>")
    .append("  <Equal valueType=\"boolean\">")
    .append("    <Property>jppf.channel.local</Property>")
    .append("    <Value>true</Value>")
    .append("  </Equal>")
    .append("</jppf:ExecutionPolicy>")
    .toString();
  /** */
  private static final String SERVER_XML = new StringBuilder()
    .append("<jppf:ExecutionPolicy>")
    .append("  <Equal valueType=\"string\" ignoreCase=\"false\">")
    .append("    <Property>jppf.node.uuid</Property>")
    .append("    <Value>n2</Value>")
    .append("  </Equal>")
    .append("</jppf:ExecutionPolicy>")
    .toString();
  /** */
  private static final String PACKAGE_PATH = TestDefaultPolicies.class.getPackage().getName().replace('.', '/');

  /**
   * Setup.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void classSetup() throws Exception {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> print(false, false, "Uncaught exception in thread %s%n%s", t, ExceptionUtils.getStackTrace(e)));
  }

  /**
   * Reset the configuration.
   * @throws Exception if any error occurs.
   */
  @After
  public void resetConfig() throws Exception {
    JPPFConfiguration.reset();
  }

  /**
   * Test a default server-side policy parsed from an inline script in the config.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDefaultServerPolicyFromInlineScript() throws Exception {
    TypedProperties config = JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, false).set(JOB_SLA_DEFAULT_POLICY, "inline | " + SERVER_SCRIPT);
    print(false, "default server policy property = %s", config.get(JOB_SLA_DEFAULT_POLICY));
    config = new TypedProperties().fromString(config.asString());
    print(false, "default server policy property = %s", config.get(JOB_SLA_DEFAULT_POLICY));
    try (final JPPFClient client = new JPPFClient(config)) {
      client.awaitActiveConnectionPool();
      assertNotNull(client.getDefaultPolicy());
      checkPolicyResults(client, "n2");
    }
  }

  /**
   * Test a default server-side policy parsed from an inline XML definition in the config.
   * @throws Exception if any error occurs.
   */
  //@Test
  @Test(timeout = TEST_TIMEOUT)
  public void testDefaultServerPolicyFromInlineXML() throws Exception {
    // source type is ommitted, it should default to "inline"
    TypedProperties config = JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, false).set(JOB_SLA_DEFAULT_POLICY, SERVER_XML);
    print(false, "default server policy property = %s", config.get(JOB_SLA_DEFAULT_POLICY));
    config = new TypedProperties().fromString(config.asString());
    print(false, "default server policy property = %s", config.get(JOB_SLA_DEFAULT_POLICY));
    try (final JPPFClient client = new JPPFClient(config)) {
      client.awaitActiveConnectionPool();
      assertNotNull(client.getDefaultPolicy());
      checkPolicyResults(client, "n2");
    }
  }

  /**
   * Test a default client-side policy parsed from an inline script in the config.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDefaultClientPolicyFromInlineScript() throws Exception {
    TypedProperties config = JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true).set(JOB_CLIENT_SLA_DEFAULT_POLICY, "inline | " + CLIENT_SCRIPT);
    print(false, "default client policy property = %s", config.get(JOB_CLIENT_SLA_DEFAULT_POLICY));
    config = new TypedProperties().fromString(config.asString());
    print(false, "default client policy property = %s", config.get(JOB_CLIENT_SLA_DEFAULT_POLICY));
    try (final JPPFClient client = new JPPFClient(config)) {
      client.awaitActiveConnectionPool();
      assertNotNull(client.getDefaultClientPolicy());
      checkPolicyResults(client, "local_client");
    }
  }

  /**
   * Test a default client-side policy parsed from an inline XML definition in the config.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDefaultClientPolicyFromInlineXML() throws Exception {
    TypedProperties config = JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true).set(JOB_CLIENT_SLA_DEFAULT_POLICY, "inline | " + CLIENT_XML);
    print(false, "default client policy property = %s", config.get(JOB_CLIENT_SLA_DEFAULT_POLICY));
    config = new TypedProperties().fromString(config.asString());
    print(false, "default client policy property = %s", config.get(JOB_CLIENT_SLA_DEFAULT_POLICY));
    try (final JPPFClient client = new JPPFClient(config)) {
      client.awaitActiveConnectionPool();
      assertNotNull(client.getDefaultClientPolicy());
      checkPolicyResults(client, "local_client");
    }
  }

  /**
   * Test a default server-side policy parsed from an XML resource in the classpath.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDefaultServerPolicyFromXMLResource() throws Exception {
    TypedProperties config = JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, false).set(JOB_SLA_DEFAULT_POLICY, "file | " + PACKAGE_PATH + "/server_xml_policy.xml");
    print(false, "default server policy property = %s", config.get(JOB_SLA_DEFAULT_POLICY));
    config = new TypedProperties().fromString(config.asString());
    print(false, "default server policy property = %s", config.get(JOB_SLA_DEFAULT_POLICY));
    try (final JPPFClient client = new JPPFClient(config)) {
      client.awaitActiveConnectionPool();
      assertNotNull(client.getDefaultPolicy());
      checkPolicyResults(client, "n2");
    }
  }

  /**
   * Test a default client-side policy parsed from an XML resource in the classpath.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDefaultClientPolicyFromXMLResource() throws Exception {
    TypedProperties config = JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true).set(JOB_CLIENT_SLA_DEFAULT_POLICY, "file | " + PACKAGE_PATH + "/client_xml_policy.xml");
    print(false, "default server policy property = %s", config.get(JOB_CLIENT_SLA_DEFAULT_POLICY));
    config = new TypedProperties().fromString(config.asString());
    print(false, "default server policy property = %s", config.get(JOB_CLIENT_SLA_DEFAULT_POLICY));
    try (final JPPFClient client = new JPPFClient(config)) {
      client.awaitActiveConnectionPool();
      assertNotNull(client.getDefaultClientPolicy());
      checkPolicyResults(client, "local_client");
    }
  }

  /**
   * 
   * @param client the client that submits the job.
   * @param nodeUuid the uuid of the node on which the tasks are expected to execute.
   * @throws Exception if any error occurs.
   */
  private static void checkPolicyResults(final JPPFClient client, final String nodeUuid) throws Exception {
    final int nbTasks = 50;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 0L);
    int i = 0;
    for (final Task<?> task: job.getJobTasks()) task.setId("" + i++);
    print(false, false, "submitting job");
    final List<Task<?>> results = client.submit(job);
    print(false, false, "got job results");
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    final String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
    for (i=0; i<nbTasks; i++) {
      final Task<?> ta = results.get(i);
      assertTrue(ta instanceof LifeCycleTask);
      final LifeCycleTask task = (LifeCycleTask) ta;
      final Throwable t = task.getThrowable();
      final String message = "task " + i + " has an exception; ";
      if (t != null) {
        print(false, false, "%s\n%s", message, ExceptionUtils.getStackTrace(t));
        fail(message + t);
      }
      assertEquals("result of task " + i + " should be " + msg + " but is " + task.getResult(), msg, task.getResult());
      assertEquals(job.getJobTasks().get(i).getId(), task.getId());
      assertEquals(i, task.getPosition());
      assertEquals(nodeUuid, task.getNodeUuid());
    }
  }
}
