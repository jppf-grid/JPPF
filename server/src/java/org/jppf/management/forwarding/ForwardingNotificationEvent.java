/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.EventObject;

import javax.management.Notification;

/**
 * Instances of this class represent events emitted each time a JMX notification is received from a node.
 * @author Laurent Cohen
 * @exclude
 */
public class ForwardingNotificationEvent extends EventObject
{
  /**
   * Name of the MBean who emitted the notification.
   */
  private final String mBeanName;
  /**
   * The notification to dispatch.
   */
  private final Notification notification;

  /**
   * Initialize this event.
   * @param nodeUuid the uuid of the node that emitted the event, used as event source.
   * @param mBeanName the name of the MBean who emitted the notification.
   * @param notification the notification to dispatch.
   */
  public ForwardingNotificationEvent(final String nodeUuid, final String mBeanName, final Notification notification)
  {
    super(nodeUuid);
    this.mBeanName = mBeanName;
    this.notification = notification;
  }

  /**
   * Get the the uuid of the node that emitted the event.
   * @return the node uuid as a string.
   */
  public String getNodeUuid()
  {
    return (String) getSource();
  }

  /**
   * Get the name of the MBean who emitted the notification.
   * @return the MBean name as a string.
   */
  public String getMBeanName()
  {
    return mBeanName;
  }

  /**
   * Get the notification to dispatch.
   * @return a {@link Notification} instance.
   */
  public Notification getNotification()
  {
    return notification;
  }
}
