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

package test.org.jppf.client;

import static org.junit.Assert.*;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.jppf.client.*;
import org.jppf.discovery.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.Test;
import org.slf4j.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-523">JPPF-523</a>.
 * @author Laurent Cohen
 */
public class TestJPPFClientInit extends Setup1D1N {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TestJPPFClientInit.class);

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testSystemProperties() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, 1, LifeCycleTask.class, 0L);
    job.getJobTasks().get(0).setId("task0");
    final UncaughtExceptionHandler oldEh = Thread.getDefaultUncaughtExceptionHandler();
    final MyUncaughtExceptionHandler myEh = new MyUncaughtExceptionHandler();
    for (int i=1; i<=10_000; i++) System.setProperty("test" + i, "123");
    final TypedProperties config = BaseSetup.resetClientConfig();
    config.set(JPPFProperties.REMOTE_EXECUTION_ENABLED, false);
    try (JPPFClient client = new JPPFClient(config)) {
      client.addDriverDiscovery(new SimpleDiscovery());
      Thread.sleep(1L);
      Thread.setDefaultUncaughtExceptionHandler(myEh);
      for (int i=1; i<=10_000; i++) System.getProperties().remove("test" + i);
      client.submitJob(job);
      final List<Task<?>> results = job.get(3L, TimeUnit.SECONDS);
      if (myEh.uncaught != null) throw myEh.uncaught;
      assertNotNull(results);
      assertEquals(1, results.size());
      final String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      final Task<?> task = results.get(0);
      final Throwable t = task.getThrowable();
      assertNull("task has an exception " + t, t);
      assertEquals("result of task should be " + msg + " but is " + task.getResult(), msg, task.getResult());
    } finally {
      JPPFConfiguration.set(JPPFProperties.REMOTE_EXECUTION_ENABLED, true);
      Thread.setDefaultUncaughtExceptionHandler(oldEh);
    }
  }

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs
   */
  @Test(timeout=25000)
  public void testMultipleRestartsInSequence() throws Exception {
    final TypedProperties config = BaseSetup.resetClientConfig();
    for (int i=1; i<=20; i++) {
      print(false, false, "<<< start of iteration #%d >>>", i);
      try (JPPFClient client = new JPPFClient(config)) {
        print(false, false, ">>> awaiting connection pool");
        final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
        print(false, false, ">>> awaiting JJMX connection");
        pool.awaitWorkingJMXConnection();
        final List<JPPFJob> jobs = new ArrayList<>();
        print(false, false, ">>> creating jobs");
        for (int j=1; j<=2; j++) {
          final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-" + i + (j == 1 ? "a" : "b"), false, false, 1, LifeCycleTask.class, 0L);
          job.getSLA().setPriority(10 / j);
          job.getJobTasks().get(0).setId("task" + i);
          jobs.add(job);
        }
        print(false, false, ">>> submitting jobs");
        for (JPPFJob job: jobs) client.submitJob(job);
        for (JPPFJob job: jobs) {
          print(false, false, ">>> awaiting results for job %s", job.getName());
          final List<Task<?>> results = job.get(3L, TimeUnit.SECONDS);
          print(false, false, ">>> checking results for job %s", job.getName());
          assertNotNull(results);
          assertEquals(1, results.size());
          final String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
          final Task<?> task = results.get(0);
          final Throwable t = task.getThrowable();
          assertNull("task has an exception " + t, t);
          assertEquals("result of task should be " + msg + " but is " + task.getResult(), msg, task.getResult());
        }
      }
    }
  }

  /**
   * A simple driver discovery implementation.
   */
  private static class SimpleDiscovery extends ClientDriverDiscovery {
    @Override
    public void discover() throws InterruptedException {
      try {
        newConnection(new ClientConnectionPoolInfo("myDriver", false, "localhost", 11101));
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
        System.out.println(ExceptionUtils.getStackTrace(e));
      }
    }
  }

  /** */
  static class MyUncaughtExceptionHandler implements UncaughtExceptionHandler {
    /** */
    Exception uncaught;

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
      e.printStackTrace();
      if (e.getClass() == NullPointerException.class) {
        final StackTraceElement elt = e.getStackTrace()[0];
        if (elt.getClassName().equals(Hashtable.class.getName())) uncaught = (Exception) e;
      }
    }
  }
}
