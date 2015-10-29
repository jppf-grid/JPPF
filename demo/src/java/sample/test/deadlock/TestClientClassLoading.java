/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package sample.test.deadlock;

import java.io.File;
import java.net.*;

/**
 * This class is run without "jppf-client.jar" in the classpath.
 * @author Laurent Cohen
 */
public class TestClientClassLoading {
  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      //URL[] urls = { new File("jppf-client.jar").toURI().toURL() };
      URL[] urls = { new File("../client/classes").toURI().toURL() };
      URLClassLoader loader = new URLClassLoader(urls, TestClientClassLoading.class.getClassLoader());
      Thread.currentThread().setContextClassLoader(loader);
      Class<?> c = Class.forName("org.jppf.client.JPPFClient", true, loader);
      c.newInstance();
      System.out.println("done");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
