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

package org.jppf.management.forwarding;

import javax.management.*;

/**
 * Convenience interface to use in place of {@link NotificationListener} when regstering a notification listener
 * that receives notifications from a {@link NodeForwardingMBean}.
 * @author Laurent Cohen
 */
@FunctionalInterface
public interface ForwardingNotificationListener extends NotificationListener {
  @Override
  default void handleNotification(Notification notification, Object handback) {
    if (notification instanceof JPPFNodeForwardingNotification) handleNotification((JPPFNodeForwardingNotification) notification, handback);
  }

  /**
   * Called when a {@link JPPFNodeForwardingNotification} occurs.
   * @param notification the notification to handle.
   * @param handback an opaque object which helps the listener to associate information regarding the MBean emitter.
   */
  void handleNotification(JPPFNodeForwardingNotification notification, Object handback);
}
