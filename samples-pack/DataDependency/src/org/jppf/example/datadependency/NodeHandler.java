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
	 * The driver's JMX wrapper.
	 */
	private JMXDriverConnectionWrapper driver = null;
	/**
	 * The nodes' JMX wrappers.
	 */
	private List<JMXNodeConnectionWrapper> nodes = new ArrayList<JMXNodeConnectionWrapper>();

	/**
	 * Perform the nodes' initializations.
	 * @param marketDataList the initial market data to send to each node.
	 * @return the number of nodes. 
	 * @throws Exception if any error is raised.
	 */
	public int initNodes(List<MarketData> marketDataList) throws Exception
	{
		JMXDriverConnectionWrapper driver = new JMXDriverConnectionWrapper("localhost", 11198);
		driver.connectAndWait(0);
		Collection<JPPFManagementInfo> nodesInfo = driver.nodesInformation();
		MarketData[] data = marketDataList.toArray(new MarketData[0]);
		int nodeCount = 0;
		for (JPPFManagementInfo info: nodesInfo)
		{
			JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
			node.connectAndWait(0);
			nodes.add(node);
			JPPFSystemInformation sysInfo = node.systemInformation();
			TypedProperties config = sysInfo.getJppf();
			nodeCount++;
			config.setProperty("id", "" + nodeCount);
			Map<String, String> newConfig = new HashMap<String, String>();
			for (Object o: config.keySet())
			{
				if (!(o instanceof String)) continue;
				String key = (String ) o;
				Object o2 = config.get(key);
				if (!(o2 instanceof String)) continue;
				newConfig.put(key, (String) o2);
			}
			node.updateConfiguration(newConfig, false);
			node.invoke("org.jppf.example.mbean:name=Data,type=node", "updateMarketData", new Object[] { data }, new String[] { MARKET_DATA_ARRAY_CLASS_NAME });
		}
		return nodes.size();
	}

	/**
	 * Called when a piece of market data was updated.
	 * @param event encapsulated the market data update.
	 * @see org.jppf.example.datadependency.simulation.TickerListener#marketDataUpdated(org.jppf.example.datadependency.simulation.TickerEvent)
	 */
	public void marketDataUpdated(TickerEvent event)
	{
		MarketData[] data = new MarketData[] { event.getMarketData() };
		for (JMXNodeConnectionWrapper node: nodes)
		{
			try
			{
				node.invoke("org.jppf.example.mbean:name=Data,type=node", "updateMarketData", new Object[] { data }, new String[] { MARKET_DATA_ARRAY_CLASS_NAME });
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
