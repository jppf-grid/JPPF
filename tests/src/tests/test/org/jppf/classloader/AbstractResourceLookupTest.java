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

import java.net.URL;
import java.util.List;

import org.jppf.node.protocol.*;
import org.jppf.utils.ReflectionUtils;
import org.junit.Test;

import test.org.jppf.test.setup.AbstractNonStandardSetup;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Unit tests for the disabling of resources lookup in the file system.
 * @author Laurent Cohen
 */
public abstract class AbstractResourceLookupTest extends AbstractNonStandardSetup {
  /**
   * Test timeout.
   */
  private static final long TEST_TIMEOUT = 20_000L;

  /**
   * Test a simple job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=TEST_TIMEOUT)
  public void testResourcesLookup() throws Exception {
    final URL url = getClass().getClassLoader().getResource("client-resource-1.txt");
    print(false, false, "local url = %s", url);
    final String name = ReflectionUtils.getCurrentMethodName();
    final List<Task<?>> results = client.submit(BaseTestHelper.createJob(name, false, 1, MyTask.class));
    assertNotNull(results);
    assertEquals(1, results.size());
    assertTrue(results.get(0) instanceof MyTask);
    final MyTask task = (MyTask) results.get(0);
    assertNull(task.getThrowable());
    assertEquals("success", task.getResult());
    assertNotNull(task.driverResource1);
    assertNotNull(task.clientResource1);
    if (task.localNode) {
      assertNotNull(task.driverResource2);
      assertNotNull(task.clientResource2);
    } else {
      assertNull(task.driverResource2);
      assertNull(task.clientResource2);
    }
  }

  /** */
  public static class MyTask extends AbstractTask<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    public String driverResource1, driverResource2, clientResource1, clientResource2;
    /** */
    public boolean localNode;

    @Override
    public void run() {
      localNode = getNode().isLocal();
      driverResource1 = lookup("driver-resource-1.txt"); // in classpath
      driverResource2 = lookup("test-resources/driver1/driver-resource-1.txt"); // in file system only
      clientResource1 = lookup("client-resource-1.txt"); // in classpath
      clientResource2 = lookup("test-resources/client1/client-resource-1.txt"); // in file system only
      setResult("success");
    }

    /**
     * Looup the specified resource.
     * @param resName the name of the resource to lookup.
     * @return the url of the resource as a string, or {@code null} if the reosurce was not found.
     */
    private String lookup(final String resName) {
      final URL url = getClass().getClassLoader().getResource(resName);
      return (url == null) ? null : url.toString();
    }
  }
}
