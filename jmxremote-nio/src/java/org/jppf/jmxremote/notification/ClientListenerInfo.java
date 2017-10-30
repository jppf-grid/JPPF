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
 * 
 * @author Laurent Cohen
 */
public class ClientListenerInfo {
  /**
   * The server-side listener ID.
   */
  private final int listenerID;
  /**
   * The name of the mbean the listener is registered with.
   */
  private final ObjectName mbeanName;
  /**
   * The notification listener. May be null, in which case {@link #listenerName} must not be null.
   */
  private final NotificationListener listener;
  /**
   * The associated notification filter to apply. 
   */
  private final NotificationFilter filter;
  /**
   * The listener handback.
   */
  private final Object handback;

  /**
   * Initialize with the specified parameters.
   * @param listenerID the server-side listener ID.
   * @param mbeanName the object name of the mbean to register the listener with.
   * @param listener the listener to register.
   * @param filter the notification filter.
   * @param handback the associated handback.
   */
  public ClientListenerInfo(final int listenerID, final ObjectName mbeanName, final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    this.listenerID = listenerID;
    this.mbeanName = mbeanName;
    this.listener = listener;
    this.filter = filter;
    this.handback = handback;
  }

  /**
   * @return the server-side listener ID.
   */
  public int getListenerID() {
    return listenerID;
  }

  /**
   * @return the notification listener.
   */
  public NotificationListener getListener() {
    return listener;
  }

  /**
   * @return the listener handback object.
   */
  public Object getHandback() {
    return handback;
  }

  /**
   * @return the notification listener. May be null, in which case {@link #getListenerName()} must not return null.
   */
  public ObjectName getMbeanName() {
    return mbeanName;
  }

  /**
   * @return the associated notification filter to apply.
   */
  public NotificationFilter getFilter() {
    return filter;
  }
}
