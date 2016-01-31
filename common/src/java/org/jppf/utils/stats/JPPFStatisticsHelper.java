/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.utils.stats;

import java.util.*;

import org.jppf.utils.*;


/**
 * This helper class holds the constants definitions for the labels
 * of the statistics snapshots used in JPPF, along with utility methods.
 * @author Laurent Cohen
 */
public final class JPPFStatisticsHelper {
  /**
   * Location of the localization resource bundles.
   */
  private  static final String I18N_BASE = "org.jppf.utils.stats.i18n.StatsLabels";
  /**
   * Count of tasks dispatched to nodes.
   */
  public static final String TASK_DISPATCH = "task.dispatch";
  /**
   * Execution times including server/nodes transport overhead.
   */
  public static final String EXECUTION = "execution";
  /**
   * Execution times in the nodes.
   */
  public static final String NODE_EXECUTION = "node.execution";
  /**
   * JPPF and network transport overhead.
   */
  public static final String TRANSPORT_TIME = "transport.time";
  /**
   * Total queued tasks.
   */
  public static final String TASK_QUEUE_TOTAL = "task.queue.total";
  /**
   * Queue tasks count.
   */
  public static final String TASK_QUEUE_COUNT = "task.queue.count";
  /**
   * Queue tasks times.
   */
  public static final String TASK_QUEUE_TIME = "task.queue.time";
  /**
   * Total number of submitted jobs.
   */
  public static final String JOB_TOTAL = "job.total";
  /**
   * Jobs counters.
   */
  public static final String JOB_COUNT = "job.count";
  /**
   * Jobs times.
   */
  public static final String JOB_TIME = "job.time";
  /**
   * Number of tasks in jobs.
   */
  public static final String JOB_TASKS = "job.tasks";
  /**
   * Number of connected nodes.
   */
  public static final String NODES = "nodes";
  /**
   * Number of idle connected nodes.
   */
  public static final String IDLE_NODES = "idle.nodes";
  /**
   * Number of client connections.
   */
  public static final String CLIENTS = "clients";
  /**
   * Time for class loading requests from nodes to complete.
   */
  public static final String NODE_CLASS_REQUESTS_TIME = "node.class.requests.time";
  /**
   * Time for class loading requests from nodes to complete.
   */
  public static final String CLIENT_CLASS_REQUESTS_TIME = "client.class.requests.time";
  /**
   * Bytes received from remote nodes.
   */
  public static final String NODE_IN_TRAFFIC = "node.traffic.in";
  /**
   * Bytes sent to remote nodes.
   */
  public static final String NODE_OUT_TRAFFIC = "node.traffic.out";
  /**
   * Bytes received from remote clients.
   */
  public static final String CLIENT_IN_TRAFFIC = "client.traffic.in";
  /**
   * Bytes sent to remote clients.
   */
  public static final String CLIENT_OUT_TRAFFIC = "client.traffic.out";
  /**
   * Bytes received from remote peer servers.
   */
  public static final String PEER_IN_TRAFFIC = "peer.traffic.in";
  /**
   * Bytes sent to remote peer servers.
   */
  public static final String PEER_OUT_TRAFFIC = "peer.traffic.out";
  /**
   * Bytes received from unidentified remote peers.
   */
  public static final String UNIDENTIFIED_IN_TRAFFIC = "unidentified.traffic.in";
  /**
   * Bytes sent to unidentified remote peers.
   */
  public static final String UNIDENTIFIED_OUT_TRAFFIC = "unidentified.traffic.out";

  /**
   * Determine wether the specified snapshot is a single value snapshot.
   * @param snapshot the snapshot to evaluate.
   * @return {@code true} if the snapshot is a single value snapshot, {@code false} otherwise.
   */
  public static boolean isSingleValue(final JPPFSnapshot snapshot) {
    return snapshot instanceof SingleValueSnapshot;
  }

  /**
   * Determine wether the specified snapshot is a cumulative snapshot.
   * @param snapshot the snapshot to evaluate.
   * @return {@code true} if the snapshot is a cumulative snapshot, {@code false} otherwise.
   */
  public static boolean isCumulative(final JPPFSnapshot snapshot) {
    return snapshot instanceof CumulativeSnapshot;
  }

  /**
   * Determine wether the specified snapshot is a non-cumulative snapshot.
   * @param snapshot the snapshot to evaluate.
   * @return {@code true} if the snapshot is a non-cumulative snapshot, {@code false} otherwise.
   */
  public static boolean isNonCumulative(final JPPFSnapshot snapshot) {
    return snapshot instanceof NonCumulativeSnapshot;
  }

  /**
   * Get the localized translation of the label of the specified snapshot in the current locale.
   * @param snapshot the snapshot whose label to translate.
   * @return a translation of the label, or the label itself if no translation could be found.
   */
  public static String getLocalizedLabel(final JPPFSnapshot snapshot) {
    return getLocalizedLabel(snapshot, Locale.getDefault());
  }

  /**
   * Get the localized translation of the label of the specified snapshot in the specified locale.
   * @param snapshot the snapshot whose label to translate.
   * @param locale the locale in which to translate.
   * @return a translation of the label, or the label itself if no translation could be found.
   */
  public static String getLocalizedLabel(final JPPFSnapshot snapshot, final Locale locale) {
    String label = snapshot.getLabel();
    return LocalizationUtils.getLocalized(I18N_BASE, label, label, locale);
  }

  /**
   * Create a statistics object initialized with all the required server snapshots.
   * @return a {@link JPPFStatistics} instance.
   * @exclude
   */
  public static JPPFStatistics createServerStatistics() {
    JPPFStatistics statistics = new JPPFStatistics();
    Iterator<JPPFFilteredStatisticsListener> it = ServiceFinder.lookupProviders(JPPFFilteredStatisticsListener.class);
    while (it.hasNext()) {
      JPPFFilteredStatisticsListener listener = it.next();
      statistics.addListener(listener, listener.getFilter());
    }
    statistics.createSnapshots(false, EXECUTION, NODE_EXECUTION, TRANSPORT_TIME, TASK_QUEUE_TIME, JOB_TIME, JOB_TASKS, TASK_DISPATCH,
        NODE_CLASS_REQUESTS_TIME, CLIENT_CLASS_REQUESTS_TIME);
    statistics.createSnapshots(true, TASK_QUEUE_COUNT, JOB_COUNT, NODES, IDLE_NODES, CLIENTS);
    statistics.createSingleValueSnapshots(TASK_QUEUE_TOTAL, JOB_TOTAL, NODE_IN_TRAFFIC, NODE_OUT_TRAFFIC, CLIENT_IN_TRAFFIC, CLIENT_OUT_TRAFFIC,
        PEER_IN_TRAFFIC, PEER_OUT_TRAFFIC, UNIDENTIFIED_IN_TRAFFIC, UNIDENTIFIED_OUT_TRAFFIC);
    return statistics;
  }
}
