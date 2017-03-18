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

import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 * @since 5.1
 * @exclude
 */
public final class JPPFNodeConnectionNotifier extends NotificationBroadcasterSupport implements JPPFNodeConnectionNotifierMBean {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFNodeConnectionNotifier.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
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
    if (debugEnabled) log.debug("sending node connected notification for {}", info);
    notify(info, true);
  }

  /**
   * Called when a node is disconnected from the driver.
   * @param info information about the disconnected node.
   */
  public void onNodeDisconnected(final JPPFManagementInfo info) {
    if (debugEnabled) log.debug("sending node disconnected notification for {}", info);
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

  @Override
  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    if (debugEnabled) log.debug("adding notification listener");
    super.addNotificationListener(listener, filter, handback);
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    if (debugEnabled) log.debug("removing notification listener");
    super.removeNotificationListener(listener);
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
    if (debugEnabled) log.debug("removing notification listener with filter");
    super.removeNotificationListener(listener, filter, handback);
  }
}
