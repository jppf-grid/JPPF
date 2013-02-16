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
    List<JPPFTask> results = client.submit(BaseTestHelper.createJob(name + "1", true, false, 1, MyTask.class, resource));
    results = client.submit(BaseTestHelper.createJob(name + "2", true, false, 1, MyTask.class, resource));
    assertNotNull(results);
    assertEquals(1, results.size());
    JPPFTask task = results.get(0);
    assertNotNull(task);
    assertNull(task.getException());
    assertEquals("success", task.getResult());
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
     * Initiialize with a resource name that doesn't exist in the classpath.
     * @param resource the resource name.
     */
    public MyTask(final String resource) {
      this.resource = resource;
    }

    @Override
    public void run() {
      try {
        Enumeration<URL> res = getClass().getClassLoader().getResources(resource);
        setResult("success");
      } catch(Exception e) {
        setException(e);
      }
    }
  }
}
