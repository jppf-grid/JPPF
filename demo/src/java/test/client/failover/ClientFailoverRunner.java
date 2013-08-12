/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package test.client.failover;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.utils.ClientWithFailover;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;


/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class ClientFailoverRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ClientFailoverRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static ClientWithFailover clientFailover = null;
  /**
   * JMX connection to the driver.
   */
  private static JMXDriverConnectionWrapper jmx = null;

  /**
   * Entry point for this class.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      clientFailover = new ClientWithFailover();
      List<JPPFJob> jobs = new ArrayList<JPPFJob>();
      for (int i=1; i<=2; i++) {
        JPPFJob job = createJob("job " + i, 30, 3000L);
        job.getSLA().setPriority(10 - i);
        jobs.add(job);
        clientFailover.submit(job);
      }
      for (JPPFJob job: jobs) {
        processJob(job);
      }
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      if (clientFailover != null) clientFailover.close();
    }
  }

  /**
   * Create a non-blocking job with the specified name and priority.
   * @param name the job's name.
   * @param nbTasks number of tasks in the job.
   * @param length the length of each task in milliseconds.
   * @return the created job.
   * @throws Exception if an error is raised during the job creation.
   */
  private static JPPFJob createJob(final String name, final int nbTasks, final long length) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    job.getClientSLA().setExecutionPolicy(null);
    for (int i=0; i<nbTasks; i++) {
      LongTask task = new LongTask(length, false);
      task.setId("" + (i+1));
      job.addTask(task);
    }
    job.setBlocking(false);
    return job;
  }

  /**
   * Wait for and display the results of the specified job.
   * @param job the job to process.
   * @throws Exception if any error is raised.
   */
  private static void processJob(final JPPFJob job) throws Exception {
    JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
    List<JPPFTask> results = collector.waitForResults();
    for (JPPFTask task: results) {
      if (task.getException() != null) {
        print("task got exception:" + ExceptionUtils.getStackTrace(task.getException()));
        return;
      }
    }
    print("all " + results.size() + " results are good!");
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg) {
    log.info(msg);
    System.out.println(msg);
  }
}
