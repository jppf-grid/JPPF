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

package sample.test.resubmit;

import java.io.File;
import java.util.List;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.*;

/**
 *
 * @author Laurent Cohen
 */
public class ResubmitRunner {
  /**
   * 
   */
  static final String FILE_NAME = "C:/Workspaces/JPPF-b4.2/demo/nodeKilled.txt";
  /**
   * 
   */
  static final int NB_TASKS = 10;
  /**
  *
  */
 private static JMXDriverConnectionWrapper jmx = null;

  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      getJmxConnection(client);
      int nbIterations = 100;
      for (int i=0; i<nbIterations; i++) {
        if (i > 0) System.out.println("");
        System.out.printf("***** Iteration %d/%4%n%n", (i+1), nbIterations);
        perform(client, i);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param client the jppf client
   * @param iteration iteration index.
   * @throws Exception if any error occurs.
   */
  private static void perform(final JPPFClient client, final int iteration) throws Exception {
    File file = new File(FILE_NAME);
    if (file.exists()) file.delete();
    updateSlaveNodes(client, NB_TASKS -1);
    for (int i=0; i<2; i++) {
      if (i > 0) Thread.sleep(3000L);
      JPPFJob job = createJob(2*iteration  + i);
      List<Task<?>> results = client.submitJob(job);
      System.out.printf("'%s' has %d results%n", job.getName(), results.size());
      for (Task<?> task : results) {
        if (task.getThrowable() != null) System.out.printf("got exception for %s: %s%n", task.getId(), ExceptionUtils.getStackTrace(task.getThrowable()));
        else System.out.printf("got result for %s: %s%n", task.getId(), task.getResult());
      }
    }
  }

  /**
   * Create a job.
   * @param index an index to build the job name.
   * @return a {@code JPPFJob} instance.
   * @throws Exception if any error occurs.
   */
  private static JPPFJob createJob(final int index) throws Exception {
    JPPFJob job = new JPPFJob();
    String name = "job" + index;
    job.setName(name);
    job.getSLA().setMaxTaskResubmits(1);
    job.getSLA().setApplyMaxResubmitsUponNodeError(true);
    for (int i=0; i<NB_TASKS; i++) job.add(new MyTask((index % 2 == 0) && (i == NB_TASKS - 1))).setId(name + ":task" + i);
    return job;
  }

  /** Update the number of running slave nodes.
   * @param client the JPPF client to get a JMX connection from.
   * @param nbSlaves the number of slave nodes to reach.
   * @throws Exception if any error occurs. */
  private static void updateSlaveNodes(final JPPFClient client, final int nbSlaves) throws Exception {
    System.out.printf("ensuring %d slave nodes ...%n", nbSlaves);
    JMXDriverConnectionWrapper driverJmx = getJmxConnection(client);
    JPPFNodeForwardingMBean forwarder = driverJmx.getNodeForwarder();
    String mbeanName = JPPFNodeProvisioningMBean.MBEAN_NAME;
    Object[] params = { nbSlaves, null };
    NodeSelector masterSelector = new NodeSelector.ExecutionPolicySelector(
        new Equal("jppf.node.provisioning.master", true).and(new Equal("jppf.management.port", true, "12001")));
    // request that <nbSlaves> slave nodes be provisioned
    forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", params, new String[] { int.class.getName(), TypedProperties.class.getName() });
    int nbNodes = 0;
    while ((nbNodes = driverJmx.nbNodes()) != 1 + nbSlaves) Thread.sleep(50L);
    System.out.printf("got the %d slave nodes%n", nbSlaves);
  }

  /** Get a JMX connectionf rom the specified client.
   * @param client the client to get the connection from.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  static synchronized JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    if (jmx == null) {
      JPPFConnectionPool pool = null;
      List<JPPFConnectionPool> list = null;
      while ((list = client.findConnectionPools(JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)).isEmpty()) Thread.sleep(1L);
      pool = list.get(0);
      while ((jmx = pool.getJmxConnection()) == null) Thread.sleep(1L);
      while (!jmx.isConnected()) Thread.sleep(1L);
    }
    return jmx;
  }
}
