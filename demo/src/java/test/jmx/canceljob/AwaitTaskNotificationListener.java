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

package test.jmx.canceljob;

import java.util.Collection;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;

/**
 * A notification listener that can wait until a specified task notification is sent.
 */
public class AwaitTaskNotificationListener implements NotificationListener {
  /**
   * A message we expect to receive as a notification.
   */
  private final String expectedMessage;
  /**
   * The expected notificatiosn message count.
   */
  private final int expectedCount;
  /**
   * The JPPF client.
   */
  private final JPPFClient client;
  /**
   * A JMX connection to the driver.
   */
  private final JMXDriverConnectionWrapper jmx;
  /**
   * Registration ID for this listener.
   */
  private String listenerId;
  /**
   * The current notifications count.
   */
  private int count = 0;

  /**
   * Intiialize with an expected message.
   * @param client the JPPF client from which to get a JMX connection.
   * @param expectedMessage a message we expect to receive as a notification.
   * @throws Exception if any error occurs.
   */
  public AwaitTaskNotificationListener(final JPPFClient client, final String expectedMessage) throws Exception {
    this(client, expectedMessage, 1);
  }

  /**
   * Intiialize with an expected message.
   * @param client the JPPF client from which to get a JMX connection.
   * @param expectedMessage a message we expect to receive as a notification.
   * @param expectedCount the expected message count.
   * @throws Exception if any error occurs.
   */
  public AwaitTaskNotificationListener(final JPPFClient client, final String expectedMessage, final int expectedCount) throws Exception {
    this.expectedMessage = expectedMessage;
    this.expectedCount = expectedCount;
    this.client = client;
    this.jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, this, null, null);
  }

  @Override
  public synchronized void handleNotification(final Notification notification, final Object handback) {
    JPPFNodeForwardingNotification wrapping = (JPPFNodeForwardingNotification) notification;
    TaskExecutionNotification actualNotif = (TaskExecutionNotification) wrapping.getNotification();
    if (!actualNotif.isUserNotification()) return;
    String data = (String) actualNotif.getUserData();
    if (data.startsWith(expectedMessage)) {
      count++;
      System.out.printf("received notification %s, count=%d%n", data, count);
      if (count == expectedCount) notifyAll();
    }
  }

  /**
   * Wait for the epxected message to be received.
   * @throws Exception if any error occurs.
   */
  public synchronized void await() throws Exception {
    if (listenerId != null) {
      wait();
      jmx.unregisterForwardingNotificationListener(listenerId);
      listenerId = null;
    }
  }

  /**
   * Submit the specified jobs and Wait for the epxected message to be received atomically.
   * @param jobs the jobs to submit.
   * @throws Exception if any error occurs.
   */
  public synchronized void submitAndAwait(final Collection<JPPFJob> jobs) throws Exception {
    for (JPPFJob job: jobs) client.submitJob(job);
    System.out.printf("submitted %d jobs%n", jobs.size());
    await();
  }

  /**
   * Submit the specified jobs and Wait for the epxected message to be received atomically.
   * @param jobs the jobs to submit.
   * @throws Exception if any error occurs.
   */
  public synchronized void submitAndAwait(final JPPFJob...jobs) throws Exception {
    for (JPPFJob job: jobs) client.submitJob(job);
    await();
  }
}
