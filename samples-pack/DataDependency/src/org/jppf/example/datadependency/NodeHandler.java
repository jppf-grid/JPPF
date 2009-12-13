/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.example.datadependency;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.example.datadependency.model.MarketData;
import org.jppf.example.datadependency.simulation.*;
import org.jppf.management.*;
import org.jppf.utils.TypedProperties;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeHandler implements TickerListener
{
	/**
	 * Name of the class of an array of <code>MarketData</code> objects. 
	 */
	private static final String MARKET_DATA_ARRAY_CLASS_NAME = MarketData[].class.getName();
	/**
	 * Name of the class of an array of <code>MarketData</code> objects. 
	 */
	private static final String[] SIGNATURE = { MarketData[].class.getName() };
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeHandler.class);
	/**
	 * Debug enabled flag.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The driver's JMX wrapper.
	 */
	private JMXDriverConnectionWrapper driver = null;
	/**
	 * The nodes' JMX wrappers.
	 */
	private List<JMXNodeConnectionWrapper> nodes = new ArrayList<JMXNodeConnectionWrapper>();
	/**
	 * Executes the market data updates in the nodes.
	 */
	private ExecutorService nodeExecutor = Executors.newFixedThreadPool(1);

	/**
	 * Perform the nodes' initializations.
	 * @param marketDataList the initial market data to send to each node.
	 * @return the list of node ids. 
	 * @throws Exception if any error is raised.
	 */
	public List<Integer> initNodes(List<MarketData> marketDataList) throws Exception
	{
		JMXDriverConnectionWrapper driver = new JMXDriverConnectionWrapper("localhost", 11198);
		driver.connectAndWait(0);
		Collection<JPPFManagementInfo> nodesInfo = driver.nodesInformation();
		
		MarketData[] data = marketDataList.toArray(new MarketData[0]);
		List<Integer> idList = new ArrayList<Integer>();
		int nodeCount = 0;
		for (JPPFManagementInfo info: nodesInfo)
		{
			JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
			node.connectAndWait(0);
			JPPFSystemInformation sysInfo = node.systemInformation();
			TypedProperties config = sysInfo.getJppf();
			nodeCount++;
			int n = config.getInt("id", -1);
			if (n >= 0)
			{
				nodes.add(node);
				idList.add(n);
			}
			node.invoke("org.jppf.example.mbean:name=Data,type=node", "updateMarketData", new Object[] { data }, SIGNATURE );
		}
		return idList;
	}

	/**
	 * Called when a piece of market data was updated.
	 * @param event encapsulated the market data update.
	 * @see org.jppf.example.datadependency.simulation.TickerListener#marketDataUpdated(org.jppf.example.datadependency.simulation.TickerEvent)
	 */
	public void marketDataUpdated(TickerEvent event)
	{
		nodeExecutor.submit(new NodesUpdateTask(new MarketData[] { event.getMarketData() }));
	}

	/**
	 * Close this node handler and release the resources it uses.
	 */
	public void close()
	{
		for (JMXNodeConnectionWrapper node: nodes)
		{
			try
			{
				node.close();
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
			nodeExecutor.shutdownNow();
		}
	}

	/**
	 * Performs the nodes updates.
	 */
	public class NodesUpdateTask implements Runnable
	{
		/**
		 * The update data to send to the nodes.
		 */
		private Object[] arguments;
		
		/**
		 * Initialize this task with the update dmarket data.
		 * @param data the update ddata.
		 */
		public NodesUpdateTask(final MarketData[] data)
		{
			arguments = new Object[] { data };
		}

		/**
		 * Execute this task.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			for (int i=0; i<nodes.size(); i++)
			{
				final JMXNodeConnectionWrapper node = nodes.get(i);
				Runnable r = new Runnable()
				{
					public void run()
					{
						try
						{
							node.invoke("org.jppf.example.mbean:name=Data,type=node", "updateMarketData", arguments, SIGNATURE);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				};
				new Thread(r).start();
			}
		}
	}
}
