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
package org.jppf.ui.monitoring.data;

import static org.jppf.ui.monitoring.data.Fields.*;

import java.text.NumberFormat;
import java.util.*;

import org.jppf.server.JPPFStats;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides a set of methods to format the statistics data received from the server.
 * @author Laurent Cohen
 */
public final class StatsFormatter implements StatsConstants
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(StatsFormatter.class);
	/**
	 * Formatter for integer values.
	 */
	private static NumberFormat integerFormatter = initIntegerFormatter();
	/**
	 * Formatter for floating point values.
	 */
	private static NumberFormat doubleFormatter = initDoubleFormatter();

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
		stringValueMap.put(TOTAL_EXECUTION_TIME, formatTime(stats.getExecution().getTotal()));
		stringValueMap.put(LATEST_EXECUTION_TIME, formatDouble(stats.getExecution().getLatest()));
		stringValueMap.put(MIN_EXECUTION_TIME, formatDouble(stats.getExecution().getMin()));
		stringValueMap.put(MAX_EXECUTION_TIME, formatDouble(stats.getExecution().getMax()));
		stringValueMap.put(AVG_EXECUTION_TIME, formatDouble(stats.getExecution().getAvg()));
		stringValueMap.put(TOTAL_NODE_EXECUTION_TIME, formatTime(stats.getNodeExecution().getTotal()));
		stringValueMap.put(LATEST_NODE_EXECUTION_TIME, formatDouble(stats.getNodeExecution().getLatest()));
		stringValueMap.put(MIN_NODE_EXECUTION_TIME, formatDouble(stats.getNodeExecution().getMin()));
		stringValueMap.put(MAX_NODE_EXECUTION_TIME, formatDouble(stats.getNodeExecution().getMax()));
		stringValueMap.put(AVG_NODE_EXECUTION_TIME, formatDouble(stats.getNodeExecution().getAvg()));
		stringValueMap.put(TOTAL_TRANSPORT_TIME, formatTime(stats.getTransport().getTotal()));
		stringValueMap.put(LATEST_TRANSPORT_TIME, formatDouble(stats.getTransport().getLatest()));
		stringValueMap.put(MIN_TRANSPORT_TIME, formatDouble(stats.getTransport().getMin()));
		stringValueMap.put(MAX_TRANSPORT_TIME, formatDouble(stats.getTransport().getMax()));
		stringValueMap.put(AVG_TRANSPORT_TIME, formatDouble(stats.getTransport().getAvg()));
		QueueStats queue = stats.getTaskQueue();
		stringValueMap.put(LATEST_QUEUE_TIME, formatDouble(queue.getTimes().getLatest()));
		stringValueMap.put(TOTAL_QUEUE_TIME, formatTime(queue.getTimes().getTotal()));
		stringValueMap.put(MIN_QUEUE_TIME, formatDouble(queue.getTimes().getMin()));
		stringValueMap.put(MAX_QUEUE_TIME, formatDouble(queue.getTimes().getMax()));
		stringValueMap.put(AVG_QUEUE_TIME, formatDouble(queue.getTimes().getAvg()));
		stringValueMap.put(TOTAL_QUEUED, formatInt(queue.getSizes().getTotal()));
		stringValueMap.put(QUEUE_SIZE, formatInt(queue.getSizes().getLatest()));
		stringValueMap.put(MAX_QUEUE_SIZE, formatInt(queue.getSizes().getMax()));
		stringValueMap.put(NB_NODES, formatInt(stats.getNodes().getLatest()));
		stringValueMap.put(MAX_NODES, formatInt(stats.getNodes().getMax()));
		stringValueMap.put(NB_IDLE_NODES, formatInt(stats.getIdleNodes().getLatest()));
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
		doubleValueMap.put(TOTAL_EXECUTION_TIME, (double) stats.getExecution().getTotal());
		doubleValueMap.put(LATEST_EXECUTION_TIME, (double) stats.getExecution().getLatest());
		doubleValueMap.put(MIN_EXECUTION_TIME, (double) (stats.getExecution().getMin() == Long.MAX_VALUE ? 0L : stats.getExecution().getMin()));
		doubleValueMap.put(MAX_EXECUTION_TIME, (double) stats.getExecution().getMax());
		doubleValueMap.put(AVG_EXECUTION_TIME, stats.getExecution().getAvg());
		doubleValueMap.put(TOTAL_NODE_EXECUTION_TIME, (double) stats.getNodeExecution().getTotal());
		doubleValueMap.put(LATEST_NODE_EXECUTION_TIME, (double) stats.getNodeExecution().getLatest());
		doubleValueMap.put(MIN_NODE_EXECUTION_TIME, (double) (stats.getNodeExecution().getMin() == Long.MAX_VALUE ? 0L : stats.getNodeExecution().getMin()));
		doubleValueMap.put(MAX_NODE_EXECUTION_TIME, (double) stats.getNodeExecution().getMax());
		doubleValueMap.put(AVG_NODE_EXECUTION_TIME, stats.getNodeExecution().getAvg());
		doubleValueMap.put(TOTAL_TRANSPORT_TIME, (double) stats.getTransport().getTotal());
		doubleValueMap.put(LATEST_TRANSPORT_TIME, (double) stats.getTransport().getLatest());
		doubleValueMap.put(MIN_TRANSPORT_TIME, (double) (stats.getTransport().getMin() == Long.MAX_VALUE ? 0L : stats.getTransport().getMin()));
		doubleValueMap.put(MAX_TRANSPORT_TIME, (double) stats.getTransport().getMax());
		doubleValueMap.put(AVG_TRANSPORT_TIME, stats.getTransport().getAvg());
		QueueStats queue = stats.getTaskQueue();
		doubleValueMap.put(LATEST_QUEUE_TIME, (double) queue.getTimes().getLatest());
		doubleValueMap.put(TOTAL_QUEUE_TIME, (double) queue.getTimes().getTotal());
		doubleValueMap.put(MIN_QUEUE_TIME, (double) (queue.getTimes().getMin() == Long.MAX_VALUE ? 0L : queue.getTimes().getMin()));
		doubleValueMap.put(MAX_QUEUE_TIME, (double) queue.getTimes().getMax());
		doubleValueMap.put(AVG_QUEUE_TIME, queue.getTimes().getAvg());
		doubleValueMap.put(TOTAL_QUEUED, (double) queue.getSizes().getTotal());
		doubleValueMap.put(QUEUE_SIZE, (double) queue.getSizes().getLatest());
		doubleValueMap.put(MAX_QUEUE_SIZE, (double) queue.getSizes().getMax());
		doubleValueMap.put(NB_NODES, (double) stats.getNodes().getLatest());
		doubleValueMap.put(MAX_NODES, (double) stats.getNodes().getMax());
		doubleValueMap.put(NB_IDLE_NODES, (double) stats.getIdleNodes().getLatest());
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
	private static String formatDouble(long value)
	{
		return (value == Long.MAX_VALUE) ? "" : doubleFormatter.format(value);
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
