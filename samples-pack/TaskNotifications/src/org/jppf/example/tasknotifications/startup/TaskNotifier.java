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

package org.jppf.example.tasknotifications.startup;

import javax.management.*;

import org.jppf.example.tasknotifications.mbean.TaskNotificationsMBean;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.startup.JPPFNodeStartupSPI;

/**
 * This is a test of a node startup class.
 * @author Laurent Cohen
 */
public class TaskNotifier implements JPPFNodeStartupSPI
{
  /**
   * The proxy to the mbean that sends the actual notifications.
   */
  private static TaskNotificationsMBean mbean = null;

  /**
   * This is a test of a node startup class.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    System.out.println("Initializing the tasks notifier");
    initNotifier();
  }

  /**
   * Initialize the task notifications MBean proxy.
   */
  private static void initNotifier()
  {
    // Get a reference ot the local MBean server
    JMXNodeConnectionWrapper jmxWrapper = new JMXNodeConnectionWrapper();
    jmxWrapper.connectAndWait(3000L);
    if (!jmxWrapper.isConnected())
    {
      System.out.println("Error: could not connect to the local MBean server");
      return;
    }
    System.out.println("  connected to the local MBean server");
    try
    {
      ObjectName objectName = new ObjectName(TaskNotificationsMBean.MBEAN_NAME);
      //obtain the MBean server connection
      MBeanServerConnection mbsc = jmxWrapper.getMbeanConnection();
      // create the proxy instance
      mbean = MBeanServerInvocationHandler.newProxyInstance(
          mbsc, objectName, TaskNotificationsMBean.class, true);
      System.out.println("  task notifier successfully initialized");
    }
    catch(Exception e)
    {
      System.out.println("Error: " + e.getMessage());
    }
  }

  /**
   * Send a notification message to all registered listeners.
   * @param message the message to send to all registered listeners.
   */
  public static void addNotification(final String message)
  {
    if (mbean == null) return;
    mbean.sendTaskNotification(message);
  }

  /**
   * Send a notification message to all registered listeners.
   * @param message the message to send to all registered listeners.
   * @param userData additional (non trivial) data that may additionally be sent with the message.
   */
  public static void addNotification(final String message, final Object userData)
  {
    if (mbean == null) return;
    mbean.sendTaskNotification(message, userData);
  }
}
