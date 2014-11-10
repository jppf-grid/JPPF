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

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.policy.*;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ProvisioningThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProvisioningThread.class);
  /**
   * 
   */
  private final JPPFClient client;
  /**
   * 
   */
  private final long waitTime;

  /**
   * 
   * @param client the JPPF client.
   * @param waitTime .
   */
  public ProvisioningThread(final JPPFClient client, final long waitTime) {
    this.client = client;
    this.waitTime = waitTime;
  }

  @Override
  public void run() {
    log.info("starting ProvisioningThread, waitTime={}", waitTime);
    JPPFNodeForwardingMBean forwarder = null;
    ExecutionPolicy masterPolicy = new Equal("jppf.node.provisioning.master", true);
    NodeSelector masterSelector = new NodeSelector.ExecutionPolicySelector(masterPolicy);
    String[] sig = { int.class.getName() };
    while (!isStopped()) {
      if (forwarder == null) {
        try {
          JMXDriverConnectionWrapper jmx = DeadlockRunner.getJmxConnection(client);
          log.info("getting forwarder");
          forwarder = jmx.getNodeForwarder();
        } catch (Exception e) {
          e.printStackTrace();
          return;
        }
        log.info("got forwarder");
      }
      if (isStopped()) break;
      goToSleep(1000L);
      if (isStopped()) break;
      try {
        Map<String, Object> map = forwarder.forwardInvoke(masterSelector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] {40}, sig);
        for (Map.Entry<String, Object> entry: map.entrySet()) {
          if (entry.getValue() instanceof Exception) throw (Exception) entry.getValue();
        }
      } catch(Exception e) {
        e.printStackTrace();
        System.exit(1);
        return;
      }
      if (isStopped()) break;
      goToSleep(waitTime);
      if (isStopped()) break;
      try {
        Map<String, Object> map = forwarder.forwardInvoke(masterSelector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] {0}, sig);
        for (Map.Entry<String, Object> entry: map.entrySet()) {
          if (entry.getValue() instanceof Exception) throw (Exception) entry.getValue();
        }
      } catch(Exception e) {
        e.printStackTrace();
        System.exit(1);
        return;
      }
    }
    try {
      forwarder.forwardInvoke(masterSelector, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] {0}, sig);
    } catch(Exception e) {
      //e.printStackTrace();
      return;
    }
  }
}
