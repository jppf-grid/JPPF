/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.ui.monitoring.job;

import java.util.List;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.job.JobInformation;
import org.jppf.management.*;
import org.jppf.server.job.management.*;
import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.job.actions.*;
import org.jppf.ui.treetable.*;
import org.jppf.utils.SynchronizedTask;
import org.slf4j.*;

/**
 * Panel displaying the tree of all driver connections and attached nodes.
 * @author Laurent Cohen
 */
public class JobDataPanel extends AbstractTreeTableOption implements ClientListener
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(JobDataPanel.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Separate thread used to sequentialize events that impact the job data tree.
	 */
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	/**
	 * The object that manages updates to and navigation within the tree table.
	 */
	private JobDataPanelManager panelManager = null;

	/**
	 * Initialize this panel with the specified information.
	 */
	public JobDataPanel()
	{
		if (debugEnabled) log.debug("initializing NodeDataPanel");
		BASE = "org.jppf.ui.i18n.JobDataPage";
		createTreeTableModel();
		createUI();
		panelManager = new JobDataPanelManager(this);
	  populateTreeTableModel();
	  StatsHandler.getInstance().getJppfClient(null).addClientListener(this);
		treeTable.expandAll();
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private void createTreeTableModel()
	{
		treeTableRoot = new DefaultMutableTreeNode(localize("job.tree.root.name"));
		model = new JobTreeTableModel(treeTableRoot);
	}

	/**
	 * Refresh the tree from the latest data found in the server.
	 * This method will clear the entire tree and repopulate it.
	 */
	public synchronized void refresh()
	{
		executor.submit(new RefreshTask());
	}

	/**
	 * Create and initialize the tree table model holding the drivers and nodes data.
	 */
	private synchronized void populateTreeTableModel()
	{
		List<JPPFClientConnection> list = StatsHandler.getInstance().getJppfClient(null).getAllConnections();
		for (JPPFClientConnection c: list)
		{
			panelManager.driverAdded(c);
			JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) c;
			JMXDriverConnectionWrapper wrapper = connection.getJmxConnection();
			if (wrapper == null) continue;
			String driverName = wrapper.getId();
			DefaultMutableTreeNode driverNode = panelManager.findDriver(driverName);
			if (driverNode == null) continue;
			JobData driverData = (JobData) driverNode.getUserObject();
			DriverJobManagementMBean proxy = driverData.getProxy();
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
				}
				if (jobInfo == null) continue;
				panelManager.jobAdded(driverName, jobInfo);
				DefaultMutableTreeNode jobNode = panelManager.findJob(driverNode, jobInfo);
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
				for (NodeJobInformation nji: subJobInfo) panelManager.subJobAdded(driverName, nji.jobInfo, nji.nodeInfo);
			}
		}
	}

	/**
	 * Create, initialize and layout the GUI components displayed in this panel.
	 */
	public void createUI()
	{
	  treeTable = new JPPFTreeTable(model);
	  treeTable.getTree().setLargeModel(true);
	  treeTable.getTree().setRootVisible(false);
	  treeTable.getTree().setShowsRootHandles(true);
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
		treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		treeTable.doLayout();
		treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		treeTable.getTree().setCellRenderer(new JobRenderer());
		treeTable.setDefaultRenderer(Object.class, new JobTableCellRenderer());
		JScrollPane sp = new JScrollPane(treeTable);
		setUIComponent(sp);
		//setupActions();
	}

	/**
	 * Called to notify that a driver was added.
	 * @param clientConnection a reference to the driver connection.
	 */
	public void driverAdded(final JPPFClientConnection clientConnection)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				panelManager.driverAdded(clientConnection);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param driverName the name of the driver to remove.
	 */
	public void driverRemoved(final String driverName)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				panelManager.driverRemoved(driverName);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a job was submitted to a driver.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the submitted job.
	 */
	public void jobAdded(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				panelManager.jobAdded(driverName, jobInfo);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a job was removed from a driver.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the job.
	 */
	public void jobRemoved(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				panelManager.jobRemoved(driverName, jobInfo);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a job was removed from a driver.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the job.
	 */
	public void jobUpdated(final String driverName, final JobInformation jobInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				panelManager.jobUpdated(driverName, jobInfo);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a sub-job was dispatched to a node.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the sub-job.
	 * @param nodeInfo information about the node where the sub-job was dispatched.
	 */
	public void subJobAdded(final String driverName, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				panelManager.subJobAdded(driverName, jobInfo, nodeInfo);
			}
		};
		executor.submit(r);
	}

	/**
	 * Called to notify that a sub-job was removed from a node.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the job.
	 * @param nodeInfo information about the node where the sub-job was dispatched.
	 */
	public void subJobRemoved(final String driverName, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo)
	{
		Runnable r = new SynchronizedTask(this)
		{
			public void perform()
			{
				panelManager.subJobRemoved(driverName, jobInfo, nodeInfo);
			}
		};
		executor.submit(r);
	}

	/**
	 * Notifiy this listener that a new driver connection was created.
	 * @param event the event to notify this listener of.
	 * @see org.jppf.client.event.ClientListener#newConnection(org.jppf.client.event.ClientEvent)
	 */
	public synchronized void newConnection(ClientEvent event)
	{
		driverAdded(event.getConnection());
	}

	/**
	 * {@inheritDoc}
	 */
	public void connectionFailed(ClientEvent event)
	{
		JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) event.getConnection();
		driverRemoved(c.getJmxConnection().getId());
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
	 * Initialize all actions used in the panel.
	 */
	public void setupActions()
	{
		actionHandler = new JTreeTableActionHandler(treeTable);
		actionHandler.putAction("cancel.job", new CancelJobAction());
		actionHandler.putAction("suspend.job", new SuspendJobAction());
		actionHandler.putAction("suspend_requeue.job", new SuspendRequeueJobAction());
		actionHandler.putAction("resume.job", new ResumeJobAction());
		actionHandler.putAction("max.nodes.job", new UpdateMaxNodesAction());
		actionHandler.updateActions();
		treeTable.addMouseListener(new JobTreeTableMouseListener(actionHandler));
		Runnable r = new ActionsInitializer(this, "/job.toolbar");
		new Thread(r).start();
	}

	/**
	 * This task refreshes the entire job data panel.
	 */
	public class RefreshTask extends SynchronizedTask
	{
		/**
		 * Initialize this task.
		 */
		public RefreshTask()
		{
			super(JobDataPanel.this);
		}

		/**
		 * Perform the refresh.
		 */
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
				catch(Exception e)
				{
					if (debugEnabled) log.debug("while refreshing: " + e.getMessage(), e);
				}
				model.removeNodeFromParent(driverNode);
			}
			populateTreeTableModel();
			refreshUI();
		}
	}
}
