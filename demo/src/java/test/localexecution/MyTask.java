/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
package test.localexecution;

import java.net.URL;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.collections.CollectionUtils;

/**
 * Test task.
 * @author Laurent Cohen
 */
public class MyTask extends JPPFTask
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Path to some jar on the client side.
   */
  private static final String JAR_PATH = "../samples-pack/shared/lib/hazelcast-1.9.3.jar";
  /**
   * Path to some jar on the client side.
   */
  private static final String[] JAR_PATHS = { "../samples-pack/shared/lib/hazelcast-1.9.3.jar", "../samples-pack/shared/lib/jaligner.jar", "../samples-pack/shared/lib/js.jar", "lib/jppf-common-node.jar" };
  /**
   * To determine if we must load the jars or not.
   */
  private static boolean initialized = false;

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    try
    {
      System.out.println("starting task");
      AbstractJPPFClassLoader cl = (AbstractJPPFClassLoader) getClass().getClassLoader();
      if (!initialized) loadJars(cl);
      Class c = cl.loadClass("com.hazelcast.core.Hazelcast");
      System.out.println("found class " + c);
      c = cl.loadClass("jaligner.Sequence");
      System.out.println("found class " + c);
      c = cl.loadClass("org.mozilla.javascript.Evaluator");
      System.out.println("found class " + c);
      setResult("ok");
    }
    catch (Exception e)
    {
      e.printStackTrace();
      setThrowable(e);
    }
  }

  /**
   * Load the required jars and add them to the classpath.
   * @param cl the class loader to use to load the jars.
   * @throws Exception if any error occurs.
   */
  private static synchronized void loadJars(final AbstractJPPFClassLoader cl) throws Exception
  {
    if (initialized) return;
    initialized = true;
    System.out.println("loading jars");
    /*
		URL url = cl.getResource(JAR_PATH);
		System.out.println("got URL: " + url);
		if (url != null) cl.addURL(url);
     */
    URL[] urls = cl.getMultipleResources(JAR_PATHS);
    System.out.println("got URLs: " + CollectionUtils.list(urls));
    for (URL url: urls) if (url != null) cl.addURL(url);
  }
}
