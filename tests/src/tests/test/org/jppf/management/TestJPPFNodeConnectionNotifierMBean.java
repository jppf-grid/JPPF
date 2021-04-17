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

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.util.*;

import javax.management.*;

import org.apache.log4j.Level;
import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.node.policy.IsMasterNode;
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
    ConfigurationHelper.setLoggerLevel(Level.DEBUG, "org.jppf.node.provisioning");
    client = BaseSetup.setup(1, 1, true, true, createConfig("provisioning"));
  }

  /**
   * Test that notifications of node connections are received.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testConnectionNotifications() throws Exception {
    final long waitTime = 50L;
    final int nbSlaves = 2;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    print("waiting for master node");
    while (driver.nbIdleNodes() < 1) Thread.sleep(waitTime);
    driver.addNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, this);
    final NodeForwardingMBean forwarder = driver.getForwarder();
    final NodeSelector selector = new ExecutionPolicySelector(new IsMasterNode());
    print("provisioning %d slave nodes", nbSlaves);
    forwarder.provisionSlaveNodes(selector, nbSlaves);
    print("waiting for %d slave nodes", nbSlaves);
    while (driver.nbIdleNodes() < nbSlaves + 1) Thread.sleep(waitTime);
    print("waiting for %d connected notifications", nbSlaves);
    while (getNotifListSize() < nbSlaves) Thread.sleep(waitTime);
    print("terminating slave nodes");
    forwarder.provisionSlaveNodes(selector, 0);
    print("waiting for slave nodes termination");
    while (driver.nbIdleNodes() > 1) Thread.sleep(waitTime);
    print("waiting for %d notifications", 2 * nbSlaves);
    while (getNotifListSize() < 2 * nbSlaves) Thread.sleep(waitTime);
    driver.removeNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, this);
    int connectedCount = 0;
    int disconnectedCount = 0;
    for (final Notification notif: notifList) {
      print("notifList[%d] = %s, %s", (connectedCount + disconnectedCount), notif.getType(), notif.getUserData());
      assertEquals(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, notif.getSource());
      switch(notif.getType()) {
        case JPPFNodeConnectionNotifierMBean.CONNECTED:
          connectedCount++;
          break;
        case JPPFNodeConnectionNotifierMBean.DISCONNECTED:
          disconnectedCount++;
          break;
        default:
          throw new IllegalStateException("notification has an invalid type: " + notif);
      }
      assertTrue(notif.getUserData() instanceof JPPFManagementInfo);
    }
    assertEquals(nbSlaves, connectedCount);
    assertEquals(nbSlaves, disconnectedCount);
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    final JPPFManagementInfo info = (JPPFManagementInfo) notification.getUserData();
    print("received '%s' notification for %s", notification.getType(), info);
    if (info.isMasterNode()) return;
    synchronized(notifList) {
      notifList.add(notification);
    }
  }

  /**
   * @return the number of notifications received.
   */
  private int getNotifListSize() {
    synchronized(notifList) {
      return notifList.size();
    }
  }
}
