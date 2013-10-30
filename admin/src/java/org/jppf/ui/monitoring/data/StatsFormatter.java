/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.text.NumberFormat;
import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.stats.*;
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
  public static Map<Fields, String> getStringValuesMap(final JPPFStatistics stats)
  {
    Map<Fields, String> map = new HashMap<>();
    map.put(TOTAL_TASKS_EXECUTED, formatInt(stats.getSnapshot(TASK_DISPATCH).getTotal()));
    JPPFSnapshot snapshot = stats.getSnapshot(EXECUTION);
    map.put(TOTAL_EXECUTION_TIME, formatTime(snapshot.getTotal()));
    map.put(LATEST_EXECUTION_TIME, formatDouble(snapshot.getLatest()));
    map.put(MIN_EXECUTION_TIME, formatDouble(snapshot.getMin()));
    map.put(MAX_EXECUTION_TIME, formatDouble(snapshot.getMax()));
    map.put(AVG_EXECUTION_TIME, formatDouble(snapshot.getAvg()));
    snapshot = stats.getSnapshot(NODE_EXECUTION);
    map.put(TOTAL_NODE_EXECUTION_TIME, formatTime(snapshot.getTotal()));
    map.put(LATEST_NODE_EXECUTION_TIME, formatDouble(snapshot.getLatest()));
    map.put(MIN_NODE_EXECUTION_TIME, formatDouble(snapshot.getMin()));
    map.put(MAX_NODE_EXECUTION_TIME, formatDouble(snapshot.getMax()));
    map.put(AVG_NODE_EXECUTION_TIME, formatDouble(snapshot.getAvg()));
    snapshot = stats.getSnapshot(TRANSPORT_TIME);
    map.put(TOTAL_TRANSPORT_TIME, formatTime(snapshot.getTotal()));
    map.put(LATEST_TRANSPORT_TIME, formatDouble(snapshot.getLatest()));
    map.put(MIN_TRANSPORT_TIME, formatDouble(snapshot.getMin()));
    map.put(MAX_TRANSPORT_TIME, formatDouble(snapshot.getMax()));
    map.put(AVG_TRANSPORT_TIME, formatDouble(snapshot.getAvg()));
    snapshot = stats.getSnapshot(TASK_QUEUE_TIME);
    map.put(LATEST_QUEUE_TIME, formatDouble(snapshot.getLatest()));
    map.put(TOTAL_QUEUE_TIME, formatTime(snapshot.getTotal()));
    map.put(MIN_QUEUE_TIME, formatDouble(snapshot.getMin()));
    map.put(MAX_QUEUE_TIME, formatDouble(snapshot.getMax()));
    map.put(AVG_QUEUE_TIME, formatDouble(snapshot.getAvg()));
    map.put(TOTAL_QUEUED, formatInt(stats.getSnapshot(TASK_QUEUE_TOTAL).getTotal()));
    snapshot = stats.getSnapshot(TASK_QUEUE_COUNT);
    map.put(QUEUE_SIZE, formatInt(snapshot.getLatest()));
    map.put(MAX_QUEUE_SIZE, formatInt(snapshot.getMax()));
    snapshot = stats.getSnapshot(NODES);
    map.put(NB_NODES, formatInt(snapshot.getLatest()));
    map.put(MAX_NODES, formatInt(snapshot.getMax()));
    map.put(NB_IDLE_NODES, formatInt(stats.getSnapshot(IDLE_NODES).getLatest()));
    snapshot = stats.getSnapshot(CLIENTS);
    map.put(NB_CLIENTS, formatInt(snapshot.getLatest()));
    map.put(MAX_CLIENTS, formatInt(snapshot.getMax()));
    map.put(JOBS_TOTAL, formatInt(stats.getSnapshot(JOB_TOTAL).getTotal()));
    snapshot = stats.getSnapshot(JOB_COUNT);
    map.put(JOBS_LATEST, formatInt(snapshot.getLatest()));
    map.put(JOBS_MAX, formatInt(snapshot.getMax()));
    snapshot = stats.getSnapshot(JOB_TIME);
    map.put(JOBS_LATEST_TIME, formatDouble(snapshot.getLatest()));
    map.put(JOBS_MIN_TIME, formatDouble(snapshot.getMin()));
    map.put(JOBS_MAX_TIME, formatDouble(snapshot.getMax()));
    map.put(JOBS_AVG_TIME, formatDouble(snapshot.getAvg()));
    snapshot = stats.getSnapshot(JOB_TASKS);
    map.put(JOBS_MIN_TASKS, formatInt(snapshot.getMin()));
    map.put(JOBS_MAX_TASKS, formatInt(snapshot.getMax()));
    map.put(JOBS_AVG_TASKS, formatDouble(snapshot.getAvg()));
    return map;
  }

  /**
   * Get the map of values represented as double for a specified data snapshot.
   * @param stats the data snapshot to map.
   * @return a map of field names to their corresponding double values.
   */
  public static Map<Fields, Double> getDoubleValuesMap(final JPPFStatistics stats)
  {
    Map<Fields, Double> map = new HashMap<>();
    map.put(TOTAL_TASKS_EXECUTED, stats.getSnapshot(TASK_DISPATCH).getTotal());
    JPPFSnapshot snapshot = stats.getSnapshot(EXECUTION);
    map.put(TOTAL_EXECUTION_TIME, snapshot.getTotal());
    map.put(LATEST_EXECUTION_TIME, snapshot.getLatest());
    map.put(MIN_EXECUTION_TIME, snapshot.getMin() == Long.MAX_VALUE ? 0L : snapshot.getMin());
    map.put(MAX_EXECUTION_TIME, snapshot.getMax());
    map.put(AVG_EXECUTION_TIME, snapshot.getAvg());
    snapshot = stats.getSnapshot(NODE_EXECUTION);
    map.put(TOTAL_NODE_EXECUTION_TIME, snapshot.getTotal());
    map.put(LATEST_NODE_EXECUTION_TIME, snapshot.getLatest());
    map.put(MIN_NODE_EXECUTION_TIME, snapshot.getMin() == Long.MAX_VALUE ? 0L : snapshot.getMin());
    map.put(MAX_NODE_EXECUTION_TIME, snapshot.getMax());
    map.put(AVG_NODE_EXECUTION_TIME, snapshot.getAvg());
    snapshot = stats.getSnapshot(TRANSPORT_TIME);
    map.put(TOTAL_TRANSPORT_TIME, snapshot.getTotal());
    map.put(LATEST_TRANSPORT_TIME, snapshot.getLatest());
    map.put(MIN_TRANSPORT_TIME, snapshot.getMin() == Long.MAX_VALUE ? 0L : snapshot.getMin());
    map.put(MAX_TRANSPORT_TIME, snapshot.getMax());
    map.put(AVG_TRANSPORT_TIME, snapshot.getAvg());
    snapshot = stats.getSnapshot(TASK_QUEUE_TIME);
    map.put(LATEST_QUEUE_TIME, snapshot.getLatest());
    map.put(TOTAL_QUEUE_TIME, snapshot.getTotal());
    map.put(MIN_QUEUE_TIME, snapshot.getMin() == Long.MAX_VALUE ? 0L : snapshot.getMin());
    map.put(MAX_QUEUE_TIME, snapshot.getMax());
    map.put(AVG_QUEUE_TIME, snapshot.getAvg());
    map.put(TOTAL_QUEUED, stats.getSnapshot(TASK_QUEUE_TOTAL).getTotal());
    snapshot = stats.getSnapshot(TASK_QUEUE_COUNT);
    map.put(QUEUE_SIZE, snapshot.getLatest());
    map.put(MAX_QUEUE_SIZE, snapshot.getMax());
    snapshot = stats.getSnapshot(NODES);
    map.put(NB_NODES, snapshot.getLatest());
    map.put(MAX_NODES, snapshot.getMax());
    map.put(NB_IDLE_NODES, stats.getSnapshot(IDLE_NODES).getLatest());
    snapshot = stats.getSnapshot(CLIENTS);
    map.put(NB_CLIENTS, snapshot.getLatest());
    map.put(MAX_CLIENTS, snapshot.getMax());
    map.put(JOBS_TOTAL, stats.getSnapshot(JOB_TOTAL).getTotal());
    snapshot = stats.getSnapshot(JOB_COUNT);
    map.put(JOBS_LATEST, snapshot.getTotal());
    map.put(JOBS_MAX, snapshot.getMax());
    snapshot = stats.getSnapshot(JOB_TIME);
    map.put(JOBS_LATEST_TIME,snapshot.getLatest());
    map.put(JOBS_MIN_TIME, snapshot.getMin());
    map.put(JOBS_MAX_TIME, snapshot.getMax());
    map.put(JOBS_AVG_TIME, snapshot.getAvg());
    snapshot = stats.getSnapshot(JOB_TASKS);
    map.put(JOBS_MIN_TASKS, snapshot.getMin());
    map.put(JOBS_MAX_TASKS, snapshot.getMax());
    map.put(JOBS_AVG_TASKS, snapshot.getAvg());
    return map;
  }

  /**
   * Format an integer value.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private static String formatInt(final long value)
  {
    return (value == Long.MAX_VALUE) ? "" : integerFormatter.format(value);
  }

  /**
   * Format an integer value.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private static String formatInt(final double value)
  {
    return (value == Long.MAX_VALUE) ? "" : integerFormatter.format(value);
  }

  /**
   * Format a floating point value.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private static String formatDouble(final double value)
  {
    return doubleFormatter.format(value);
  }

  /**
   * Format a floating point value.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private static String formatDouble(final long value)
  {
    return (value == Long.MAX_VALUE) ? "" : doubleFormatter.format(value);
  }

  /**
   * Format a a time (or duration) value in format hh:mm:ss&#46;ms.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private static String formatTime(final long value)
  {
    return StringUtils.toStringDuration(value);
  }

  /**
   * Format a a time (or duration) value in format hh:mm:ss&#46;ms.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private static String formatTime(final double value)
  {
    return StringUtils.toStringDuration((long) value);
  }
}
