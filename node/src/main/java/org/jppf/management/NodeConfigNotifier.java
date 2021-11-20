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

import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

import org.jppf.node.Node;
import org.jppf.utils.TypedProperties;
import org.slf4j.*;

/**
 * This MBean notifies any listener of changes to the number of threads of a node.
 * @author Laurent Cohen
 */
public class NodeConfigNotifier extends NotificationBroadcasterSupport implements NodeConfigNotifierMBean {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeConfigNotifier.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * AN incrementing sequence number.
   */
  private static final AtomicLong sequence = new AtomicLong(0L);

  /**
   * @param node the node holding this MBean.
   */
  public NodeConfigNotifier(final Node node) {
    if (debugEnabled) log.debug("new NodeConfigNotifier for node " + node.getUuid());
  }
  

  /**
   * Send a notification of changes in the configuration.
   * @param nodeUuid the uuid of the node, sent as the notification source.
   * @param configuration the changed configuration.
   */
  public void sendNotification(final String nodeUuid, final TypedProperties configuration) {
    final Notification notif = new Notification("config.notifier", nodeUuid, sequence.incrementAndGet(), System.currentTimeMillis());
    notif.setUserData(configuration);
    sendNotification(notif);
  }
}
