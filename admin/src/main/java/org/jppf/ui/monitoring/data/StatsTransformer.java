/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import static org.jppf.ui.monitoring.data.FieldsEnum.*;
import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;

import org.jppf.management.diagnostics.*;
import org.jppf.utils.configuration.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * This class provides a set of methods to format the statistics data received from the server.
 * @author Laurent Cohen
 */
public class StatsTransformer {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(StatsTransformer.class);

  /**
   * Instantiation of this class is not allowed.
   */
  public StatsTransformer() {
  }

  /**
   * Get the map of values represented as double for a specified data snapshot.
   * @param stats the data snapshot to map.
   * @param snapshot the health data snapshot to map.
   * @return a map of field names to their corresponding double values.
   */
  public Map<Fields, Double> formatDoubleValues(final JPPFStatistics stats, final HealthSnapshot snapshot) {
    final Map<Fields, Double> map = new HashMap<>();
    formatDoubleStatsValues(map, stats);
    formatDoubleStatsValues(map, snapshot);
    return map;
  }

  /**
   * Get the map of values represented as double for a specified data snapshot.
   * @param map the map to fill.
   * @param stats the data snapshot to map.
   * @return a map of field names to their corresponding double values.
   */
  private static Map<Fields, Double> formatDoubleStatsValues(final Map<Fields, Double> map, final JPPFStatistics stats) {
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
    final double d = snapshot.getLatest();
    map.put(NB_NODES, d);
    map.put(MAX_NODES, snapshot.getMax());
    final double idle = stats.getSnapshot(IDLE_NODES).getLatest();
    map.put(NB_IDLE_NODES, idle);
    map.put(NB_BUSY_NODES, d - idle);
    snapshot = stats.getSnapshot(CLIENTS);
    map.put(NB_CLIENTS, snapshot.getLatest());
    map.put(MAX_CLIENTS, snapshot.getMax());
    snapshot = stats.getSnapshot(NODE_CLASS_REQUESTS_TIME);
    map.put(NODE_TOTAL_CL_REQUEST_COUNT, (double) snapshot.getValueCount());
    map.put(NODE_AVG_CL_REQUEST_TIME, snapshot.getAvg());
    map.put(NODE_MIN_CL_REQUEST_TIME, snapshot.getMin());
    map.put(NODE_MAX_CL_REQUEST_TIME, snapshot.getMax());
    map.put(NODE_LATEST_CL_REQUEST_TIME, snapshot.getLatest());
    snapshot = stats.getSnapshot(CLIENT_CLASS_REQUESTS_TIME);
    map.put(CLIENT_TOTAL_CL_REQUEST_COUNT, (double) snapshot.getValueCount());
    map.put(CLIENT_AVG_CL_REQUEST_TIME, snapshot.getAvg());
    map.put(CLIENT_MIN_CL_REQUEST_TIME, snapshot.getMin());
    map.put(CLIENT_MAX_CL_REQUEST_TIME, snapshot.getMax());
    map.put(CLIENT_LATEST_CL_REQUEST_TIME, snapshot.getLatest());
    formatDoubleStatsValues2(map, stats);
    return map;
  }

  /**
   * Fill the map of values represented as doubles for a specified data snapshot.
   * @param map the map to fill.
   * @param stats the data snapshot to map.
   */
  private static void formatDoubleStatsValues2(final Map<Fields, Double> map, final JPPFStatistics stats) {
    map.put(JOBS_TOTAL, stats.getSnapshot(JOB_TOTAL).getTotal());
    JPPFSnapshot snapshot = stats.getSnapshot(JOB_COUNT);
    final double totalJobs = snapshot.getTotal();
    map.put(JOBS_LATEST, totalJobs);
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

    map.put(JOB_DISPATCHES_TOTAL, stats.getSnapshot(JOB_DISPATCH_TOTAL).getTotal());
    snapshot = stats.getSnapshot(JOB_DISPATCH_COUNT);
    map.put(JOB_DISPATCHES_LATEST, snapshot.getTotal());
    map.put(JOB_DISPATCHES_MAX, snapshot.getMax());
    snapshot = stats.getSnapshot(JOB_DISPATCH_TIME);
    map.put(JOB_DISPATCHES_LATEST_TIME,snapshot.getLatest());
    map.put(JOB_DISPATCHES_MIN_TIME, snapshot.getMin());
    map.put(JOB_DISPATCHES_MAX_TIME, snapshot.getMax());
    map.put(JOB_DISPATCHES_AVG_TIME, snapshot.getAvg());
    snapshot = stats.getSnapshot(JOB_DISPATCH_TASKS);
    map.put(JOB_DISPATCHES_MIN_TASKS, snapshot.getMin());
    map.put(JOB_DISPATCHES_MAX_TASKS, snapshot.getMax());
    map.put(JOB_DISPATCHES_AVG_TASKS, snapshot.getAvg());
    snapshot = stats.getSnapshot(DISPATCH_PER_JOB_COUNT);
    map.put(DISPATCHES_PER_JOB_MIN, snapshot.getMin());
    map.put(DISPATCHES_PER_JOB_MAX, snapshot.getMax());
    map.put(DISPATCHES_PER_JOB_AVG, snapshot.getAvg());

    double d = 0d;
    double sum = (d = stats.getSnapshot(CLIENT_IN_TRAFFIC).getTotal());
    map.put(CLIENT_INBOUND_MB, d);
    sum += (d = stats.getSnapshot(NODE_IN_TRAFFIC).getTotal());
    map.put(NODE_INBOUND_MB, d);
    //sum += (d= stats.getSnapshot(PEER_IN_TRAFFIC).getTotal());
    //map.put(PEER_INBOUND_MB, d);
    sum += (d = stats.getSnapshot(JMX_IN_TRAFFIC).getTotal());
    map.put(JMX_INBOUND_MB, d);
    map.put(TOTAL_INBOUND_MB, sum);
    sum = (d = stats.getSnapshot(CLIENT_OUT_TRAFFIC).getTotal());
    map.put(CLIENT_OUTBOUND_MB, d);
    sum += (d = stats.getSnapshot(NODE_OUT_TRAFFIC).getTotal());
    map.put(NODE_OUTBOUND_MB, d);
    //sum += (d = stats.getSnapshot(PEER_OUT_TRAFFIC).getTotal());
    //map.put(PEER_OUTBOUND_MB, d);
    sum += (d = stats.getSnapshot(JMX_OUT_TRAFFIC).getTotal());
    map.put(JMX_OUTBOUND_MB, d);
    map.put(TOTAL_OUTBOUND_MB, sum);
  }

  /**
   * Fill the map of values represented as doubles for a specified data snapshot.
   * @param map the map to fill.
   * @param snapshot the data snapshot to map.
   */
  private static void formatDoubleStatsValues(final Map<Fields, Double> map, final HealthSnapshot snapshot) {
    try {
      final List<JPPFProperty<?>> properties = MonitoringDataProviderHandler.getAllProperties();
      for (final JPPFProperty<?> prop: properties) {
        if (prop instanceof NumberProperty) {
          final String name = prop.getName();
          final double d = snapshot.getDouble(name);
          final Fields field = StatsConstants.getFieldForName(name);
          map.put(field, d >= 0d ? d : 0d);
        }
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
