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

package org.jppf.management.forwarding;

import javax.management.*;

/**
 * This notification filter wraps the user-defined filter and determines whether
 * notifications should be sent to the JMX client based, on the associated node
 * selector and MBean name, in addition to the wrapped filter's logic.
 * @author Laurent Cohen
 * @exclude
 */
public class InternalNotificationFilter implements NotificationFilter
{
  /**
   * The ID of the associated listener.
   */
  private final String listenerID;
  /**
   * The user-provided notification filter.
   */
  private final NotificationFilter filter;

  /**
   * Initialize this filter.
   * @param listenerID ID of the associated listener.
   * @param filter user-provided notification filter.
   */
  public InternalNotificationFilter(final String listenerID, final NotificationFilter filter)
  {
    if (listenerID == null) throw new IllegalArgumentException("listener ID cannot be null");
    this.listenerID = listenerID;
    this.filter = filter;
  }

  @Override
  public boolean isNotificationEnabled(final Notification notification)
  {
    if (!(notification instanceof JPPFNodeForwardingNotification)) return false;
    JPPFNodeForwardingNotification notif = (JPPFNodeForwardingNotification) notification;
    NodeForwardingHelper helper = NodeForwardingHelper.getInstance();
    String uuid = notif.getNodeUuid();
    NotificationListenerWrapper wrapper = helper.getListener(listenerID);
    return
        helper.isNodeAccepted(uuid, wrapper.getSelector()) &&
        wrapper.getMBeanName().equals(notif.getMBeanName()) &&
        ((filter == null) || filter.isNotificationEnabled(notif.getNotification()));
  }
}
