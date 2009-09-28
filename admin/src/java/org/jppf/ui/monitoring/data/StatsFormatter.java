/*
 * Java Parallel Processing Framework.
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
package org.jppf.ui.monitoring.data;

import static org.jppf.ui.monitoring.data.Fields.*;
import java.text.NumberFormat;
import java.util.*;
import org.apache.commons.logging.*;
import org.jppf.server.JPPFStats;
import org.jppf.utils.StringUtils;

/**
 * This class provides a set of methods to format the statistics data received from the server.
 * @author Laurent Cohen
 */
public final class StatsFormatter implements StatsConstants
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(StatsFormatter.class);
	/**
	 * Formatter for integer values.
	 */
	private static NumberFormat integerFormatter = initIntegerFormatter();
	/**
	 * Formatter for floating point values.
	 */
	private static NumberFormat doubleFormatter = initDoubleFormatter();
	/**
	 * Formatter for floating point values.
	 */
	private static NumberFormat smallDoubleFormatter = initSmallDoubleFormatter();

	/**
	 * Instantiation of this class is not allowed.
	 */
	private StatsFormatter()
	{
	}

	/**
	 * Initialize the formatter for double values.
	 * @return a <code>NumberFormat</code> instance.
	 */
	private static NumberFormat initSmallDoubleFormatter()
	{
		NumberFormat doubleFormatter = NumberFormat.getInstance();
		doubleFormatter.setGroupingUsed(true);
		doubleFormatter.setMinimumFractionDigits(2);
		doubleFormatter.setMaximumFractionDigits(10);
		doubleFormatter.setMinimumIntegerDigits(1);
		return doubleFormatter;
	}

	/**
	 * Initialize the formatter for double values.
	 * @return a <code>NumberFormat</code> instance.
	 */
	private static NumberFormat initDoubleFormatter()
	{
		NumberFormat doubleFormatter = NumberFormat.getInstance();
		doubleFormatter.setGroupingUsed(true);
		doubleFormatter.setMinimumFractionDigits(2);
		doubleFormatter.setMaximumFractionDigits(2);
		doubleFormatter.setMinimumIntegerDigits(1);
		return doubleFormatter;
	}

	/**
	 * Initialize the formatter for integer values.
	 * @return a <code>NumberFormat</code> instance.
	 */
	private static NumberFormat initIntegerFormatter()
	{
		NumberFormat integerFormatter = NumberFormat.getInstance();
		integerFormatter.setGroupingUsed(true);
		integerFormatter.setMinimumFractionDigits(0);
		integerFormatter.setMaximumFractionDigits(0);
		integerFormatter.setMinimumIntegerDigits(1);
		return integerFormatter;
	}

	/**
	 * Get the map of values represented as strings for a specified data snapshot.
	 * @param stats the data snapshot to map.
	 * @return a map of field names to their corresponding string values.
	 */
	public static Map<Fields, String> getStringValuesMap(JPPFStats stats)
	{
		Map<Fields, String> stringValueMap = new HashMap<Fields, String>();
		stringValueMap.put(TOTAL_TASKS_EXECUTED, formatInt(stats.getTotalTasksExecuted()));

		stringValueMap.put(TOTAL_EXECUTION_TIME, formatTime(stats.getExecution().getTotalTime()));
		stringValueMap.put(LATEST_EXECUTION_TIME, formatDouble(stats.getExecution().getLatestTime()));
		String s = (stats.getExecution().getMinTime() == Long.MAX_VALUE) ? "" : formatDouble(stats.getExecution().getMinTime());
		stringValueMap.put(MIN_EXECUTION_TIME, s);
		stringValueMap.put(MAX_EXECUTION_TIME, formatDouble(stats.getExecution().getMaxTime()));
		stringValueMap.put(AVG_EXECUTION_TIME, formatDouble(stats.getExecution().getAvgTime()));
		stringValueMap.put(TOTAL_NODE_EXECUTION_TIME, formatTime(stats.getNodeExecution().getTotalTime()));
		stringValueMap.put(LATEST_NODE_EXECUTION_TIME, formatDouble(stats.getNodeExecution().getLatestTime()));
		s = (stats.getNodeExecution().getMinTime() == Long.MAX_VALUE) ? "" : formatDouble(stats.getNodeExecution().getMinTime());
		stringValueMap.put(MIN_NODE_EXECUTION_TIME, s);
		stringValueMap.put(MAX_NODE_EXECUTION_TIME, formatDouble(stats.getNodeExecution().getMaxTime()));
		stringValueMap.put(AVG_NODE_EXECUTION_TIME, formatDouble(stats.getNodeExecution().getAvgTime()));
		stringValueMap.put(TOTAL_TRANSPORT_TIME, formatTime(stats.getTransport().getTotalTime()));
		stringValueMap.put(LATEST_TRANSPORT_TIME, formatDouble(stats.getTransport().getLatestTime()));
		s = (stats.getTransport().getMinTime() == Long.MAX_VALUE) ? "" : formatDouble(stats.getTransport().getMinTime());
		stringValueMap.put(MIN_TRANSPORT_TIME, s);
		stringValueMap.put(MAX_TRANSPORT_TIME, formatDouble(stats.getTransport().getMaxTime()));
		stringValueMap.put(AVG_TRANSPORT_TIME, formatDouble(stats.getTransport().getAvgTime()));
		stringValueMap.put(LATEST_QUEUE_TIME, formatDouble(stats.getQueue().getLatestTime()));
		stringValueMap.put(TOTAL_QUEUE_TIME, formatTime(stats.getQueue().getTotalTime()));
		s = (stats.getQueue().getMinTime() == Long.MAX_VALUE) ? "" : formatDouble(stats.getQueue().getMinTime());
		stringValueMap.put(MIN_QUEUE_TIME, s);
		stringValueMap.put(MAX_QUEUE_TIME, formatDouble(stats.getQueue().getMaxTime()));
		stringValueMap.put(AVG_QUEUE_TIME, formatDouble(stats.getQueue().getAvgTime()));
		stringValueMap.put(TOTAL_QUEUED, formatInt(stats.getTotalQueued()));
		stringValueMap.put(QUEUE_SIZE, formatInt(stats.getQueueSize()));
		stringValueMap.put(MAX_QUEUE_SIZE, formatInt(stats.getMaxQueueSize()));
		stringValueMap.put(NB_NODES, formatInt(stats.getNbNodes()));
		stringValueMap.put(MAX_NODES, formatInt(stats.getMaxNodes()));
		stringValueMap.put(NB_CLIENTS, formatInt(stats.getNbClients()));
		stringValueMap.put(MAX_CLIENTS, formatInt(stats.getMaxClients()));
		return stringValueMap;
	}
	
	/**
	 * Get the map of values represented as double for a specified data snapshot.
	 * @param stats the data snapshot to map.
	 * @return a map of field names to their corresponding double values.
	 */
	public static Map<Fields, Double> getDoubleValuesMap(JPPFStats stats)
	{
		Map<Fields, Double> doubleValueMap = new HashMap<Fields, Double>();
		doubleValueMap.put(TOTAL_TASKS_EXECUTED, (double) stats.getTotalTasksExecuted());

		doubleValueMap.put(TOTAL_EXECUTION_TIME, (double) stats.getExecution().getTotalTime());
		doubleValueMap.put(LATEST_EXECUTION_TIME, (double) stats.getExecution().getLatestTime());
		doubleValueMap.put(MIN_EXECUTION_TIME, (double) (stats.getExecution().getMinTime() == Long.MAX_VALUE ? 0L : stats.getExecution().getMinTime()));
		doubleValueMap.put(MAX_EXECUTION_TIME, (double) stats.getExecution().getMaxTime());
		doubleValueMap.put(AVG_EXECUTION_TIME, stats.getExecution().getAvgTime());
		doubleValueMap.put(TOTAL_NODE_EXECUTION_TIME, (double) stats.getNodeExecution().getTotalTime());
		doubleValueMap.put(LATEST_NODE_EXECUTION_TIME, (double) stats.getNodeExecution().getLatestTime());
		doubleValueMap.put(MIN_NODE_EXECUTION_TIME, (double) (stats.getNodeExecution().getMinTime() == Long.MAX_VALUE ? 0L : stats.getNodeExecution().getMinTime()));
		doubleValueMap.put(MAX_NODE_EXECUTION_TIME, (double) stats.getNodeExecution().getMaxTime());
		doubleValueMap.put(AVG_NODE_EXECUTION_TIME, stats.getNodeExecution().getAvgTime());
		doubleValueMap.put(TOTAL_TRANSPORT_TIME, (double) stats.getTransport().getTotalTime());
		doubleValueMap.put(LATEST_TRANSPORT_TIME, (double) stats.getTransport().getLatestTime());
		doubleValueMap.put(MIN_TRANSPORT_TIME, (double) (stats.getTransport().getMinTime() == Long.MAX_VALUE ? 0L : stats.getTransport().getMinTime()));
		doubleValueMap.put(MAX_TRANSPORT_TIME, (double) stats.getTransport().getMaxTime());
		doubleValueMap.put(AVG_TRANSPORT_TIME, stats.getTransport().getAvgTime());
		doubleValueMap.put(LATEST_QUEUE_TIME, (double) stats.getQueue().getLatestTime());
		doubleValueMap.put(TOTAL_QUEUE_TIME, (double) stats.getQueue().getTotalTime());
		doubleValueMap.put(MIN_QUEUE_TIME, (double) (stats.getQueue().getMinTime() == Long.MAX_VALUE ? 0L : stats.getQueue().getMinTime()));
		doubleValueMap.put(MAX_QUEUE_TIME, (double) stats.getQueue().getMaxTime());
		doubleValueMap.put(AVG_QUEUE_TIME, stats.getQueue().getAvgTime());
		doubleValueMap.put(TOTAL_QUEUED, (double) stats.getTotalQueued());
		doubleValueMap.put(QUEUE_SIZE, (double) stats.getQueueSize());
		doubleValueMap.put(MAX_QUEUE_SIZE, (double) stats.getMaxQueueSize());
		doubleValueMap.put(NB_NODES, (double) stats.getNbNodes());
		doubleValueMap.put(MAX_NODES, (double) stats.getMaxNodes());
		doubleValueMap.put(NB_CLIENTS, (double) stats.getNbClients());
		doubleValueMap.put(MAX_CLIENTS, (double) stats.getMaxClients());
		return doubleValueMap;
	}

	/**
	 * Format an integer value.
	 * @param value the value to format.
	 * @return the formatted value as a string.
	 */
	private static String formatInt(long value)
	{
		return integerFormatter.format(value);
	}

	/**
	 * Format a floating point value.
	 * @param value the value to format.
	 * @return the formatted value as a string.
	 */
	private static String formatDouble(double value)
	{
		return doubleFormatter.format(value);
	}
	
	/**
	 * Format a floating point value.
	 * @param value the value to format.
	 * @return the formatted value as a string.
	 */
	protected static String formatSmallDouble(double value)
	{
		return smallDoubleFormatter.format(value);
	}
	
	/**
	 * Format a a time (or duration) value in format hh:mm:ss&#46;ms.
	 * @param value the value to format.
	 * @return the formatted value as a string.
	 */
	private static String formatTime(long value)
	{
		return StringUtils.toStringDuration(value);
	}
}
