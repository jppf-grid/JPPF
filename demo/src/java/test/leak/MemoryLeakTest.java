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

package test.leak;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class MemoryLeakTest {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(MemoryLeakTest.class);
  /**
   * The runtime options for this test.
   */
  private static final Configuration options = new Configuration();
  /**
   *
   */
  private static JMXDriverConnectionWrapper jmx = null;
  /**
   *
   */
  private static final AtomicLong cancelCount = new AtomicLong(0L);

  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    System.out.println("configuration = " + options);
    try (JPPFClient client = new JPPFClient()) {
      //client.addClientQueueListener(new MyQueueListener());
      JMXDriverConnectionWrapper jmx = getJmxConnection(client);
      // register the task notifications listener on all nodes
      String listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, new MyNotificationHandler(client), null, null);
      long start = System.nanoTime();
      for (int i=1; i<=options.nbJobs; i++) {
        JPPFJob job = new JPPFJob();
        job.setName("test" + i);
        for (int j=1; j<=options.nbTasks; j++) job.add(new MyTask(options.duration, options.dataSize)).setId(job.getName() + "_task" + j);
        List<Task<?>> results = client.submitJob(job);
        MyTask task = (MyTask) results.get(0);
        String res = task.getResult();
        System.out.printf("job '%s' terminated with status '%s'%n", job.getName(), (res == null ? "cancelled" : "completed"));
      }
      long elapsed = (System.nanoTime() - start) / 1_000_000L;
      System.out.printf("test finished in %,d ms; cancel count = %,d%n", elapsed, cancelCount.get());
      jmx.unregisterForwardingNotificationListener(listenerId);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get a JMX connection from the specified client.
   * @param client the client ot get the connection from.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private static synchronized JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    if (jmx == null) jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    return jmx;
  }

  /**
   * This JMX notification listener cancels a job once it receives a specific notification from its task.
   */
  private static class MyNotificationHandler implements NotificationListener {
    /**
     */
    private final JPPFClient client;

    /**
     * @param client .
     */
    public MyNotificationHandler(final JPPFClient client) {
      this.client = client;
    }

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      try {
        JPPFNodeForwardingNotification wrapping = (JPPFNodeForwardingNotification) notification;
        TaskExecutionNotification actualNotif = (TaskExecutionNotification) wrapping.getNotification();
        if (MyTask.NOTIF_MESSAGE.equals(actualNotif.getUserData())) {
          client.cancelJob(actualNotif.getTaskInformation().getJobId());
          cancelCount.incrementAndGet();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * A simple task.
   */
  public static class MyTask extends AbstractTask<String> {
    /**
     */
    private static final String NOTIF_MESSAGE = "time_to_cancel";
    /**
     * Some dummy data to simulate a memory footprint.
     */
    private final byte[] dummyData;
    /**
     * How long this task will sleep.
     */
    private final long sleepTime;

    /**
     * 
     * @param sleepTime how long this task will sleep.
     * @param dataSize the minimum memory footprint of this task.
     */
    public MyTask(final long sleepTime, final int dataSize) {
      this.sleepTime = sleepTime;
      this.dummyData = (dataSize >= 0) ? new byte[dataSize] : null;
    }

    @Override
    public void run() {
      try {
        // send the JMX notification
        fireNotification(NOTIF_MESSAGE, true);
        // wait long enough to ensure the client has time to receive the notification and cancel the job
        Thread.sleep(sleepTime);
        // if this task is cancelled, the next 2 lines are not executed
        setResult("execution success for " + getId());
        System.out.printf("%s (data size = %,d)%n", getResult(), dummyData.length);
      } catch (Exception e) {
        setThrowable(e);
      }
    }

    @Override
    public void onCancel() {
      System.out.printf("%s cancelled (data size = %,d)%n", getId(), dummyData.length);
    }
  }

  /**
   */
  private static class MyQueueListener implements ClientQueueListener {
    @Override
    public void jobAdded(final ClientQueueEvent event) {
      log.info(String.format("added job '%s' to the queue", event.getJob().getName()));
    }

    @Override
    public void jobRemoved(final ClientQueueEvent event) {
      String name = event.getJob().getName();
      if ("test1".equals(name)) log.info(String.format("removed job '%s' from the queue, callstack=%n%s", name, ExceptionUtils.getCallStack()));
      else log.info(String.format("removed job '%s' from the queue", name));
    }
  }

  /**
   * Runtime configuration for this test.
   */
  private final static class Configuration {
    /**
     * The JPPF configuration.
     */
    private static final TypedProperties config = JPPFConfiguration.getProperties();
    /**
     * The number of jobs to submit.
     */
    public final int nbJobs = config.getInt("leak.nbJobs", 1000);
    /**
     * The number fo tasks in each job.
     */
    public final int nbTasks = config.getInt("leak.nbTasks", 2);
    /**
     * How long each task will sleep.
     */
    public final long duration = config.getLong("leak.duration", 2000L);
    /**
     * The minimum memory footprint of each task.
     */
    public final int dataSize = config.getInt("leak.dataSize", 5 * 1024 * 1024);

    /**
     */
    private Configuration() {
      configure();
    }

    /**
     * Set JPPF config properties overrides.
     */
    public void configure() {
      config
        // load balancing
        .set(JPPFProperties.LOAD_BALANCING_ALGORITHM, "manual")
        .set(JPPFProperties.LOAD_BALANCING_PROFILE, "manual")
        .setInt(JPPFProperties.LOAD_BALANCING_PROFILE.getName() + ".manual.size", 1)
        // server connection
        .set(JPPFProperties.DISCOVERY_ENABLED, false)
        .set(JPPFProperties.DRIVERS, new String[] {"driver1"})
        .setString("driver1.jppf.server.host", "localhost")
        .setInt("driver1.jppf.server.port", 11111);
    }

    @Override
    public String toString() {
      return "Configuration[nbJobs=" + nbJobs + ", nbTasks=" + nbTasks + ", duration=" + duration + ", dataSize=" + dataSize + "]";
    }
  }
}
