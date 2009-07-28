/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.ui.monitoring.job;

import java.util.List;
import java.util.concurrent.*;

import javax.management.*;
import javax.swing.*;
import javax.swing.tree.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.job.*;
import org.jppf.management.*;
import org.jppf.server.job.management.*;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.treetable.JTreeTable;
import org.jppf.utils.LocalizationUtils;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class JobDataPanel extends AbstractOption implements ClientListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JobDataPanel.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Base name for localization bundle lookups.
	 */
	private static final String BASE = "org.jppf.ui.i18n.JobDataPage";
	/**
	 * A tree table component displaying the driver and nodes information. 
	 */
	private JobTreeTable treeTable = null;
	/**
	 * The tree table model associated witht he tree table.
	 */
	private transient JobTreeTableModel model = null;
	/**
	 * The root of the tree model.
	 */
	private DefaultMutableTreeNode root = null;
	/**
	 * Reference to the JPPF client.
	 */
	private JPPFClient client = null;
	/**
	 * Separate thread used to sequentialize events that impact the job data tree.
	 */
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	/**
	 * Initialize this panel with the specified information.
	 */
	public JobDataPanel()
	{
		if (debugEnabled) log.debug("initializing NodeDataPanel");
		client = StatsHandler.getInstance().getJppfClient(null);
		createTreeTableModel();
		createUI();
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private void createTreeTableModel()
	{
		root = new DefaultMutableTreeNode(localize("tree.root.name"));
		model = new JobTreeTableModel(root);
	}

	/**
	 * Refresh the tree from the latest data found in the server.
	 * This method will clear the entire tree and repopulate it.
	 */
	public synchronized void refresh()
	{
		for (int i=0; i<root.getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) root.getChildAt(i);
			model.removeNodeFromParent(driverNode);
		}
		populateTreeTableModel();
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private synchronized void populateTreeTableModel()
	{
		try
		{
			List<JPPFClientConnection> list = client.getAllConnections();
			for (JPPFClientConnection c: list)
			{
				JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) c;
				JMXDriverConnectionWrapper wrapper = connection.getJmxConnection();
				JobData data = new JobData(wrapper);
				DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(data);
				model.insertNodeInto(driverNode, root, root.getChildCount());
				DriverJobManagementMBean proxy = data.getProxy();
				String[] jobIds = proxy.getAllJobIds();
				for (String id: jobIds)
				{
					JobInformation jobInfo = proxy.getJobInformation(id);
					if (jobInfo != null)
					{
						JobData jobData = new JobData(jobInfo);
						DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(jobData);
						model.insertNodeInto(jobNode, driverNode, driverNode.getChildCount());
						NodeJobInformation[] subJobInfo = proxy.getNodeInformation(id);
						for (NodeJobInformation nji: subJobInfo)
						{
							JobData subJobData = new JobData(nji.jobInfo, nji.nodeInfo);
							DefaultMutableTreeNode subJobNode = new DefaultMutableTreeNode(subJobData);
							model.insertNodeInto(subJobNode, jobNode, jobNode.getChildCount());
						}
					}
				}
				proxy.addNotificationListener(new JobNotificationListener(wrapper.getId()), null, null);
			}
			client.addClientListener(this);
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
	}

	/**
	 * Create, initialize and layout the GUI components displayed in this panel.
	 */
	public void createUI()
	{
	  treeTable = new JobTreeTable(model);
	  treeTable.getTree().setRootVisible(false);
	  treeTable.getTree().setShowsRootHandles(true);
	  populateTreeTableModel();
		treeTable.expandAll();
		//treeTable.addMouseListener(new NodeTreeTableMouseListener());
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		treeTable.doLayout();
		treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane sp = new JScrollPane(treeTable);
		setUIComponent(sp);
	}

	/**
	 * Called to notify that a driver was added.
	 * @param wrapper - wrapper for a connection to the jmx server of a driver.
	 */
	public synchronized void driverAdded(final JMXDriverConnectionWrapper wrapper)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
				DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(new JobData(wrapper));
				if (debugEnabled) log.debug("adding driver: " + wrapper.getId());
				model.insertNodeInto(driverNode, root, root.getChildCount());
				if (root.getChildCount() == 1) expandAndResizeColumns();
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param driverName - the name of the driver to remove.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#driverRemoved(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public synchronized void driverRemoved(final String driverName)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (debugEnabled) log.debug("removing driver: " + driverName);
				if (driverNode != null) model.removeNodeFromParent(driverNode);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a job was submitted to a driver.
	 * @param driverName - the name of the driver the job was submitted to.
	 * @param jobInfo - information about the submitted job.
	 */
	public synchronized void jobAdded(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (driverNode == null) return;
				JobData data = new JobData(jobInfo);
				DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(data);
				if (debugEnabled) log.debug("adding job: " + jobInfo.getJobId() + " to driver " + driverName);
				model.insertNodeInto(jobNode, driverNode, driverNode.getChildCount());
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
				if (root.getChildCount() == 1) expandAndResizeColumns();
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a job was removed from a driver.
	 * @param driverName - the name of the driver the job was submitted to.
	 * @param jobInfo - information about the job.
	 */
	public synchronized void jobRemoved(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (driverNode == null) return;
				DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
				if (jobNode == null) return;
				if (debugEnabled) log.debug("removing job: " + jobInfo.getJobId() + " from driver " + driverName);
				model.removeNodeFromParent(jobNode);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a job was removed from a driver.
	 * @param driverName - the name of the driver the job was submitted to.
	 * @param jobInfo - information about the job.
	 */
	public synchronized void jobUpdated(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (driverNode == null) return;
				DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
				if (jobNode == null) return;
				if (debugEnabled) log.debug("updating job: " + jobInfo.getJobId() + " from driver " + driverName);
				JobData data = new JobData(jobInfo);
				jobNode.setUserObject(data);
				model.changeNode(jobNode);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a sub-job was dispatched to a node.
	 * @param driverName - the name of the driver the job was submitted to.
	 * @param jobInfo - information about the sub-job.
	 * @param nodeInfo - information about the node where the sub-job was dispatched.
	 */
	public synchronized void subJobAdded(final String driverName, final JobInformation jobInfo, final NodeManagementInfo nodeInfo)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (driverNode == null) return;
				DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
				if (jobNode == null) return;
				JobData data = new JobData(jobInfo, nodeInfo);
				DefaultMutableTreeNode subJobNode = new DefaultMutableTreeNode(data);
				if (debugEnabled) log.debug("sub-job: " + jobInfo.getJobId() + " dispatched to node " + nodeInfo.getHost() + ":" + nodeInfo.getPort());
				model.insertNodeInto(subJobNode, jobNode, jobNode.getChildCount());
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
				if (root.getChildCount() == 1) expandAndResizeColumns();
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a sub-job was removed from a node.
	 * @param driverName - the name of the driver the job was submitted to.
	 * @param jobInfo - information about the job.
	 * @param nodeInfo - information about the node where the sub-job was dispatched.
	 */
	public synchronized void subJobRemoved(final String driverName, final JobInformation jobInfo, final NodeManagementInfo nodeInfo)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (driverNode == null) return;
				DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
				if (jobNode == null) return;
				DefaultMutableTreeNode subJobNode = findSubJob(jobNode, nodeInfo);
				if (subJobNode == null) return;
				if (debugEnabled) log.debug("removing sub-job: " + jobInfo.getJobId() + " from node " + nodeInfo.getHost() + ":" + nodeInfo.getPort());
				model.removeNodeFromParent(subJobNode);
			}
		};
		executor.submit(r);
	}

	/**
	 * Find the driver tree node with the specified driver name.
	 * @param driverName name of the dirver to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
	 */
	private DefaultMutableTreeNode findDriver(String driverName)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		for (int i=0; i<root.getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) root.getChildAt(i);
			JobData data = (JobData) driverNode.getUserObject();
			JMXDriverConnectionWrapper wrapper = data.getJmxWrapper();
			if (wrapper.getId().equals(driverName)) return driverNode;
		}
		return null;
	}

	/**
	 * Find the job with the specified id that was submitted to the specified driver.
	 * @param driverNode - the driver where the job was submitted.
	 * @param jobInfo - information about the job to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the job could not be found.
	 */
	private DefaultMutableTreeNode findJob(DefaultMutableTreeNode driverNode, JobInformation jobInfo)
	{
		for (int i=0; i<driverNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
			JobData data = (JobData) node.getUserObject();
			if (data.getJobInformation().getJobId().equals(jobInfo.getJobId())) return node;
		}
		return null;
	}

	/**
	 * Find the sub-job with the specified id that was dispatched to the specified JPPF node.
	 * @param jobNode - the job whose sub-job we are looking for.
	 * @param nodeInfo - holds information on the node to which the sub-job was dispatched.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the sub-job could not be found.
	 */
	private DefaultMutableTreeNode findSubJob(DefaultMutableTreeNode jobNode, NodeManagementInfo nodeInfo)
	{
		for (int i=0; i<jobNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) jobNode.getChildAt(i);
			JobData data = (JobData) node.getUserObject();
			if (data.getNodeInformation().toString().equals(nodeInfo.toString())) return node;
		}
		return null;
	}

	/**
	 * Get a localized message given its unique name and the current locale.
	 * @param message - the unique name of the localized message.
	 * @return a message in the current locale, or the default locale 
	 * if the localization for the current locale is not found. 
	 */
	private String localize(String message)
	{
		return LocalizationUtils.getLocalized(BASE, message);
	}

	/**
	 * Get the tree table component displaying the driver and nodes information. 
	 * @return a <code>JXTreeTable</code> instance.
	 */
	public synchronized JTreeTable getTreeTable()
	{
		return treeTable;
	}

	/**
	 * Not implemented.
	 * @param enabled - not used.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
	}

	/**
	 * Not implemented.
	 * @param enabled - not used.
	 * @see org.jppf.ui.options.OptionElement#setEventsEnabled(boolean)
	 */
	public void setEventsEnabled(boolean enabled)
	{
	}

	/**
	 * Not implemented.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * Create, initialize and layout the GUI components displayed in this panel.
	 */
	private void expandAndResizeColumns()
	{
		treeTable.expandAll();
		treeTable.sizeColumnsToFit(0);
	  //for (int i=0; i<model.getColumnCount(); i++) treeTable.sizeColumnsToFit(i);
	}

	/**
	 * Determine whether only nodes are currently selected.
	 * @return true if at least one node and no driver is selected, false otherwise. 
	 */
	public boolean areOnlyNodesSelected()
	{
		return areOnlyTypeSelected(true);
	}

	/**
	 * Determine whether only drivers are currently selected.
	 * @return true if at least one driver and no node is selected, false otherwise. 
	 */
	public boolean areOnlyDriversSelected()
	{
		return areOnlyTypeSelected(false);
	}

	/**
	 * Determine whether only tree elements of the specified type are currently selected.
	 * @param checkNodes - true to check if nodes only are selected, false to check if drivers only are selected.
	 * @return true if at least one element of the specified type and no element of another type is selected, false otherwise. 
	 */
	private boolean areOnlyTypeSelected(boolean checkNodes)
	{
		int[] rows = treeTable.getSelectedRows();
		if ((rows == null) || (rows.length <= 0)) return false;
		int nbNodes = 0;
		int nbDrivers = 0;
		for (int n: rows)
		{
			TreePath path = treeTable.getPathForRow(n);
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
			if (treeNode.getParent() == null) continue;
			if (treeNode.getUserObject() instanceof NodeInfoHolder) nbNodes++;
			else nbDrivers++;
		}
		return (checkNodes && (nbNodes > 0) && (nbDrivers == 0)) || (!checkNodes && (nbNodes == 0) && (nbDrivers > 0));
	}

	/**
	 * Notifiy this listener that a new driver connection was created.
	 * @param event - the event to notify this listener of.
	 * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
	 */
	public void newConnection(ClientEvent event)
	{
		JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) event.getConnection()).getJmxConnection();
		driverAdded(wrapper);
	}

	/**
	 * Implementation of a notifiaction listeners for processing of job events.
	 */
	public class JobNotificationListener implements NotificationListener
	{
		/**
		 * String identifying the driver that sends the notifications.
		 */
		private String driverName = null;

		/**
		 * Initialize this listener with the specified driver name.
		 * @param driverName - a string identifying the driver that sends the notifications.
		 */
		public JobNotificationListener(String driverName)
		{
			this.driverName = driverName;
		}

		/**
		 * Handle notifications of job events.
		 * @param notification - encapsulates the job event ot handle.
		 * @param handback - not used.
		 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
		 */
		public void handleNotification(Notification notification, Object handback)
		{
			if (!(notification instanceof JobNotification)) return;
			JobNotification notif = (JobNotification) notification;
			switch(notif.getEventType())
			{
				case JOB_QUEUED:
					jobAdded(driverName, notif.getJobInformation());
					break;
				case JOB_ENDED:
					jobRemoved(driverName, notif.getJobInformation());
					break;
				case JOB_UPDATED:
					jobUpdated(driverName, notif.getJobInformation());
					break;
				case JOB_DISPATCHED:
					subJobAdded(driverName, notif.getJobInformation(), notif.getNodeInfo());
					break;
				case JOB_RETURNED:
					subJobRemoved(driverName, notif.getJobInformation(), notif.getNodeInfo());
					break;
			}
		}
	}
}
