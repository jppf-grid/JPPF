/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package test.org.jppf.node.provisioning;

import static org.junit.Assert.*;

import java.util.*;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.management.forwarding.*;
import org.jppf.node.protocol.ScriptedTask;
import org.jppf.node.provisioning.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D1N1C;

/**
 * Unit tests for the {@link ScriptedTask} class.
 * @author Laurent Cohen
 */
public class TestJPPFNodeProvisioning extends Setup1D1N1C {
  /**
   * Test the provisioning using a direct JMX connection to the node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 8000)
  public void testDirectNodeConnection() throws Exception {
    final Pair<JMXDriverConnectionWrapper, JPPFManagementInfo> pair = getManagementInfo();
    final JMXDriverConnectionWrapper jmxDriver = pair.first();
    final JPPFManagementInfo info = pair.second();
    final JMXNodeConnectionWrapper nodeJmx = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
    assertTrue(nodeJmx.connectAndWait(5000L));
    final NodeProvisioningListener listener = new NodeProvisioningListener(false);
    nodeJmx.addNotificationListener(JPPFNodeProvisioningMBean.MBEAN_NAME, listener);
    final JPPFNodeProvisioningMBean provisioner = nodeJmx.getJPPFNodeProvisioningProxy();
    assertNotNull(provisioner);
    provisioner.provisionSlaveNodes(2);
    assertTrue(ConcurrentUtils.awaitCondition(() -> listener.notifs.size() == 2, 5000L, 250L, false));
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmxDriver.nbNodes() == 3, 5000L, 250L, false));
    checkNotifs(listener, JPPFNodeProvisioningMBean.SLAVE_STARTED_NOTIFICATION_TYPE, info);
    listener.notifs.clear();
    provisioner.provisionSlaveNodes(0);
    assertTrue(ConcurrentUtils.awaitCondition(() -> listener.notifs.size() == 2, 5000L, 250L, false));
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmxDriver.nbNodes() == 1, 5000L, 250L, false));
    checkNotifs(listener, JPPFNodeProvisioningMBean.SLAVE_STOPPED_NOTIFICATION_TYPE, info);
    nodeJmx.removeNotificationListener(JPPFNodeProvisioningMBean.MBEAN_NAME, listener);
  }

  /**
   * Test the provisioning using a JMX forwarding through the driver.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 8000)
  public void testViaNodeForwarding() throws Exception {
    final Pair<JMXDriverConnectionWrapper, JPPFManagementInfo> pair = getManagementInfo();
    final JMXDriverConnectionWrapper jmxDriver = pair.first();
    final JPPFManagementInfo info = pair.second();
    final NodeProvisioningListener listener = new NodeProvisioningListener(true);
    final NodeSelector selector = new UuidSelector(info.getUuid());
    final String listenerID = jmxDriver.registerForwardingNotificationListener(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, listener, null, null);
    print(false, false, "got listenerID = %s", listenerID);
    final JPPFNodeForwardingMBean forwarder = jmxDriver.getNodeForwarder();
    assertNotNull(forwarder);
    forwarder.provisionSlaveNodes(selector, 2);
    assertTrue(ConcurrentUtils.awaitCondition(() -> listener.notifs.size() == 2, 5000L, 250L, false));
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmxDriver.nbNodes() == 3, 5000L, 250L, false));
    checkNotifs(listener, JPPFNodeProvisioningMBean.SLAVE_STARTED_NOTIFICATION_TYPE, info);
    listener.notifs.clear();
    forwarder.provisionSlaveNodes(selector, 0);
    assertTrue(ConcurrentUtils.awaitCondition(() -> listener.notifs.size() == 2, 5000L, 250L, false));
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmxDriver.nbNodes() == 1, 5000L, 250L, false));
    checkNotifs(listener, JPPFNodeProvisioningMBean.SLAVE_STOPPED_NOTIFICATION_TYPE, info);
    print(false, false, "unregistered forwarding listeners: %s", jmxDriver.unregisterAllForwardingNotificationListeners());
  }

  /**
   * Test the provisioning using a direct JMX connection to the node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 8000)
  public void testConfigOverrides() throws Exception {
    final Pair<JMXDriverConnectionWrapper, JPPFManagementInfo> pair = getManagementInfo();
    final JMXDriverConnectionWrapper jmxDriver = pair.first();
    final JPPFManagementInfo info = pair.second();
    final JPPFNodeForwardingMBean forwarder = jmxDriver.getNodeForwarder();
    assertNotNull(forwarder);
    final TypedProperties overrides = new TypedProperties().setString("prop.string", "string value").setInt("prop.int", 11);
    final NodeSelector selector = new UuidSelector(info.getUuid());
    final NodeProvisioningListener listener = new NodeProvisioningListener(true);
    final String listenerID = jmxDriver.registerForwardingNotificationListener(selector, JPPFNodeProvisioningMBean.MBEAN_NAME, listener, null, null);
    print(false, false, "got listenerID = %s", listenerID);
    forwarder.provisionSlaveNodes(selector, 2, overrides);
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmxDriver.nbNodes() == 3, 5000L, 250L, false));
    final Map<String, Object> resultMap = forwarder.systemInformation(NodeSelector.ALL_NODES);
    for (final Map.Entry<String, Object> entry: resultMap.entrySet()) {
      assertFalse(entry.getValue() instanceof Throwable);
      final String uuid = entry.getKey();
      final JPPFSystemInformation sysInfo = (JPPFSystemInformation) entry.getValue();
      final TypedProperties config = sysInfo.getJppf();
      final boolean master = info.getUuid().equals(uuid);
      for (final Object o: overrides.keySet()) {
        assertTrue(o instanceof String);
        final String key = (String) o;
        final String value = overrides.getProperty(key);
        if (master) {
          assertNull("property " + key + " should be null", config.getProperty(key));
        } else {
          final String configValue = config.getProperty(key);
          assertNotNull("property " + key + " should not be null", configValue);
          assertEquals(value, configValue);
        }
      }
    }
    listener.notifs.clear();
    forwarder.provisionSlaveNodes(selector, 0);
    assertTrue(ConcurrentUtils.awaitCondition(() -> listener.notifs.size() == 2, 5000L, 250L, false));
    assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> jmxDriver.nbNodes() == 1, 5000L, 250L, false));
    print(false, false, "unregistered forwarding listeners: %s", jmxDriver.unregisterAllForwardingNotificationListeners());
  }

  /**
   * 
   * @return a pairing of {@link JMXDriverConnectionWrapper} and {@link JPPFManagementInfo} for the node.
   * @throws Exception if any erorr occurs.
   */
  private static Pair<JMXDriverConnectionWrapper, JPPFManagementInfo> getManagementInfo() throws Exception {
    final JMXDriverConnectionWrapper jmxDriver = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    assertTrue(jmxDriver.isConnected());
    assertEquals(1, (int) jmxDriver.nbNodes());
    final Collection<JPPFManagementInfo> infos = jmxDriver.nodesInformation();
    assertNotNull(infos);
    assertEquals(1, infos.size());
    final JPPFManagementInfo info = infos.iterator().next();
    return new Pair<>(jmxDriver, info);
  }

  /**
   * Check the received notifications.
   * @param listener the ilstener who captured the notifications.
   * @param notifType the expceted notification type.
   * @param masterInfo information on the master node.
   * @throws Exception if any error occcurs.
   */
  private static void checkNotifs(final NodeProvisioningListener listener, final String notifType, final JPPFManagementInfo masterInfo) throws Exception {
    for (final Notification notif: listener.notifs) {
      assertEquals(notifType, notif.getType());
      final JPPFProvisioningInfo pInfo = (JPPFProvisioningInfo) notif.getUserData();
      assertEquals(masterInfo.getUuid(), pInfo.getMasterUuid());
      assertEquals(JPPFNodeProvisioningMBean.SLAVE_STOPPED_NOTIFICATION_TYPE.equals(notifType) ? 0 : -1, pInfo.getExitCode());
    }
  }

  /** */
  private static class NodeProvisioningListener implements NotificationListener {
    /**
     * Captures the notifications received by this listener.
     */
    final List<Notification> notifs = new Vector<>();
    /** */
    final boolean forwarded;

    /**
     * @param forwarded .
     */
    public NodeProvisioningListener(final boolean forwarded) {
      this.forwarded = forwarded;
    }

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      final Notification notif = forwarded ? ((JPPFNodeForwardingNotification) notification).getNotification() : notification;
      notifs.add(notif);
      final JPPFProvisioningInfo info = (JPPFProvisioningInfo) notif.getUserData();
      print(false, false, "received provisiong notification %s with %s", notif, info);
    }
  }
}
