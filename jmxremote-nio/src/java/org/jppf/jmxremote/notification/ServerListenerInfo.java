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

package org.jppf.jmxremote.notification;

import javax.management.*;

/**
 * Server-side wrapper for a registered notiication listener.
 * @author Laurent Cohen
 */
public class ServerListenerInfo {
  /**
   * A notification filter which accepts all notifications.
   */
  private static final NotificationFilter ACCEPT_ALL = new NotificationFilter() {
    @Override
    public boolean isNotificationEnabled(final Notification notification) {
      return true;
    }
  };
  /**
   * The server-side listener ID.
   */
  private final int listenerID;
  /**
   * The notification filter.
   */
  private final NotificationFilter filter;
  /**
   * The connection ID.
   */
  private final String connectionID;

  /**
   * Initialize with the specified parameters.
   * @param listenerID the server-side listener ID.
   * @param filter the notification filter.
   * @param connectionID the connection ID.
   */
  public ServerListenerInfo(final int listenerID, final NotificationFilter filter, final String connectionID) {
    this.listenerID = listenerID;
    this.filter = (filter == null) ? ACCEPT_ALL : filter;
    this.connectionID = connectionID;
  }

  /**
   * @return the server-side listener ID.
   */
  public int getListenerID() {
    return listenerID;
  }

  /**
   * @return the notification filter.
   */
  public NotificationFilter getFilter() {
    return filter;
  }

  /**
   * @return the connection ID.
   */
  public String getConnectionID() {
    return connectionID;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("listenerID=").append(listenerID)
      .append(", connectionID=").append(connectionID)
      .append(", filter=").append(filter)
      .append(']').toString();
  }
}
