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

import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;

/**
 * Instances of this notification class wrap any JMX notification received from a node.
 * They provide additional information to allow users to know from which node and which MBean the notification was emitted.
 * @author Laurent Cohen
 */
public class JPPFNodeForwardingNotification extends Notification {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Internal notification sequence number.
   */
  private final static AtomicLong sequence = new AtomicLong(0L);
  /**
   * The notification to forward.
   */
  private final Notification notification;
  /**
   * The uuid of the originating node.
   */
  private final String nodeUuid;
  /**
   * The name of the originating MBean in the node.
   */
  private final String mBeanName;

  /**
   * Initialize this notification with the actual node notification that was received.
   * @param notification the notification to forward.
   * @param nodeUuid the uuid of the originating node.
   * @param mBeanName name of the originating MBean in the node.
   */
  public JPPFNodeForwardingNotification(final Notification notification, final String nodeUuid, final String mBeanName) {
    super("NodeForwardingNotification", NodeForwardingMBean.MBEAN_NAME, sequence.incrementAndGet());
    this.notification = notification;
    this.nodeUuid = nodeUuid;
    this.mBeanName = mBeanName;
  }

  /**
   * Get the actual notification forwarded from the node.
   * @return an instance of {@link Notification}.
   */
  public Notification getNotification() {
    return notification;
  }

  /**
   * Get the uuid of the originating node.
   * @return the node uuid as a string.
   */
  public String getNodeUuid() {
    return nodeUuid;
  }

  /**
   * Get the name of the originating MBean in the node.
   * @return the MBean name as a string.
   */
  public String getMBeanName() {
    return mBeanName;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName())
      .append("[type=").append(getType()).append(", source=").append(getSource())
      .append(", mBeanName=").append(mBeanName).append(", nodeUuid=").append(nodeUuid)
      .append(", sequenceNumber=").append(getSequenceNumber()).append(", timeStamp=").append(getTimeStamp())
      .append(", notification=").append(notification).toString();
  }
}
