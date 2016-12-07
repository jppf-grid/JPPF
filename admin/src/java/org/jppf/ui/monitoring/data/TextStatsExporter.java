/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import static org.jppf.ui.monitoring.data.StatsConstants.*;

import java.util.*;

import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.utils.*;

/**
 * Exports the latest statistics snapshot to the clipboard in plain text format.
 * @author Laurent Cohen
 */
public class TextStatsExporter implements StatsExporter {
  /**
   * Base name for localization bundle lookups.
   */
  private static final String BASE = "org.jppf.ui.i18n.StatsPage";
  /**
   * Base name for localization bundle lookups.
   */
  private static final String FIELD_BASE = "org.jppf.ui.i18n.StatFields";
  /**
   * A map of the longest field name per locale.
   */
  private final static Map<Locale, Integer> maxNameLengthMap  = new HashMap<>();
  /**
   * The object from which to get the values.
   */
  private final BaseStatsHandler statsHandler;
  /**
   * The length of the longest field name.
   */
  private int maxNameLength = 0;
  /**
   * The length of the longest field value.
   */
  private int maxValueLength = 0;
  /**
   * The driver for which statistics are exported.
   */
  private final TopologyDriver driver;
  /**
   * The locale to export to.
   */
  private final Locale locale;

  /**
   * @param statsHandler the object from which to get the values.
   * @param driver the driver for which statistics are exported.
   * @param locale the locale to export to.
   */
  public TextStatsExporter(final BaseStatsHandler statsHandler, final TopologyDriver driver, final Locale locale) {
    this.statsHandler = statsHandler;
    this.driver = driver;
    this.locale = locale;
  }

  @Override
  public String formatAll() {
    StringBuilder sb = new StringBuilder();
    sb.append("JPPF driver statistics\n\n");
    Map<Fields, String> map = new HashMap<>(statsHandler.getLatestStringValues(locale, driver));
    maxNameLength = 0;
    maxValueLength = 0;
    updateMaxLengths(map, StatsConstants.ALL_FIELDS);
    sb.append(format(map, EXECUTION_FIELDS, "ExecutionTable.label"));
    sb.append(format(map, NODE_EXECUTION_FIELDS, "NodeExecutionTable.label"));
    sb.append(format(map, TRANSPORT_FIELDS, "NetworkOverheadTable.label"));
    sb.append(format(map, JOB_FIELDS, "JobTable.label"));
    sb.append(format(map, QUEUE_FIELDS, "QueueTable.label"));
    sb.append(format(map, CONNECTION_FIELDS, "ConnectionsTable.label"));
    sb.append(format(map, NODE_CL_REQUEST_TIME_FIELDS, "NodeClassLoadingRequestTable.label"));
    sb.append(format(map, CLIENT_CL_REQUEST_TIME_FIELDS, "ClientClassLoadingRequestTable.label"));
    sb.append(format(map, INBOUND_NETWORK_TRAFFIC_FIELDS, "InboundTrafficTable.label"));
    sb.append(format(map, OUTBOUND_NETWORK_TRAFFIC_FIELDS, "OutboundTrafficTable.label"));
    return sb.toString();
  }

  /**
   * Format a set of values.
   * @param map the map of field names to values.
   * @param fields the labels for the values.
   * @param label the title given to the set of values.
   * @return the values formatted as plain text.
   */
  private String format(final Map<Fields, String> map, final Fields[] fields, final String label) {
    StringBuilder sb = new StringBuilder();
    String title = LocalizationUtils.getLocalized(BASE, label, locale);
    sb.append(title).append('\n');
    sb.append(StringUtils.padRight("", '-', title.length())).append("\n\n");
    for (Fields field : fields) {
      String value = map.get(field);
      String name = LocalizationUtils.getLocalized(FIELD_BASE, field.name(), locale);
      sb.append(StringUtils.padRight(name, ' ', maxNameLength));
      sb.append(" = ");
      sb.append(StringUtils.padLeft(value, ' ', maxValueLength));
      sb.append("\n");
    }
    sb.append("\n");
    return sb.toString();
  }

  /**
   * Format a set of values.
   * @param map the map of field names to values.
   * @param fieldsArrays the labels for the values.
   */
  private void updateMaxLengths(final Map<Fields, String> map, final Fields[]... fieldsArrays) {
    maxNameLength = getMaxLength(locale);
    for (Fields[] fields : fieldsArrays) {
      for (Fields field : fields) {
        String value = map.get(field);
        if (value != null ) maxValueLength = Math.max(maxValueLength, value.length());
      }
    }
  }

  /**
   * 
   * @param locale the locale for which to retrieve or compute the length.
   * @return the length of the longest field name in the specified locale.
   */
  private static int getMaxLength(final Locale locale) {
    synchronized(maxNameLengthMap) {
      if (maxNameLengthMap.containsKey(locale)) return maxNameLengthMap.get(locale);
      int max = 0;
      for (Fields field: Fields.values()) {
        String name = LocalizationUtils.getLocalized(FIELD_BASE, field.name(), locale);
        if (name != null) max = Math.max(max, name.length());
      }
      maxNameLengthMap.put(locale, max);
      return max;
    }
  }
}
