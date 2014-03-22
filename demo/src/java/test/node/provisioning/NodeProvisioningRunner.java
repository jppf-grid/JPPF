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

package test.node.provisioning;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.*;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.*;

/**
 * Runs a simple demo which requests that 4 slaves be sarted on each master,
 * runs a job on the slaves only, then requests that all slaves be terminated.
 * @author Laurent Cohen
 */
public class NodeProvisioningRunner {
  /**
   * Entry point ofr this application.
   * @param args not used.
   */
  public static void main(final String[] args) {
    JPPFClient client = null;
    try {
      client = new JPPFClient();
      // wait until a JMX connection to the driver is properly established
      JPPFClientConnection connection = null;
      while ((connection = client.getClientConnection()) == null) Thread.sleep(10L);
      //while (!client.hasAvailableConnection()) Thread.sleep(10L);
      JMXDriverConnectionWrapper jmxDriver = null;
      while ((jmxDriver = connection.getJmxConnection()) == null) Thread.sleep(10L);
      while (!jmxDriver.isConnected()) Thread.sleep(10L);
      // get a proxy to the mbean that forwards management requests to the nodes
      JPPFNodeForwardingMBean forwarder = jmxDriver.getProxy(JPPFNodeForwardingMBean.MBEAN_NAME, JPPFNodeForwardingMBean.class);

      // create a node selector that only selects master nodes
      ExecutionPolicy masterPolicy = new Equal("jppf.node.provisioning.master", true);
      NodeSelector masterSelector = new NodeSelector.ExecutionPolicySelector(masterPolicy);
      
      // specify configuration overrides for the new slave nodes
      TypedProperties overrides = new TypedProperties();
      overrides.setInt("jppf.processing.threads", 2);
      overrides.setString("jppf.jvm.options", "-server -Xmx256m -Djava.util.logging.config.file=logging-node.properties");
      System.out.println("requesting 4 new slaves ...");
      // the signature of the provisioning mbean method to invoke
      String[] signature = {int.class.getName(), TypedProperties.class.getName()};
      String mbeanName = JPPFNodeProvisioningMBean.MBEAN_NAME;
      // actually request that 4 slave nodes be provisioned
      Object o = forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", new Object[] { 4, overrides }, signature);
      Thread.sleep(3000L);
      // now we check that we have effectively 4 slave nodes for each master
      Map<String, Object> resultsMap = forwarder.forwardGetAttribute(masterSelector, mbeanName, "NbSlaves");
      for (Map.Entry<String, Object> entry: resultsMap.entrySet()) {
        if (entry.getValue() instanceof Throwable) System.out.println("node " + entry.getKey() + " raised " + ExceptionUtils.getStackTrace((Throwable) entry.getValue()));
        else System.out.println("master node " + entry.getKey() + " has " + entry.getValue() + " slaves");
      }

      // now we submit a job executed on the slaves
      System.out.println("submitting job ...");
      JPPFJob job = new JPPFJob();
      job.setName("Hello World");
      for (int i=1; i<=20; i++) job.add(new ExampleTask()).setId("task " + i);
      // set the policy to execute on slaves only
      ExecutionPolicy slavePolicy = ExecutionPolicy.Not(masterPolicy);
      job.getSLA().setExecutionPolicy(slavePolicy);
      List<Task<?>> results = client.submitJob(job);
      System.out.println("got " + results.size() + " results for job");
      // ... do whatever with the results ...

      // finally, we terminate the slave nodes with a provisioning request for 0 nodes
      System.out.println("shutting down all slaves ...");
      forwarder.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", new Object[] { 0, null }, signature);
      Thread.sleep(3000L);
      // and we check that each master now has zero slave nodes
      resultsMap = forwarder.forwardGetAttribute(masterSelector, mbeanName, "NbSlaves");
      for (Map.Entry<String, Object> entry: resultsMap.entrySet()) {
        if (entry.getValue() instanceof Throwable) System.out.println("node " + entry.getKey() + " raised " + ExceptionUtils.getStackTrace((Throwable) entry.getValue()));
        else System.out.println("node " + entry.getKey() + " has " + entry.getValue() + " slaves");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
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
