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

import java.util.*;

import org.jppf.ui.monitoring.LocalizedListItem;
import org.jppf.utils.collections.CollectionUtils;

/**
 * Constants for the JPPF statistics collected from the servers.
 * @author Laurent Cohen
 */
public class StatsConstants {
  /**
   * Base name for localization bundle lookups.
   */
  private static final String STATS_BASE = "org.jppf.ui.i18n.StatsPage";
  /**
   * A double values map with no values.
   */
  protected static final Map<Fields, Double> NO_DOUBLE_VALUES = new HashMap<>();
  /**
   * A string values map with no values.
   */
  protected static final Map<Fields, String> NO_STRING_VALUES = new HashMap<>();
  /**
   * List of stats properties related to network connections.
   */
  public static final Fields[] CONNECTION_FIELDS = { NB_NODES, MAX_NODES, NB_IDLE_NODES, NB_BUSY_NODES, NB_CLIENTS, MAX_CLIENTS };
  /**
   * List of stats properties related to queue operations.
   */
  public static final Fields[] QUEUE_FIELDS = { LATEST_QUEUE_TIME, TOTAL_QUEUE_TIME, MIN_QUEUE_TIME, MAX_QUEUE_TIME, AVG_QUEUE_TIME, TOTAL_QUEUED, QUEUE_SIZE, MAX_QUEUE_SIZE };
  /**
   * List of stats properties related to tasks execution.
   */
  public static final Fields[] EXECUTION_FIELDS = { TOTAL_TASKS_EXECUTED, TOTAL_EXECUTION_TIME, LATEST_EXECUTION_TIME, MIN_EXECUTION_TIME, MAX_EXECUTION_TIME, AVG_EXECUTION_TIME };
  /**
   * List of stats properties related to tasks execution.
   */
  public static final Fields[] NODE_EXECUTION_FIELDS = { TOTAL_NODE_EXECUTION_TIME, LATEST_NODE_EXECUTION_TIME, MIN_NODE_EXECUTION_TIME, MAX_NODE_EXECUTION_TIME, AVG_NODE_EXECUTION_TIME };
  /**
   * List of stats properties related to tasks execution.
   */
  public static final Fields[] TRANSPORT_FIELDS = { TOTAL_TRANSPORT_TIME, LATEST_TRANSPORT_TIME, MIN_TRANSPORT_TIME, MAX_TRANSPORT_TIME, AVG_TRANSPORT_TIME };
  /**
   * List of stats properties related to job execution.
   */
  public static final Fields[] JOB_FIELDS = { JOBS_TOTAL, JOBS_LATEST, JOBS_MAX, JOBS_LATEST_TIME, JOBS_MIN_TIME, JOBS_MAX_TIME, JOBS_AVG_TIME, JOBS_MIN_TASKS, JOBS_MAX_TASKS, JOBS_AVG_TASKS };
  /**
   * List of stats properties related to class loading requests to the clients.
   */
  public static final Fields[] CLIENT_CL_REQUEST_TIME_FIELDS = { CLIENT_TOTAL_CL_REQUEST_COUNT, CLIENT_AVG_CL_REQUEST_TIME, CLIENT_MIN_CL_REQUEST_TIME, CLIENT_MAX_CL_REQUEST_TIME,
    CLIENT_LATEST_CL_REQUEST_TIME };
  /**
   * List of stats properties related to class loading requests from the nodes.
   */
  public static final Fields[] NODE_CL_REQUEST_TIME_FIELDS = { NODE_TOTAL_CL_REQUEST_COUNT, NODE_AVG_CL_REQUEST_TIME, NODE_MIN_CL_REQUEST_TIME, NODE_MAX_CL_REQUEST_TIME,
    NODE_LATEST_CL_REQUEST_TIME };
  /**
   * List of stats properties for inbound network traffic.
   */
  public static final Fields[] INBOUND_NETWORK_TRAFFIC_FIELDS = { CLIENT_INBOUND_MB, NODE_INBOUND_MB, UNIDENTIFIED_INBOUND_MB, TOTAL_INBOUND_MB };
  /**
   * List of stats properties for inbound network traffic.
   */
  public static final Fields[] OUTBOUND_NETWORK_TRAFFIC_FIELDS = { CLIENT_OUTBOUND_MB, NODE_OUTBOUND_MB, UNIDENTIFIED_OUTBOUND_MB, TOTAL_OUTBOUND_MB };
  /**
   * List of properties for health snapshots.
   * @since 5.0
   */
  public static final Fields[] HEALTH_FIELDS = { HEALTH_HEAP, HEALTH_HEAP_PCT, HEALTH_NON_HEAP, HEALTH_NON_HEAP_PCT, HEALTH_RAM, HEALTH_RAM_PCT, HEALTH_THREADS, HEALTH_CPU, HEALTH_SYSTEM_CPU };
  /**
   * List of all fields displayed in the server stats view.
   */
  public static final Fields[] ALL_FIELDS = CollectionUtils.concatArrays(EXECUTION_FIELDS, NODE_EXECUTION_FIELDS, TRANSPORT_FIELDS, JOB_FIELDS, QUEUE_FIELDS, CONNECTION_FIELDS,
    NODE_CL_REQUEST_TIME_FIELDS, INBOUND_NETWORK_TRAFFIC_FIELDS, OUTBOUND_NETWORK_TRAFFIC_FIELDS);
  /**
   * List of all fields available in the charts.
   * @since 5.0
   */
  public static final Fields[] ALL_CHART_FIELDS = CollectionUtils.concatArrays(EXECUTION_FIELDS, NODE_EXECUTION_FIELDS, TRANSPORT_FIELDS, JOB_FIELDS, QUEUE_FIELDS, CONNECTION_FIELDS,
    NODE_CL_REQUEST_TIME_FIELDS, INBOUND_NETWORK_TRAFFIC_FIELDS, OUTBOUND_NETWORK_TRAFFIC_FIELDS, HEALTH_FIELDS);
  /**
   * Name of the execution table.
   */
  public static final String EXECUTION = "ExecutionTable";
  /**
   * Name of the node execution table.
   */
  public static final String NODE_EXECUTION = "NodeExecutionTable";
  /**
   * Name of the network overhead table.
   */
  public static final String TRANSPORT = "NetworkOverheadTable";
  /**
   * Name of the connections table.
   */
  public static final String CONNECTION = "ConnectionsTable";
  /**
   * Name of the tasks queue table.
   */
  public static final String QUEUE = "QueueTable";
  /**
   * Name of the jobs queue table.
   */
  public static final String JOB = "JobTable";
  /**
   * Name of the node class loading requests table.
   */
  public static final String NODE_CL_REQUEST_TIME = "NodeClassLoadingRequestTable";
  /**
   * Name of the client class loading requests table.
   */
  public static final String CLIENT_CL_REQUEST_TIME = "ClientClassLoadingRequestTable";
  /**
   * Name of the inboud network traffic requests table.
   */
  public static final String INBOUND_NETWORK_TRAFFIC = "InboundTrafficTable";
  /**
   * Name of the outboud network traffic requests table.
   */
  public static final String OUTBOUND_NETWORK_TRAFFIC = "OutboundTrafficTable";
  /**
   * Mapping of table names to the associated fields.
   */
  public static final Map<String, Fields[]> ALL_TABLES_MAP = createFieldsMap();

  /**
   * Create a mapping of table names to the corresponding set of fields.
   * @return a map of names to {@code Field[]}.
   */
  private static Map<String, Fields[]> createFieldsMap() {
    Map<String, Fields[]> map = new LinkedHashMap<>();
    map.put(EXECUTION, EXECUTION_FIELDS);
    map.put(NODE_EXECUTION, NODE_EXECUTION_FIELDS);
    map.put(TRANSPORT, TRANSPORT_FIELDS);
    map.put(CONNECTION, CONNECTION_FIELDS);
    map.put(QUEUE, QUEUE_FIELDS);
    map.put(JOB, JOB_FIELDS);
    map.put(NODE_CL_REQUEST_TIME, NODE_CL_REQUEST_TIME_FIELDS);
    map.put(CLIENT_CL_REQUEST_TIME, CLIENT_CL_REQUEST_TIME_FIELDS);
    map.put(INBOUND_NETWORK_TRAFFIC, INBOUND_NETWORK_TRAFFIC_FIELDS);
    map.put(OUTBOUND_NETWORK_TRAFFIC, OUTBOUND_NETWORK_TRAFFIC_FIELDS);
    return Collections.unmodifiableMap(map);
  }

  /**
   * Create the map of localized items fo all the statistics tables.
   * @param locale the locale to localize into.
   * @return a mapping of non-localized names to {@link LocalizedListItem} objects.
   */
  public static Map<String, LocalizedListItem> createLocalizedItems(final Locale locale) {
    Map<String, LocalizedListItem> map = new LinkedHashMap<>();
    int i = 0;
    for (String name: ALL_TABLES_MAP.keySet()) map.put(name, new LocalizedListItem(name, i++, STATS_BASE, locale));
    return Collections.unmodifiableMap(map);
  }
}
