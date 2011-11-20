/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package test.classversion;

import java.util.List;
import java.util.logging.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;


/**
 * 
 * @author Laurent Cohen
 */
public class ExampleGridRunner {

  /**
   * 
   */
  private static Logger log = Logger.getLogger(ExampleGridRunner.class.getName());

  /** @param args not used*/
  public static void main(final String... args) {
    JPPFClient client = new JPPFClient("all your base");

    JPPFJob job = new JPPFJob();
    try {
      for (int i = 0; i < 20; i++) {
        job.addTask(new ExampleTask());
      }
    } catch (JPPFException ex) {
      log.log(Level.WARNING, null, ex);
    }
    List<JPPFTask> results = null;
    try {
      results = client.submit(job);
    } catch (Exception ex) {
      log.log(Level.WARNING, null, ex);
    }

    if (results != null) {
      for (JPPFTask task : results) {
        if (task.getException() != null) {
          System.out.println("An exception was raised: " + task.getException().getMessage());
        } else {
          System.out.println("Execution result: " + task.getResult());
        }
      }
    }

    client.close();
  }
}
