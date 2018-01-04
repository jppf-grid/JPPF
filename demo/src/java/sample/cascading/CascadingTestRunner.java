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
package sample.cascading;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.slf4j.*;

/**
 * Runner class used for testing the framework.
 * @author Laurent Cohen
 */
public class CascadingTestRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(CascadingTestRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class, performs a matrix multiplication a number of times.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      jppfClient = new JPPFClient();
      performCommand();
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      jppfClient.close();
    }
    System.exit(0);
  }

  /**
   * .
   * @throws Exception .
   */
  private static void performCommand() throws Exception {
    final JPPFJob job = new JPPFJob();
    job.add(new Task1());
    job.getSLA().setExecutionPolicy(new Equal("id", 1));
    final List<Task<?>> results = jppfClient.submitJob(job);
    for (final Task<?> task: results) {
      if (task.getThrowable() != null) task.getThrowable().printStackTrace();
      else System.out.println("result: " + task.getResult());
    }
  }
}
