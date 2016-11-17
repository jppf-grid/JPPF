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

package org.jppf.admin.web.topology;

/**
 * 
 * @author Laurent Cohen
 */
public class TopologyConstants {
  /**
   * Server stop/restart action id.
   */
  public static String SERVER_STOP_RESTART_ACTION = "topology.server_stop_restart";
  /**
   * Server reset stats action id.
   */
  public static String SERVER_RESET_STATS_ACTION = "topology.server_reset_stats";
  /**
   * Server load-balancing settings action id.
   */
  public static String SERVER_LOAD_BALANCING_ACTION = "topology.load_balancing";
  /**
   * Node configuration update action id.
   */
  public static String NODE_CONFIG_ACTION = "topology.node_config";
  /**
   * System info action id.
   */
  public static String SYSTEM_INFO_ACTION = "topology.info";
  /**
   * Node thread pool config action id.
   */
  public static String NODE_THREADS_ACTION = "topology.node_threads";
  /**
   * Node reset task counter action id.
   */
  public static String NODE_RESET_TASKS_ACTION = "topology.node_reset_tasks";
  /**
   * Cancel pending action id.
   */
  public static String CANCEL_PENDING_ACTION = "topology.cancel_pending_action";
  /**
   * Stop node action id.
   */
  public static String NODE_STOP_ACTION = "topology.node_stop";
  /**
   * Restart node action id.
   */
  public static String NODE_RESTART_ACTION = "topology.node_restart";
  /**
   * Deferred stop node action id.
   */
  public static String NODE_STOP_DEFERRED_ACTION = "topology.node_stop_deferred";
  /**
   * Deferred restart node action id.
   */
  public static String NODE_RESTART_DEFERRED_ACTION = "topology.node_restart_deferred";
  /**
   * Deferred suspend node action id.
   */
  public static String NODE_SUSPEND_ACTION = "topology.node_suspend";
  /**
   * Provisioning action id.
   */
  public static String PROVISIONING_ACTION = "topology.provisioning";
  /**
   * Expand all action id.
   */
  public static String EXPAND_ALL_ACTION = "topology.expand";
  /**
   * Collapse action id.
   */
  public static String COLLAPSE_ALL_ACTION = "topology.collapse";
  /**
   * Select drivers action id.
   */
  public static String SELECT_DRIVERS_ACTION = "topology.select_drivers";
  /**
   * Select nodes action id.
   */
  public static String SELECT_NODES_ACTION = "topology.select_nodes";
  /**
   * Select all action id.
   */
  public static String SELECT_ALL_ACTION = "topology.select_all";
}
