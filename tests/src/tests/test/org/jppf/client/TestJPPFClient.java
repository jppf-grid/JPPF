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

package test.org.jppf.client;

import static org.jppf.utils.configuration.JPPFProperties.*;
import static org.junit.Assert.*;

import java.io.NotSerializableException;
import java.lang.management.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.execute.AbstractThreadManager;
import org.jppf.job.JobEventType;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.junit.Test;
import org.slf4j.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFClient</code>.
 * @author Laurent Cohen
 */
public class TestJPPFClient extends Setup1D1N {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TestJPPFClient.class);

  /**
   * Invocation of the <code>JPPFClient()</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testDefaultConstructor() throws Exception {
    Exception exception = null;
    try (JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
    } catch(Exception e) {
      exception = e;
      e.printStackTrace();
    }
    if (exception != null) throw exception;
  }

  /**
   * Invocation of the <code>JPPFClient(String uuid)</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testConstructorWithUuid() throws Exception {
    try (JPPFClient client = new JPPFClient("some_uuid")) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
    }
  }

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testSubmit() throws Exception {
    try (JPPFClient client = BaseSetup.createClient(null)) {
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 0L);
      int i = 0;
      for (Task<?> task: job.getJobTasks()) task.setId("" + i++);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      for (i=0; i<nbTasks; i++) {
        Task<?> task = results.get(i);
        Throwable t = task.getThrowable();
        assertNull("task " + i +" has an exception " + t, t);
        assertEquals("result of task " + i + " should be " + msg + " but is " + task.getResult(), msg, task.getResult());
      }
    }
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  @SuppressWarnings("deprecation")
  @Test(timeout=10000)
  public void testCancelJob() throws Exception {
    String name = ReflectionUtils.getCurrentMethodName();
    try (JPPFClient client = BaseSetup.createClient(null)) {
      int nbTasks = 10;
      AwaitJobNotificationListener listener = new AwaitJobNotificationListener(client, JobEventType.JOB_DISPATCHED);
      JPPFJob job = BaseTestHelper.createJob(name + "-1", false, false, nbTasks, LifeCycleTask.class, 5000L);
      client.submitJob(job);
      listener.await();
      client.cancelJob(job.getUuid());
      List<Task<?>> results = job.awaitResults();
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      int count = 0;
      for (Task<?> task: results) {
        if (task.getResult() == null) count++;
      }
      assertTrue(count > 0);
      JPPFJob job2 = BaseTestHelper.createJob(name + "-2", true, false, nbTasks, LifeCycleTask.class, 1L);
      results = client.submitJob(job2);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      for (Task<?> task: results) {
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      }
    }
  }

  /**
   * Test that the number of threads for local execution is the configured one.
   * See bug <a href="http://sourceforge.net/tracker/?func=detail&aid=3539111&group_id=135654&atid=733518">3539111 - Local execution does not use configured number of threads</a>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testLocalExecutionNbThreads() throws Exception {
    int nbThreads = 2;
    JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true).set(LOCAL_EXECUTION_THREADS, nbThreads);
    try (JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      // submit a job to ensure all local execution threads are created
      int nbTasks = 100;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 0L);
      int i = 0;
      for (Task<?> task: job) task.setId("" + i++);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      for (Task<?> task: results) {
        Throwable t = task.getThrowable();
        assertNull(t);
        assertEquals(msg, task.getResult());
      }
      ThreadMXBean mxbean = ManagementFactory.getThreadMXBean();
      long[] ids = mxbean.getAllThreadIds();
      ThreadInfo[] allInfo = mxbean.getThreadInfo(ids);
      int count = 0;
      for (ThreadInfo ti: allInfo) {
        if (ti == null) continue;
        String name = ti.getThreadName();
        if (name == null) continue;
        if (name.startsWith(AbstractThreadManager.THREAD_NAME_PREFIX)) count++;
      }
      assertEquals(nbThreads, count);
    } finally {
      JPPFConfiguration.reset();
    }
  }

  /**
   * Test that the thread context class loader during local execution of a task is not null.
   * See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-174">JPPF-174 Thread context class loader is null for client-local execution</a>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testLocalExecutionContextClassLoader() throws Exception {
    JPPFConfiguration.set(REMOTE_EXECUTION_ENABLED, false).set(LOCAL_EXECUTION_ENABLED, true);
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, ThreadContextClassLoaderTask.class);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      Task<?> task = results.get(0);
      assertEquals(null, task.getThrowable());
      assertNotNull(task.getResult());
    } finally {
      JPPFConfiguration.reset();
    }
  }

  /**
   * Test that the thread context class loader during remote execution of a task is not null, that it matches the task classloader
   * and that both are client class loader.
   * See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-153">JPPF-153 In the node, context class loader and task class loader do not match after first job execution</a>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testRemoteExecutionContextClassLoader() throws Exception {
    JPPFConfiguration.set(REMOTE_EXECUTION_ENABLED, true).set(LOCAL_EXECUTION_ENABLED, false);
    try (JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      String name = ReflectionUtils.getCurrentClassAndMethod();
      client.submitJob(BaseTestHelper.createJob(name + "-1", true, false, 1, ThreadContextClassLoaderTask.class));
      JPPFJob job = BaseTestHelper.createJob(name + "-2", true, false, 1, ThreadContextClassLoaderTask.class);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      Task<?> task = results.get(0);
      assertEquals(null, task.getThrowable());
      assertNotNull(task.getResult());
    } finally {
      JPPFConfiguration.reset();
    }
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when submitting a job to a driver is properly handled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testNotSerializableExceptionFromClient() throws Exception {
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, NotSerializableTask.class, true);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      Task<?> task = results.get(0);
      assertNotNull(task.getThrowable());
      assertTrue(task.getThrowable() instanceof NotSerializableException);
    }
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when a node returns execution results is properly handled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testNotSerializableExceptionFromNode() throws Exception {
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, 1, NotSerializableTask.class, false);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      Task<?> task = results.get(0);
      assertTrue(task instanceof NotSerializableTask);
      Throwable t = task.getThrowable();
      assertNotNull(t);
      assertTrue("wrong exception: " + ExceptionUtils.getStackTrace(t), t instanceof NotSerializableException);
    }
  }

  /**
   * Test that new load-balancing settings are applied as expected.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testChangeLoadBalancerSettings() throws Exception {
    MyJobListener listener = null;
    JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true);
    int nbTasks = 20;
    try (JPPFClient client = new JPPFClient()) {
      client.awaitWorkingConnectionPool();
      // try with "manual" algo
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 0L);
      job.addJobListener(listener = new MyJobListener());
      job.getClientSLA().setMaxChannels(10);
      TypedProperties props = new TypedProperties().setInt("size", 1);
      client.setLoadBalancerSettings("manual", props);
      client.submitJob(job);
      assertEquals(nbTasks, listener.dispatchCount.get());
      assertEquals(nbTasks, listener.tasksPerDispatch.size());
      for (int i=1; i<=listener.tasksPerDispatch.size(); i++) {
        Integer n = listener.tasksPerDispatch.get(i);
        assertNotNull(n);
        assertEquals(1, n.intValue());
      }
      // try with "proportional" algo
      job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "2", true, false, nbTasks, LifeCycleTask.class, 10L);
      job.addJobListener(listener = new MyJobListener());
      job.getClientSLA().setMaxChannels(10);
      props = new TypedProperties().setInt("initialSize", 5).setInt("proportionalityFactor", 1);
      client.setLoadBalancerSettings("proportional", props);
      client.submitJob(job);
      assertTrue(listener.dispatchCount.get() >= 3);
      assertTrue(listener.tasksPerDispatch.size() >= 3);
      for (int i=1; i<=listener.tasksPerDispatch.size(); i++) assertNotNull(listener.tasksPerDispatch.get(i));
      assertEquals(5, listener.tasksPerDispatch.get(1).intValue());
      assertEquals(5, listener.tasksPerDispatch.get(2).intValue());
    } finally {
      JPPFConfiguration.reset();
    }
  }

  /**
   * Test that the last load-balancing settings can be retrieved.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testGetLoadBalancerSettings() throws Exception {
    JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true);
    try (JPPFClient client = new JPPFClient()) {
      client.awaitWorkingConnectionPool();
      // try with "manual" algo
      TypedProperties props = new TypedProperties().setInt("size", 2);
      client.setLoadBalancerSettings("manual", props);
      LoadBalancingInformation lbi = client.getLoadBalancerSettings();
      assertEquals("manual", lbi.getAlgorithm());
      assertEquals(2, lbi.getParameters().getInt("size"));
      // try with "proportional" algo
      props = new TypedProperties().setInt("initialSize", 5).setInt("proportionalityFactor", 1);
      client.setLoadBalancerSettings("proportional", props);
      lbi = client.getLoadBalancerSettings();
      assertEquals("proportional", lbi.getAlgorithm());
      assertEquals(5, lbi.getParameters().getInt("initialSize"));
      assertEquals(1, lbi.getParameters().getInt("proportionalityFactor"));
    } finally {
      JPPFConfiguration.reset();
    }
  }

  /**
   * A task that checks the current thread context class loader during its execution.
   */
  public static class ThreadContextClassLoaderTask extends AbstractTask<String> {
    @Override
    public void run() {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) throw new IllegalStateException("thread context class loader is null for " + (isInNode() ? "remote" : "local")  + " execution");
      if (isInNode()) {
        if (!(cl instanceof AbstractJPPFClassLoader)) throw new IllegalStateException("thread context class loader for remote execution should be an AbstractJPPFClassLoader, but is " + cl);
        AbstractJPPFClassLoader ajcl2 = (AbstractJPPFClassLoader) getTaskClassLoader();
        if (cl != ajcl2) {
          throw new IllegalStateException("thread context class loader and task class loader do not match:\n" +
            "thread context class loader = " + cl + "\n" +
            "task class loader = " + ajcl2);
        }
        if (!ajcl2.isClientClassLoader()) throw new IllegalStateException("class loader is not a client class loader:" + ajcl2);
      }
      setResult(cl.toString());
    }
  }

  /** */
  static class MyJobListener extends JobListenerAdapter {
    /** */
    AtomicInteger dispatchCount = new AtomicInteger(0);
    /** */
    ConcurrentHashMap<Integer, Integer> tasksPerDispatch = new ConcurrentHashMap<>();

    @Override
    public void jobDispatched(final JobEvent event) {
      int n = dispatchCount.incrementAndGet();
      tasksPerDispatch.put(n, event.getJobTasks().size());
    }
  }
}
