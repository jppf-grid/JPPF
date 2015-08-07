/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.job.broadcast;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;
import org.jppf.utils.ExceptionUtils;

/**
 *
 * @author Laurent Cohen
 */
public class BroadcastRunner {
  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (JPPFClient client = new JPPFClient()) {
      JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool()
        .awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
      final Map<String, Boolean> responseMap = new ConcurrentHashMap<>();
      // register a notification listener on all nodes
      NotificationListener listener = new NotificationListener() {
        @Override
        public void handleNotification(final Notification notification, final Object handback) {
          JPPFNodeForwardingNotification notif = (JPPFNodeForwardingNotification) notification;
          TaskExecutionNotification taskNotif = (TaskExecutionNotification) notif.getNotification();
          if (taskNotif.isUserNotification()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) taskNotif.getUserData();
            boolean success = (Boolean) result.get("success");
            responseMap.put(notif.getNodeUuid(), success);
            if (!success) {
              Exception e = (Exception) result.get("exception");
              System.out.printf("Exception occurred in node '%s' : %s%n", notif.getNodeUuid(), ExceptionUtils.getStackTrace(e));
            }
          }
        }
      };
      String listeneerID = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, listener, null, null);
      JPPFJob job = new JPPFJob();
      job.setName("my broadcast job");
      job.getSLA().setBroadcastJob(true);
      job.add(new BroadcastTask()).setId("broadcast task id");
      client.submitJob(job);
      // count of node is our exit condition
      int nbNodes = jmx.nbNodes();
      // some notification may arrive in the client after the job is done
      while (responseMap.size() < nbNodes) Thread.sleep(10L);
      // handle the results
      int successCount = 0, errorCount = 0;
      for (Map.Entry<String, Boolean> entry: responseMap.entrySet()) {
        boolean success = entry.getValue();
        System.out.printf("Execution on node '%s' is a %s%n", entry.getKey(), (success ? "success" : "failure"));
        if (success) successCount++;
        else errorCount++;
      }
      System.out.printf("got tresponses from all nodes: success count = %d, error count = %d%n", successCount, errorCount);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
