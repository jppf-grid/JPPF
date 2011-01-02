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

package org.jppf.example.datadependency.mbean;

import java.util.*;

import org.jppf.example.datadependency.model.*;

/**
 * Implementation of the DataMBean interface.
 * @author Laurent Cohen
 */
public class Data implements DataMBean
{
	/**
	 * Mapping of the market data.
	 */
	private static Map<String, MarketData> dataMap = new Hashtable<String, MarketData>();

	/**
	 * Mapping of the market data.
	 */
	private static Map<String, Trade> tradeMap = new Hashtable<String, Trade>();

	/**
	 * Default constructor.
	 */
	public Data()
	{
		System.out.println("Initializing Data mbean");
	}
	
	/**
	 * Update the specified market data.
	 * @param data the pieces of data to update.
	 * @see org.jppf.example.datadependency.mbean.DataMBean#updateMarketData(org.jppf.example.datadependency.model.MarketData[])
	 */
	public void updateMarketData(MarketData...data)
	{
		synchronized(dataMap)
		{
			for (MarketData md: data) dataMap.put(md.getId(), md);
		}
	}

	/**
	 * Get the market data object corresponding to the specified id.
	 * @param id the id of the market data to retrieve.
	 * @return a <code>MarketData</code> instance.
	 */
	public static MarketData getMarketData(String id)
	{
		synchronized(dataMap)
		{
			return dataMap.get(id);
		}
	}

	/**
	 * Update the specified trade in the table.
	 * @param trade the trade object to update.
	 */
	public static void updateTrade(Trade trade)
	{
		synchronized(tradeMap)
		{
			tradeMap.put(trade.getId(), trade);
		}
	}
}
