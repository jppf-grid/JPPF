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

package sample.test.deadlock;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.job.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * An illustration of the patterns for submitting multiple jobs in parallel.
 */
public class DeadlockRunner {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProvisioningThread.class);
  /**
   *
   */
  private static JMXDriverConnectionWrapper jmx = null;
  /**
   * 
   */
  private static final AtomicLong cancelCount = new AtomicLong(0L);

  /**
   * Entry point for this demo.
   * @param args the first argument is a function number that determines which method to call.
   */
  public static void main(final String[] args) {
    try {
      DeadlockRunner runner = new DeadlockRunner();
      runner.jobStreaming();
      //runner.testNodes();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Execute a stream of non-blocking jobs from a single thread, process the results asynchronously.
   */
  public void jobStreaming() {
    final RunOptions ro = new RunOptions();
    printf("Running with conccurencyLimit=%d, nbJobs=%d, tasksPerJob=%d, taskDuration=%d", ro.concurrencyLimit, ro.nbJobs, ro.tasksPerJob, ro.taskDuration);
    ProvisioningThread pt = null;
    MasterNodeMonitoringThread mnmt = null;
    try (JPPFClient client = new JPPFClient(); JobStreamImpl jobProvider = new JobStreamImpl(ro)) {
      JMXDriverConnectionWrapper jmx = getJmxConnection(client);
      jmx.addNotificationListener(DriverJobManagementMBean.MBEAN_NAME, new MyNotificationHandler(client));
      //ro.callback = new MyCallback(client);
      ensureSufficientConnections(client, ro.clientConnections);
      if (ro.slaves >= 0) updateSlaveNodes(client, ro.slaves);
      if (ro.simulateNodeCrashes) {
        pt = new ProvisioningThread(client, ro.waitTime);
        mnmt = new MasterNodeMonitoringThread(client, 5000L, pt);
      }
      try {
        if (ro.simulateNodeCrashes) {
          new Thread(pt, "ProvisioningThread").start();
          new Thread(mnmt, "MasterNodeMonitoringThread").start();
        }
        long start = System.nanoTime();
        int count = 0;
        for (JPPFJob job: jobProvider) {
          if ((job != null) && !client.isClosed()) client.submitJob(job);
          if (count == ro.triggerNodeDeadlockAfter) {
            JPPFJob deadlockingJob = new JPPFJob();
            deadlockingJob.setName("deadlock trigger job");
            deadlockingJob.setBlocking(false);
            deadlockingJob.add(new DeadlockingTask());
            client.submitJob(deadlockingJob);
          }
          count++;
          //requestNodeShutdown(client);
        }
        while (jobProvider.hasPendingJob()) Thread.sleep(10L);
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        printf("*** executed a total of %,d jobs and %,d tasks in %s", jobProvider.getJobCount(), jobProvider.getTaskCount(), StringUtils.toStringDuration(elapsed));
        printf("*** cancel count = %d%n", cancelCount.get());
      } finally {
        if (ro.simulateNodeCrashes) {
          pt.setStopped(true);
          mnmt.setStopped(true);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   */
  public void testNodes() {
    TypedProperties config = JPPFConfiguration.getProperties();
    int[] nbSlaves = StringUtils.parseIntValues(config.getString("deadlock.nbSlaves", "0"));
    try (JPPFClient jppfClient = new JPPFClient()) {
      ensureSufficientConnections(jppfClient, 1);
      updateSlaveNodes(jppfClient, 0);
      long start = System.nanoTime();
      for (int n: nbSlaves) updateSlaveNodes(jppfClient, n);
      long elapsed = (System.nanoTime() - start) / 1_000_000L;
      printf("total time: %s", StringUtils.toStringDuration(elapsed));
      updateSlaveNodes(jppfClient, 0);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Process the results of a job.
   * @param job the JPPF job whose results are printed.
   */
  public static void processResults(final JPPFJob job) {
    printf("*** results for job '%s' ***", job.getName());
    List<Task<?>> results = job.getAllResults();
    for (Task<?> task: results) {
      if (task.getThrowable() != null) printf("%s raised an exception : %s", task.getId(), ExceptionUtils.getMessage(task.getThrowable()));
      //else System.out.printf("result of %s : %s\n", task.getId(), task.getResult());
    }
  }

  /**
   * Ensure that the JPPF client has the specified number of connections.
   * @param client the jppf client.
   * @param nbConnections the desired number of connections.
   * @throws Exception if any error occurs.
   */
  private static void ensureSufficientConnections(final JPPFClient client, final int nbConnections) throws Exception {
    printf("***** ensuring %d connections ...", nbConnections);
    JPPFConnectionPool pool;
    while ((pool = client.getConnectionPool()) == null) Thread.sleep(1L);
    printf("***** ensuring %d connections, found pool = %s", nbConnections, pool);
    pool.setMaxSize(nbConnections);
    printf("***** ensuring %d connections, called setSize(%d)", nbConnections, nbConnections);
    while (pool.connectionCount(JPPFClientConnectionStatus.ACTIVE) < nbConnections) Thread.sleep(1L);
    printf("***** ensuring %d connections, after pool.await()", nbConnections);
  }

  /**
   * Update the number of running slave nodes.
   * @param client the JPPF client to get a JMX connection from.
   * @param nbSlaves the number of slave nodes to reach.
   * @throws Exception if any error occurs.
   */
  private static void updateSlaveNodes(final JPPFClient client, final int nbSlaves) throws Exception {
    printf("ensuring %d slaves ...", nbSlaves);
    JMXDriverConnectionWrapper jmx = getJmxConnection(client);
    if (jmx.nbNodes() == nbSlaves + 1) return;
    JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
    NodeSelector masterSelector = new NodeSelector.ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));
    // request that <nbSlaves> slave nodes be provisioned
    long start = System.nanoTime();
    //forwarder.provisionSlaveNodes(masterSelector, nbSlaves, null);
    forwarder.forwardInvoke(masterSelector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] {nbSlaves, null}, new String[] {"int", TypedProperties.class.getName()});
    while (jmx.nbNodes() != nbSlaves + 1) Thread.sleep(10L);
    long elapsed = (System.nanoTime() - start) / 1_000_000L;
    printf("slaves confirmation wait time: %s", StringUtils.toStringDuration(elapsed));
  }

  /**
   * Request that a node be shutdown.
   * @param client the JPPF client to get a JMX connection from.
   * @throws Exception if any error occurs.
   */
  private static void requestNodeShutdown(final JPPFClient client) throws Exception {
    printf("requesting node shutdown ...");
    JMXDriverConnectionWrapper jmx = getJmxConnection(client);
    NodeSelector selector = new NodeSelector.ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));
    jmx.getNodeForwarder().shutdown(selector);
  }

  /**
   * Get a JMX connectionf rom the specified client.
   * @param client the client ot get the connection from.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  static synchronized JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    if (jmx == null) {
      JPPFConnectionPool pool;
      while ((pool = client.getConnectionPool()) == null) Thread.sleep(1L);
      while ((jmx = pool.getJmxConnection(true)) == null) Thread.sleep(1L);
    }
    return jmx;
  }

  /**
   * Submit a job with a single task wich triggers a deadlock int he node where it executes.
   * @param client the client which submits the job.
   * @throws Exception if any error occurs.
   */
  private static void submitDeadlockingJob(final JPPFClient client) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName("Deadlocking job");
    job.setBlocking(false);
    job.add(new DeadlockingTask());
    client.submitJob(job);
  }

  /**
   * Print and log the specified formatted message.
   * @param format the message format.
   * @param params the parameters of the message.
   */
  static void printf(final String format, final Object...params) {
    String msg = String.format(format, params);
    System.out.println(msg);
    log.info(msg);
  }

  /**
   *
   */
  private static class MyNotificationHandler implements NotificationListener {
    /**
     *
     */
    private final JPPFClient client;

    /**
     *
     * @param client .
     */
    public MyNotificationHandler(final JPPFClient client) {
      this.client = client;
    }

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      JobNotification notif = (JobNotification) notification;
      if (notif.getEventType() == JobEventType.JOB_DISPATCHED) {
        try {
          client.cancelJob(notif.getJobInformation().getJobUuid());
          cancelCount.incrementAndGet();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
