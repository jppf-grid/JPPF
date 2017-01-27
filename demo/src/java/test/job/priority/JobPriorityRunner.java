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
package test.job.priority;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class JobPriorityRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JobPriorityRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * JMX connection to the driver.
   */
  private static JMXDriverConnectionWrapper jmx = null;

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      JPPFConfiguration.set(JPPFProperties.DISCOVERY_ENABLED, true).set(JPPFProperties.POOL_SIZE, 2);
      jppfClient = new JPPFClient();
      perform();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
   * @throws Exception if an error is raised during the execution.
   */
  private static void perform() throws Exception {
    try {
      DriverJobManagementMBean jobMgt = getJobManagement();
      long start = System.nanoTime();
      JPPFJob job1 = createJob("job 1", 1, 10, 1000);
      JPPFJob job2 = createJob("job 2", 0, 10, 1000);
      JobRunner runner1 = new JobRunner(job1);
      JobRunner runner2 = new JobRunner(job2);
      runner1.start();
      runner2.start();
      Thread.sleep(2000L);
      jobMgt.updatePriority(job2.getUuid(), 10);
      runner1.join();
      runner2.join();
      // submit the tasks for execution
      long elapsed = DateTimeUtils.elapsedFrom(start);
      print("elapsed time: " + StringUtils.toStringDuration(elapsed));
    } catch (Exception e) {
      throw new JPPFException(e.getMessage(), e);
    }
  }

  /**
   * Create a non-blocking job with the specified name and priority.
   * @param name the job's name.
   * @param priority the job priority.
   * @param nbTasks number of tasks in the job.
   * @param length the length of each task in milliseconds.
   * @return the created job.
   * @throws Exception if an error is raised during the job creation.
   */
  private static JPPFJob createJob(final String name, final int priority, final int nbTasks, final int length) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    job.getSLA().setPriority(priority);
    for (int i = 0; i < nbTasks; i++) {
      LongTask task = new LongTask(length, false);
      task.setId("" + (i + 1));
      job.add(task);
    }
    job.setBlocking(false);
    return job;
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
   * Get a proxy to the driver's job management MBean.
   * @return an instance of {@link DriverJobManagementMBean}.
   * @throws Exception if any error occurs.
   */
  private static DriverJobManagementMBean getJobManagement() throws Exception {
    return getJmxConnection().getJobManager();
  }

  /**
   * Get the jmx connection to the driver.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private static JMXDriverConnectionWrapper getJmxConnection() throws Exception {
    if (jmx == null) {
      /* while (!jppfClient.hasAvailableConnection()) Thread.sleep(10L);
       * JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
       * jmx = c.getJmxConnection();
       * boolean b = jmx.isConnected();
       * if (!b) jmx.connect(); */
      jmx = new JMXDriverConnectionWrapper("localhost", 11198);
      jmx.connect();
      while (!jmx.isConnected())
        Thread.sleep(10L);
    }
    return jmx;
  }

  /**
   * Restart the driver.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unused")
  private static void restartDriver() throws Exception {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        try {
          String s = getJmxConnection().restartShutdown(100L, 2000L);
          System.out.println("response for restart: " + s);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    //new Thread(r).start();
    r.run();
  }

  /**
   * 
   */
  private static class JobRunner extends Thread {
    /**
     * The job to execute.
     */
    private final JPPFJob job;

    /**
     * Initialize this job runner.
     * @param job the job to execute.
     */
    public JobRunner(final JPPFJob job) {
      this.job = job;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      try {
        jppfClient.submitJob(job);
        List<Task<?>> results = job.awaitResults();
        print("job '" + job.getName() + "' complete");
        for (Task<?> task : results) {
          StringBuilder sb = new StringBuilder();
          sb.append("results for task [").append(job.getName()).append("] ").append(task.getId()).append(" : ");
          Throwable e = task.getThrowable();
          if (e != null) sb.append(ExceptionUtils.getStackTrace(e));
          else sb.append(task.getResult());
          print(sb.toString());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
