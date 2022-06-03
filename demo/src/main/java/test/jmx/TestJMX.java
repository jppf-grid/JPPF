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

package test.jmx;

import java.io.StringWriter;
import java.util.*;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.diagnostics.*;
import org.jppf.management.forwarding.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.Operator;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class TestJMX {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TestJMX.class);
  /**
   * 
   */
  private static JMXDriverConnectionWrapper driverJmx = null;
  /**
   * 
   */
  private static JPPFClient client = null;

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      client = new JPPFClient();
      driverJmx = client.awaitActiveConnectionPool().awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
      System.out.println("waiting till jmx is connected ...");
      while (!driverJmx.isConnected()) Thread.sleep(10L);
      perform3();
    } catch (final Throwable e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * Test diagnostics.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unused")
  private static void perform1() throws Exception {
    final DiagnosticsMBean diag = driverJmx.getDiagnosticsProxy();
    final ThreadDump td = diag.threadDump();
    final StringWriter sw = new StringWriter();
    final HTMLThreadDumpWriter writer = new HTMLThreadDumpWriter(sw, "driver " + driverJmx.getDisplayName());
    writer.printThreadDump(td);
    writer.close();
    output("driver thread dump:");
    output(sw.toString());
    output("... jmx connected");
  }

  /**
   * Test notification forwarding.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unused")
  private static void perform2() throws Exception {
    Thread.sleep(500L);
    final NodeNotificationListener listener = new NodeNotificationListener();
    final String listenerID = driverJmx.registerForwardingNotificationListener(new AllNodesSelector(), JPPFNodeTaskMonitorMBean.MBEAN_NAME, listener, null, "testing");
    final JPPFJob job = new JPPFJob();
    for (int i = 0; i < 5; i++) job.add(new LongTask(100L)).setId(String.valueOf(i + 1));
    final List<Task<?>> results = client.submit(job);
    Thread.sleep(500L);
    driverJmx.unregisterForwardingNotificationListener(listenerID);
    Thread.sleep(500L);
  }

  /**
   * Test cancelling tasks from node listener.
   * @throws Exception if any error occurs.
   */
  private static void perform3() throws Exception {
    final int nbJobs = 10;
    final int nbTasks = 100;
    for (int i = 0; i < nbJobs; i++) {
      final JPPFJob job = new JPPFJob();
      job.setName("job" + (i + 1));
      for (int j = 0; j < nbTasks; j++) {
        final Task<String> task = new LongTask(100L);
        task.setTimeoutSchedule(new JPPFSchedule(50L));
        job.add(task).setId(String.valueOf(j + 1));
      }
      final List<Task<?>> results = client.submit(job);
      output(job.getName() + " : received " + results.size() + " results");
    }
  }

  /**
   * Prints qnd logs the specified ;essqge.
   * @param message ;essqge to print.
   */
  private static void output(final String message) {
    System.out.println(message);
    log.info(message);
  }

  /**
   * 
   */
  public static class NodeNotificationListener implements ForwardingNotificationListener {
    /**
     * The task information received as notifications from the node.
     */
    public List<Notification> notifs = new ArrayList<>();
    /**
     * 
     */
    public Exception exception = null;

    @Override
    public void handleNotification(final JPPFNodeForwardingNotification notif, final Object handback) {
      try {
        System.out.println("received notification " + notif);
        System.out.println("nodeUuid=" + notif.getNodeUuid() + ", mBeanName='" + notif.getMBeanName() + "', inner notification=" + notif.getNotification());
        notifs.add(notif);
      } catch (final Exception e) {
        if (exception == null) exception = e;
      }
    }
  }
}
