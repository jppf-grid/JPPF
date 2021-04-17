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

package org.jppf.test.addons.mbeans;

import java.util.concurrent.atomic.*;

import javax.management.*;

import org.jppf.utils.concurrent.DeadlockDetector;
import org.slf4j.*;

/**
 * Implementation of {@link NodeTestMBean}.
 * @author Laurent Cohen
 */
public class NodeTest extends NotificationBroadcasterSupport implements NodeTestMBean {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeTest.class);
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Notification sequence number.
   */
  private static AtomicLong sequence = new AtomicLong(0L);
  /**
   * Counts the number of registered listeners.
   */
  final AtomicInteger nbListeners = new AtomicInteger(0);

  /**
   * Default constructor.
   */
  public NodeTest() {
    System.out.println("initialized NodeTest");
    DeadlockDetector.setup("node");
  }

  @Override
  public void sendUserObject(final Object userObject) throws Exception {
    final Notification notif = new Notification("NodeTest", NodeTestMBean.MBEAN_NAME, sequence.incrementAndGet());
    notif.setUserData(userObject);
    sendNotification(notif);
  }

  @Override
  public long getTotalNotifications() throws Exception {
    return sequence.get();
  }

  @Override
  public synchronized void sendNotification(final Notification notif) {
    final String message = String.format("sending notification to %d listeners: type=%s, sequence=%d, userData=%s", nbListeners.get(), notif.getType(), notif.getSequenceNumber(), notif.getUserData());
    System.out.println(message);
    log.info(message);
    super.sendNotification(notif);
  }

  @Override
  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    super.addNotificationListener(listener, filter, handback);
    log.info("registered notification listener = {}, filter = {}, handback = {}, nbListeners = {}", listener, filter, handback, nbListeners.incrementAndGet());
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    super.removeNotificationListener(listener);
    log.info("unregistered notification listener = {}, nbListeners = {}", listener, nbListeners.decrementAndGet());
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
    super.removeNotificationListener(listener, filter, handback);
    log.info("unregistered notification listener = {}, filter = {}, handback = {}, nbListeners = {}", listener, filter, handback, nbListeners.decrementAndGet());
  }
}
