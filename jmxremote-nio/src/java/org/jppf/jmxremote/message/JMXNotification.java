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

package org.jppf.jmxremote.message;

import java.util.Arrays;

import javax.management.Notification;

/**
 * A specialized message that represents a JMX notification to dispatch client-side.
 * @author Laurent Cohen
 */
public class JMXNotification extends AbstractJMXMessage {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The notification to dispatch.
   */
  private final Notification notification;
  /**
   * The ids of the listeners to dispatch to.
   */
  private final Integer[] listenerIDs;

  /**
   * Initialize this request with the specified ID, request type and parameters.
   * @param messageID the message id.
   * @param notification the notification to dispatch.
   * @param listenerIDs ids of the listeners to dispatch the notification to.
   */
  public JMXNotification(final long messageID, final Notification notification, final Integer[] listenerIDs) {
    super(messageID, JMXMessageType.NOTIFICATION);
    this.notification = notification;
    this.listenerIDs = listenerIDs;
  }

  /**
   * @return the actual notification.
   */
  public Notification getNotification() {
    return notification;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("messageID=").append(messageID)
      .append(", messageType=").append(messageType)
      .append(", listenerIDs=").append(Arrays.asList(listenerIDs))
      .append(", notification=").append(Arrays.asList(notification))
      .append(']').toString();
  }

  /**
   * @return the ids of the listeners to dispatch to.
   */
  public Integer[] getListenerIDs() {
    return listenerIDs;
  }
}
