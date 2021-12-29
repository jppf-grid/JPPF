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

package org.jppf.example.nodetray;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFNodeTaskMonitorMBean;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.NodeLifeCycleListenerAdapter;
import org.jppf.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a system tray icon for node monitoring.
 * The tray icon displays a message when the state of the connection to the server changes,
 * and changes the icon accordingly.
 * @author Laurent Cohen
 */
public class NodeSystemTray extends NodeLifeCycleListenerAdapter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeSystemTray.class);
  /**
   * The icon displayed in the system tray.
   */
  private static TrayIcon trayIcon;
  /**
   * The icons displayed.
   */
  private static Image[] images = initializeImages();
  /**
   * The wrapper used to access management functionalities.
   */
  private JMXNodeConnectionWrapper jmx;
  /**
   * Proxy to the task monitor MBean.
   */
  private JPPFNodeTaskMonitorMBean taskMonitor;
  /**
   * Used to synchronize tooltip and icon updates.
   */
  private ReentrantLock lock = new ReentrantLock();
  /**
   * The node's management information.
   */
  private JPPFManagementInfo jmxInfo;

  /**
   * Default constructor.
   */
  public NodeSystemTray() {
    try {
      initJMX();
      final SystemTray tray = SystemTray.getSystemTray();
      lock.lock();
      try {
        if (trayIcon != null) tray.remove(trayIcon);
        trayIcon = new TrayIcon(images[1]);
        trayIcon.setImageAutoSize(true);
        tray.add(trayIcon);
        trayIcon.setToolTip(generateTooltipText());
      } finally {
        lock.unlock();
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Initialize the JMX wrapper and register a listener to the task monitor mbean notifications.
   */
  private void initJMX() {
    try {
      jmx = new JMXNodeConnectionWrapper();
      jmx.connect();
      taskMonitor = jmx.getNodeTaskMonitor();
      taskMonitor.addNotificationListener(new JMXNotificationListener(), null, null);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Generate the tooltip for the tray icon.
   * @return the tooltip as a string.
   */
  private String generateTooltipText() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Node ");
    if (jmxInfo != null) sb.append(jmxInfo.getHost()).append(':').append(jmxInfo.getPort());
    sb.append('\n');
    if (taskMonitor != null) {
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
  public class JMXNotificationListener implements NotificationListener {
    /**
     * Handle task-level notifications. Here, we simply update the tray icon's tooltip text.
     * @param notification the notification.
     * @param handback not used.
     */
    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      if (lock.tryLock()) {
        try {
          final String s = generateTooltipText();
          trayIcon.setToolTip(s);
        } finally {
          lock.unlock();
        }
      }
    }
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    trayIcon.displayMessage("JPPF Node connected", null, MessageType.INFO);
    if (jmxInfo == null) {
      try {
        jmxInfo = event.getNode().getManagementInfo();
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    lock.lock();
    try {
      // node is disconnected, display the green icon
      trayIcon.setImage(images[0]);
      final String s = generateTooltipText();
      trayIcon.setToolTip(s);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
    trayIcon.displayMessage("JPPF Node disconnected from the server!", "attempting reconnection ...", MessageType.ERROR);
    lock.lock();
    try {
      // node is disconnected, display the red icon
      trayIcon.setImage(images[1]);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Initialize the icons used in the tray panel.
   * @return an array of {@link Image} objects.
   */
  private static Image[] initializeImages() {
    final Image[] img = new Image[2];
    img[0] = Toolkit.getDefaultToolkit().getImage(NodeSystemTray.class.getResource("/org/jppf/example/nodetray/node_green.gif"));
    img[1] = Toolkit.getDefaultToolkit().getImage(NodeSystemTray.class.getResource("/org/jppf/example/nodetray/node_red.gif"));
    return img;
  }
}
