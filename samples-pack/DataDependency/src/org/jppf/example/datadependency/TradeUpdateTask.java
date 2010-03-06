/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import org.jppf.example.datadependency.model.Trade;
import org.jppf.server.protocol.JPPFTask;

/**
 * JPPF task whose role is to recompute a trade when some market data was updated.
 * @author Laurent Cohen
 */
public class TradeUpdateTask extends JPPFTask
{
	/**
	 * The trade to recompute
	 */
	private Trade trade = null;
	/**
	 * The identifiers for the market data that was updated.
	 */
	private String[] marketDataId = null;
	/**
	 * Simulated duration of this task.
	 */
	private long taskDuration = 1000L;

	/**
	 * Initialize this task with the specified trade and ids of updated market data. 
	 * @param trade the trade to recompute.
	 * @param marketDataId the identifiers for the market data that was updated.
	 */
	public TradeUpdateTask(Trade trade, String...marketDataId)
	{
		this.trade = trade;
		this.marketDataId = marketDataId;
	}

	/**
	 * Recompute the trade.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		long taskStart = System.currentTimeMillis();
		// perform some dummy cpu-consuming computation
		for (long elapsed = 0L; elapsed < taskDuration; elapsed = System.currentTimeMillis() - taskStart)
		{
			String s = "";
			for (int i=0; i<10; i++) s += "A"+"10";
		}
	}

	/**
	 * Get the trade.
	 * @return a trade object.
	 */
	public Trade getTrade()
	{
		return trade;
	}

	/**
	 * Get the simulated duration of this task.
	 * @return the duration in milliseconds.
	 */
	public long getTaskDuration()
	{
		return taskDuration;
	}

	/**
	 * Set the simulated duration of this task.
	 * @param taskDuration the duration in milliseconds.
	 */
	public void setTaskDuration(long taskDuration)
	{
		this.taskDuration = taskDuration;
	}
}
