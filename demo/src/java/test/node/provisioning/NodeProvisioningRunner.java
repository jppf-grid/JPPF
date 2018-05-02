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

package test.node.provisioning;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.*;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.Operator;

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
  private static NodeSelector masterSelector = new ExecutionPolicySelector(new Equal("jppf.node.provisioning.master", true));

  /**
   * Entry point ofr this application.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      perform2(client);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param client the lcient to use.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unused")
  private static void perform1(final JPPFClient client) throws Exception {
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    final JMXDriverConnectionWrapper jmxDriver = pool.awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
    final JPPFNodeForwardingMBean forwarder = jmxDriver.getNodeForwarder();
    
    final int nbSlaves = 3;
    System.out.printf("provisioning %d slaves%n", nbSlaves);
    final Object o = forwarder.provisionSlaveNodes(masterSelector, nbSlaves, false);
    Thread.sleep(3000L);
    printNbSlaves(forwarder);

    System.out.println("submitting job ...");
    final JPPFJob job = new JPPFJob();
    job.setBlocking(false);
    job.setName("Hello World");
    for (int i=1; i<=4; i++) job.add(new LongTask(30_000L)).setId("task " + i);
    client.submitJob(job);

    Thread.sleep(2000L);
    System.out.printf("provisioning 0 slaves%n");
    forwarder.provisionSlaveNodes(masterSelector, 0, false);      
    Thread.sleep(3000L);
    printNbSlaves(forwarder);
    System.out.printf("driver has %d nodes%n", jmxDriver.nbNodes());

    Thread.sleep(2000L);
    System.out.printf("provisioning %d slaves%n", nbSlaves);
    forwarder.provisionSlaveNodes(masterSelector, nbSlaves, false);      
    Thread.sleep(3000L);
    printNbSlaves(forwarder);
    System.out.printf("driver has %d nodes%n", jmxDriver.nbNodes());

    final List<Task<?>> results = job.awaitResults();
    System.out.println("got " + results.size() + " results for job");

    System.out.println("shutting down all slaves ...");
    forwarder.provisionSlaveNodes(masterSelector, 0, false);
    Thread.sleep(3000L);
    printNbSlaves(forwarder);
  }

  /**
   * 
   * @param client the lcient to use.
   * @throws Exception if any error occurs.
   */
  private static void perform2(final JPPFClient client) throws Exception {
    final JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitJMXConnection(true);
    final JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
    final int nbSlaves = 10;
    long totalElapsed = 0L;
    for (int i=1; i<=10; i++) {
      System.out.println("*******************");
      System.out.printf("iteration %d: provisioning %d slaves%n", i, nbSlaves);
      long start = System.nanoTime();
      forwarder.provisionSlaveNodes(masterSelector, nbSlaves, true);
      while (jmx.nbIdleNodes() != nbSlaves + 1) Thread.sleep(1L);
      long elapsed = System.nanoTime() - start;
      totalElapsed += elapsed;
      System.out.printf("iteration %d: provisioning %d slaves took %,d ms%n", i, nbSlaves, elapsed/1_000_000L);
      System.out.printf("iteration %d: un-provisioning %d slaves%n", i, nbSlaves);
      start = System.nanoTime();
      forwarder.provisionSlaveNodes(masterSelector, 0, true);
      while (jmx.nbIdleNodes() != 1) Thread.sleep(1L);
      elapsed = System.nanoTime() - start;
      totalElapsed += elapsed;
      System.out.printf("iteration %d: un-provisioning %d slaves took %,d ms%n", i, nbSlaves, elapsed/1_000_000L);
    }
    System.out.println("*******************");
    System.out.printf("total time: %,d ms%n", totalElapsed/1_000_000L);
  }

  /**
   * 
   * @param forwarder .
   * @throws Exception .
   */
  private static void printNbSlaves(final JPPFNodeForwardingMBean forwarder) throws Exception {
    final Map<String, Object> resultsMap = forwarder.getNbSlaves(masterSelector);
    for (final Map.Entry<String, Object> entry: resultsMap.entrySet()) {
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
      final String message = "hello from " + getId();
      // this should be printed in the stdout.log of the slave node
      System.out.println(message);
      setResult(message);
    }
  }
}
