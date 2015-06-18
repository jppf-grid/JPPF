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

package test.node.provisioning;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.*;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.ExceptionUtils;

import sample.dist.tasklength.LongTask;

/**
 * Runs a simple demo which requests that 4 slaves be sarted on each master,
 * runs a job on the slaves only, then requests that all slaves be terminated.
 * @author Laurent Cohen
 */
public class NodeProvisioningRunner {
  /**
   * 
   */
  private static String[] signature = {int.class.getName(), boolean.class.getName()};
  /**
   * 
   */
  private static String mbeanName = JPPFNodeProvisioningMBean.MBEAN_NAME;
  /**
   * 
   */
  private static NodeSelector masterSelector = new ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));

  /**
   * Entry point ofr this application.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      JMXDriverConnectionWrapper jmxDriver = pool.awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
      JPPFNodeForwardingMBean forwarder = jmxDriver.getNodeForwarder();
      
      int nbSlaves = 3;
      System.out.printf("provisioning %d slaves%n", nbSlaves);
      Object o = forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", new Object[] { nbSlaves, false }, signature);
      Thread.sleep(3000L);
      printNbSlaves(forwarder);

      System.out.println("submitting job ...");
      JPPFJob job = new JPPFJob();
      job.setBlocking(false);
      job.setName("Hello World");
      for (int i=1; i<=4; i++) job.add(new LongTask(30_000L)).setId("task " + i);
      client.submitJob(job);

      Thread.sleep(2000L);
      System.out.printf("provisioning 0 slaves%n");
      forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", new Object[] { 0, false }, signature);      
      Thread.sleep(3000L);
      printNbSlaves(forwarder);
      System.out.printf("driver has %d nodes%n", jmxDriver.nbNodes());

      Thread.sleep(2000L);
      System.out.printf("provisioning %d slaves%n", nbSlaves);
      forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", new Object[] { nbSlaves, false }, signature);      
      Thread.sleep(3000L);
      printNbSlaves(forwarder);
      System.out.printf("driver has %d nodes%n", jmxDriver.nbNodes());

      List<Task<?>> results = job.awaitResults();
      System.out.println("got " + results.size() + " results for job");

      System.out.println("shutting down all slaves ...");
      forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", new Object[] { 0, false }, signature);
      Thread.sleep(3000L);
      printNbSlaves(forwarder);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param forwarder .
   * @throws Exception .
   */
  private static void printNbSlaves(final JPPFNodeForwardingMBean forwarder) throws Exception {
    Map<String, Object> resultsMap = forwarder.forwardGetAttribute(masterSelector, mbeanName, "NbSlaves");
    for (Map.Entry<String, Object> entry: resultsMap.entrySet()) {
      if (entry.getValue() instanceof Throwable) System.out.printf("node %s raised %s%n", entry.getKey(), ExceptionUtils.getStackTrace((Throwable) entry.getValue()));
      else System.out.printf("master node %s has %d slaves%n", entry.getKey(), entry.getValue());
    }
  }

  /**
   * A simple task example.
   */
  public static class ExampleTask extends AbstractTask<String> {
    @Override
    public void run() {
      String message = "hello from " + getId();
      // this should be printed in the stdout.log of the slave node
      System.out.println(message);
      setResult(message);
    }
  }
}
