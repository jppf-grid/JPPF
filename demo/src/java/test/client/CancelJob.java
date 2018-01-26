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

package test.client;

import java.io.Serializable;
import java.lang.management.ManagementFactory;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;

/**
 *
 * @author Laurent Cohen
 */
public class CancelJob {
  /**
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      perform2();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @throws Exception if any error occurs.
   */
  static void perform2() throws Exception {
    final String name = "org.jppf:name=my.test,type=test";
    final ObjectName objName = ObjectNameCache.getObjectName(name);
    final int port = 4444;
    JPPFConfiguration.set(JPPFProperties.MANAGEMENT_PORT, port);
    System.out.println("registering MBean");
    final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    final My my = new My();
    mbeanServer.registerMBean(my, objName);
    System.out.println("starting JMXMP server");
    final JMXMPServer server = new JMXMPServer("clientTest", false, JPPFProperties.MANAGEMENT_PORT);
    server.start(CancelJob.class.getClassLoader());
    System.out.println("connecting JMX client");
    try (JMXConnectionWrapper client = new JMXConnectionWrapper("localhost", port, false)) {
      client.connectAndWait(3000L);
      if (!client.isConnected()) throw new IllegalStateException("JMX client not connected");
      System.out.println("registering notification listener");
      final NotificationListener listener = new MyNotifListener();
      client.addNotificationListener(name, listener);
      System.out.println("sending notification");
      final Notification notif = new Notification("type", "source", 1L, "message");
      final JPPFSystemInformation info = new JPPFSystemInformation("uuid", true, false);
      notif.setUserData(info);
      my.sendNotification(notif);
      Thread.sleep(1000L);
      System.out.println("unregistering notification listener");
      client.removeNotificationListener(name, listener);
      System.out.println("closing client and server");
    }
    server.stop();
    System.out.println("done");
  }

  /** */
  public static final class MyNotifListener implements NotificationListener {
    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      System.out.println("got notification: " + notification);
    }
  }

  /** */
  public static interface MyMBean extends Serializable, NotificationEmitter {
  }

  /** */
  public static class My extends NotificationBroadcasterSupport implements MyMBean {
  }
}
