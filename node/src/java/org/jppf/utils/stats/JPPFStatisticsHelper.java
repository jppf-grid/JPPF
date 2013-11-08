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

package org.jppf.utils.stats;


/**
 * 
 * @author Laurent Cohen
 */
public final class JPPFStatisticsHelper
{
  /**
   * Count of tasks dispatched to nodes.
   */
  public static final String TASK_DISPATCH = "task.dispatch";
  /**
   * Execution times including server/nodes trnasport overhead.
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
   * Tasks count.
   */
  public static final String TASK_QUEUE_COUNT = "task.queue.count";
  /**
   * Tasks times.
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
   * Create a statistics object initialized with all the required server snapshots.
   * @return a {@link JPPFStatistics} instance.
   */
  public static JPPFStatistics createServerStatistics()
  {
    JPPFStatistics statistics = new JPPFStatistics();
    statistics.createSnapshots(false, EXECUTION, NODE_EXECUTION, TRANSPORT_TIME, TASK_QUEUE_TOTAL, TASK_QUEUE_TIME, JOB_TOTAL, JOB_TIME, JOB_TASKS,
      TASK_DISPATCH, NODE_CLASS_REQUESTS_TIME, CLIENT_CLASS_REQUESTS_TIME);
    statistics.createSnapshots(true, TASK_QUEUE_COUNT, JOB_COUNT, NODES, IDLE_NODES, CLIENTS);
    return statistics;
  }
}
