/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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


import static org.junit.Assert.assertEquals;

import javax.management.*;

import org.apache.log4j.Level;
import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.utils.*;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Tests node restart operations.
 * @author Laurent Cohen
 */
public class TestNodeRestart extends BaseTest {
  /** */
  @Rule
  public TestWatcher setup1D1N1CWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, true, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * Launches a driver and node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    ConfigurationHelper.setLoggerLevel(Level.DEBUG, "org.jppf.node", "org.jppf.server.node");
    ConfigurationHelper.setLoggerLevel(Level.INFO, "org.jppf.client");
    final TestConfiguration config = BaseSetup.DEFAULT_CONFIG.copy();
    config.driver.log4j = CONFIG_ROOT_DIR + "log4j-driver.TestNodeRestart.properties";
    config.node.log4j = CONFIG_ROOT_DIR + "log4j-node.TestNodeRestart.properties";
    client = BaseSetup.setup(1, 1, true, true, config);
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    BaseSetup.cleanup();
  }

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
    RetryUtils.runWithRetryTimeout(5000L, 500L, () -> {
      if (driver.nbIdleNodes() != 1) throw new IllegalStateException("number of nodes should be 1");
      return true;
    });
    final MyNodeConnectionListener myListener = new MyNodeConnectionListener();
    driver.addNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, myListener);
    print(false, false, ">>> getting forwarder");
    final NodeForwardingMBean forwarder = driver.getForwarder();
    print(false, false, ">>> got JPPFNodeForwardingMBean");
    for (int i=0; i<nbRestarts; i++) {
      myListener.reset();
      print(false, false, "<<< restart #%d of the node >>>", i + 1);
      forwarder.restart(NodeSelector.ALL_NODES, true);
      print(false, false, ">>> awaiting CONNECTED state for node");
      myListener.await();
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 0L);
      print(false, false, ">>> submitting job '%s'", job.getName());
      client.submit(job);
      print(false, false, ">>> got job results");
      assertEquals(1, myListener.disconnectedCount);
      assertEquals(1, myListener.connectedCount);
    }
    driver.removeNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, myListener);
  }

  /** */
  public class MyNodeConnectionListener implements NotificationListener {
    /** */
    private String state;
    /** */
    private int connectedCount, disconnectedCount;

    @Override
    public synchronized void handleNotification(final Notification notif, final Object handback) {
      try {
        state = notif.getType();
        if (JPPFNodeConnectionNotifierMBean.CONNECTED.equals(state)) {
          connectedCount++;
          print(false, false, ">>> received node connected notification, connectedCount=%d", connectedCount);
        } else {
          disconnectedCount++;
          print(false, false, ">>> received node disconnected notification, disconnectedCount=%d", disconnectedCount);
        }
      } finally {
        notifyAll();
      }
    }

    /**
     * Wait for the node to be connected to the driver.
     * @throws Exception if any error occurs.
     */
    private synchronized void await() throws Exception {
      while (!JPPFNodeConnectionNotifierMBean.CONNECTED.equals(state)) wait();
    }

    /**
     * Reset this listener's state.
     */
    private synchronized void reset() {
      state = JPPFNodeConnectionNotifierMBean.DISCONNECTED;
      connectedCount = 0;
      disconnectedCount = 0;
    }
  }
}
