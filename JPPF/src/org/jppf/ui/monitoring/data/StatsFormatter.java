/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring.data;

import java.text.NumberFormat;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.server.JPPFStats;
import org.jppf.utils.StringUtils;

/**
 * This class provides a convenient access to the statistics obtained from the JPPF server.
 * @author Laurent Cohen
 */
public final class StatsFormatter implements StatsConstants
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(StatsFormatter.class);
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
	public static Map<String, String> getStringValuesMap(JPPFStats stats)
	{
		Map<String, String> stringValueMap = new HashMap<String, String>();
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
		stringValueMap.put(AVG_KILOBYTE_TRANPORT, formatSmallDouble(stats.avgTransportPerByte));
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
	public static Map<String, Double> getDoubleValuesMap(JPPFStats stats)
	{
		Map<String, Double> doubleValueMap = new HashMap<String, Double>();
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
		doubleValueMap.put(AVG_KILOBYTE_TRANPORT, stats.avgTransportPerByte);
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
	private static String formatSmallDouble(double value)
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
