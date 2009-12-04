/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
		stringValueMap.put(TOTAL_TASKS_EXECUTED, formatInt(stats.totalTasksExecuted));

		stringValueMap.put(TOTAL_EXECUTION_TIME, formatTime(stats.execution.totalTime));
		stringValueMap.put(LATEST_EXECUTION_TIME, formatDouble(stats.execution.latestTime));
		String s = (stats.execution.minTime == Long.MAX_VALUE) ? "" : formatDouble(stats.execution.minTime);
		stringValueMap.put(MIN_EXECUTION_TIME, s);
		stringValueMap.put(MAX_EXECUTION_TIME, formatDouble(stats.execution.maxTime));
		stringValueMap.put(AVG_EXECUTION_TIME, formatDouble(stats.execution.avgTime));
		stringValueMap.put(TOTAL_NODE_EXECUTION_TIME, formatTime(stats.nodeExecution.totalTime));
		stringValueMap.put(LATEST_NODE_EXECUTION_TIME, formatDouble(stats.nodeExecution.latestTime));
		s = (stats.nodeExecution.minTime == Long.MAX_VALUE) ? "" : formatDouble(stats.nodeExecution.minTime);
		stringValueMap.put(MIN_NODE_EXECUTION_TIME, s);
		stringValueMap.put(MAX_NODE_EXECUTION_TIME, formatDouble(stats.nodeExecution.maxTime));
		stringValueMap.put(AVG_NODE_EXECUTION_TIME, formatDouble(stats.nodeExecution.avgTime));
		stringValueMap.put(TOTAL_TRANSPORT_TIME, formatTime(stats.transport.totalTime));
		stringValueMap.put(LATEST_TRANSPORT_TIME, formatDouble(stats.transport.latestTime));
		s = (stats.transport.minTime == Long.MAX_VALUE) ? "" : formatDouble(stats.transport.minTime);
		stringValueMap.put(MIN_TRANSPORT_TIME, s);
		stringValueMap.put(MAX_TRANSPORT_TIME, formatDouble(stats.transport.maxTime));
		stringValueMap.put(AVG_TRANSPORT_TIME, formatDouble(stats.transport.avgTime));
		stringValueMap.put(LATEST_QUEUE_TIME, formatDouble(stats.queue.latestTime));
		stringValueMap.put(TOTAL_QUEUE_TIME, formatTime(stats.queue.totalTime));
		s = (stats.queue.minTime == Long.MAX_VALUE) ? "" : formatDouble(stats.queue.minTime);
		stringValueMap.put(MIN_QUEUE_TIME, s);
		stringValueMap.put(MAX_QUEUE_TIME, formatDouble(stats.queue.maxTime));
		stringValueMap.put(AVG_QUEUE_TIME, formatDouble(stats.queue.avgTime));
		stringValueMap.put(TOTAL_QUEUED, formatInt(stats.totalQueued));
		stringValueMap.put(QUEUE_SIZE, formatInt(stats.queueSize));
		stringValueMap.put(MAX_QUEUE_SIZE, formatInt(stats.maxQueueSize));
		stringValueMap.put(NB_NODES, formatInt(stats.nbNodes));
		stringValueMap.put(MAX_NODES, formatInt(stats.maxNodes));
		stringValueMap.put(NB_CLIENTS, formatInt(stats.nbClients));
		stringValueMap.put(MAX_CLIENTS, formatInt(stats.maxClients));
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
		doubleValueMap.put(TOTAL_TASKS_EXECUTED, (double) stats.totalTasksExecuted);

		doubleValueMap.put(TOTAL_EXECUTION_TIME, (double) stats.execution.totalTime);
		doubleValueMap.put(LATEST_EXECUTION_TIME, (double) stats.execution.latestTime);
		doubleValueMap.put(MIN_EXECUTION_TIME, (double) (stats.execution.minTime == Long.MAX_VALUE ? 0L : stats.execution.minTime));
		doubleValueMap.put(MAX_EXECUTION_TIME, (double) stats.execution.maxTime);
		doubleValueMap.put(AVG_EXECUTION_TIME, stats.execution.avgTime);
		doubleValueMap.put(TOTAL_NODE_EXECUTION_TIME, (double) stats.nodeExecution.totalTime);
		doubleValueMap.put(LATEST_NODE_EXECUTION_TIME, (double) stats.nodeExecution.latestTime);
		doubleValueMap.put(MIN_NODE_EXECUTION_TIME, (double) (stats.nodeExecution.minTime == Long.MAX_VALUE ? 0L : stats.nodeExecution.minTime));
		doubleValueMap.put(MAX_NODE_EXECUTION_TIME, (double) stats.nodeExecution.maxTime);
		doubleValueMap.put(AVG_NODE_EXECUTION_TIME, stats.nodeExecution.avgTime);
		doubleValueMap.put(TOTAL_TRANSPORT_TIME, (double) stats.transport.totalTime);
		doubleValueMap.put(LATEST_TRANSPORT_TIME, (double) stats.transport.latestTime);
		doubleValueMap.put(MIN_TRANSPORT_TIME, (double) (stats.transport.minTime == Long.MAX_VALUE ? 0L : stats.transport.minTime));
		doubleValueMap.put(MAX_TRANSPORT_TIME, (double) stats.transport.maxTime);
		doubleValueMap.put(AVG_TRANSPORT_TIME, stats.transport.avgTime);
		doubleValueMap.put(LATEST_QUEUE_TIME, (double) stats.queue.latestTime);
		doubleValueMap.put(TOTAL_QUEUE_TIME, (double) stats.queue.totalTime);
		doubleValueMap.put(MIN_QUEUE_TIME, (double) (stats.queue.minTime == Long.MAX_VALUE ? 0L : stats.queue.minTime));
		doubleValueMap.put(MAX_QUEUE_TIME, (double) stats.queue.maxTime);
		doubleValueMap.put(AVG_QUEUE_TIME, stats.queue.avgTime);
		doubleValueMap.put(TOTAL_QUEUED, (double) stats.totalQueued);
		doubleValueMap.put(QUEUE_SIZE, (double) stats.queueSize);
		doubleValueMap.put(MAX_QUEUE_SIZE, (double) stats.maxQueueSize);
		doubleValueMap.put(NB_NODES, (double) stats.nbNodes);
		doubleValueMap.put(MAX_NODES, (double) stats.maxNodes);
		doubleValueMap.put(NB_CLIENTS, (double) stats.nbClients);
		doubleValueMap.put(MAX_CLIENTS, (double) stats.maxClients);
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
