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

/**
 * An illustration of the patterns for submitting multiple jobs in parallel.
 */
public class DeadlockRunner {

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
    TypedProperties config = JPPFConfiguration.getProperties();
    int concurrencyLimit = config.getInt("deadlock.concurrencyLimit", 4);
    int[] nbSlaves = StringUtils.parseIntValues(config.getString("deadlock.nbSlaves", "0"));
    int nbJobs = config.getInt("deadlock.nbJobs", 10);
    int tasksPerJob = config.getInt("deadlock.tasksPerJob", 10);
    long taskDuration = config.getLong("deadlock.taskDuration", 10L);
    System.out.printf("Running with conccurencyLimit=%d, nbJobs=%d, tasksPerJob=%d, taskDuration=%d\n", concurrencyLimit, nbJobs, tasksPerJob, taskDuration);
    try (JPPFClient client = new JPPFClient();
        JobStreamImpl jobProvider = new JobStreamImpl(concurrencyLimit, nbJobs, tasksPerJob, taskDuration)) {
      ensureSufficientConnections(client, concurrencyLimit);
      //for (int n: nbSlaves) updateSlaveNodes(client, n);
      TimeMarker marker = new TimeMarker().start();
      for (JPPFJob job: jobProvider) {
        if (job != null) client.submitJob(job);
        //requestNodeShutdown(client);
      }
      while (jobProvider.hasPendingJob()) Thread.sleep(10L);
      System.out.printf("*** executed a total of %d jobs and %d tasks in %s\n", jobProvider.getJobCount(), jobProvider.getTaskCount(), marker.stop().getLastElapsedAsString());
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
   * @param jppfClient the jppf client.
   * @param nbConnections the desried number of connections.
   * @throws Exception if any error occurs.
   */
  private static void ensureSufficientConnections(final JPPFClient jppfClient, final int nbConnections) throws Exception {
    System.out.printf("ensuring %d connections ...\n", nbConnections);
    jppfClient.awaitActiveConnectionPool().awaitActiveConnections(nbConnections);
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
    NodeSelector masterSelector = new NodeSelector.ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));
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
    NodeSelector selector = new NodeSelector.ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));
    forwarder.forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "shutdown", new Object[] {false}, new String[] {Boolean.class.getName()});
  }
}
