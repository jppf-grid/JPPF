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

package org.jppf.management.forwarding;

import org.jppf.management.NodeSelector;

/**
 * Instances of this class represent an association between a notification listener id
 * and the corresponding node selector and MBean name.
 * @author Laurent Cohen
 * @exclude
 */
class NotificationListenerWrapper {
  /**
   * The notification listener for the client side.
   */
  private final String listenerID;
  /**
   * The node selector.
   */
  private final NodeSelector selector;
  /**
   * The node MBean name.
   */
  private final String mBeanName;

  /**
   * Initialize this wrapper.
   * @param listenerID the notification listener for the client side.
   * @param selector the node selector.
   * @param mBeanName the node MBean name.
   */
  public NotificationListenerWrapper(final String listenerID, final NodeSelector selector, final String mBeanName) {
    this.listenerID = listenerID;
    this.selector = selector;
    this.mBeanName = mBeanName;
  }

  /**
   * Get the notification listener ID for the client side.
   * @return a listener ID as a string.
   */
  public String getListenerID() {
    return listenerID;
  }

  /**
   * Get the node selector.
   * @return a {@link NodeSelector} instance.
   */
  public NodeSelector getSelector() {
    return selector;
  }

  /**
   * Get the node MBean name.
   * @return the MBean name as a string.
   */
  public String getMBeanName() {
    return mBeanName;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("listenerID=").append(listenerID);
    sb.append(", selector=").append(selector);
    sb.append(", mBeanName=").append(mBeanName);
    sb.append(']');
    return sb.toString();
  }
}
