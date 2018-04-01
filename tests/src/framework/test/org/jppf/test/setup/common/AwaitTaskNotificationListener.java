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

package test.org.jppf.test.setup.common;

import javax.management.*;

import org.jppf.client.JPPFClient;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;
import org.slf4j.*;

import test.org.jppf.test.setup.BaseSetup;

/**
 * A notification listener that can wait until a specified task notification is sent.
 */
public class AwaitTaskNotificationListener implements NotificationListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AwaitTaskNotificationListener.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A message we expect to receive as a notification.
   */
  private final String expectedMessage;
  /**
   * Whether the expected message was received as a task notification.
   */
  private boolean receivedMessage;
  /**
   * A jmx connection to the driver.
   */
  private final JMXDriverConnectionWrapper jmx;
  /**
   * Registration ID for this listener.
   */
  private String listenerId;

  /**
   * Intiialize with an expected message.
   * @param client the JPPF client from which to get a JMX connection.
   * @param expectedMessage a message we expect to receive as a notification.
   * @throws Exception if any error occurs.
   */
  public AwaitTaskNotificationListener(final JPPFClient client, final String expectedMessage) throws Exception {
    this(BaseSetup.getJMXConnection(client), expectedMessage);
  }

  /**
   * Intiialize with an expected message.
   * @param jmx a jmx connection to the driver.
   * @param expectedMessage a message we expect to receive as a notification.
   * @throws Exception if any error occurs.
   */
  public AwaitTaskNotificationListener(final JMXDriverConnectionWrapper jmx, final String expectedMessage) throws Exception {
    this.expectedMessage = expectedMessage;
    this.jmx = jmx;
    listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, this, null, null);
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    final JPPFNodeForwardingNotification wrapping = (JPPFNodeForwardingNotification) notification;
    final TaskExecutionNotification actualNotif = (TaskExecutionNotification) wrapping.getNotification();
    final Object data = actualNotif.getUserData();
    if ((expectedMessage == null) || expectedMessage.equals(data)) {
      if (debugEnabled) log.debug("received expected task notification {}", expectedMessage);
      synchronized(this) {
        receivedMessage = true;
        notifyAll();
      }
    }
  }

  /**
   * Wait for the epxected message to be received.
   * @throws Exception if any error occurs.
   */
  public synchronized void await() throws Exception {
    if (listenerId != null) {
      while (!receivedMessage) wait(100L);
      jmx.unregisterForwardingNotificationListener(listenerId);
      listenerId = null;
    }
  }
}
