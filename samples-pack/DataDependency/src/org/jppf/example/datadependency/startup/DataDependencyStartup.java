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

package org.jppf.example.datadependency.startup;

import java.util.Map;

import org.jppf.example.datadependency.model.*;
import org.jppf.startup.JPPFNodeStartupSPI;

import com.hazelcast.core.*;

/**
 * 
 * @author Laurent Cohen
 */
public class DataDependencyStartup implements JPPFNodeStartupSPI
{
	/**
	 * Mapping of the market data.
	 */
	private static Map<String, MarketData> dataMap = null;
	/**
	 * Mapping of the trades.
	 */
	private static Map<String, Trade> tradeMap = null;

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		//-Dhazelcast.wait.seconds.before.join=1
		System.setProperty("hazelcast.wait.seconds.before.join", "1"); 
		System.out.println("Initializing distributed maps");
		dataMap = Hazelcast.getMap("MarketData");
		tradeMap = Hazelcast.getMap("Trade");
		System.out.println("Data initialization complete");
	}

	/**
	 * Get the market data object with the specified id.
	 * @param id the id of the data to lookup.
	 * @return a {@link MarketData} instance, or null if none has the specified id. 
	 */
	public static MarketData getMarketData(String id)
	{
		return dataMap.get(id);
	}

	/**
	 * Get the trade object with the specified id.
	 * @param id the id of the trade to lookup.
	 * @return a {@link Trade} instance, or null if none has the specified id. 
	 */
	public static Trade getTrade(String id)
	{
		return tradeMap.get(id);
	}

	/**
	 * Update the specified trade object.
	 * @param trade the trade to update.
	 */
	public static void updateTrade(Trade trade)
	{
		try
		{
			((IMap) tradeMap).lock(trade.getId());
			//((IMap) tradeMap).put(trade.getId(), trade, 1000L, TimeUnit.SECONDS);
			tradeMap.put(trade.getId(), trade);
		}
		finally
		{
			((IMap) tradeMap).unlock(trade.getId());
		}
		
	}
}
