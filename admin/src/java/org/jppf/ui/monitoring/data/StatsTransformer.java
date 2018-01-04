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
import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;

import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.utils.stats.*;

/**
 * This class provides a set of methods to format the statistics data received from the server.
 * @author Laurent Cohen
 */
public class StatsTransformer {
  /**
   * Value of 1 megabyte.
   */
  private static final long MB = 1024L * 1024L;

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
    double d = 0d;
    double sum = (d = stats.getSnapshot(CLIENT_IN_TRAFFIC).getTotal());
    map.put(CLIENT_INBOUND_MB, d);
    sum += (d = stats.getSnapshot(NODE_IN_TRAFFIC).getTotal());
    map.put(NODE_INBOUND_MB, d);
    //sum += (d= stats.getSnapshot(PEER_IN_TRAFFIC).getTotal());
    //map.put(PEER_INBOUND_MB, d);
    sum += (d = stats.getSnapshot(UNIDENTIFIED_IN_TRAFFIC).getTotal());
    map.put(UNIDENTIFIED_INBOUND_MB, d);
    map.put(TOTAL_INBOUND_MB, sum);
    sum = (d = stats.getSnapshot(CLIENT_OUT_TRAFFIC).getTotal());
    map.put(CLIENT_OUTBOUND_MB, d);
    sum += (d = stats.getSnapshot(NODE_OUT_TRAFFIC).getTotal());
    map.put(NODE_OUTBOUND_MB, d);
    //sum += (d = stats.getSnapshot(PEER_OUT_TRAFFIC).getTotal());
    //map.put(PEER_OUTBOUND_MB, d);
    sum += (d = stats.getSnapshot(UNIDENTIFIED_OUT_TRAFFIC).getTotal());
    map.put(UNIDENTIFIED_OUTBOUND_MB, d);
    map.put(TOTAL_OUTBOUND_MB, sum);
  }

  /**
   * Fill the map of values represented as doubles for a specified data snapshot.
   * @param map the map to fill.
   * @param snapshot the data snapshot to map.
   */
  private static void formatDoubleStatsValues(final Map<Fields, Double> map, final HealthSnapshot snapshot) {
    map.put(HEALTH_HEAP, (double) snapshot.getHeapUsed() / MB);
    double d = snapshot.getHeapUsedRatio();
    if (d < 0d) d = 0d;
    map.put(HEALTH_HEAP_PCT, 100d * d);
    map.put(HEALTH_NON_HEAP, (double) snapshot.getNonheapUsed() / MB);
    d = snapshot.getNonheapUsedRatio();
    map.put(HEALTH_NON_HEAP_PCT, d < 0d ? 0d : 100d * d);
    map.put(HEALTH_RAM, (double) snapshot.getRamUsed() / MB);
    d = snapshot.getRamUsedRatio();
    map.put(HEALTH_RAM_PCT, d < 0d ? 0d : 100d * d);
    map.put(HEALTH_THREADS, (double) snapshot.getLiveThreads());
    d = snapshot.getCpuLoad();
    map.put(HEALTH_CPU, d < 0d ? 0d : 100d * d);
    d = snapshot.getSystemCpuLoad();
    map.put(HEALTH_SYSTEM_CPU, d < 0d ? 0d : 100d * d);
  }
}
