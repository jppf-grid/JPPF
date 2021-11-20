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
import org.jppf.client.balancer.JobManagerClient;
import org.jppf.client.event.*;
import org.jppf.execute.AbstractThreadManager;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.junit.*;

import test.org.jppf.test.runner.IgnoreForEmbeddedGrid;
import test.org.jppf.test.setup.Setup1D1N;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@code JPPFClient}.
 * @author Laurent Cohen
 */
public class TestJPPFClient extends Setup1D1N {
  /**
   * Setup.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void classSetup() throws Exception {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> print(false, false, "Uncaught exception in thread %s%n%s", t, ExceptionUtils.getStackTrace(e)));
  }

  /**
   * Cleanup after each test.
   * @throws Exception if any error occurs.
   */
  @Before
  public void testJPPFClientCleanup()  throws Exception {
    final String[] suffixes = { "TaskQUeueChecker", "JPPF Client-", "driver1" };
    final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
    final long[] ids = mbean.getAllThreadIds();
    final long start = System.currentTimeMillis();
    boolean ok = false;
    while (!ok && (System.currentTimeMillis() -start < 5_000L)) {
      for (final long id: ids) {
        final ThreadInfo info = mbean.getThreadInfo(id);
        if ((info == null) || (info.getThreadState() == Thread.State.TERMINATED)) continue;
        if (StringUtils.startsWithOneOf(info.getThreadName(), false, suffixes)) {
          Thread.sleep(100L);
          break;
        }
      }
      ok = true;
    }
    if (!ok) throw new IllegalStateException("some JPPF threads are still alive");
  }
 
  /**
   * Invocation of the <code>JPPFClient()</code> constructor.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testDefaultConstructor() throws Exception {
    Exception exception = null;
    try (JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
    } catch(final Exception e) {
      exception = e;
      e.printStackTrace();
    }
    if (exception != null) throw exception;
  }

  /**
   * Invocation of the <code>JPPFClient(String uuid)</code> constructor.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testConstructorWithUuid() throws Exception {
    try (JPPFClient client = new JPPFClient("some_uuid")) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
    }
  }

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testSubmit() throws Exception {
    CommonClientTests.testSubmit(JPPFConfiguration.getProperties());
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testCancelJob() throws Exception {
    CommonClientTests.testCancelJob(JPPFConfiguration.getProperties());
  }

  /**
   * Test that the number of threads for local execution is the configured one.
   * See bug <a href="http://sourceforge.net/tracker/?func=detail&aid=3539111&group_id=135654&atid=733518">3539111 - Local execution does not use configured number of threads</a>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testLocalExecutionNbThreads() throws Exception {
    final int nbThreads = 2;
    JPPFConfiguration.set(LOCAL_EXECUTION_ENABLED, true).set(LOCAL_EXECUTION_THREADS, nbThreads).set(REMOTE_EXECUTION_ENABLED, false);
    try (JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      // submit a job to ensure all local execution threads are created
      final int nbTasks = 100;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 0L);
      int i = 0;
      for (final Task<?> task: job) task.setId("" + i++);
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      final String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
      for (final Task<?> task: results) {
        final Throwable t = task.getThrowable();
        if (t != null) {
          final String errorMsg = String.format("task result has an error:\n%s", ExceptionUtils.getStackTrace(t));
          print(false, false, errorMsg);
          fail(errorMsg);
        }
        assertEquals(msg, task.getResult());
      }
      final ThreadMXBean mxbean = ManagementFactory.getThreadMXBean();
      final long[] ids = mxbean.getAllThreadIds();
      final ThreadInfo[] allInfo = mxbean.getThreadInfo(ids);
      int count = 0;
      for (final ThreadInfo ti: allInfo) {
        if (ti == null) continue;
        final String name = ti.getThreadName();
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
    try (final JPPFClient client = new JPPFClient()) {
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, 1, ThreadContextClassLoaderTask.class);
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      final Task<?> task = results.get(0);
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
  @IgnoreForEmbeddedGrid
  public void testRemoteExecutionContextClassLoader() throws Exception {
    JPPFConfiguration.set(REMOTE_EXECUTION_ENABLED, true).set(LOCAL_EXECUTION_ENABLED, false);
    try (final JPPFClient client = new JPPFClient()) {
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      final String name = ReflectionUtils.getCurrentClassAndMethod();
      client.submit(BaseTestHelper.createJob(name + "-1", false, 1, ThreadContextClassLoaderTask.class));
      final JPPFJob job = BaseTestHelper.createJob(name + "-2", false, 1, ThreadContextClassLoaderTask.class);
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      final Task<?> task = results.get(0);
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
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, 1, NotSerializableTask.class, true);
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      final Task<?> task = results.get(0);
      final Throwable t = task.getThrowable();
      assertNotNull(t);
      print(false, false, "throwable is: %s", ExceptionUtils.getStackTrace(t));
      assertTrue(String.format("throwable = %s", t), t instanceof NotSerializableException);
    }
  }

  /**
   * Test that a {@link java.io.NotSerializableException} occurring when a node returns execution results is properly handled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testNotSerializableExceptionFromNode() throws Exception {
    try (final JPPFClient client = new JPPFClient()) {
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, 1, NotSerializableTask.class, false);
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(1, results.size());
      final Task<?> task = results.get(0);
      assertTrue(task instanceof NotSerializableTask);
      final Throwable t = task.getThrowable();
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
    final int nbTasks = 20;
    try (JPPFClient client = new JPPFClient()) {
      client.awaitWorkingConnectionPool();
      // try with "manual" algo
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 0L);
      job.addJobListener(listener = new MyJobListener());
      job.getClientSLA().setMaxChannels(10);
      TypedProperties props = new TypedProperties().setInt("size", 1);
      client.setLoadBalancerSettings("manual", props);
      client.submit(job);
      assertEquals(nbTasks, listener.dispatchCount.get());
      assertEquals(nbTasks, listener.tasksPerDispatch.size());
      for (int i=1; i<=listener.tasksPerDispatch.size(); i++) {
        final Integer n = listener.tasksPerDispatch.get(i);
        assertNotNull(n);
        assertEquals(1, n.intValue());
      }
      // try with "proportional" algo
      job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "2", false, nbTasks, LifeCycleTask.class, 10L);
      job.addJobListener(listener = new MyJobListener());
      job.getClientSLA().setMaxChannels(10);
      final JobManagerClient jmc = (JobManagerClient) client.getJobManager();
      while (jmc.nbAvailableConnections() < 2) Thread.sleep(10L);
      props = new TypedProperties().setInt("initialSize", 5).setInt("proportionalityFactor", 1);
      client.setLoadBalancerSettings("proportional", props);
      client.submit(job);
      assertTrue("expected at least <3> but got <" + listener.dispatchCount.get() + ">", listener.dispatchCount.get() >= 3);
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
   * Test that the client recovers from disconnecting from the driver and finishes jobs to completion.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15_000)
  public void testClientClose() throws Exception {
    final TypedProperties config = new TypedProperties(JPPFConfiguration.getProperties())
      .set(LOAD_BALANCING_ALGORITHM, "manual")
      .set(LOAD_BALANCING_PROFILE, "test")
      .setInt(LOAD_BALANCING_PROFILE.getName() + ".test.size", 1);
    try (final MyClient client = new MyClient(config)) {
      final int nbJobs = 5;
      final int nbTasks = 3;
      for (int i = 1; i<=nbJobs; i++) {
        final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "-" + i, false, nbTasks, LifeCycleTask.class, 0L);
        print(false, false, "----- iteration #%03d -----", i);
        int count = 0;
        for (final Task<?> task: job.getJobTasks()) task.setId("" + count++);
        final AwaitJobListener listener = AwaitJobListener.of(job, JobEvent.Type.JOB_RETURN);
        print(false, false, "submitting job");
        client.submitAsync(job);
        print(false, false, "awaiting first result");
        listener.await();
        print(false, false, "resetting client");
        final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
        pool.close();
        print(false, false, "client queue: %s", client.getQueuedJobs());
        ThreadUtils.startDaemonThread(() -> client.initPools(config), "InitPools");
        print(false, false, "awaiting job results");
        final List<Task<?>> results = job.awaitResults();
        print(false, false, "got job results");
        assertNotNull(results);
        assertEquals(nbTasks, results.size());
        final String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
        for (final Task<?> task: results) {
          final Throwable t = task.getThrowable();
          assertNull("task " + task.getId() + " has an exception " + t, t);
          assertEquals("result of task " + task.getId() + " should be " + msg + " but is " + task.getResult(), msg, task.getResult());
        }
      }
    }
  }

  /**
   * A task that checks the current thread context class loader during its execution.
   */
  public static class ThreadContextClassLoaderTask extends AbstractTask<String> {
    /** Explicit serialVersionUID. */
    private static final long serialVersionUID = 1L;

    @Override
    public void run() {
      final ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) throw new IllegalStateException("thread context class loader is null for " + (isInNode() ? "remote" : "local")  + " execution");
      if (isInNode()) {
        if (!(cl instanceof AbstractJPPFClassLoader)) throw new IllegalStateException("thread context class loader for remote execution should be an AbstractJPPFClassLoader, but is " + cl);
        final AbstractJPPFClassLoader ajcl2 = (AbstractJPPFClassLoader) getTaskClassLoader();
        if (cl != ajcl2) throw new IllegalStateException("thread context class loader and task class loader do not match:\n" + "thread context class loader = " + cl + "\ntask class loader = " + ajcl2);
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
      final int n = dispatchCount.incrementAndGet();
      tasksPerDispatch.put(n, event.getJobTasks().size());
    }
  }

  /**
   * Subclass to make the {@code initPools()} method public.
   */
  public static class MyClient extends JPPFClient {
    /**
     * @param config .
     * @param listeners .
     */
    public MyClient(final TypedProperties config, final ConnectionPoolListener... listeners) {
      super(config, listeners);
    }

    @Override
    public void initPools(final TypedProperties config) {
      super.initPools(config);
    }
  }
}
