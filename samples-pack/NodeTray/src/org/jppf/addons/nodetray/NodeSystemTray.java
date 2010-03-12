/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.addons.nodetray;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.*;

import org.apache.commons.logging.*;
import org.jppf.management.*;
import org.jppf.node.*;
import org.jppf.node.event.*;
import org.jppf.utils.*;

/**
 * Adds a system tray icon for node monitoring.
 * The tray icon displays a message when the state of the connection to the server changes,
 * and changes the icon accordingly.
 * @author Laurent Cohen
 */
public class NodeSystemTray
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeSystemTray.class);
	/**
	 * The icon displayed in the system tray.
	 */
	private TrayIcon trayIcon = null;
	/**
	 * The icons displayed.
	 */
	private static Image[] images = null;
	/**
	 * The wrapper used to access management functionalities.
	 */
	private JMXNodeConnectionWrapper wrapper = null;
	/**
	 * A reference to the node.
	 */
	private MonitoredNode node = null;
	/**
	 * Proxy to the task monitor MBean.
	 */
	private JPPFNodeTaskMonitorMBean proxy = null;
	/**
	 * Used to synchronize tooltip and icon updates.
	 */
	private ReentrantLock lock = new ReentrantLock();

	/**
	 * Default constructor.
	 */
	public NodeSystemTray()
	{
		try
		{
			SystemTray tray = SystemTray.getSystemTray();
			images = new Image[2];
			images[0] = Toolkit.getDefaultToolkit().getImage(NodeSystemTray.class.getResource("/org/jppf/addons/nodetray/node_green.gif"));
			images[1] = Toolkit.getDefaultToolkit().getImage(NodeSystemTray.class.getResource("/org/jppf/addons/nodetray/node_red.gif"));
			node = NodeRunner.getNode();
			TrayIcon oldTrayIcon = (TrayIcon) NodeRunner.getPersistentData("JPPFNodeTrayIcon");
			if (oldTrayIcon != null) tray.remove(oldTrayIcon);
			trayIcon = new TrayIcon(images[1]);
			trayIcon.setImageAutoSize(true);
			tray.add(trayIcon);
			NodeRunner.setPersistentData("JPPFNodeTrayIcon", trayIcon);
			initJMX();
			initNodeListener();
			String s = generateTooltipText();
			trayIcon.setToolTip(s);
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
			wrapper = new JMXNodeConnectionWrapper();
			wrapper.connectAndWait(5000);
			NodeEventType status = NodeEventType.valueOf(wrapper.state().getConnectionStatus());
			Image image = (NodeEventType.END_CONNECT.equals(status)) ? images[0] : images[1];
			trayIcon.setImage(image);
			ObjectName objectName = new ObjectName(JPPFNodeTaskMonitorMBean.TASK_MONITOR_MBEAN_NAME);
		  MBeanServerConnection mbsc = wrapper.getMbeanConnection();
		  proxy = (JPPFNodeTaskMonitorMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, objectName, JPPFNodeTaskMonitorMBean.class, true);
		  proxy.addNotificationListener(new JMXNotificationListener(), null, null);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Initialize adds a node listener to be notified of server connection state changes.
	 */
	private void initNodeListener()
	{
		try
		{
			node.addNodeListener(new NodeStateListener());
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
    sb.append("Node localhost:").append(JPPFConfiguration.getProperties().getInt("jppf.management.port")).append("\n");
    if (proxy != null)
    {
	    sb.append("Tasks executed: ").append(proxy.getTotalTasksExecuted()).append("\n");
	    sb.append("  successfull: ").append(proxy.getTotalTasksSucessfull()).append("\n");
	    sb.append("  in error: ").append(proxy.getTotalTasksInError()).append("\n");
	    sb.append("CPU time: ").append(StringUtils.toStringDuration(proxy.getTotalTaskCpuTime())).append("\n");
	    sb.append("Clock time: ").append(StringUtils.toStringDuration(proxy.getTotalTaskElapsedTime()));
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
		public void handleNotification(Notification notification, Object handback)
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

	/**
	 * Listens to the node state changes, display appropriates messages in the tray notification area,
	 * and manages the icon accordingly.
	 */
	public class NodeStateListener implements NodeListener
	{
		/**
		 * The latest node connection state.
		 */
		private NodeEventType lastEventType = NodeEventType.DISCONNECTED;
		/**
		 * The states we're interested in.
		 */
		private EnumSet<NodeEventType> interestStates = EnumSet.of(NodeEventType.START_CONNECT, NodeEventType.END_CONNECT, NodeEventType.DISCONNECTED);

		/**
		 * Called to notify a listener that a node event has occurred.
		 * This method detects whether the node was disconnected or just re-connected, and displays a corresponding
		 * message in the tray notification area. It also changes the icon to green/red depending on the state.
		 * @param event the event that triggered the notification.
		 * @see org.jppf.node.event.NodeListener#eventOccurred(org.jppf.node.event.NodeEvent)
		 */
		public void eventOccurred(NodeEvent event)
		{
			NodeEventType type = event.getType();
			// if not a state we're interested in, or no connection state change occurred, do nothing.
			if (!interestStates.contains(type) || type.equals(lastEventType)) return;
			Image image = NodeEventType.END_CONNECT.equals(type) ? images[0] : images[1];
			// if the node was disconnected from the server
			if (NodeEventType.START_CONNECT.equals(type) || NodeEventType.DISCONNECTED.equals(type))
			{
				trayIcon.displayMessage("JPPF Node disconnected from the server!", "attempting reconnection ...", MessageType.ERROR);
			}
			// if the node has reconnected to the server
			else if (NodeEventType.END_CONNECT.equals(type))
			{
				trayIcon.displayMessage("JPPF Node re-connected", null, MessageType.INFO);
			}
			lastEventType = type;
  		lock.lock();
  		try
  		{
  			// display the green or red icon, depending on the connection state
      	trayIcon.setImage(image);
  		}
  		finally
  		{
  			lock.unlock();
  		}
		}
	}
}
