/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.*;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
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
      JPPFJob job = new JPPFJob();
      String name = "redirect output";
      job.setName(name);
      int nbTasks = 10;
      for (int i=1; i<=nbTasks; i++) job.addTask(new RedirectOutputTask()).setId(name + " - task " + i);
      List<JPPFTask> results = client.submit(job);
      for (JPPFTask task: results) {
        Exception e = task.getException();
        if (e != null)
          System.out.println("'" + task.getId() + "' raised an exception: " + ExceptionUtils.getStackTrace(e));
        else {
          String[] result = (String[]) task.getResult();
          System.out.println("result for '" + task.getId() + "' : ");
          System.out.print("*** standard output: " + result[0]);
          System.out.println("*** error output:    " + result[1]);
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
    System.out.println("... done");
  }
}
