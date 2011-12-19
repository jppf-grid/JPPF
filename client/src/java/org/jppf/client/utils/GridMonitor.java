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
	 * The host of the driver. 
	 */
	private String driverHost;
	/**
	 * Management port used by the driver.
	 */
	private int managementPort;
	/**
	 * Wrapper for the driver's JMX connection.
	 */
	private JPPFManagement driver;
	/**
	 * Holds the memory usage snapshots for all the nodes.
	 */
	private Map<String, JPPFManagement> nodes = new TreeMap<String, JPPFManagement>();
	/**
	 * The timer that schedules regular memory snapshots of the nodes.
	 */
	private Timer timer = null;
	/**
	 * The timer task that takes the snapshots.
	 */
	private DataUpdateTask dataUpdateTask = null;
	/**
	 * Holds the memory usage snapshots for all the nodes.
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
	 * Initialize this grid monitor with the specified parameters.
	 * @param driverHost the host of the driver.
	 * @param managementPort management port used by the driver.
	 * @throws Exception if any error occurs.
	 */
	public GridMonitor(String driverHost, int managementPort) throws Exception
	{
		this.driverHost = driverHost;
		this.managementPort = managementPort;
		init();
	}

	/**
	 * Initialize this grid monitor with the specified parameters.
	 * @param driverHost the host of the driver.
	 * @param managementPort management port used by the driver.
	 * @param snapshotInterval the interval at which memory usage snapshots are taken.
	 * @throws Exception if any error occurs.
	 */
	public GridMonitor(String driverHost, int managementPort, long snapshotInterval) throws Exception
	{
		this.driverHost = driverHost;
		this.managementPort = managementPort;
		init();
	}

	/**
	 * Initialize this grid monitor with the specified JPPF client and the default (1 second) snpashot interval.
	 * @param jppfClient the JPPF client from which to get the JMX connections.
	 * @throws Exception if any error occurs during initialization.
	 */
	public GridMonitor(JPPFClient jppfClient) throws Exception
	{
		this.jppfClient = jppfClient;
		init();
	}

	/**
	 * Initialize this grid monitor with the specified JPPF client and snpashot interval.
	 * @param jppfClient the JPPF client from which to get the JMX connections.
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
		driver = new JPPFManagement();
		// if the JPPF client was provided in the constructor
		if (jppfClient != null)
		{
			JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
			// get the connection to the driver's JMX connection
			driver.jmx = connection.getJmxConnection();
		}
		// else if we provided the driver host and jmx port
		else
		{
			driver.jmx = new JMXDriverConnectionWrapper(driverHost, managementPort);
			driver.jmx.connect();
		}
		// wait until the jmx connection is established with the driver
		while (!driver.jmx.isConnected()) Thread.sleep(1L);
		driver.memoryMBean = ManagementFactory.newPlatformMXBeanProxy(driver.jmx.getMbeanConnection(), "java.lang:type=Memory", MemoryMXBean.class);
		driver.runtimeMBean = ManagementFactory.newPlatformMXBeanProxy(driver.jmx.getMbeanConnection(), "java.lang:type=Runtime", RuntimeMXBean.class);
		// get the JMX connection information for the nodes attached to the driver 
		Collection<JPPFManagementInfo> infoCollection = ((JMXDriverConnectionWrapper) driver.jmx).nodesInformation();
		for (JPPFManagementInfo info: infoCollection)
		{
			// get the connection to the node's JMX connection
			JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
			node.connect();
			// wait until the jmx connection is established with the node
			while (!node.isConnected()) Thread.sleep(1L);
			// get a proxy to the MXBean that collects heap usage data for the node
			JPPFManagement nodeMgt = new JPPFManagement();
			nodeMgt.jmx = node;
			nodeMgt.memoryMBean = ManagementFactory.newPlatformMXBeanProxy(node.getMbeanConnection(), "java.lang:type=Memory", MemoryMXBean.class);
			nodeMgt.runtimeMBean = ManagementFactory.newPlatformMXBeanProxy(node.getMbeanConnection(), "java.lang:type=Runtime", RuntimeMXBean.class);
			nodes.put(node.getId(), nodeMgt);
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
		driver.jmx.close();
		driver = null;
		for (JPPFManagement node: nodes.values()) node.jmx.close();
		nodes.clear();
	}

	/**
	 * Container class that holds references to JMX-related objects for one node.
	 * @see java.lang.management.MemoryMXBean
	 */
	public static class JPPFManagement
	{
		/**
		 * Wrapper around the node JMX connection.
		 */
		public JMXConnectionWrapper jmx;
		/**
		 * Proxy to the MXBean that collects heap usage data for the node.
		 */
		public MemoryMXBean memoryMBean;
		/**
		 * Proxy to the MXBean that holds runtime information about the JVM.
		 */
		public RuntimeMXBean runtimeMBean;
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
				for (Map.Entry<String, JPPFManagement> entry: nodes.entrySet())
				{
					JPPFManagement node = entry.getValue();
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

	/**
	 * Get the object that holds the JMX connection to the driver.
	 * @return the <code>JPPFManagement</code> instance for the driver.
	 */
	public JPPFManagement getDriver()
	{
		return driver;
	}


	/**
	 * Get the objects that hold the JMX connection to the nodes.
	 * @return an array of <code>JPPFManagement</code> instances for the nodes.
	 */
	public JPPFManagement[] getNodes()
	{
		return nodes.values().toArray(new JPPFManagement[0]);
	}

	/**
	 * This method demonstrates how the APIs in this class can be used to
	 * find the Process ID for the driver and nodes. 
	 */
	public void testPIDs()
	{
		JPPFManagement driver = getDriver();
		System.out.println("driver: " + getInfo(driver));
		JPPFManagement[] nodes = getNodes();
		for (JPPFManagement node: nodes) System.out.println("node: " + getInfo(node));
	}

	/**
	 * Compute a string that provides information about a driver or node, including the PID
	 * @param mgt the driver or node to process.
	 * @return a string that contains the host name or IP, jmx port number and PID for the node or driver.
	 */
	public String getInfo(JPPFManagement mgt)
	{
		StringBuilder sb = new StringBuilder();
		// toString() is "hostname:port" as displayed in the JPPF admin console
		sb.append(mgt.jmx.getId()).append(", PID = ");
		// we expect the name to be in '<pid>@hostname' format - this is JVM dependant
		String name = mgt.runtimeMBean.getName();
		int pid = -1;
		int idx = name.indexOf('@');
		if (idx >= 0)
		{
			try
			{
				pid = Integer.valueOf(name.substring(0, idx));
			}
			catch (Exception ignore)
			{
			}
		}
		sb.append(pid);
		return sb.toString();
	}
}
