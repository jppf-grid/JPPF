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

package sample.test.jppfcallable;

import java.net.*;
import java.util.Stack;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;


/**
 * 
 */
public class MyTask extends JPPFTask {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MyTask.class);
  /**
   * Duration of the callable.
   */
  private final long time;
  /**
   * Uuid of the client that submitted this task.
   */
  private  final String clientUuid;

  /**
   * Initialize this task with the specified callable duration.
   * @param time the duration of the callable.
   * @param clientUuid the uuid of the client that submitted this task.
   */
  public MyTask(final long time, final String clientUuid) {
    this.time = time;
    this.clientUuid = clientUuid;
  }

  @Override
  public void run() {
    try {
      print("-----------------------------------------------------------------------------------------------------------------------------");
      print("starting task '" + getId() + "' from client uuid = " + clientUuid);
      MyCallable mc = new MyCallable(getId(), time);
      print("context class loader  = " + toString(Thread.currentThread().getContextClassLoader()));
      print("task class loader     = " + toString(getClass().getClassLoader()));
      print("callable class loader = " + toString(mc.getClass().getClassLoader()));
      String s = compute(mc);
      setResult(s);
      print("result of MyCallable[id=" + getId() + "].call() = " + s);
    } catch (Throwable t) {
      setThrowable(t);
      log.error("error in task " + getId(), t);
    }
  }

  /**
   * Print a top-down representation of a class loader hierarchy into a string.
   * @param leafClassLoader the class loader at the bottom of the hierarchy.
   * @return a string representation of the class loader hierarchy.
   */
  private String printCLHierarchy(final ClassLoader leafClassLoader) {
    StringBuilder sb = new StringBuilder();
    ClassLoader cl = leafClassLoader;
    Stack<String> stack = new Stack<>();
    while (cl != null) {
      if (cl instanceof AbstractJPPFClassLoader) stack.push(cl.toString());
      else if (cl instanceof URLClassLoader) stack.push(toString((URLClassLoader) cl));
      else  stack.push(cl.toString());
      cl = cl.getParent();
    }
    int count = 0;
    while (!stack.isEmpty()) {
      for (int i=0; i<2*count; i++) sb.append(' ');
      sb.append(stack.pop());
      if (!stack.isEmpty()) sb.append('\n');
      count++;
    }
    return sb.toString();
  }

  /**
   * Print a representation of a <code>URLClassLoader</code> into a string.
   * The resulting string includes the class loader's classpath.
   * @param cl  the classloader to print.
   * @return a string representation of the input class loader.
   */
  private String toString(final ClassLoader cl) {
    StringBuilder sb = new StringBuilder();
    if (cl instanceof AbstractJPPFClassLoader) sb.append(cl.toString());
    else if (cl instanceof URLClassLoader) {
      sb.append(cl.getClass().getSimpleName()).append("[classpath=");
      URL[] urls = ((URLClassLoader) cl).getURLs();
      if ((urls != null) && (urls.length > 0)) {
        for (int i=0; i<urls.length; i++) {
          if (i > 0) sb.append(System.getProperty("path.separator"));
          sb.append(urls[i]);
        }
      }
      sb.append(']');
    } else  sb.append(cl.toString());    
    return sb.toString();
  }

  /**
   * Print a message to both the console and the log.
   * @param message the message to print.
   */
  private void print(final String message) {
    System.out.println(message);
    log.info(message);
  }
}
