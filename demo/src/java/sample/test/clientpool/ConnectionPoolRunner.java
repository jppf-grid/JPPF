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

package sample.test.clientpool;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
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
  private static final String DRIVER_NAME = "driver";
  /**
   * 
   */
  private static long start;

  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    JPPFClient client = null;
    int nbTasks = 80;
    long duration = 3000L;
    //int[] nbJobs = { 1, 5, 1 };
    int[] nbJobs = { 1 };
    try {
      configure(2);
      client = new JPPFClient();
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      /*
      JPPFConnectionPool pool = client.findConnectionPool(DRIVER_NAME);
      if (pool == null) throw new IllegalStateException("connection pool '" + DRIVER_NAME + "' not found");
      print("found connection pool = %s", pool);
      */
      start = System.nanoTime();
      for (int n=0; n<nbJobs.length; n++) {
        int size = nbJobs[n];
        //int nbConnections = nbJobs[n];
        int nbConnections = 2;
        print("----------------------------------------------------------------------");
        print("running %d jobs/%d connections", size, nbConnections);
        /*
        pool.setMaxSize(nbConnections);
        waitForNbConnections(pool, nbConnections, 5000L);
        */
        waitForNbPools(client, nbConnections, 5000L);
        List<JPPFJob> jobs = new ArrayList<>(size);
        for (int i=1; i<=size; i++) jobs.add(createJob(String.format("job_%d_%d", (n + 1), i), nbTasks, duration));
        for (JPPFJob job: jobs) {
          client.submitJob(job);
          print("[" + getElapsed() + "] submittted job '" + job.getName() + "'");
        }
        for (JPPFJob job: jobs) printJobResults(job);
      }
      Thread.sleep(2500L);
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
    job.getClientSLA().setMaxChannels(2);
    for (int i=1; i<=nbTasks; i++) job.add(new MyTask(taskDuration)).setId(name + ":task_" + i);
    return job;
  }

  /**
   * Display the results of a job.
   * @param job the job whose results to print.
   */
  private static void printJobResults(final JPPFJob job) {
    List<Task<?>> results = job.awaitResults();
    long elapsed = (System.nanoTime() - start) / 1_000_000L;
    print("[" + getElapsed() + "] ***** results for job %s :", job.getName());
    for (Task<?> task: results) {
      String id = task.getId();
      if (task.getThrowable() != null) print("task %s raised an error: %s", id, ExceptionUtils.getStackTrace(task.getThrowable()));
      else print("task %s result: %s", id, task.getResult());
    }
  }

  /**
   * Configure the JPPF client.
   * @param nbPools the number of pools to configure
   */
  private static void configure(final int nbPools) {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.set(JPPFProperties.DISCOVERY_ENABLED, false);
    StringBuilder sb = new StringBuilder();
    for (int i=1; i<=nbPools; i++) {
      if (i > 1) sb.append(' ');
      String name = DRIVER_NAME + i;
      sb.append(name);
      config.setString(name + ".jppf.server.host", "localhost")
        .setInt(name + ".jppf.server.port", 11110 + i)
        .setInt(name + '.' + JPPFProperties.POOL_SIZE.getName(), 1)
        .setInt(name + ".jppf.priority", 1);
    }
    config.set(JPPFProperties.DRIVERS, new String[] {sb.toString()});
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

  /**
   * Wait until the specified number of pools are active, or the specified timeout is reached, whichever happens first.
   * @param client the client to check for pools.
   * @param n the number of connections to wait for.
   * @param timeout the timeout in millis.
   * @throws IllegalStateException if the wait timed out.
   */
  private static void waitForNbPools(final JPPFClient client, final int n, final long timeout) {
    long elapsed = 0L;
    long start = System.nanoTime();
    while (elapsed < timeout * 1_000_000L) {
      List<JPPFConnectionPool> pools = client.findConnectionPools(JPPFClientConnectionStatus.ACTIVE);
      if ((pools != null) && (pools.size() >= n)) return;
      elapsed = System.nanoTime() - start;
      if (elapsed >= timeout * 1_000_000L) throw new IllegalStateException("timed out while waiting for " + n + " active pools");
    }
  }

  /**
   * Get a JMX connection to the driver.
   * @param pool the pool of connections tot he driver.
   * @return a {@code JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private static JMXDriverConnectionWrapper getJmx(final JPPFConnectionPool pool) throws Exception {
    JMXDriverConnectionWrapper jmx = null;
    while ((jmx = pool.getJmxConnection()) == null) Thread.sleep(10L);
    while (!jmx.isConnected()) Thread.sleep(10L);
    return jmx;
  }

  /**
   * 
   * @return the elapsed time in millis.
   */
  private static long getElapsed() {
    return (System.nanoTime() - start) / 1_000_000L;
  }

  /**
   *
   */
  public static class MyTask extends LongTask {
    /**
     * Initialize the task.
     * @param taskLength the duration of this task.
     */
    public MyTask(final long taskLength) {
      super(taskLength);

    }

    @Override
    public void run() {
      this.fireNotification("start notification from " + getId(), true);
      super.run();
      this.fireNotification("end notification from " + getId(), true);
    }
  }
}
