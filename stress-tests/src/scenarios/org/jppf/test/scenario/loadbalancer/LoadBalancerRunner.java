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

package org.jppf.test.scenario.loadbalancer;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;

import javax.management.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.utils.*;
import org.jppf.job.*;
import org.jppf.management.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class LoadBalancerRunner extends AbstractScenarioRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(LoadBalancerRunner.class);
  /**
   * 
   */
  private JMXDriverConnectionWrapper[] driverJmx;
  /**
   * 
   */
  private MyJobNotificationListener listener = null;

  @Override
  public void run()
  {
    try
    {
      TypedProperties props = getConfiguration().getProperties();
      int nbJobs = props.getInt("nbJobs", 10);
      int nbTasks = props.getInt("nbTasks", 40);
      int dataSize = props.getInt("dataSize", 1024);
      long duration = props.getLong("duration", 1250);
      System.out.println("Running with " + nbTasks + " tasks of size=" + dataSize + " for " + nbJobs + " jobs");
      Thread.sleep(1000L);
      perform(nbTasks, duration, dataSize, nbJobs);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Execute the specified number of tasks for the specified number of iterations.
   * @param nbTasks the number of tasks to send at each iteration.
   * @param duration the duration of each task in ms.
   * @param dataSize the size of a byte[] associated witht he task (to emulate different memory footprints).
   * @param nbJobs the number of times the the tasks will be sent.
   * @throws Exception if an error is raised during the execution.
   */
  public void perform(final int nbTasks, final long duration, final int dataSize, final int nbJobs) throws Exception
  {
    try
    {
      File file = new File(getConfiguration().getConfigDir(), "lb.csv");
      listener = new MyJobNotificationListener(file);
      initJmx();
      for (int i=1; i<=nbJobs; i++)
      {
        long start = System.nanoTime();
        JPPFJob job = JobHelper.createJob("Job" + StringUtils.padLeft(""+i, '0', 2), true, false, nbTasks, LifeCycleTask.class, duration, dataSize);
        List<JPPFTask> results = getSetup().getClient().submit(job);
        long elapsed = System.nanoTime() - start;
        System.out.println("Iteration #" + i + " performed in " + StringUtils.toStringDuration(elapsed/1000000));
      }
    }
    finally
    {
      if (listener != null) listener.dispose();
    }
  }

  /**
   * 
   * @throws Exception if any error occurs.
   */
  public void initJmx() throws Exception
  {
    driverJmx = new JMXDriverConnectionWrapper[2];
    MyNotificationFilter filter = new MyNotificationFilter();
    for (int i=0; i<2; i++)
    {
      JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11201 + i, false);
      jmx.connect();
      while (!jmx.isConnected()) Thread.sleep(10L);
      driverJmx[i] = jmx;
      DriverJobManagementMBean proxy = jmx.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
      proxy.addNotificationListener(listener, null, "d" + (i+1));
    }
  }

  /**
   * this class prints a message each time a job is added to the server's queue
   */
  public static class MyJobNotificationListener implements NotificationListener
  {
    /**
     * 
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    /**
     * 
     */
    private Writer writer = null;

    /**
     *
     * @param file the file into which to write the csv.
     * @throws Exception if any error occurs.
     */
    public MyJobNotificationListener(final File file) throws Exception
    {
      writer = new FileWriter(file);
      writer.write("job,driver,node,initial,count\n");
    }

    @Override
    public void handleNotification(final Notification notification, final Object handback)
    {
      JobNotification notif = (JobNotification) notification;
      if (notif.getEventType() == JobEventType.JOB_DISPATCHED) executor.submit(new WriterTask((JobNotification) notification, handback.toString()));
    }

    /**
     * 
     */
    public void dispose()
    {
      executor.shutdownNow();
      StreamUtils.closeSilent(writer);
    }

    /**
     * 
     */
    private class WriterTask implements Runnable
    {
      /**
       * The notif to log.
       */
      final JobNotification notif;
      /**
       * 
       */
      final String driverId;

      /**
       * 
       * @param notif the notif to log.
       * @param driverId id of the driver that emitted the notification.
       */
      public WriterTask(final JobNotification notif, final String driverId)
      {
        this.notif = notif;
        this.driverId = driverId;
      }

      @Override
      public void run()
      {
        try
        {
          JobInformation job = notif.getJobInformation();
          JPPFManagementInfo node = notif.getNodeInfo();
          StringBuilder sb = new StringBuilder();
          sb.append(job.getJobName()).append(',').append(driverId).append(',').append(node.getId()).append(',');
          sb.append(job.getInitialTaskCount()).append(',').append(job.getTaskCount()).append('\n');
          writer.write(sb.toString());
          System.out.print("logged " + sb.toString());
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * A notification filter that only accepts notifs of type <code>JobEventType.JOB_DISPATCHED</code>.
   */
  public static class MyNotificationFilter implements NotificationFilter
  {
    @Override
    public boolean isNotificationEnabled(final Notification notification)
    {
      JobNotification jobNotif = (JobNotification) notification;
      return (jobNotif.getEventType() == JobEventType.JOB_DISPATCHED);
    }
  }
}
