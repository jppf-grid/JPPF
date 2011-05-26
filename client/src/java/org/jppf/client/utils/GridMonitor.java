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

package org.jppf.client.utils;

import java.io.*;
import java.lang.management.*;
import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;

/**
 * This class monitors each of the nodes and their memory usage during execution.
 * <p>example use:<br/>
 * <pre>
 * JPPFClient client = ...;
 * // take memory usage snapshots at approximately 1 second intervals
 * GridMonitor monitor = new GridMonitor(client, 1000L);
 * monitor.startMonitoring();
 * JPPFJob job = ...;
 * List&lt;JPPFTask&gt; results = client.submit(job);
 * monitor.stopMonitoring();
 * // store snapshots in the specified directory
 * // there will be one .csv file per node
 * monitor.storeData("./GridMonitoring");
 * monitor.close();
 * </pre>
 * @author Laurent Cohen
 */
public class GridMonitor
{
	/**
	 * The JPPF client from which to get the JMX connections.
	 */
	private JPPFClient jppfClient = null;
	/**
	 * Wrapper for the driver's JMX connection.
	 */
	private JMXDriverConnectionWrapper driver;
	/**
	 * Holds the memory usage snapshots for al the nodes.
	 */
	private Map<String, NodeManagement> nodes = new TreeMap<String, NodeManagement>();
	/**
	 * The timer that schedules regular memory snapshots of the nodes.
	 */
	private Timer timer = null;
	/**
	 * The timer task that takes the snapshots.
	 */
	private DataUpdateTask dataUpdateTask = null;
	/**
	 * Holds the memory usage snapshots for al the nodes.
	 */
	private Map<String, List<NodeData>> dataMap = new TreeMap<String, List<NodeData>>();
	/**
	 * Interval at which memory usage snapshots are taken.
	 */
	private long snapshotInterval = 1000L;

	/**
	 * The time at which this monitor is started.
	 */
	private long startTime = 0;

	/**
	 * Initialize this grid monitor with the specified JPPF client and the default (1 second) snpashot interval.
	 * @param jppfClient the JPPF client from which to egt the JMX connections.
	 * @throws Exception if any error occurs during initialization.
	 */
	public GridMonitor(JPPFClient jppfClient) throws Exception
	{
		this.jppfClient = jppfClient;
		init();
	}

	/**
	 * Initialize this grid monitor with the specified JPPF client and snpashot interval.
	 * @param jppfClient the JPPF client from which to egt the JMX connections.
	 * @param snapshotInterval the interval at which memory usage snapshots are taken.
	 * @throws Exception if any error occurs during initialization.
	 */
	public GridMonitor(JPPFClient jppfClient, long snapshotInterval) throws Exception
	{
		this.jppfClient = jppfClient;
		this.snapshotInterval = snapshotInterval;
		init();
	}

	/**
	 * Obtain a JMX conection to each node using the driver's management MBean
	 * @throws Exception if any error occurs.
	 * @see org.jppf.management.JMXDriverConnectionWrapper
	 * @see org.jppf.management.JMXNodeConnectionWrapper
	 * @see java.lang.management.ManagementFactory
	 * @see java.lang.management.MemoryMXBean
	 */
	private void init() throws Exception
	{
		JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
		// get the connection to the driver's JMX connection
		driver = connection.getJmxConnection();
		// get the JMX connection information for the nodes attached to the driver 
		Collection<JPPFManagementInfo> infoCollection = driver.nodesInformation();
		for (JPPFManagementInfo info: infoCollection)
		{
			// get the connection to the node's JMX connection
			JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
			node.connect();
			while (!node.isConnected()) Thread.sleep(1L);
			// get a proxy to the MXBean that collects heap usage data for the node
			MemoryMXBean proxy = ManagementFactory.newPlatformMXBeanProxy(node.getMbeanConnection(), "java.lang:type=Memory", MemoryMXBean.class);
			nodes.put(node.getId(), new NodeManagement(node, proxy));
		}
	}

	/**
	 * Start this monitor and associated resources.
	 */
	public void startMonitoring()
	{
		// all snapshots timestamps are relative to the start time.
		startTime = System.currentTimeMillis();
		// start the timer
		timer = new Timer("GridMonitoring");
		timer.schedule(new DataUpdateTask(), 0L, snapshotInterval);
	}

	/**
	 * Stop monitoring the nodes.
	 */
	public void stopMonitoring()
	{
		if (timer != null)
		{
			// cancel all scheduled tasks and prevent new ones from being scheduled
			timer.cancel();
			// remove all cancelled tasks from the timer's queue
			timer.purge();
			timer = null;
		}
	}

	/**
	 * Close this monitpr and release the resources it uses.
	 * @throws Exception if any error occurs.
	 */
	public void close() throws Exception
	{
		stopMonitoring();
		driver.close();
		driver = null;
		for (NodeManagement node: nodes.values()) node.nodeMBean.close();
		nodes.clear();
	}

	/**
	 * Container class that holds references to JMX-related objects for one node.
	 * @see java.lang.management.MemoryMXBean
	 */
	public static class NodeManagement
	{
		/**
		 * Wrapper around the node JMX connection.
		 */
		public JMXNodeConnectionWrapper nodeMBean;
		/**
		 * Proxy to the MXBean that collects heap usage data for the node.
		 */
		public MemoryMXBean memoryMBean;

		/**
		 * Initialize this object with the specified parameters.
		 * @param nodeMBean rapper around the node JMX connection.
		 * @param memoryMBean proxy to the MXBean that collects heap usage data for the node.
		 */
		public NodeManagement(JMXNodeConnectionWrapper nodeMBean, MemoryMXBean memoryMBean)
		{
			this.nodeMBean = nodeMBean;
			this.memoryMBean = memoryMBean;
		}
	}

	/**
	 * Instances of this class represent a snapshot of the heap
	 * memory usage of a node at a given time.
	 */
	public static class NodeData
	{
		/**
		 * Date and time (on the client) at wich this snapshot is taken.
		 */
		public long timestamp;
		/**
		 * current heap size.
		 */
		public long committed;
		/**
		 * used heap.
		 */
		public long used;
		/**
		 * max heap size - used heap
		 */
		public long maxAvailable;

		/**
		 * Get the header.
		 * @return The list of column titles in csv format.
		 */
		public static String getHeader()
		{
			return "timestamp,committed,used,maxAvailable";
		}

		/**
		 * Get this data snapshot as a CSV-formatted string.
		 * @return the data in CSV format.
		 */
		public String toCSV()
		{
			StringBuilder sb = new StringBuilder();
			sb.append(timestamp).append(",");
			sb.append(committed).append(",");
			sb.append(used).append(",");
			sb.append(maxAvailable);
			return sb.toString();
		}
	}

	/**
	 * This task is run at regular intervals by a timer
	 * and takes a snapshot of the heap memory usage for each node.
	 * @see java.lang.management.ManagementFactory
	 * @see java.lang.management.MemoryMXBean
	 */
	public class DataUpdateTask extends TimerTask
	{
		/**
		 * {@inheritDoc}
		 */
		public void run()
		{
			try
			{
				for (Map.Entry<String, NodeManagement> entry: nodes.entrySet())
				{
					NodeManagement node = entry.getValue();
					String id = entry.getKey();
					NodeData data = new NodeData();
					// timestamps are relative to the start time.
					data.timestamp = System.currentTimeMillis() - startTime;
					// get the snapshot of the nodes memory usage
					MemoryUsage memUsage = node.memoryMBean.getHeapMemoryUsage();
					// current heap size
					data.committed = memUsage.getCommitted();
					// current used memory
					data.used = memUsage.getUsed();
					// current maximum available heap 
					data.maxAvailable = memUsage.getMax() - data.used;
					synchronized(dataMap)
					{
						List<NodeData> list = dataMap.get(id);
						if (list == null)
						{
							list = new ArrayList<NodeData>();
							dataMap.put(id, list);
						}
						list.add(data);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Store all data snapshots to file. One CSV file is created for each node.
	 * @param folder path to the folder where the files are created. This folder is created if needed.
	 */
	public void storeData(String folder)
	{
		try
		{
			File dir = new File(folder);
			// create the folders if they don't exst
			if (!dir.exists()) dir.mkdirs();
			int nodeCount = 1;
			for (Map.Entry<String, List<NodeData>> entry: dataMap.entrySet())
			{
				// create a file for each node, named node-x.csv
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir, "node-" + nodeCount + ".csv")));
				// write the node name or ip
				writer.write("node:," + entry.getKey() + "\n");
				// write the comumn names
				writer.write(NodeData.getHeader() + "\n");
				// write each sdapshot as a CSV-formatted row
				for (NodeData data: entry.getValue()) writer.write(data.toCSV() + "\n");
				writer.flush();
				writer.close();
				nodeCount++;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
