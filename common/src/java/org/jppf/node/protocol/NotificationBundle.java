/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.node.protocol;

/**
 *
 * @author Laurent Cohen
 */
public class NotificationBundle extends AbstractNotificationBundle {
  /**
   * Possible types of notifications.
   */
  public enum NotificationType {
    /**
     * Node throttling notification.
     */
    THROTTLING
  }

  /**
   * 
   * @param notifType the type of notification this bundle represents.
   */
  public NotificationBundle(final NotificationType notifType) {
    setParameter(BundleParameter.NODE_NOTIFICATION_TYPE, notifType);
  }

  @Override
  public boolean isNotification() {
    return true;
  }

  /**
   * @return the type of notification this bundle represents.
   */
  public NotificationType getNotificationType() {
    return getParameter(BundleParameter.NODE_NOTIFICATION_TYPE);
  }
}
