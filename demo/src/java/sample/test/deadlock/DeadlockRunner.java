/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
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
  //private static JPPFConnectionPool pool = null;

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
    RunOptions ro = new RunOptions();
    ro.jobCreationCallback = new JobCreationCallback() {
      @Override
      public void jobCreated(final JPPFJob job) {
        //job.getSLA().setMaxNodeProvisioningGroups(1);
        /*
        ExecutionPolicy policy = new Equal("jppf.node.provisioning.slave", true).and(new Equal("job.uuid", false, job.getUuid()));
        job.getSLA().setExecutionPolicy(policy);
        job.getSLA().setSuspended(true);
        */
      }
    };
    System.out.printf("Running with conccurencyLimit=%d, nbJobs=%d, tasksPerJob=%d, taskDuration=%d\n", ro.concurrencyLimit, ro.nbJobs, ro.tasksPerJob, ro.taskDuration);
    ProvisioningThread pt = null;
    MasterNodeMonitoringThread mnmt = null;
    try (JPPFClient client = new JPPFClient(); JobStreamImpl jobProvider = new JobStreamImpl(ro)) {
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
        TimeMarker marker = new TimeMarker().start();
        int count = 0;
        for (JPPFJob job: jobProvider) {
          if (job != null) client.submitJob(job);
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
        System.out.printf("*** executed a total of %,d jobs and %,d tasks in %s\n", jobProvider.getJobCount(), jobProvider.getTaskCount(), marker.stop().getLastElapsedAsString());
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
      TimeMarker marker = new TimeMarker().start();
      for (int n: nbSlaves) updateSlaveNodes(jppfClient, n);
      System.out.printf("total time: %s\n", marker.stop().getLastElapsedAsString());
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
    System.out.printf("*** results for job '%s' ***\n", job.getName());
    List<Task<?>> results = job.getAllResults();
    for (Task<?> task: results) {
      if (task.getThrowable() != null) System.out.printf("%s raised an exception : %s\n", task.getId(), ExceptionUtils.getMessage(task.getThrowable()));
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
    System.out.printf("ensuring %d connections ...\n", nbConnections);
    client.awaitActiveConnectionPool().awaitActiveConnections(Operator.AT_LEAST, nbConnections);
  }

  /**
   * Update the number of running slave nodes.
   * @param client the JPPF client to get a JMX connection from.
   * @param nbSlaves the number of slave nodes to reach.
   * @throws Exception if any error occurs.
   */
  private static void updateSlaveNodes(final JPPFClient client, final int nbSlaves) throws Exception {
    System.out.printf("ensuring %d slaves ...\n", nbSlaves);
    JMXDriverConnectionWrapper jmx = client.getConnectionPool().getJmxConnection();
    if (jmx.nbNodes() == nbSlaves + 1) return;
    JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
    String mbeanName = JPPFNodeProvisioningMBean.MBEAN_NAME;
    Object[] params = { nbSlaves, null };
    String[] sig = new String[] {int.class.getName(), TypedProperties.class.getName()};
    NodeSelector masterSelector = new ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));
    // request that <nbSlaves> slave nodes be provisioned
    TimeMarker marker = new TimeMarker().start();
    forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", params, sig);
    while (jmx.nbNodes() != nbSlaves + 1) Thread.sleep(10L);
    System.out.printf("slaves confirmation wait time: %s\n", marker.stop().getLastElapsedAsString());
  }

  /**
   * Request that a node be shutdown.
   * @param client the JPPF client to get a JMX connection from.
   * @throws Exception if any error occurs.
   */
  private static void requestNodeShutdown(final JPPFClient client) throws Exception {
    System.out.println("requesting node shutdown ...");
    JMXDriverConnectionWrapper jmx = client.getConnectionPool().getJmxConnection();
    JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
    NodeSelector selector = new ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));
    forwarder.forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "shutdown", new Object[] {false}, new String[] {Boolean.class.getName()});
  }

  /**
   * Get a JMX connectionf rom the specified client.
   * @param client the client ot get the connection from.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  static synchronized JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    if (jmx == null) {
      JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      jmx = pool.awaitJMXConnections(Operator.AT_LEAST, 2, true).get(0);
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
}
