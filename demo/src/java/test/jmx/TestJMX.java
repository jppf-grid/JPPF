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
import org.jppf.management.*;
import org.jppf.management.forwarding.*;
import org.jppf.server.protocol.JPPFTask;

import sample.dist.tasklength.LongTask;


/**
 * 
 * @author Laurent Cohen
 */
public class TestJMX
{
  /**
   * Entry point.
   * @param args - not used.
   */
  public static void main(final String...args)
  {
    JPPFClient client = null;
    JPPFNodeForwardingMBean forwarder = null;
    try
    {
      client = new JPPFClient();
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      AbstractJPPFClientConnection conn = (AbstractJPPFClientConnection) client.getClientConnection();
      JMXDriverConnectionWrapper driverJmx = conn.getJmxConnection();
      System.out.println("waiting till jmx is connected ...");
      while (!driverJmx.isConnected()) Thread.sleep(10L);
      System.out.println("... jmx connected");
      Thread.sleep(500L);
      NodeNotificationListener listener = new NodeNotificationListener();
      String listenerID = driverJmx.registerForwardingNotificationListener(new NodeSelector.AllNodesSelector(), JPPFNodeTaskMonitorMBean.MBEAN_NAME, listener, null, "testing");
      JPPFJob job = new JPPFJob();
      for (int i=0; i<5; i++) job.addTask(new LongTask(100L)).setId(String.valueOf(i+1));
      List<JPPFTask> results = client.submit(job);
      Thread.sleep(500L);
      driverJmx.unregisterForwardingNotificationListener(listenerID);
      Thread.sleep(500L);
    }
    catch(Throwable e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (client != null) client.close();
    }
  }

  /**
   * 
   */
  public static class NodeNotificationListener implements NotificationListener
  {
    /**
     * The task information received as notifications from the node.
     */
    public List<Notification> notifs = new ArrayList<Notification>();
    /**
     * 
     */
    public Exception exception = null;

    @Override
    public void handleNotification(final Notification notification, final Object handback)
    {
      try
      {
        System.out.println("received notification " + notification);
        JPPFNodeForwardingNotification notif = (JPPFNodeForwardingNotification) notification;
        System.out.println("nodeUuid=" + notif.getNodeUuid() + ", mBeanName='" + notif.getMBeanName() + "', inner notification=" + notif.getNotification());
        notifs.add(notification);
      }
      catch (Exception e)
      {
        if (exception == null) exception = e;
      }
    }
  }
}
