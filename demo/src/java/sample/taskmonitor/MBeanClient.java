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

package sample.taskmonitor;

import java.util.Set;
import java.util.concurrent.atomic.*;

import javax.management.*;

import org.jppf.management.*;

/**
 * 
 * @author Laurent Cohen
 */
public class MBeanClient extends JMXConnectionWrapper implements NotificationListener
{
  /**
   * Count of executed tasks.
   */
  private AtomicInteger taskCount = new AtomicInteger(0);
  /**
   * Total cpu time on the node(s).
   */
  private AtomicLong cpuTime = new AtomicLong(0L);
  /**
   * Total elapsed time on the node(s).
   */
  private AtomicLong elapsedTime = new AtomicLong(0L);

  /**
   * Entry point.
   * @param args - not used.
   */
  public static void main(final String...args)
  {
    try
    {
      String mbeanName = JPPFNodeTaskMonitorMBean.MBEAN_NAME;
      ObjectName objectName = new ObjectName(mbeanName);
      MBeanClient client = new MBeanClient("lolo-quad", 12001);
      client.connect();
      while (!client.isConnected()) Thread.sleep(100);
      MBeanServerConnection mbsc = client.getMbeanConnection();
      JPPFNodeTaskMonitorMBean proxy = MBeanServerInvocationHandler.newProxyInstance(mbsc, objectName, JPPFNodeTaskMonitorMBean.class, true);
      proxy.getTotalTasksExecuted();
      //client.invoke(mbeanName, "test", (Object[]) null, (String[]) null);
      Set set = mbsc.queryNames(null, null);
      System.out.println("all mbeans: " + set);
      boolean b = mbsc.isInstanceOf(objectName, "javax.management.NotificationBroadcaster");
      System.out.println('\"'+mbeanName+"\" instance of NotificationBroadcaster: "+b);
      if (b)
      {
        mbsc.addNotificationListener(objectName, client, null, null);
        Thread.sleep(1000000L);
      }
      client.close();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host - the host the server is running on.
   * @param port - the RMI port used by the server.
   */
  public MBeanClient(final String host, final int port)
  {
    super(host, port, JPPFAdminMBean.NODE_SUFFIX);
  }

  /**
   * Handle an MBean notification.
   * @param notification - the notification sent by the node.
   * @param handback - handback object.
   * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
   */
  @Override
  public void handleNotification(final Notification notification, final Object handback)
  {
    TaskInformation info = ((TaskExecutionNotification) notification).getTaskInformation();
    int n = taskCount.incrementAndGet();
    if (n % 50 == 0)
    {
      long cpu = cpuTime.addAndGet(info.getCpuTime());
      long elapsed = elapsedTime.addAndGet(info.getElapsedTime());
      System.out.println("nb tasks = " + n + ", cpu time = " + cpu + " ms, elapsed time = " + elapsed +" ms");
    }
  }
}
