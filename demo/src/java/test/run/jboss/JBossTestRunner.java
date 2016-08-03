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

package test.run.jboss;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class JBossTestRunner {
  /**
   * The JPPF client.
   */
  static JPPFClient client = null;

  /**
   * Entry point into the test.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      System.out.println("Starting ...");
      client = new JPPFClient();
      perform();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
    System.out.println("... done");
  }

  /**
   * Submit non-blocking tasks.
   * @throws Exception if any error occurs.
   */
  public static void perform() throws Exception {
    JPPFJob job = new JPPFJob("JBoss Runner");
    job.add(new JBossTask("C:/Tools/jboss-5.1.0.GA", "jppf"));
    job.setBlocking(false);
    job.getSLA().setBroadcastJob(true);
    client.submitJob(job);
    List<Task<?>> results = job.awaitResults();
    Task<?> task = results.get(0);
    if (task.getThrowable() != null) {
      System.out.println("task ended with exception:\n" + ExceptionUtils.getStackTrace(task.getThrowable()));
    } else {
      System.out.println("task result: " + task.getResult());
    }
  }
}
