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
package test.serialization.overflow;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Runner class for the matrix multiplication demo.
 * @author Laurent Cohen
 */
public class SerializationOverflowRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(SerializationOverflowRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      jppfClient = new JPPFClient();
      for (int i = 1; i <= 1; i++)
        perform(i);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the test.
   * @param i number of iterations
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform(final int i) throws Exception {
    output("Start of iteration " + i);
    long totalTime = System.nanoTime();
    submitJob("SerializationOverflow-" + i + "/1", 1, 0L, true);
    //submitJob(" job " + i + "/2",   2, 1L, false);
    totalTime = DateTimeUtils.elapsedFrom(totalTime);
    //output("Computation time for iteration " + i + ": " + StringUtils.toStringDuration(totalTime));
  }

  /**
   * submit a job.
   * @param name the job name.
   * @param nbTasks number of tasks in the job
   * @param time duration of each tiask in millis.
   * @param blocking whether the job is blocking.
   * @throws Exception if an error is raised during the execution.
   */
  private static void submitJob(final String name, final int nbTasks, final long time, final boolean blocking) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    for (int j = 1; j <= nbTasks; j++)
      job.add(new SerializationOverflowTask(time, j));
    job.setBlocking(blocking);
    if (blocking) {
      output("* submitting job '" + job.getName() + "'");
      List<Task<?>> results = jppfClient.submitJob(job);
      output("+ got results for job " + job.getName());
      for (Task<?> task : results) {
        Throwable e = task.getThrowable();
        if (e != null) {
          output("task got exception: " + ExceptionUtils.getStackTrace(e));
          output("result is: " + task.getResult());
        } else output("task result: " + task.getResult());
      }
      //for (JPPFTask t: results) output((String) t.getResult());
    } else {
      job.getSLA().setCancelUponClientDisconnect(true);
      jppfClient.submitJob(job);
      output("job '" + job.getName() + "' submitted");
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  private static void output(final String message) {
    System.out.println(message);
    log.info(message);
  }
}
