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

package sample.test.redirect;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class RedirectOutputRunner {
  /**
   * The JPPF client.
   */
  static JPPFClient client = null;

  /**
   * Entry point into the test.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      System.out.println("Starting ...");
      client = new JPPFClient();
      final JPPFJob job = new JPPFJob();
      final String name = "redirect output";
      job.setName(name);
      final int nbTasks = 10;
      for (int i=1; i<=nbTasks; i++) job.add(new RedirectOutputTask()).setId(name + " - task " + i);
      final List<Task<?>> results = client.submitJob(job);
      for (final Task <?>task: results) {
        final Throwable e = task.getThrowable();
        if (e != null) System.out.println("'" + task.getId() + "' raised an exception: " + ExceptionUtils.getStackTrace(e));
        else {
          final String[] result = (String[]) task.getResult();
          System.out.println("result for '" + task.getId() + "' : ");
          System.out.print("*** standard output: " + result[0]);
          System.out.println("*** error output:    " + result[1]);
        }
      }
    } catch(final Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
    System.out.println("... done");
  }
}
