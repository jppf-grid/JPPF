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

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.job.JobInformation;
import org.jppf.management.*;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.ui.treetable.JPPFTreeTable;
import org.slf4j.*;

/**
 * This class manages updates to, and navigation within, the tree table
 * for the job data panel.
 * @author Laurent Cohen
 */
class JobDataPanelManager
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(JobDataPanelManager.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The job data panel holding this manager.
	 */
	private JobDataPanel jobPanel = null;
	/**
	 * Mapping of connection names to status listener.
	 */
	private Map<String, ConnectionStatusListener> listenerMap = new Hashtable<String, ConnectionStatusListener>();

	/**
	 * Initialize this job data panel manager.
	 * @param jobPanel the job data panel holding this manager.
	 */
	public JobDataPanelManager(final JobDataPanel jobPanel)
	{
		this.jobPanel = jobPanel;
	}

	/**
	 * Called to notify that a driver was added.
	 * @param clientConnection a reference to the driver connection.
	 */
	public void driverAdded(final JPPFClientConnection clientConnection)
	{
		JMXDriverConnectionWrapper wrapper = ((JPPFClientConnectionImpl) clientConnection).getJmxConnection();
		String driverName = wrapper.getId();
		int index = driverInsertIndex(driverName);
		if (index < 0) return;
		JobData data = new JobData(clientConnection);
		if (listenerMap.get(wrapper.getId()) == null)
		{
			ConnectionStatusListener listener = new ConnectionStatusListener(wrapper.getId());
			clientConnection.addClientConnectionStatusListener(listener);
			listenerMap.put(driverName, listener);
		}
		DriverJobManagementMBean proxy = data.getProxy();
		if (proxy != null) proxy.addNotificationListener(new JobNotificationListener(jobPanel, wrapper.getId()), null, null);
		DefaultMutableTreeNode driverNode = new DefaultMutableTreeNode(data);
		if (debugEnabled) log.debug("adding driver: " + driverName + " at index " + index);
		jobPanel.getModel().insertNodeInto(driverNode, jobPanel.getTreeTableRoot(), index);
		JPPFTreeTable treeTable = jobPanel.getTreeTable();
		if (treeTable != null)
		{
			treeTable.expand(jobPanel.getTreeTableRoot());
			treeTable.expand(driverNode);
		}
	}

	/**
	 * Called to notify that a driver was removed.
	 * @param driverName the name of the driver to remove.
	 */
	public void driverRemoved(final String driverName)
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
		jobPanel.getModel().removeNodeFromParent(driverNode);
	}

	/**
	 * Called to notify that a job was submitted to a driver.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the submitted job.
	 */
	public void jobAdded(final String driverName, final JobInformation jobInfo)
	{
		DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		JobData data = new JobData(jobInfo);
		JobData driverData = (JobData) driverNode.getUserObject();
		data.setJmxWrapper(driverData.getJmxWrapper());
		int index  = jobInsertIndex(driverNode, jobInfo);
		if (index < 0) return;
		DefaultMutableTreeNode jobNode = new DefaultMutableTreeNode(data);
		if (debugEnabled) log.debug("adding job: " + jobInfo.getJobId() + " to driver " + driverName + " at index " + index);
		jobPanel.getModel().insertNodeInto(jobNode, driverNode, index);
		jobPanel.getTreeTable().expand(driverNode);
	}

	/**
	 * Called to notify that a job was removed from a driver.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the job.
	 */
	public void jobRemoved(final String driverName, final JobInformation jobInfo)
	{
		DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
		if (jobNode == null) return;
		if (debugEnabled) log.debug("removing job: " + jobInfo.getJobId() + " from driver " + driverName);
		jobPanel.getModel().removeNodeFromParent(jobNode);
		jobPanel.getTreeTable().repaint();
	}

	/**
	 * Called to notify that a job was removed from a driver.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the job.
	 */
	public void jobUpdated(final String driverName, final JobInformation jobInfo)
	{
		DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
		if (jobNode == null) return;
		if (debugEnabled) log.debug("updating job: " + jobInfo.getJobId() + " from driver " + driverName);
		JobData data = new JobData(jobInfo);
		JobData driverData = (JobData) driverNode.getUserObject();
		data.setJmxWrapper(driverData.getJmxWrapper());
		jobNode.setUserObject(data);
		jobPanel.getModel().changeNode(jobNode);
		//treeTable.invalidate();
	}

	/**
	 * Called to notify that a sub-job was dispatched to a node.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the sub-job.
	 * @param nodeInfo information about the node where the sub-job was dispatched.
	 */
	public void subJobAdded(final String driverName, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo)
	{
		DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
		if (jobNode == null) return;
		JobData data = new JobData(jobInfo, nodeInfo);
		int index = subJobInsertIndex(jobNode, nodeInfo);
		if (index < 0) return;
		DefaultMutableTreeNode subJobNode = new DefaultMutableTreeNode(data);
		if (debugEnabled) log.debug("sub-job: " + jobInfo.getJobId() + " dispatched to node " + nodeInfo.getHost() + ':' + nodeInfo.getPort() + "(index " + index + ')');
		jobPanel.getModel().insertNodeInto(subJobNode, jobNode, index);
		jobPanel.getTreeTable().expand(jobNode);
	}

	/**
	 * Called to notify that a sub-job was removed from a node.
	 * @param driverName the name of the driver the job was submitted to.
	 * @param jobInfo information about the job.
	 * @param nodeInfo information about the node where the sub-job was dispatched.
	 */
	public void subJobRemoved(final String driverName, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo)
	{
		DefaultMutableTreeNode driverNode = findDriver(driverName);
		if (driverNode == null) return;
		DefaultMutableTreeNode jobNode = findJob(driverNode, jobInfo);
		if (jobNode == null) return;
		DefaultMutableTreeNode subJobNode = findSubJob(jobNode, nodeInfo);
		if (subJobNode == null) return;
		if (debugEnabled) log.debug("removing sub-job: " + jobInfo.getJobId() + " from node " + nodeInfo.getHost() + ':' + nodeInfo.getPort());
		jobPanel.getModel().removeNodeFromParent(subJobNode);
		jobPanel.getTreeTable().repaint();
	}

	/**
	 * Find the driver tree node with the specified driver name.
	 * @param driverName name of the dirver to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the driver could not be found.
	 */
	DefaultMutableTreeNode findDriver(final String driverName)
	{
		for (int i=0; i<jobPanel.getTreeTableRoot().getChildCount(); i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) jobPanel.getTreeTableRoot().getChildAt(i);
			JobData data = (JobData) driverNode.getUserObject();
			JMXDriverConnectionWrapper wrapper = data.getJmxWrapper();
			if (wrapper.getId().equals(driverName)) return driverNode;
		}
		return null;
	}

	/**
	 * Find the job with the specified id that was submitted to the specified driver.
	 * @param driverNode the driver where the job was submitted.
	 * @param jobInfo information about the job to find.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the job could not be found.
	 */
	DefaultMutableTreeNode findJob(final DefaultMutableTreeNode driverNode, final JobInformation jobInfo)
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
	 * @param jobNode the job whose sub-job we are looking for.
	 * @param nodeInfo holds information on the node to which the sub-job was dispatched.
	 * @return a <code>DefaultMutableTreeNode</code> or null if the sub-job could not be found.
	 */
	DefaultMutableTreeNode findSubJob(final DefaultMutableTreeNode jobNode, final JPPFManagementInfo nodeInfo)
	{
		if (nodeInfo == null) return null;
		for (int i=0; i<jobNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) jobNode.getChildAt(i);
			JobData data = (JobData) node.getUserObject();
			if ((data == null) || (data.getNodeInformation() == null)) return null;
			if (data.getNodeInformation().getId().equals(nodeInfo.getId())) return node;
		}
		return null;
	}

	/**
	 * Find the position at which to insert a driver,
	 * using the sorted lexical order of driver names.
	 * @param driverName the name of the driver to insert.
	 * @return the index at which to insert the driver, or -1 if the driver is already in the tree.
	 */
	int driverInsertIndex(final String driverName)
	{
		DefaultMutableTreeNode root = jobPanel.getTreeTableRoot();
		int n = root.getChildCount();
		for (int i=0; i<n; i++)
		{
			DefaultMutableTreeNode driverNode = (DefaultMutableTreeNode) root.getChildAt(i);
			JobData data = (JobData) driverNode.getUserObject();
			String name = data.getJmxWrapper().getId();
			if (name.equals(driverName)) return -1;
			else if (driverName.compareTo(name) < 0) return i;
		}
		return n;
	}

	/**
	 * Find the position at which to insert a job, using the sorted lexical order of job names.
	 * @param driverNode name the parent tree node of the job to insert.
	 * @param jobInfo information about the job to insert.
	 * @return the index at which to insert the job, or -1 if the job is already in the tree.
	 */
	int jobInsertIndex(final DefaultMutableTreeNode driverNode, final JobInformation jobInfo)
	{
		int n = driverNode.getChildCount();
		String jobName = jobInfo.getJobId();
		for (int i=0; i<n; i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
			JobData jobData = (JobData) node.getUserObject();
			String name = jobData.getJobInformation().getJobId();
			if (jobName.equals(name)) return -1;
			else if (jobName.compareTo(name) < 0) return i;
		}
		return n;
	}

	/**
	 * Find the position at which to insert a subjob, using the sorted lexical order of subjob names.
	 * @param jobNode the parent tree node of the subjob to insert.
	 * @param nodeInfo information about the subjob to insert.
	 * @return the index at which to insert the subjob, or -1 if the subjob is already in the tree.
	 */
	int subJobInsertIndex(final DefaultMutableTreeNode jobNode, final JPPFManagementInfo nodeInfo)
	{
		int n = jobNode.getChildCount();
		String subJobName = nodeInfo.toString();
		for (int i=0; i<n; i++)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) jobNode.getChildAt(i);
			JobData jobData = (JobData) node.getUserObject();
			if ((jobData == null) || (jobData.getNodeInformation() == null)) continue;
			String name = jobData.getNodeInformation().toString();
			if (subJobName.equals(name)) return -1;
			else if (subJobName.compareTo(name) < 0) return i;
		}
		return n;
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
		 * @param driverName the name of the connection.
		 */
		public ConnectionStatusListener(final String driverName)
		{
			this.driverName = driverName;
		}

		/**
		 * Invoked when thew conneciton status has changed.
		 * @param event the connection status event.
		 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
		 */
		@Override
		public void statusChanged(final ClientConnectionStatusEvent event)
		{
			DefaultMutableTreeNode driverNode = findDriver(driverName);
			if (driverNode != null) jobPanel.getModel().changeNode(driverNode);
		}
	}
}
