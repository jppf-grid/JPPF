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

import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.job.JobInformation;
import org.jppf.management.*;
import org.jppf.server.job.management.*;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.treetable.*;
import org.jppf.utils.SynchronizedTask;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class JobDataPanel extends AbstractTreeTableOption implements ClientListener
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(JobDataPanel.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Separate thread used to sequentialize events that impact the job data tree.
	 */
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	/**
	 * Mapping of connection names to status listener.
	 */
	private Map<String, ConnectionStatusListener> listenerMap = new Hashtable<String, ConnectionStatusListener>();

	/**
	 * Initialize this panel with the specified information.
	 */
	public JobDataPanel()
	{
		if (debugEnabled) log.debug("initializing NodeDataPanel");
		BASE = "org.jppf.ui.i18n.JobDataPage";
		createTreeTableModel();
		createUI();
	  populateTreeTableModel();
	  StatsHandler.getInstance().getJppfClient(null).addClientListener(this);
		treeTable.expandAll();
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private void createTreeTableModel()
	{
		treeTableRoot = new DefaultMutableTreeNode(localize("tree.root.name"));
		model = new JobTreeTableModel(treeTableRoot);
	}

	/**
	 * Refresh the tree from the latest data found in the server.
	 * This method will clear the entire tree and repopulate it.
	 */
	public synchronized void refresh()
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				int n = treeTableRoot.getChildCount();
				for (int i=n-1; i>=0; i--)
				{
					DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
					try
					{
						((JobData) driverNode.getUserObject()).changeNotificationListener(null);
					}
					catch(Exception e) { if (debugEnabled) log.debug("while refreshing: " + e.getMessage(), e); }
					model.removeNodeFromParent(driverNode);
				}
				populateTreeTableModel();
				refreshUI();
			}
		};
		executor.submit(r);
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private synchronized void populateTreeTableModel()
	{
		List<JPPFClientConnection> list = StatsHandler.getInstance().getJppfClient(null).getAllConnections();
		for (JPPFClientConnection c: list)
		{
			JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) c;
			JobData data = new JobData(connection);
			DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(data);
			model.insertNodeInto(driverNode, treeTableRoot, treeTableRoot.getChildCount());
			JMXDriverConnectionWrapper wrapper = connection.getJmxConnection();
			if (wrapper == null) continue;
			if (listenerMap.get(wrapper.getId()) == null)
			{
				ConnectionStatusListener listener = new ConnectionStatusListener(wrapper.getId());
				connection.addClientConnectionStatusListener(listener);
				listenerMap.put(wrapper.getId(), listener);
			}
			DriverJobManagementMBean proxy = data.getProxy();
			if (proxy == null) continue;
			String[] jobIds = null;
			try
			{
				jobIds = proxy.getAllJobIds();
			}
			catch(Exception ex)
			{
				if (debugEnabled) log.debug("populating model: " + ex.getMessage(), ex);
				continue;
			}
			for (String id: jobIds)
			{
				JobInformation jobInfo = null;
				try
				{
					jobInfo = proxy.getJobInformation(id);
				}
				catch(Exception e)
				{
					if (debugEnabled) log.debug("populating model: " + e.getMessage(), e);
					continue;
				}
				if (jobInfo != null)
				{
					JobData jobData = new JobData(jobInfo);
					DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(jobData);
					model.insertNodeInto(jobNode, driverNode, driverNode.getChildCount());
					NodeJobInformation[] subJobInfo = null;
					try
					{
						subJobInfo = proxy.getNodeInformation(id);
					}
					catch(Exception e)
					{
						if (debugEnabled) log.debug("populating model: " + e.getMessage(), e);
						continue;
					}
					for (NodeJobInformation nji: subJobInfo)
					{
						JobData subJobData = new JobData(nji.jobInfo, nji.nodeInfo);
						DefaultMutableTreeNode subJobNode = new DefaultMutableTreeNode(subJobData);
						model.insertNodeInto(subJobNode, jobNode, jobNode.getChildCount());
					}
				}
			}
			try
			{
				data.changeNotificationListener(new JobNotificationListener(this, wrapper.getId()));
			}
			catch(Exception e)
			{
				if (debugEnabled) log.debug("populating model: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Create, initialize and layout the GUI components displayed in this panel.
	 */
	public void createUI()
	{
	  treeTable = new JPPFTreeTable(model);
	  treeTable.getTree().setRootVisible(false);
	  treeTable.getTree().setShowsRootHandles(true);
		//treeTable.addMouseListener(new NodeTreeTableMouseListener());
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		treeTable.doLayout();
		treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		treeTable.getTree().setCellRenderer(new JobNodeRenderer());
		JScrollPane sp = new JScrollPane(treeTable);
		setUIComponent(sp);
	}

	/**
	 * Called to notify that a driver was added.
	 * @param clientConnection - a reference to the driver connection.
	 */
	public void driverAdded(final JPPFClientConnection clientConnection)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) clientConnection).getJmxConnection();
				if (findDriver(wrapper.getId()) != null) return;
				JobData data = new JobData(clientConnection);
				if (listenerMap.get(wrapper.getId()) == null)
				{
					ConnectionStatusListener listener = new ConnectionStatusListener(wrapper.getId());
					clientConnection.addClientConnectionStatusListener(listener);
					listenerMap.put(wrapper.getId(), listener);
				}
				DriverJobManagementMBean proxy = data.getProxy();
				proxy.addNotificationListener(new JobNotificationListener(JobDataPanel.this, wrapper.getId()), null, null);
				DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(data);
				if (debugEnabled) log.debug("adding driver: " + wrapper.getId());
				model.insertNodeInto(driverNode, treeTableRoot, treeTableRoot.getChildCount());
				treeTable.expand(driverNode);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param driverName - the name of the driver to remove.
	 * @see org.jppf.ui.monitoring.event.NodeHandlerListener#driverRemoved(org.jppf.ui.monitoring.event.NodeHandlerEvent)
	 */
	public void driverRemoved(final String driverName)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (debugEnabled) log.debug("removing driver: " + driverName);
				if (driverNode == null) return;
				try
				{
					JobData data = (JobData) driverNode.getUserObject();
					data.changeNotificationListener(null);
				}
				catch(Exception e)
				{
					log.error(e.getMessage(), e);
				}
				model.removeNodeFromParent(driverNode);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a job was submitted to a driver.
	 * @param driverName - the name of the driver the job was submitted to.
	 * @param jobInfo - information about the submitted job.
	 */
	public void jobAdded(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (driverNode == null) return;
				JobData data = new JobData(jobInfo);
				DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(data);
				if (debugEnabled) log.debug("adding job: " + jobInfo.getJobId() + " to driver " + driverName);
				model.insertNodeInto(jobNode, driverNode, driverNode.getChildCount());
				treeTable.expand(jobNode);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a job was removed from a driver.
	 * @param driverName - the name of the driver the job was submitted to.
	 * @param jobInfo - information about the job.
	 */
	public void jobRemoved(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
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
	public void jobUpdated(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
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
	public void subJobAdded(final String driverName, final JobInformation jobInfo, final NodeManagementInfo nodeInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				DefaultMutableTreeNode driverNode = findDriver(driverName);
				if (driverNode == null) return;
				DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
				if (jobNode == null) return;
				JobData data = new JobData(jobInfo, nodeInfo);
				DefaultMutableTreeNode subJobNode = new DefaultMutableTreeNode(data);
				if (debugEnabled) log.debug("sub-job: " + jobInfo.getJobId() + " dispatched to node " + nodeInfo.getHost() + ":" + nodeInfo.getPort());
				model.insertNodeInto(subJobNode, jobNode, jobNode.getChildCount());
				treeTable.expand(subJobNode);
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
	public void subJobRemoved(final String driverName, final JobInformation jobInfo, final NodeManagementInfo nodeInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
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
		for (int i=0; i<treeTableRoot.getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) treeTableRoot.getChildAt(i);
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
	 * Notifiy this listener that a new driver connection was created.
	 * @param event - the event to notify this listener of.
	 * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
	 */
	public synchronized void newConnection(ClientEvent event)
	{
		driverAdded(event.getConnection());
	}

	/**
	 * Refreshes the tree table display.
	 */
	public void refreshUI()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				treeTable.invalidate();
				treeTable.doLayout();
				treeTable.updateUI();
			}
		});
	}

	/**
	 * Listens to JPPF client connection status changes for rendering purposes.
	 */
	public class ConnectionStatusListener implements ClientConnectionStatusListener
	{
		/**
		 * The name of the connection.
		 */
		String driverName = null;

		/**
		 * Initialize this listener with the specified connection name.
		 * @param driverName - the name of the connection.
		 */
		public ConnectionStatusListener(String driverName)
		{
			this.driverName = driverName;
		}

		/**
		 * Invoked when thew conneciton status has changed.
		 * @param event - the connection status event.
		 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
		 */
		public void statusChanged(ClientConnectionStatusEvent event)
		{
			DefaultMutableTreeNode driverNode = findDriver(driverName);
			if (driverNode != null) model.changeNode(driverNode);
		}
	}
}
