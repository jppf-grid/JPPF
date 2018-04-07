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

package test.org.jppf.node;


import static org.junit.Assert.*;

import java.util.concurrent.Callable;

import javax.management.*;

import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Tests node restart operations.
 * @author Laurent Cohen
 */
public class TestNodeRestart extends Setup1D1N1C {
  /**
   * Test that a node can be restarted multiple times in a row without problems.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 20000)
  public void testNodeRestart() throws Exception {
    final int nbRestarts = 5;
    final int nbTasks = 20;
    print(false, false, ">>> getting JMX connection to the driver");
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    print(false, false, ">>> waiting for 1 connected node");
    RetryUtils.runWithRetryTimeout(5000L, 500L, new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        if (driver.nbNodes() != 1) throw new IllegalStateException("number of nodes should be 1");
        return true;
      }
    });
    final MyNodeConnectionListener myListener = new MyNodeConnectionListener();
    driver.addNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, myListener);
    print(false, false, ">>> getting forwarder");
    final JPPFNodeForwardingMBean forwarder = driver.getNodeForwarder();
    print(false, false, ">>> got JPPFNodeForwardingMBean");
    for (int i=0; i<nbRestarts; i++) {
      assertEquals(i, myListener.disconnectedCount);
      assertEquals(i, myListener.connectedCount);
      print(false, false, ">>> restart #%d of the node", i + 1);
      forwarder.restart(NodeSelector.ALL_NODES, true);
      myListener.await();
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 0L);
      client.submitJob(job);
      print(false, false, ">>> got job results");
    }
  }

  /**
   * 
   */
  public class MyNodeConnectionListener implements NotificationListener {
    /**
     * 
     */
    private String state = JPPFNodeConnectionNotifierMBean.DISCONNECTED;
    /**
     * 
     */
    private int connectedCount, disconnectedCount;

    @Override
    public synchronized void handleNotification(final Notification notif, final Object handback) {
      state = notif.getType();
      if (JPPFNodeConnectionNotifierMBean.CONNECTED.equals(notif.getType())) {
        connectedCount++;
        notify();
      } else disconnectedCount++;
    }

    /**
     * Wait for the node to be connected to the driver.
   * @throws Exception if any error occurs.
     */
    public synchronized void await() throws Exception {
      while (!JPPFNodeConnectionNotifierMBean.CONNECTED.equals(state)) wait();
    }
  }
}
