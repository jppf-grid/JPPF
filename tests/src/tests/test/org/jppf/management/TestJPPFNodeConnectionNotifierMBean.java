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

import static org.junit.Assert.*;

import java.util.*;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
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
  private final List<Notification> notifList = new ArrayList<>();

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
  public void testConnectionNotifications() throws Exception {
    int nbSlaves = 2;
    Thread.sleep(1000L);
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    driver.addNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, this);
    JPPFNodeForwardingMBean forwarder = driver.getNodeForwarder();
    forwarder.provisionSlaveNodes(NodeSelector.ALL_NODES, nbSlaves);
    while (driver.nbNodes() < nbSlaves + 1) Thread.sleep(10L);
    forwarder.provisionSlaveNodes(NodeSelector.ALL_NODES, 0);
    while (driver.nbNodes() > 1) Thread.sleep(10L);
    synchronized(notifList) {
      while (notifList.size() < 2 * nbSlaves) notifList.wait(10L);
    }
    driver.removeNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, this);
    int connectedCount = 0;
    int disconnectedCount = 0;
    for (Notification notif: notifList) {
      assertEquals(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, notif.getSource());
      switch(notif.getType()) {
        case JPPFNodeConnectionNotifierMBean.CONNECTED:
          connectedCount++;
          break;
        case JPPFNodeConnectionNotifierMBean.DISCONNECTED:
          disconnectedCount++;
          break;
        default:
          throw new IllegalStateException(String.format("notification has an invalid type: %s", notif));
      }
      assertTrue(notif.getUserData() instanceof JPPFManagementInfo);
    }
    assertEquals(nbSlaves, connectedCount);
    assertEquals(nbSlaves, disconnectedCount);
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    synchronized(notifList) {
      notifList.add(notification);
    }
  }
}
