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

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.Equal;
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

  /**
   * Entry point for this demo.
   * @param args the first argument is a function number that determines which method to call.
   */
  public static void main(final String[] args) {
    try {
      DeadlockRunner runner = new DeadlockRunner();
      runner.jobStreaming();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Execute a stream of non-blocking jobs from a single thread, process the results asynchronously.
   */
  public void jobStreaming() {
    TypedProperties config = JPPFConfiguration.getProperties();
    int concurrencyLimit = config.getInt("deadlock.concurrencyLimit", 4);
    int nbJobs = config.getInt("deadlock.nbJobs", 10);
    int tasksPerJob = config.getInt("deadlock.tasksPerJob", 10);
    long taskDuration = config.getLong("deadlock.taskDuration", 10L);
    long waitTime = config.getLong("deadlock.waitTime", 1000L);
    System.out.printf("Running with conccurencyLimit=%d, nbJobs=%d, tasksPerJob=%d, taskDuration=%d\n", concurrencyLimit, nbJobs, tasksPerJob, taskDuration);
    try (JPPFClient client = new JPPFClient();
        JobProvider jobProvider = new JobProvider(concurrencyLimit, nbJobs, tasksPerJob, taskDuration)) {
      ensureSufficientConnections(client, concurrencyLimit);
      ProvisioningThread pt = new ProvisioningThread(client, waitTime);
      MasterNodeMonitoringThread mnmt = new MasterNodeMonitoringThread(client, 5000L, pt);
      try {
        new Thread(pt, "ProvisioningThread").start();
        new Thread(mnmt, "MasterNodeMonitoringThread").start();
        for (JPPFJob job: jobProvider) {
          if (job != null) client.submitJob(job);
        }
        while (jobProvider.hasPendingJob()) Thread.sleep(10L);
      } finally {
        pt.setStopped(true);
        mnmt.setStopped(true);
      }
      System.out.printf("*** executed a total of %d jobs and %d tasks\n", jobProvider.getJobCount(), jobProvider.getTaskCount());
    } catch (Exception e) {
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
      //if (task.getThrowable() != null) System.out.printf("%s raised an exception : %s\n", task.getId(), ExceptionUtils.getMessage(task.getThrowable()));
      if (task.getThrowable() != null) System.out.printf("%s raised an exception : %s\n", task.getId(), ExceptionUtils.getStackTrace(task.getThrowable()));
      //else System.out.printf("result of %s : %s\n", task.getId(), task.getResult());
    }
  }

  /**
   * Ensure that the JPPF client has the specified number of connections.
   * @param client the jppf client.
   * @param nbConnections the desried number of connections.
   * @throws Exception if any error occurs.
   */
  private static void ensureSufficientConnections(final JPPFClient client, final int nbConnections) throws Exception {
    JPPFConnectionPool pool = null;
    while ((pool = client.getConnectionPool()) == null) Thread.sleep(10L);
    pool.setMaxSize(nbConnections);
    while (pool.connectionCount(JPPFClientConnectionStatus.ACTIVE) < nbConnections) Thread.sleep(10L);
    //updateSlaveNodes(jppfClient, nbConnections - 1);
  }

  /**
   * Update the number of running slave nodes.
   * @param client the JPPF client to get a JMX connection from.
   * @param nbSlaves the number of slave nodes to reach.
   * @throws Exception if any error occurs.
   */
  private static void updateSlaveNodes(final JPPFClient client, final int nbSlaves) throws Exception {
    JPPFNodeForwardingMBean forwarder = client.getConnectionPool().getJmxConnection().getNodeForwarder();
    String mbeanName = JPPFNodeProvisioningMBean.MBEAN_NAME;
    Object[] params = { nbSlaves, null };
    NodeSelector masterSelector = new NodeSelector.ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));
    // request that <nbSlaves> slave nodes be provisioned
    forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", params, new String[] {int.class.getName(), TypedProperties.class.getName()});
  }

  /**
   * Get a JMX connectionf rom the specified client.
   * @param client the client ot get the connection from.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  static synchronized JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    if (jmx == null) {
      JPPFConnectionPool pool = null;
      log.info("getting pool");
      List<JPPFConnectionPool> list = null;
      while ((list = client.findConnectionPools(JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)).isEmpty()) Thread.sleep(1L);
      pool = list.get(0);
      log.info("getting jmx connection");
      while ((jmx = pool.getJmxConnection()) == null)  Thread.sleep(1L);
      while (!jmx.isConnected()) Thread.sleep(1L);
    }
    return jmx;
  }
}
