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

package org.jppf.management;

import javax.management.NotificationListener;

import org.jppf.management.forwarding.InternalNotificationFilter;

/**
 * Wraps the information for each registered node forwarding listener.
 */
class ListenerWrapper {
  /**
   * The registered listener.
   */
  private final NotificationListener listener;
  /**
   * The notification filter.
   */
  private final InternalNotificationFilter filter;
  /**
   * the handback object.
   */
  private final Object handback;

  /**
   * Initialize this wrapper with the specified listener information.
   * @param listener the registered listener.
   * @param filter the notification filter.
   * @param handback the handback object.
   */
  ListenerWrapper(final NotificationListener listener, final InternalNotificationFilter filter, final Object handback) {
    this.listener = listener;
    this.filter = filter;
    this.handback = handback;
  }

  /**
   * Get the registered listener.
   * @return a {@link NotificationListener} instance.
   */
  NotificationListener getListener() {
    return listener;
  }

  /**
   * Get the notification filter.
   * @return an <code>InternalNotificationFilter</code> instance.
   */
  InternalNotificationFilter getFilter() {
    return filter;
  }

  /**
   * Get the handback object.
   * @return the handback object.
   */
  Object getHandback() {
    return handback;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((filter == null) ? 0 : filter.hashCode());
    result = prime * result + ((handback == null) ? 0 : handback.hashCode());
    result = prime * result + ((listener == null) ? 0 : listener.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final ListenerWrapper other = (ListenerWrapper) obj;
    if (filter == null) {
      if (other.filter != null) return false;
    } else if (!filter.equals(other.filter)) return false;
    if (handback == null) {
      if (other.handback != null) return false;
    } else if (!handback.equals(other.handback)) return false;
    if (listener == null) {
      if (other.listener != null) return false;
    } else if (!listener.equals(other.listener)) return false;
    return true;
  }
}