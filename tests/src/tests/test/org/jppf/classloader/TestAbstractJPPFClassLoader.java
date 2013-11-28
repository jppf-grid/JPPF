/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.net.URL;
import java.util.*;

import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D1N1C;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Unit tests for {@link org.jppf.classloader.AbstractJPPFClassLoader}.
 * @author Laurent Cohen
 */
public class TestAbstractJPPFClassLoader extends Setup1D1N1C
{
  /**
   * Test that no exception is raised upon calling AbstractJPPFClassloader.getResources() from 2 jobs in sequence.
   * <br/>See <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-116">JPPF-116 NPE in AbstractJPPFClassLoader.findResources()</a>
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testGetResources() throws Exception
  {
    String name = ReflectionUtils.getCurrentMethodName();
    String resource = "some_dummy_resource-" + JPPFUuid.normalUUID() + ".dfg";
    List<Task<?>> results = client.submitJob(BaseTestHelper.createJob(name + "1", true, false, 1, MyTask.class, resource));
    results = client.submitJob(BaseTestHelper.createJob(name + "2", true, false, 1, MyTask.class, resource));
    assertNotNull(results);
    assertEquals(1, results.size());
    Task<?> task = results.get(0);
    assertNotNull(task);
    assertNull(task.getThrowable());
    assertEquals("success", task.getResult());
  }

  /**
   * Test that multiple lookups of the same resource do not generate duplicate entries in the node's resource cache.<br/>
   * See <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-203">JPPF-203 Class loader resource cache generates duplicate resources</a>
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testGetResourcesNoDuplicate() throws Exception
  {
    int nbLookups = 3;
    String name = ReflectionUtils.getCurrentMethodName();
    String resource = "some_dummy_resource-" + JPPFUuid.normalUUID() + ".dfg";
    List<Task<?>> results = client.submitJob(BaseTestHelper.createJob(name, true, false, 1, ResourceLoadingTask.class, nbLookups));
    assertNotNull(results);
    assertEquals(1, results.size());
    Task<?> task = results.get(0);
    assertNotNull(task);
    Throwable t = task.getThrowable();
    assertNull(t == null ? "" : "got exception: " + ExceptionUtils.getStackTrace(t), t);
    Object o = task.getResult();
    assertNotNull(o);
    @SuppressWarnings("unchecked")
    List<List<URL>> list = (List<List<URL>>) o;
    assertEquals(nbLookups, list.size());
    URL firstURL = null;
    for (int i=0; i<nbLookups; i++) {
      List<URL> sublist = list.get(i);
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
  @Test(timeout=5000)
  public void testClassLoadersMatch() throws Exception
  {
    String name = ReflectionUtils.getCurrentMethodName();
    String resource = "some_dummy_resource-" + JPPFUuid.normalUUID() + ".dfg";
    List<Task<?>> results = client.submitJob(BaseTestHelper.createJob(name + "1", true, false, 1, MyTask.class, resource));
    results = client.submitJob(BaseTestHelper.createJob(name + "2", true, false, 1, MyTask.class, resource));
    assertNotNull(results);
    assertEquals(1, results.size());
    MyTask task = (MyTask) results.get(0);
    assertNotNull(task);
    assertNull(task.getThrowable());
    assertTrue(task.isClassLoaderMatch());
    assertNotNull(task.getContextClassLoaderStr());
    assertNotNull(task.getTaskClassLoaderStr());
    assertEquals(task.getContextClassLoaderStr(),task.getTaskClassLoaderStr()); 
  }

  /**
   * 
   */
  public static class MyTask extends JPPFTask {
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
        ClassLoader cl1 = Thread.currentThread().getContextClassLoader();
        contextClassLoaderStr = cl1 == null ? "null" : cl1.toString();
        ClassLoader cl2 = getClass().getClassLoader();
        taskClassLoaderStr = cl2 == null ? "null" : cl2.toString();
        classLoaderMatch = cl1 == cl2;
        Enumeration<URL> res = getClass().getClassLoader().getResources(resource);
        setResult("success");
      } catch(Exception e) {
        setThrowable(e);
      }
    }

    /**
     * Get the outcome of <code>Thread.currentThread().getContextClassLoader().toString()</code>.
     * @return a string describing the class loader.
     */
    public String getContextClassLoaderStr()
    {
      return contextClassLoaderStr;
    }

    /**
     * Get the outcome of <code>this.getClass(().getClassLoader().toString()</code>.
     * @return a string describing the class loader.
     */
    public String getTaskClassLoaderStr()
    {
      return taskClassLoaderStr;
    }

    /**
     * Determine whether both class loaders are identical.
     * @return <code>true</code> if the class loaders match, <code>false</code> otherwise.
     */
    public boolean isClassLoaderMatch()
    {
      return classLoaderMatch;
    }
  }
}
