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

package test.org.jppf.management;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for {@link JPPFNodeConnectionNotifierMBean}.
 * In this class, we test that the functionality of the {@code JPPFNodeConnectionNotifierMBean} from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFNodeConnectionNotifierMBean extends AbstractNonStandardSetup implements NotificationListener {
  /**
   *
   */
  private AtomicInteger connectedCount = new AtomicInteger(0);
  /**
   *
   */
  private AtomicInteger disconnectedCount = new AtomicInteger(0);

  /**
   * Launches 1 driver with 1 node attached and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    client = BaseSetup.setup(1, 1, true, createConfig("provisioning"));
  }

  /**
   * Test that notifications of node connections are received.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testCOnnectionNotifications() throws Exception {
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    driver.addNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, this);
    JPPFNodeForwardingMBean forwarder = driver.getNodeForwarder();
    // start 2 slaves
    forwarder.forwardInvoke(NodeSelector.ALL_NODES, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] { 2 }, new String[] { "int" });
    while (driver.nbNodes() < 3) Thread.sleep(10L);
    // stop all slaves
    forwarder.forwardInvoke(NodeSelector.ALL_NODES, JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes", new Object[] { 0 }, new String[] { "int" });
    while (driver.nbNodes() > 1) Thread.sleep(10L);
    Thread.sleep(1500L);
    driver.removeNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, this);
    assertEquals(2, connectedCount.get());
    assertEquals(2, disconnectedCount.get());
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    switch (notification.getType()) {
      case JPPFNodeConnectionNotifierMBean.CONNECTED:
        connectedCount.incrementAndGet();
        break;
      case JPPFNodeConnectionNotifierMBean.DISCONNECTED:
        disconnectedCount.incrementAndGet();
        break;
    }
  }
}
