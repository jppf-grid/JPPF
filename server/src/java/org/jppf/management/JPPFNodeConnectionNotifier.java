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

package org.jppf.management;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

/**
 * 
 * @author Laurent Cohen
 * @since 5.1
 * @exclude
 */
public final class JPPFNodeConnectionNotifier extends NotificationBroadcasterSupport implements JPPFNodeConnectionNotifierMBean {
  /**
   * Singleton instance of this class.
   */
  private static final JPPFNodeConnectionNotifier instance = new JPPFNodeConnectionNotifier();
  /**
   * Notifications sequence number.
   */
  private final AtomicLong sequence = new AtomicLong(0L);

  /**
   * Direct instantiation not permitted.
   */
  private JPPFNodeConnectionNotifier() {
  }

  /**
   * Called when a node is connected to the driver.
   * @param info information about the connected node.
   */
  public void onNodeConnected(final JPPFManagementInfo info) {
    notify(info, true);
  }

  /**
   * Called when a node is disconnected from the driver.
   * @param info information about the disconnected node.
   */
  public void onNodeDisconnected(final JPPFManagementInfo info) {
    notify(info, false);
  }

  /**
   * Send a notification that a node is connected to, or disconnected from, the driver.
   * @param info information about the node.
   * @param connected {@code true} to indicate that the node is connected, {@code false} otherwise.
   */
  private void notify(final JPPFManagementInfo info, final boolean connected) {
    Notification notif = new Notification(connected ? CONNECTED : DISCONNECTED, JPPFNodeConnectionNotifierMBean.MBEAN_NAME, sequence.incrementAndGet(), System.currentTimeMillis());
    notif.setUserData(info);
    sendNotification(notif);
  }

  /**
   * Get the singleton instance of this class.
   * @return a {@link JPPFNodeConnectionNotifier} object.
   */
  public static JPPFNodeConnectionNotifier getInstance() {
    return instance;
  }
}
