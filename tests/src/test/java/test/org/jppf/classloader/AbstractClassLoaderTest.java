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

package test.org.jppf.classloader;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.client.*;
import org.jppf.client.taskwrapper.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.junit.*;

import test.org.jppf.test.runner.IgnoreForEmbeddedGrid;
import test.org.jppf.test.setup.AbstractNonStandardSetup;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Unit tests for {@link org.jppf.classloader.AbstractJPPFClassLoader}.
 * @author Laurent Cohen
 */
@IgnoreForEmbeddedGrid
public abstract class AbstractClassLoaderTest extends AbstractNonStandardSetup {
  /**
   * Test timeout.
   */
  private static final long TEST_TIMEOUT = 15_000L;

  /**
   * Launches a driver and 1 node and start the client.
   * @throws Exception if a process could not be started.
   */
  @AfterClass
  public static void displayStats() throws Exception {
    print(false, false, "classpath cache stats: %s", ClasspathCache.getInstance());
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11101, false)) {
      assertTrue(jmx.connectAndWait(5_000L));
      print(false, false, "***** class loading statistics *****");
      final JPPFStatistics stats = jmx.statistics();
      final JPPFSnapshot[] snapshots = { stats.getSnapshot(JPPFStatisticsHelper.NODE_CLASS_REQUESTS_TIME), stats.getSnapshot(JPPFStatisticsHelper.CLIENT_CLASS_REQUESTS_TIME)};
      for (final JPPFSnapshot snapshot: snapshots) {
        print(false, false, "%-26s: avg time = %,.3f ms; count: %,d; total time: %,.3f ms", snapshot.getLabel(), snapshot.getAvg(), snapshot.getValueCount(), snapshot.getTotal());
      }
    }
  }

  /**
   * Test that no exception is raised upon calling AbstractJPPFClassloader.getResources() from 2 jobs in sequence.
   * <br/>See <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-116">JPPF-116 NPE in AbstractJPPFClassLoader.findResources()</a>
   * @throws Exception if any error occurs
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGetResources() throws Exception {
    final String name = ReflectionUtils.getCurrentMethodName();
    final String resource = "some_dummy_resource-" + JPPFUuid.normalUUID() + ".dfg";
    List<Task<?>> results = client.submit(BaseTestHelper.createJob(name + "1", false, 1, MyTask.class, resource));
    final JPPFJob job = BaseTestHelper.createJob(name + "2", false, 1, MyTask.class, resource);
    print(false, false, "submitting job %s", job.getName());
    results = client.submit(job);
    print(false, false, "got results");
    assertNotNull(results);
    assertEquals(1, results.size());
    final Task<?> task = results.get(0);
    assertNotNull(task);
    assertNull(task.getThrowable());
    assertEquals("success", task.getResult());
    print(false, false, "test done");
  }

  /**
   * Test that multiple lookups of the same resource do not generate duplicate entries in the node's resource cache.<br/>
   * See <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-203">JPPF-203 Class loader resource cache generates duplicate resources</a>
   * @throws Exception if any error occurs
   */
  //@Test(timeout = TEST_TIMEOUT)
  public void testGetResourcesNoDuplicate() throws Exception {
    final int nbLookups = 3;
    final String name = ReflectionUtils.getCurrentMethodName();
    final List<Task<?>> results = client.submit(BaseTestHelper.createJob(name, false, 1, ResourceLoadingTask.class, nbLookups));
    assertNotNull(results);
    assertEquals(1, results.size());
    final Task<?> task = results.get(0);
    assertNotNull(task);
    final Throwable t = task.getThrowable();
    assertNull(t == null ? "" : "got exception: " + ExceptionUtils.getStackTrace(t), t);
    final Object o = task.getResult();
    assertNotNull(o);
    @SuppressWarnings("unchecked")
    final List<List<URL>> list = (List<List<URL>>) o;
    assertEquals(nbLookups, list.size());
    URL firstURL = null;
    for (int i = 0; i < nbLookups; i++) {
      final List<URL> sublist = list.get(i);
      assertNotNull(sublist);
      assertEquals(1, sublist.size());
      if (i == 0) firstURL = sublist.get(0);
      else assertEquals(firstURL, sublist.get(0));
    }
  }

  /**
   * Test that at task startup in the node, the thread context class loader and the task class loader re the same.
   * <br/>See <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-153">JPPF-153 In the node, context class loader and task class loader do not match after first job execution)</a>
   * @throws Exception if any error occurs
   */
  //@Test(timeout = TEST_TIMEOUT)
  public void testClassLoadersMatch() throws Exception {
    final String name = ReflectionUtils.getCurrentMethodName();
    final String resource = "some_dummy_resource-" + JPPFUuid.normalUUID() + ".dfg";
    List<Task<?>> results = client.submit(BaseTestHelper.createJob(name + "1", false, 1, MyTask.class, resource));
    results = client.submit(BaseTestHelper.createJob(name + "2", false, 1, MyTask.class, resource));
    assertNotNull(results);
    assertEquals(1, results.size());
    final MyTask task = (MyTask) results.get(0);
    assertNotNull(task);
    assertNull(task.getThrowable());
    assertTrue(task.isClassLoaderMatch());
    assertNotNull(task.getContextClassLoaderStr());
    assertNotNull(task.getTaskClassLoaderStr());
    assertEquals(task.getContextClassLoaderStr(), task.getTaskClassLoaderStr());
  }

  /**
   * Test that class loading is not interrupted when a job is cancelled before class loading is over. 
   * Here we test that class loading is not interrupted even with static initializers that last longer
   * than the job expiration timeout. 
   * @throws Exception if any error occurs
   */
  //@Test(timeout=TEST_TIMEOUT)
  public void testClassLoadingInterruptionWithTask() throws Exception {
    testInterruption(false);
  }

  /**
   * Test that class loading is not interrupted when a job is cancelled before class loading is over.
   * Here we test that class loading is not interrupted even when it involves a large number of classes
   * with a total loading time larger than the job expiration timeout. 
   * @throws Exception if any error occurs
   */
  //@Test(timeout=TEST_TIMEOUT)
  public void testClassLoadingInterruptionWithCallable() throws Exception {
    testInterruption(true);
  }

  /**
   * Test that class loading works from a thread created by a JPPF task.
   * <br/>See <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-606">JPPF-606 ClassNotFoundException when submitting a Callable to an ExecutorService from a JPPF task</a>
   * @throws Exception if any error occurs
   */
  //@Test(timeout = TEST_TIMEOUT)
  public void testClassLoadingFromSeparateThread() throws Exception {
    final String name = ReflectionUtils.getCurrentMethodName();
    final List<Task<?>> results = client.submit(BaseTestHelper.createJob(name + "1", false, 1, MyTestTask.class));
    assertNotNull(results);
    assertEquals(1, results.size());
    final Task<?> task = results.get(0);
    assertNotNull(task);
    final Throwable throwable = task.getThrowable();
    if (throwable != null) {
      print(false, false, "task raised exception:\n%s", ExceptionUtils.getStackTrace(throwable));
      fail("task raised exception: " + ExceptionUtils.getMessage(throwable));
    }
    assertNotNull(task.getResult());
    assertEquals("hello from task", task.getResult());
  }

  /**
   * @param callable .
   * @throws Exception if any error occurs
   */
  private static void testInterruption(final boolean callable) throws Exception {
    final String name = ReflectionUtils.getCurrentMethodName() + "(" + (callable ? "callable" : "task") + " %d)";
    for (int i=1; i<=10; i++) {
      final JPPFJob job = new JPPFJob();
      job.setName(String.format(name, i));
      if (callable) {
        final JPPFAnnotatedTask task = (JPPFAnnotatedTask) job.add(new MyCallable(i));
        final int n = i;
        task.setCancelCallback(new JPPFTaskCallback<Object>() {
          @Override
          public void run() {
            System.out.printf("callable task %d cancelled%n", n);
          }
        });
      }
      else job.add(new MyTask2(i));
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(1000L));
      final Task<?> task = client.submit(job).get(0);
      print(false, false, "got results for job %d", i);
      final Throwable t = task.getThrowable();
      assertNull(String.format("got exception in task %d: %s", i, ExceptionUtils.getStackTrace(t)), t);
      if (i == 1) {
        assertNull(task.getResult());
        assertTrue(job.isCancelled());
      } else if (i == 10) {
        assertNotNull(task.getResult());
        assertEquals(task.getResult(), "result of job " + i);
        assertFalse(job.isCancelled());
      }
    }
  }

  /**
   * 
   */
  public static class MyTask extends AbstractTask<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Name of a resource to lookup.
     */
    private final String resource;
    /**
     * The outcome of <code>Thread.currentThread().getContextClassLoader().toString()</code>.
     */
    private String contextClassLoaderStr = null;
    /**
     * The outcome of <code>this.getClass(().getClassLoader().toString()</code>.
     */
    private String taskClassLoaderStr = null;
    /**
     * Determines whether both class loaders are identical.
     */
    private boolean classLoaderMatch = false;

    /**
     * Initiialize with a resource name that doesn't exist in the classpath.
     * @param resource the resource name.
     */
    public MyTask(final String resource) {
      this.resource = resource;
    }

    @Override
    public void run() {
      try {
        final ClassLoader cl1 = Thread.currentThread().getContextClassLoader();
        contextClassLoaderStr = cl1 == null ? "null" : cl1.toString();
        final ClassLoader cl2 = getClass().getClassLoader();
        taskClassLoaderStr = cl2 == null ? "null" : cl2.toString();
        classLoaderMatch = cl1 == cl2;
        getClass().getClassLoader().getResources(resource);
        setResult("success");
      } catch (final Exception e) {
        setThrowable(e);
      }
    }

    /**
     * Get the outcome of <code>Thread.currentThread().getContextClassLoader().toString()</code>.
     * @return a string describing the class loader.
     */
    public String getContextClassLoaderStr() {
      return contextClassLoaderStr;
    }

    /**
     * Get the outcome of <code>this.getClass(().getClassLoader().toString()</code>.
     * @return a string describing the class loader.
     */
    public String getTaskClassLoaderStr() {
      return taskClassLoaderStr;
    }

    /**
     * Determine whether both class loaders are identical.
     * @return <code>true</code> if the class loaders match, <code>false</code> otherwise.
     */
    public boolean isClassLoaderMatch() {
      return classLoaderMatch;
    }
  }

  /** */
  public static class MyCallable implements Callable<String>, Serializable, Interruptibility {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    private final int index;

    /**
     * @param index .
     */
    public MyCallable(final int index) {
      this.index = index;
    }

    @Override
    public String call() throws Exception {
      final long start = System.nanoTime();
      try {
        // scala-library.jar and scala-reflect.jar must be in the client's classpath
        Class.forName("scala.Predef$");
        return "result of job " + index;
      } finally {
        System.out.printf("[%s] job %d time=%,d ms%n", getClass().getSimpleName(), index, (System.nanoTime() - start) / 1_000_000L);
      }
    }

    @Override
    public boolean isInterruptible() {
      return false;
    }
  }

  /** */
  public static class MyTask2 extends AbstractTask<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    private final int index;

    /**
     * @param index .
     */
    public MyTask2(final int index) {
      this.index = index;
    }

    @Override
    public void run() {
      final long start = System.nanoTime();
      try {
        new Test1();
        new Test2();
        new Test3();
        setResult("result of job " + index);
      } catch(final Error e) {
        System.out.printf("[%s] job %d has exception: %s%n", getClass().getSimpleName(), index, ExceptionUtils.getStackTrace(e));
        throw e;
      } finally {
        System.out.printf("[%s] job %d time=%,d ms%n", getClass().getSimpleName(), index, (System.nanoTime() - start) / 1_000_000L);
      }
    }

    @Override
    public boolean isInterruptible() {
      return false;
    }

    @Override
    public void onCancel() {
      System.out.printf("[%s] job %d cancelled%n", getClass().getSimpleName(), index);
    }

    @Override
    public void onTimeout() {
      System.out.printf("[%s] job %d timed out%n", getClass().getSimpleName(), index);
    }
  }

  /** */
  public static class Initialization {
    /** */
    public static final long WAIT_TIME = 400L;
    /** */
    public static final boolean RETHROW = true;

    /**
     * @param simpleName the simple name (without namespace) of the class being initialized.
     */
    public static void init(final String simpleName) {
      try {
        System.out.println("initializing " + simpleName + ".class");
        Thread.sleep(WAIT_TIME);
      } catch (final Exception e) {
        System.out.println(ExceptionUtils.getStackTrace(e));
        if (RETHROW) throw new RuntimeException(e);
      }
    }
  }

  /** */
  public static class Test1 {
    static { Initialization.init("Test1"); }
  }

  /** */
  public static class Test2 {
    static { Initialization.init("Test2"); }
  }

  /** */
  public static class Test3 {
    static { Initialization.init("Test3"); }
  }
}
