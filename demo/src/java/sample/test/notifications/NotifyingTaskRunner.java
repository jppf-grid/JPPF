/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package sample.test.notifications;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

/**
 *
 * @author Laurent Cohen
 */
public class NotifyingTaskRunner {
  /** */
  private static final int nbTasks = 1;
  /** */
  private static final int nbNotifs = 10_000;

  /**
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      JMXDriverConnectionWrapper jmx = getJmxConnection(client);
      String listenerID = null;
      NotifHandler handler = new NotifHandler();
      try {
        listenerID = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, handler, null, null);
        JPPFJob job = new JPPFJob();
        String name = "notifying job";
        job.setName(name);
        job.getSLA().setMaxTaskResubmits(1);
        job.getSLA().setApplyMaxResubmitsUponNodeError(true);
        for (int i=0; i<nbTasks; i++) job.add(new NotifyingTask(nbNotifs)).setId(name + ":task" + i);
        System.out.println("submitting job ...");
        List<Task<?>> results = client.submitJob(job);
        System.out.printf("'%s' has %d results%n", job.getName(), results.size());
        for (Task<?> task : results) {
          if (task.getThrowable() != null) System.out.printf("got exception for %s: %s%n", task.getId(), ExceptionUtils.getStackTrace(task.getThrowable()));
          else System.out.printf("got result for %s: %s%n", task.getId(), task.getResult());
        }
        while (handler.nbNotifs.get() < nbNotifs) Thread.sleep(10L);
        System.out.println("received " + handler.nbNotifs.get() + " notifications");
      } finally {
        if (listenerID != null) jmx.unregisterForwardingNotificationListener(listenerID);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get a JMX connection from the specified client.
   * @param client the client to get the connection from.
   * @return a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private static synchronized JMXDriverConnectionWrapper getJmxConnection(final JPPFClient client) throws Exception {
    JMXDriverConnectionWrapper jmx = null;
    JPPFConnectionPool pool = null;
    List<JPPFConnectionPool> list = null;
    while ((list = client.findConnectionPools(JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)).isEmpty()) Thread.sleep(1L);
    pool = list.get(0);
    while ((jmx = pool.getJmxConnection()) == null) Thread.sleep(1L);
    while (!jmx.isConnected()) Thread.sleep(1L);
    return jmx;
  }

  /**
   * 
   */
  private static class NotifHandler implements NotificationListener {
    /**
     * 
     */
    public AtomicInteger nbNotifs = new AtomicInteger(0);

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      JPPFNodeForwardingNotification wrapping = (JPPFNodeForwardingNotification) notification;
      TaskExecutionNotification actualNotif = (TaskExecutionNotification) wrapping.getNotification();
      if (actualNotif.isUserNotification()) {
        nbNotifs.incrementAndGet();
      }
    }
  }
}
