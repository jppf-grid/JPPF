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

package org.jppf.example.nodetray;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.node.*;
import org.jppf.node.event.*;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Adds a system tray icon for node monitoring.
 * The tray icon displays a message when the state of the connection to the server changes,
 * and changes the icon accordingly.
 * @author Laurent Cohen
 */
public class NodeSystemTray implements NodeLifeCycleListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeSystemTray.class);
  /**
   * The icon displayed in the system tray.
   */
  private TrayIcon trayIcon = null;
  /**
   * The icons displayed.
   */
  private static Image[] images = initializeImages();
  /**
   * The wrapper used to access management functionalities.
   */
  private JMXNodeConnectionWrapper wrapper = null;
  /**
   * Proxy to the task monitor MBean.
   */
  private JPPFNodeTaskMonitorMBean taskMonitor = null;
  /**
   * Used to synchronize tooltip and icon updates.
   */
  private ReentrantLock lock = new ReentrantLock();
  /**
   * The JMX server used by the node for management and monitoring.
   */
  private JMXServer jmxServer = null;

  /**
   * Default constructor.
   */
  public NodeSystemTray()
  {
    try
    {
      SystemTray tray = SystemTray.getSystemTray();
      TrayIcon oldTrayIcon = (TrayIcon) NodeRunner.getPersistentData("JPPFNodeTrayIcon");
      if (oldTrayIcon != null) tray.remove(oldTrayIcon);
      trayIcon = new TrayIcon(images[1]);
      trayIcon.setImageAutoSize(true);
      tray.add(trayIcon);
      NodeRunner.setPersistentData("JPPFNodeTrayIcon", trayIcon);
      initJMX();
      trayIcon.setToolTip(generateTooltipText());
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Initialize the JMX wrapper and register a listener to the task monitor mbean notifications.
   */
  private void initJMX()
  {
    try
    {
      /*
      try
      {
       // if the node is running within the server's JVM (local node)
        if (JPPFDriver.getLocalNode() != null) jmxServer = JPPFDriver.getLocalNode().getJmxServer();
      }
      catch(NoClassDefFoundError e)
      {
        jmxServer = ((JPPFNode) NodeRunner.getNode()).getJmxServer();
      }
      catch(Exception e)
      {
        jmxServer = ((JPPFNode) NodeRunner.getNode()).getJmxServer();
      }
      */
      wrapper = new JMXNodeConnectionWrapper();
      wrapper.connectAndWait(5000);
      trayIcon.setImage(images[1]);
      taskMonitor = wrapper.getProxy(JPPFNodeTaskMonitorMBean.MBEAN_NAME, JPPFNodeTaskMonitorMBean.class);
      taskMonitor.addNotificationListener(new JMXNotificationListener(), null, null);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Generate the tooltip for the tray icon.
   * @return the tooltip as a string.
   */
  private String generateTooltipText()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("Node ");
    if (jmxServer != null) sb.append(jmxServer.getManagementHost()).append(':').append(jmxServer.getManagementPort());
    sb.append('\n');
    if (taskMonitor != null)
    {
      sb.append("Tasks executed: ").append(taskMonitor.getTotalTasksExecuted()).append("\n");
      sb.append("  successful: ").append(taskMonitor.getTotalTasksSucessfull()).append("\n");
      sb.append("  in error: ").append(taskMonitor.getTotalTasksInError()).append("\n");
      sb.append("CPU time: ").append(StringUtils.toStringDuration(taskMonitor.getTotalTaskCpuTime())).append("\n");
      sb.append("User time: ").append(StringUtils.toStringDuration(taskMonitor.getTotalTaskElapsedTime()));
    }
    return sb.toString();
  }

  /**
   * Listener for task-level events.
   */
  public class JMXNotificationListener implements NotificationListener
  {
    /**
     * Handle task-level notifications. Here, we simply update the tray icon's tooltip text.
     * @param notification the notification.
     * @param handback not used.
     * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
     */
    @Override
    public void handleNotification(final Notification notification, final Object handback)
    {
      if (lock.tryLock())
      {
        try
        {
          String s = generateTooltipText();
          trayIcon.setToolTip(s);
        }
        finally
        {
          lock.unlock();
        }
      }
    }
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event)
  {
    trayIcon.displayMessage("JPPF Node connected", null, MessageType.INFO);
    if (jmxServer == null)
    {
      try
      {
        jmxServer = ((AbstractNode) event.getNode()).getJmxServer();
      }
      catch (Exception e)
      {
        log.error(e.getMessage(), e);
      }
    }
    lock.lock();
    try
    {
      // node is disconnected, display the green icon
      trayIcon.setImage(images[0]);
      String s = generateTooltipText();
      trayIcon.setToolTip(s);
    }
    finally
    {
      lock.unlock();
    }
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event)
  {
    trayIcon.displayMessage("JPPF Node disconnected from the server!", "attempting reconnection ...", MessageType.ERROR);
    lock.lock();
    try
    {
      // node is disconnected, display the red icon
      trayIcon.setImage(images[1]);
    }
    finally
    {
      lock.unlock();
    }
  }

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event)
  {
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event)
  {
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event)
  {
  }

  /**
   * Initialize the icons used in the tray panel.
   * @return an array of {@link Image} objects.
   */
  private static Image[] initializeImages()
  {
    Image[] img = new Image[2];
    img[0] = Toolkit.getDefaultToolkit().getImage(NodeSystemTray.class.getResource("/org/jppf/example/nodetray/node_green.gif"));
    img[1] = Toolkit.getDefaultToolkit().getImage(NodeSystemTray.class.getResource("/org/jppf/example/nodetray/node_red.gif"));
    return img;
  }
}
