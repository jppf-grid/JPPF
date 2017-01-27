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

package org.jppf.example.interceptor;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/**
 * This is the main class for the Network Interceptor demo.
 * @author Laurent Cohen
 */
public class NetworkInterceptorDemo {
  /**
   * Entry point for the Network Interceptor demo.
   * @param args not used.
   */
  public static void main(final String[] args) {
    String userName = System.getProperty("jppf.user.name");
    System.out.println("demo: running network interceptor demo with user name = " + userName);
    // upon creating the client, the interceptor will be invoked
    try (JPPFClient client = new JPPFClient()) {
      JPPFJob job = new JPPFJob();
      job.setName("Network Interceptor Demo");
      job.add(new MyTask()).setId("task 1");
      System.out.println("demo: submitting demo job");
      List<Task<?>> results = client.submitJob(job);
      System.out.printf("demo: ***** results for '%s' *****%n", job.getName());
      for (Task<?> task : results) {
        if (task.getThrowable() != null) System.out.printf("demo: got exception: %s%n",
          ExceptionUtils.getStackTrace(task.getThrowable()));
        else System.out.printf("demo: got result: %s%n", task.getResult());
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
