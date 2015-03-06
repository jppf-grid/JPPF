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
package sample.dist.tasklength;

import java.util.List;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.job.*;
import org.jppf.management.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;
import org.slf4j.*;


/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class LongTaskRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(LongTaskRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;
  /**
   * JMX connection to the driver.
   */
  //private static JMXDriverConnectionWrapper jmx = null;
  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      jppfClient = new JPPFClient();
      //setupJobManagementListener(jppfClient);
      TypedProperties props = JPPFConfiguration.getProperties();
      int length = props.getInt("longtask.length");
      int nbTask = props.getInt("longtask.number");
      int iterations = props.getInt("longtask.iterations");
      print("Running Long Task demo with "+ nbTask + " tasks of length = " + length + " ms for " + iterations + " iterations");
      perform(nbTask, length, iterations);
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
   * @param nbTasks the number of tasks to send at each iteration.
   * @param length the executionlength of each task.
   * @param iterations the number of times the the tasks will be sent.
   * @throws Exception if any error occurs.
   */
  private static void perform(final int nbTasks, final int length, final int iterations) throws Exception {
    // perform "iteration" times
    long totalTime = 0L;
    for (int iter=1; iter<=iterations; iter++) {
      long start = System.currentTimeMillis();
      JPPFJob job = new JPPFJob();
      job.setName("Long task iteration " + iter);
      for (int i=0; i<nbTasks; i++) job.add(new LongTask(length)).setId("" + iter + ':' + (i+1));
      //job.getSLA().setMaxTaskResubmits(0);
      //job.getSLA().setApplyMaxResubmitsUponNodeError(true);
      // submit the tasks for execution
      List<Task<?>> results = jppfClient.submitJob(job);
      for (Task task: results) {
        Throwable e = task.getThrowable();
        if (e != null) System.out.printf("task %s got exception %s%n", task.getId(), ExceptionUtils.getMessage(e));
      }
      long elapsed = System.currentTimeMillis() - start;
      print("Iteration #" + iter + " performed in " + StringUtils.toStringDuration(elapsed));
      totalTime += elapsed;
    }
    print("Average iteration time: " + StringUtils.toStringDuration(totalTime/iterations));
    //JPPFStats stats = ((JPPFClientConnectionImpl) jppfClient.getClientConnection()).getJmxConnection().statistics();
    //print("End statistics :\n"+stats.toString());
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
   * @param client the JPPF client.
   * @return an instance of {@link DriverJobManagementMBean}.
   * @throws Exception if any error occurs.
   */
  private static DriverJobManagementMBean getJobManagement(final JPPFClient client) throws Exception {
    return setupJobManagementListener(client).getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
  }

  /**
   * Get the jmx connection to the driver.
   * @param client the JPPF client.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  public static JMXDriverConnectionWrapper setupJobManagementListener(final JPPFClient client) throws  Exception {
    JPPFConnectionPool pool = null;
    while ((pool = client.getConnectionPool()) == null) Thread.sleep(1L);
    JMXDriverConnectionWrapper jmx = null;
    while ((jmx = pool.getJmxConnection(true)) == null)  Thread.sleep(1L);
    DriverJobManagementMBean jobMBean = jmx.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
    jobMBean.addNotificationListener(new MyJobListener(), null, null);
    return jmx;
  }

  /**
   *
   */
  public static class MyJobListener implements NotificationListener {
    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      JobNotification jobNotif = (JobNotification) notification;
      JobInformation jobInfo = jobNotif.getJobInformation();
      JPPFManagementInfo nodeInfo = jobNotif.getNodeInfo();
      switch (jobNotif.getEventType()) {
        case JOB_QUEUED:
          System.out.printf("job '%s' queued (%d tasks)%n", jobInfo.getJobName(), jobInfo.getInitialTaskCount());
          break;
        case JOB_ENDED:
          System.out.printf("job '%s' ended%n", jobInfo.getJobName());
          break;
        case JOB_DISPATCHED:
          System.out.printf("job '%s' dispatched to node '%s' (%d tasks)%n", jobInfo.getJobName(), nodeInfo.toDisplayString(), jobInfo.getTaskCount());
          break;
        case JOB_RETURNED:
          System.out.printf("job '%s' returned from node '%s'%n", jobInfo.getJobName(), nodeInfo.toDisplayString());
          break;
      }
    }
  }
}
