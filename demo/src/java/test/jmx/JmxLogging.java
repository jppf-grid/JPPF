/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.*;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.logging.jmx.JmxLogger;
import org.jppf.management.*;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JmxLogging {

  private static JMXNodeConnectionWrapper[] jmxNodes = null;
  private static JPPFClient client = null;
  private static MyLoggingHandler loggingHandler = new MyLoggingHandler();

  public static void main(final String[] args)
  {
    try {
      client = new JPPFClient();
      JPPFJob job = new JPPFJob();
      initJmxLogging();
      for (int i=1; i<=10; i++) job.addTask(new MyTask()).setId(Integer.toString(i));
      List<JPPFTask> results = client.submit(job);
      System.out.println("received results: " + results);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
      if (jmxNodes != null) {
        for (JMXNodeConnectionWrapper jmx: jmxNodes) {
          try {
            jmx.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  public static void initJmxLogging() throws Exception {
    while (!client.hasAvailableConnection()) Thread.sleep(10L);
    JMXDriverConnectionWrapper jmxDriver = ((AbstractJPPFClientConnection) client.getClientConnection()).getJmxConnection();
    Collection<JPPFManagementInfo> coll = jmxDriver.nodesInformation();
    jmxNodes = new JMXNodeConnectionWrapper[coll.size()];
    int count = 0;
    for (JPPFManagementInfo info: coll) {
      // get a JMX connection to the node MBean server
      jmxNodes[count] = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
      jmxNodes[count].connectAndWait(5000L);
      // get a proxy to the logging MBean
      JmxLogger nodeProxy = jmxNodes[count].getProxy(JmxLogger.DEFAULT_MBEAN_NAME, JmxLogger.class);

      // use a handback object so we know where the log messages come from
      String source = "node   " + info.getHost() + ":" + info.getPort();
      // subbscribe to all notifications from the MBean
      nodeProxy.addNotificationListener(loggingHandler, null, source);
      count++;
    }
  }

  //Logging notification listener that prints remote log messagesto the console
  public static class MyLoggingHandler implements NotificationListener {
    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      String message = notification.getMessage();
      String toDisplay = handback.toString() + ": " + message;
      System.out.print(toDisplay);
    }
  }

  public static class MyTask extends JPPFTask {
    private static Logger log = LoggerFactory.getLogger(MyTask.class);

    @Override
    public void run() {
      log.info("MyTask id '" + getId() + "' running");
    }
  }
}
