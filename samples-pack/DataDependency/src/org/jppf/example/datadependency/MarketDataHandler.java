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

package org.jppf.example.datadependency;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.example.datadependency.model.MarketData;
import org.jppf.example.datadependency.simulation.*;
import org.slf4j.*;

import com.hazelcast.core.*;

/**
 * 
 * @author Laurent Cohen
 */
public class MarketDataHandler implements TickerListener
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(MarketDataHandler.class);
	/**
	 * Debug enabled flag.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Mapping of the market data.
	 */
	private static Map<String, MarketData> dataMap = null;
	/**
	 * Executes the market data updates in the nodes.
	 */
	private ExecutorService nodeExecutor = Executors.newFixedThreadPool(1);

	/**
	 * Perform the nodes' initializations.
	 * @param marketDataList the initial market data to send to each node.
	 * @throws Exception if any error is raised.
	 */
	public void populateMarketData(List<MarketData> marketDataList) throws Exception
	{
		System.out.println("populating the market data");
		dataMap = Hazelcast.getMap("MarketData");
		dataMap.clear();
		for (MarketData data: marketDataList) dataMap.put(data.getId(), data);
		System.out.println("end of populating the market data");
	}

	/**
	 * Called when a piece of market data was updated.
	 * @param event encapsulated the market data update.
	 * @see org.jppf.example.datadependency.simulation.TickerListener#marketDataUpdated(org.jppf.example.datadependency.simulation.TickerEvent)
	 */
	public void marketDataUpdated(TickerEvent event)
	{
		nodeExecutor.submit(new NodesUpdateTask(event.getMarketData()));
	}

	/**
	 * Close this node handler and release the resources it uses.
	 */
	public void close()
	{
		nodeExecutor.shutdown();
	}

	/**
	 * Performs the nodes updates.
	 */
	public class NodesUpdateTask implements Runnable
	{
		/**
		 * The update data to send to the nodes.
		 */
		private MarketData data;
		
		/**
		 * Initialize this task with the update dmarket data.
		 * @param data the update ddata.
		 */
		public NodesUpdateTask(final MarketData data)
		{
			this.data = data;
		}

		/**
		 * Execute this task.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				((IMap) dataMap).lockMap(1000L, TimeUnit.SECONDS);
				try
				{
					dataMap.put(data.getId(), data);
				}
				finally
				{
					((IMap) dataMap).unlockMap();
				}
			}
			catch (Exception e)
			{
				//e.printStackTrace();
			}
		}
	}
}
