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
package sample.test.jppfcallable;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.logging.jmx.JmxLogger;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.Operator;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class JPPFCallableRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JPPFCallableRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * Used to test JPPFTask.compute(JPPFCallable) in method {@link #testComputeCallable()}.
   */
  static String callableResult = "";
  /**
   * 
   */
  private static MyLoggingHandler loggingHandler = null;

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   * @throws Exception if an error is raised during the execution.
   */
  public static void main(final String...args) throws Exception {
    final int nbRuns = 1;
    //loggingHandler = new MyLoggingHandler();
    for (int i=1; i<=nbRuns; i++) {
      print("*---------- run "  + StringUtils.padLeft(String.valueOf(i), '0', 3) + " ----------*");
      try {
        perform();
      } catch(final Exception e) {
        e.printStackTrace();
      } finally {
        //restartDriver(1L, i < nbRuns ? 2000L : -1L);
        if (jppfClient != null) jppfClient.close();
        if (i < nbRuns) Thread.sleep(3000L);
      }
    }
  }

  /**
   * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform() throws Exception {
    final int nbTasks = 400;
    final int nbJobs = 1;
    final int maxChannels = 1;
    final int size = 1024;
    final long time = 10L;
    configure();
    jppfClient = new JPPFClient();
    while (!jppfClient.hasAvailableConnection()) Thread.sleep(20L);
    JmxLogger jmxLogger = null;
    if (loggingHandler != null) {
      jmxLogger = getJmxLogger();
      loggingHandler.register(jmxLogger);
    }
    print("submitting " + nbJobs + " jobs with " + nbTasks + " tasks");
    final List<JPPFJob> jobList = new ArrayList<>();
    for (int n=1; n<=nbJobs; n++) {
      final String name = "job-" + StringUtils.padLeft(String.valueOf(n), '0', 4);
      final JPPFJob job = new JPPFJob(name);
      job.getClientSLA().setMaxChannels(maxChannels);
      job.setBlocking(false);
      for (int i=1; i<=nbTasks; i++) job.add(new MyTask(time, size)).setId(name + ":task-" + StringUtils.padLeft(String.valueOf(i), '0', 5));
      job.addJobListener(new JobListenerAdapter() {
        @Override
        public synchronized void jobReturned(final JobEvent event) {
          print("received " + event.getJobTasks().size() + " results");
        }
      });
      //job.addJobListener(new MyJobListener());
      jobList.add(job);
    }
    callableResult = "from MyCallable";
    for (JPPFJob job: jobList) jppfClient.submitJob(job);
    for (JPPFJob job: jobList) {
      job.awaitResults();
      print("got results for job '" + job.getName() + "'");
    }
    if (loggingHandler != null) loggingHandler.unregister(jmxLogger);
  }

  /**
   * Perform optional configuration before creating the JPPF client.
   */
  public static void configure() {
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg) {
    log.info(msg);
    System.out.println(msg);
  }

  /**
   * Use JMX to stop the driver.
   * @param shutdownDelay .
   * @param restartDelay .
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unused")
  private static void restartDriver(final long shutdownDelay, final long restartDelay) throws Exception {
    final JMXDriverConnectionWrapper jmx = getDriverJmx();
    try {
      jmx.restartShutdown(shutdownDelay, restartDelay);
    } finally {
      try {
        jmx.close();
      } catch (final Exception ignore) {
      }
    }
  }

  /**
   * Get a driver JMX connection.
   * @return a {@link org.jppf.management.JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private static JMXDriverConnectionWrapper getDriverJmx() throws Exception {
    return jppfClient.awaitActiveConnectionPool().awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
  }

  /**
   * Get a proxy to the JmxLooger.
   * @return a {@link JmxLogger} instance.
   * @throws Exception if any error occurs.
   */
  private static JmxLogger getJmxLogger() throws Exception {
    return getDriverJmx().getProxy(JmxLogger.DEFAULT_MBEAN_NAME, JmxLogger.class);
  }

  /**
   * 
   */
  public static class MyJobListener extends JobListenerAdapter {
    /**
     * 
     */
    private int lastCount = 0;

    @Override
    public void jobReturned(final JobEvent event) {
      System.out.println("job '" + event.getJob().getName() + "' returned");
      final JPPFJob job = event.getJob();
      final int size = job.getResults().size();
      if (size - lastCount > 100) {
        System.out.println("received " + size + " tasks for job '" + job.getName() + "'");
        lastCount = size;
      }
    }

    @Override
    public void jobStarted(final JobEvent event) {
      System.out.println("job '" + event.getJob().getName() + "' started");
    }

    @Override
    public void jobEnded(final JobEvent event) {
      System.out.println("job '" + event.getJob().getName() + "' ended");
    }

    @Override
    public void jobDispatched(final JobEvent event) {
      System.out.println("job '" + event.getJob().getName() + "' dispatched");
    }
  }
}
