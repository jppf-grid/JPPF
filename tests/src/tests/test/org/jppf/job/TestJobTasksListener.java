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

package test.org.jppf.job;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.jppf.client.*;
import org.jppf.job.JobTasksListener;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.test.addons.jobtaskslistener.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.Setup1D1N;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Test the {@link JobTasksListener} facitlity.
 * @see org.jppf.test.addons.jobtaskslistener.MyJobTasksListener
 * @author Laurent Cohen
 */
public class TestJobTasksListener extends Setup1D1N {
  /**
   * JMX connection to the driver.
   */
  private static JMXDriverConnectionWrapper jmx;

  /**
   * Launches 2 drivers with 1 node attached to each.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    jmx = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + 1);
    assertTrue("failed to connect to " + jmx, jmx.connectAndWait(10_000L));
    final String script = new StringBuilder()
      .append("function addListener() {\n")
      .append("  var driver = serverDebug.getDriver();\n")
      .append("  var listener = org.jppf.test.addons.jobtaskslistener.MyJobTasksListener.getInstance();\n")
      .append("  driver.getJobTasksListenerManager().addJobTasksListener(listener);\n")
      .append("  return \"ok\";\n")
      .append("}\n")
      .append("addListener();").toString();
    assertEquals("ok", executeScriptOnServer(jmx, script));
    while (jmx.nbNodes() < 1) Thread.sleep(10L);
  }

  /**
   * Close the JMX connection.
   * @throws Exception if any error occurs.
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    if (jmx != null) {
      final String script = new StringBuilder()
        .append("function removeListener() {\n")
        .append("  var driver = serverDebug.getDriver();\n")
        .append("  var listener = org.jppf.test.addons.jobtaskslistener.MyJobTasksListener.getInstance();\n")
        .append("  driver.getJobTasksListenerManager().removeJobTasksListener(listener);\n")
        .append("  return \"ok\";\n")
        .append("}\n")
        .append("removeListener();").toString();
      executeScriptOnServer(jmx, script);
      Thread.sleep(100L);
      jmx.close();
    }
  }

  /**
   * Delete the files generate server-side.
   * @throws Exception if any error occurs.
   */
  @After
  public void teardown() throws Exception {
    for (File file: MyJobTasksListener.ALL_FILES)  {
      if (file.exists()) {
        final boolean success = file.delete();
        print(true, false, "%s deleting '%s'", success ? "success" : "failure", file);
      } else print(true, false, "file '%s' does not exist", file);
    }
    JPPFConfiguration.reset();
  }

  /**
   * Test that we receive notifications of taks results on the server side and that they can be processed for retrieval by the client application.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testResultsReceived() throws Exception {
    final int nbTasks = 10;
    configure();
    final JPPFJob job = new JPPFJob();
    try (JPPFClient client = new JPPFClient()) {
      client.awaitWorkingConnectionPool();
      job.setName(ReflectionUtils.getCurrentMethodName());
      job.getSLA().setCancelUponClientDisconnect(false);
      for (int i=1; i<=nbTasks; i++) job.add(new MyJobTasksListenerTask(String.format("#%02d", i), 100L));
      BaseTestHelper.printToAll(jmx, true, true, true, false, false, ">>> submitting job");
      client.submit(job);
      BaseTestHelper.printToAll(jmx, true, true, true, false, false, ">>> got job results"  );
    }
    Thread.sleep(200L);
    assertTrue(MyJobTasksListener.RESULTS_FILE.exists());
    final List<Result> results = readResults(MyJobTasksListener.RESULTS_FILE, true);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    final List<Task<?>> tasks = job.getJobTasks();
    for (int i=0; i<nbTasks; i++) {
      final Result result = results.get(i);
      assertEquals(job.getUuid(), result.jobUuid);
      assertEquals(job.getName(), result.jobName);
      final Task<?> task = tasks.get(i);
      assertEquals(task.getId(), result.taskId);
      assertNull(task.getResult());
      assertEquals(MyJobTasksListenerTask.RESULT_SUCCESS, result.taskResult);
      assertEquals(0, result.expirationCount);
      assertEquals(0, result.resubmitCount);
    }
  }

  /**
   * Test that we receive notifications of tasks results on the server side and that they can be processed for retrieval by the client application.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testWithDispatchExpiration() throws Exception {
    final int nbTasks = 1, maxExpirations = 2, nbRuns = maxExpirations + 1;
    configure();
    final JPPFJob job = new JPPFJob();
    try (final JPPFClient client = new JPPFClient()) {
      client.awaitWorkingConnectionPool();
      job.setName(ReflectionUtils.getCurrentMethodName());
      job.getSLA().setCancelUponClientDisconnect(false);
      job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(300L));
      job.getSLA().setMaxDispatchExpirations(maxExpirations);
      for (int i=1; i<=nbTasks; i++) job.add(new MyJobTasksListenerTask(String.format("#%02d", i), 5000L));
      BaseTestHelper.printToAll(jmx, true, true, true, false, false, ">>> submitting job");
      client.submit(job);
      BaseTestHelper.printToAll(jmx, true, true, true, false, false, ">>> got job results"  );
    }
    Thread.sleep(200L);
    final List<Task<?>> tasks = job.getJobTasks();
    assertTrue(MyJobTasksListener.DISPATCHED_FILE.exists());
    final List<Result> dispatched = readResults(MyJobTasksListener.DISPATCHED_FILE, false);
    assertNotNull(dispatched);
    assertEquals(nbRuns * nbTasks, dispatched.size());
    for (int run=0; run<nbRuns; run++) {
      for (int i=0; i<nbTasks; i++) {
        final Result result = dispatched.get(run * nbTasks + i);
        assertEquals(job.getUuid(), result.jobUuid);
        assertEquals(job.getName(), result.jobName);
        final Task<?> task = tasks.get(i);
        assertEquals(task.getId(), result.taskId);
        assertEquals("null", result.taskResult);
        assertEquals(run, result.expirationCount);
        assertEquals(0, result.resubmitCount);
      }
    }
    assertTrue(MyJobTasksListener.RETURNED_FILE.exists());
    final List<Result> returned = readResults(MyJobTasksListener.RETURNED_FILE, false);
    assertNotNull(returned);
    assertEquals(nbRuns * nbTasks, returned.size());
    for (int run=0; run<nbRuns; run++) {
      for (int i=0; i<nbTasks; i++) {
        final Result result = returned.get(run * nbTasks + i);
        assertEquals(job.getUuid(), result.jobUuid);
        assertEquals(job.getName(), result.jobName);
        final Task<?> task = tasks.get(i);
        assertEquals(task.getId(), result.taskId);
        assertEquals("null", result.taskResult);
        assertEquals(run + 1, result.expirationCount);
        assertEquals(0, result.resubmitCount);
      }
    }
    assertTrue(MyJobTasksListener.RESULTS_FILE.exists());
    final List<Result> results = readResults(MyJobTasksListener.RESULTS_FILE, true);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (int i=0; i<nbTasks; i++) {
      final Result result = results.get(i);
      assertEquals(job.getUuid(), result.jobUuid);
      assertEquals(job.getName(), result.jobName);
      final Task<?> task = tasks.get(i);
      assertEquals(task.getId(), result.taskId);
      assertNull(task.getResult());
      assertEquals(MyJobTasksListenerTask.RESULT_FAILURE, result.taskResult);
      assertEquals(nbRuns, result.expirationCount);
      assertEquals(0, result.resubmitCount);
    }
  }

  /** */
  private static void configure() {
    JPPFConfiguration.set(JPPFProperties.DRIVERS, new String[] {"driver1"})
      .setString("driver1.jppf.server.host", "localhost")
      .setInt("driver1.jppf.server.port", 11101)
      .setString("jppf.load.balancing.algorithm", "manual")
      .setString("jppf.load.balancing.profile", "manual")
      .setInt("jppf.load.balancing.profile.manual.size", 1000000);
  }

  /**
   * Read the results generated on the serve side from a file.
   * @param file the file to read from.
   * @param sort whether to sort the results.
   * @return a list of {@link Result} instances.
   * @throws Exception if any error occurs.
   */
  private static List<Result> readResults(final File file, final boolean sort) throws Exception {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      MyJobTasksListener.acquireLock(10_000L);
      final List<String> lines = FileUtils.textFileAsLines(reader);
      if (sort) Collections.sort(lines);
      final StringBuilder sb = new StringBuilder("content of '").append(file).append("': {\n");
      for (final String s: lines) sb.append("  ").append(s).append('\n');
      sb.append('}');
      print(true, false, sb.toString());
      final List<Result> results = new ArrayList<>(lines.size());
      final StringBuilder sb2 = new StringBuilder("results for '").append(file).append("':");
      lines.forEach(line -> {
        final Result r = new Result(line);
        results.add(r);
        sb2.append("\n  ").append(r);
      });
      print(false, false, sb2.toString());
      return results;
    } finally {
      MyJobTasksListener.releaseLock();
    }
  }

  /** */
  public static class Result {
    /** */
    public final String jobUuid, jobName, taskId, taskResult;
    /** */
    public final int expirationCount, resubmitCount, maxResubmits;

    /**
     * @param semiColumnSeparated .
     * @throws Exception if any error occurs.
     */
    public Result(final String semiColumnSeparated) {
      final String[] fields = semiColumnSeparated.split(";");
      this.jobUuid = fields[0];
      this.jobName = fields[1];
      this.taskId = fields[2];
      this.taskResult = fields[3];
      this.expirationCount = Integer.valueOf(fields[4]);
      this.resubmitCount = Integer.valueOf(fields[5]);
      this.maxResubmits = Integer.valueOf(fields[6]);
    }

    @Override
    public String toString() {
      return new StringBuilder(getClass().getSimpleName()).append('[')
        .append("jobName=").append(jobName)
        .append(", taskId=").append(taskId)
        .append(", taskResult=").append(taskResult)
        .append(", expirationCount=").append(expirationCount)
        .append(", resubmitCount=").append(resubmitCount)
        .append(", maxResubmits=").append(maxResubmits)
        .append(']').toString();
    }
  }
}
