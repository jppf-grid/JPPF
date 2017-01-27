/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * This class provides a set of methods to format the statistics data received from the server.
 * @author Laurent Cohen
 */
public final class StatsFormatter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(StatsFormatter.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Value of 1 megabyte.
   */
  private static final long MB = 1024L * 1024L;
  /**
   * The set of fields formated as int.
   */
  private static final Set<Fields> INT_FORMATTED = EnumSet.of(TOTAL_TASKS_EXECUTED, TOTAL_QUEUED, QUEUE_SIZE, MAX_QUEUE_SIZE, NB_NODES, MAX_NODES, NB_IDLE_NODES, NB_BUSY_NODES,
    NB_CLIENTS, MAX_CLIENTS, JOBS_TOTAL, JOBS_LATEST, JOBS_MAX, JOBS_MIN_TASKS, JOBS_MAX_TASKS, NODE_TOTAL_CL_REQUEST_COUNT, CLIENT_TOTAL_CL_REQUEST_COUNT, HEALTH_HEAP,
    HEALTH_NON_HEAP, HEALTH_RAM, HEALTH_THREADS);
  /**
   * The set of fields formated as int.
   */
  private static final Set<Fields> DOUBLE_FORMATTED = EnumSet.of(LATEST_EXECUTION_TIME, MIN_EXECUTION_TIME, MAX_EXECUTION_TIME, AVG_EXECUTION_TIME, LATEST_NODE_EXECUTION_TIME,
    MIN_NODE_EXECUTION_TIME, MAX_NODE_EXECUTION_TIME, AVG_NODE_EXECUTION_TIME, LATEST_TRANSPORT_TIME, MIN_TRANSPORT_TIME, MAX_TRANSPORT_TIME, AVG_TRANSPORT_TIME, LATEST_QUEUE_TIME,
    MIN_QUEUE_TIME, MAX_QUEUE_TIME, AVG_QUEUE_TIME, JOBS_LATEST_TIME, JOBS_MIN_TIME, JOBS_MAX_TIME, JOBS_AVG_TIME, JOBS_AVG_TASKS, NODE_AVG_CL_REQUEST_TIME, NODE_MIN_CL_REQUEST_TIME,
    NODE_MAX_CL_REQUEST_TIME, NODE_LATEST_CL_REQUEST_TIME, CLIENT_AVG_CL_REQUEST_TIME, CLIENT_MIN_CL_REQUEST_TIME, CLIENT_MAX_CL_REQUEST_TIME, CLIENT_LATEST_CL_REQUEST_TIME,
    HEALTH_HEAP_PCT, HEALTH_NON_HEAP_PCT, HEALTH_RAM_PCT, HEALTH_CPU, HEALTH_SYSTEM_CPU);
  /**
   * The set of fields formated as int.
   */
  private static final Set<Fields> TIME_FORMATTED = EnumSet.of(TOTAL_EXECUTION_TIME, TOTAL_NODE_EXECUTION_TIME, TOTAL_TRANSPORT_TIME, TOTAL_QUEUE_TIME);
  /**
   * The set of fields formated as int.
   */
  private static final Set<Fields> MB_FORMATTED = EnumSet.of(CLIENT_INBOUND_MB, NODE_INBOUND_MB, UNIDENTIFIED_INBOUND_MB, TOTAL_INBOUND_MB, CLIENT_OUTBOUND_MB, NODE_OUTBOUND_MB,
    UNIDENTIFIED_OUTBOUND_MB, TOTAL_OUTBOUND_MB);
  /**
   * Formatter for integer values.
   */
  private final NumberFormat intFormatter;
  /**
   * Formatter for floating point values.
   */
  private final NumberFormat doubleFormatter;
  /**
   * The locale used for formatted values.
   */
  private final Locale locale;

  /**
   * Instantiation of this class is not allowed.
   * @param locale the locale used for formatted values.
   */
  public StatsFormatter(final Locale locale) {
    this.locale = locale;
    intFormatter = initIntegerFormatter();
    doubleFormatter = initDoubleFormatter();
  }

  /**
   * Initialize the formatter for double values.
   * @return a {@code NumberFormat} instance.
   */
  private NumberFormat initDoubleFormatter() {
    NumberFormat doubleFormatter = NumberFormat.getInstance(locale);
    doubleFormatter.setGroupingUsed(true);
    doubleFormatter.setMinimumFractionDigits(2);
    doubleFormatter.setMaximumFractionDigits(2);
    doubleFormatter.setMinimumIntegerDigits(1);
    return doubleFormatter;
  }

  /**
   * Initialize the formatter for integer values.
   * @return a {@code NumberFormat} instance.
   * @since 5.0
   */
  private NumberFormat initIntegerFormatter() {
    NumberFormat intFormatter = NumberFormat.getInstance(locale);
    intFormatter.setGroupingUsed(true);
    intFormatter.setMinimumFractionDigits(0);
    intFormatter.setMaximumFractionDigits(0);
    intFormatter.setMinimumIntegerDigits(1);
    return intFormatter;
  }

  /**
   * Get the map of values represented as strings for a specified data snapshot.
   * @param values the values to convert.
   * @return a map of field names to their corresponding string values.
   */
  public Map<Fields, String> formatValues(final Map<Fields, Double> values) {
    Map<Fields, String> map = new HashMap<>();
    for (Map.Entry<Fields, Double> entry: values.entrySet()) {
      Fields field = entry.getKey();
      map.put(field, formatValue(field, entry.getValue()));
    }
    return map;
  }

  /**
   * Format the specified value for the specified field.
   * @param field the field for which to do the conversion.
   * @param value the value to format.
   * @return a map of field names to their corresponding string values.
   */
  public String formatValue(final Fields field, final double value) {
    String strValue = null;
    if (INT_FORMATTED.contains(field)) strValue = formatInt(value);
    else if (TIME_FORMATTED.contains(field)) strValue = formatTime(value);
    else if (MB_FORMATTED.contains(field)) strValue = formatMB(value);
    else if (DOUBLE_FORMATTED.contains(field)) strValue = formatDouble(value);
    else {
      strValue = formatDouble(value);
      if (debugEnabled) log.debug("field {} is not part of a formatting set, formatting as double (value = {}))", field.name(), value);
    }
    return strValue;
  }

  /**
   * Format an integer value.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private String formatInt(final double value) {
    return (value == Long.MAX_VALUE) ? "" : intFormatter.format(value);
  }

  /**
   * Format a floating point value after conversion from bytes to megabytes.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private String formatMB(final double value) {
    return doubleFormatter.format(value/MB);
  }

  /**
   * Format a floating point value.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private String formatDouble(final double value) {
    return doubleFormatter.format(value);
  }

  /**
   * Format a a time (or duration) value in format hh:mm:ss&#46;ms.
   * @param value the value to format.
   * @return the formatted value as a string.
   */
  private String formatTime(final double value) {
    return StringUtils.toStringDuration((long) value);
  }
}
