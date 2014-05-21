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

package sample.test.clientpool;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;

/**
 *
 * @author Laurent Cohen
 */
public class ConnectionPoolRunner {
  /**
   * Logger.
   */
  private static Logger log = LoggerFactory.getLogger(ConnectionPoolRunner.class);
  /**
   * The name of the driver to connect to in the configuration.
   */
  private static final String DRIVER_NAME = "driver1";
  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    JPPFClient client = null;
    int nbTasks = 1;
    long duration = 1000L;
    //int[] nbJobs = { 1, 5, 1 };
    int[] nbJobs = { 1 };
    try {
      configure();
      client = new JPPFClient();
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      JPPFConnectionPool pool = client.findConnectionPool(DRIVER_NAME);
      if (pool == null) throw new IllegalStateException("connection pool '" + DRIVER_NAME + "' not found");
      print("found connection pool = %s", pool);
      for (int n=0; n<nbJobs.length; n++) {
        int size = nbJobs[n];
        print("----------------------------------------------------------------------");
        print("running %d jobs/%d connections", size, size);
        pool.setMaxSize(size);
        waitForNbConnections(pool, size, 5000L);
        List<JPPFJob> jobs = new ArrayList<>(size);
        for (int i=1; i<=size; i++) jobs.add(createJob("job_" + i, nbTasks, duration));
        for (JPPFJob job: jobs) {
          client.submitJob(job);
          print("submittted job '" + job.getName() + "'");
        }
        for (JPPFJob job: jobs) printJobResults(job);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * Create a job.
   * @param name the job name.
   * @param nbTasks number of tasks in the job.
   * @param taskDuration duration of each task in ms.
   * @return the created {@link JPPFJob} instance.
   * @throws Exception if any error occurs.
   */
  private static JPPFJob createJob(final String name, final int nbTasks, final long taskDuration) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    job.setBlocking(false);
    for (int i=1; i<=nbTasks; i++) job.add(new LongTask(taskDuration)).setId(name + ":task_" + i);
    //job.getSLA().setCancelUponClientDisconnect(false);
    /*
    job.setResultListener(new JPPFResultCollector(job) {
      @Override
      public synchronized void resultsReceived(final TaskResultEvent event) {
        super.resultsReceived(event);
        print("result collector resultsReceived() : results = " + this.getAllResults());
      }
    });
    */
    job.addJobListener(new JobListenerAdapter () {
      @Override
      public void jobEnded(final JobEvent event) {
        //print("jobEnded() called : results = " + event.getJob().getResults().getAllResults());
      }

      @Override
      public void jobReturned(final JobEvent event) {
        print("jobReturned() received " + event.getJobTasks().size() + " tasks");
      }
    });
    return job;
  }

  /**
   * Display the results of a job.
   * @param job the job whose results to print.
   */
  private static void printJobResults(final JPPFJob job) {
    JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
    List<Task<?>> results = collector.awaitResults();
    print("**** results for job %s :", job.getName());
    for (Task<?> task: results) {
      String id = task.getId();
      if (task.getThrowable() != null) print("task %s raised an error: %s", id, ExceptionUtils.getStackTrace(task.getThrowable()));
      else print("task %s result: %s", id, task.getResult());
    }
  }

  /**
   * Configure the JPPF client.
   */
  private static void configure() {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setBoolean("jppf.discovery.enabled", false);
    config.setString("jppf.drivers", DRIVER_NAME);
    config.setString(DRIVER_NAME + ".jppf.server.host", "localhost");
    config.setInt(DRIVER_NAME + ".jppf.server.port", 11111);
    config.setInt(DRIVER_NAME + ".jppf.pool.size", 1);
    config.setInt(DRIVER_NAME + ".jppf.priority", 1);
  }

  /**
   * Print the specified message.
   * @param format the message format to print.
   * @param args the arguments to the format string.
   */
  private static void print(final String format, final Object...args) {
    String s = String.format(format, args);
    System.out.println(s);
    log.info(s);
  }

  /**
   * Wait until the specified number of connections are active in the specified connection pool,
   * or the specified timeout is reached, whichever happens first.
   * @param pool the connection pool to check.
   * @param n the number of connections to wait for.
   * @param timeout the timeout in millis.
   * @throws IllegalStateException if the wait timed out.
   */
  private static void waitForNbConnections(final JPPFConnectionPool pool, final int n, final long timeout) {
    long elapsed = 0L;
    long start = System.nanoTime();
    while (elapsed < timeout * 1_000_000L) {
      if (pool.connectionCount(JPPFClientConnectionStatus.ACTIVE) == n) return;
      elapsed = System.nanoTime() - start;
      if (elapsed >= timeout * 1_000_000L) throw new IllegalStateException("timed out while waiting for " + n + " connections");
    }
  }
}
