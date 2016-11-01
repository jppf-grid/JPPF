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

package org.jppf.admin.web.health;

/**
 * 
 * @author Laurent Cohen
 */
public class HealthConstants {
  /**
   * System gc action id.
   */
  public static String GC_ACTION = "health.gc";
  /**
   * Get a thread dump action id.
   */
  public static String THREAD_DUMP_ACTION = "health.threaddump";
  /**
   * Trigger heap dump action id.
   */
  public static String HEAP_DUMP_ACTION = "health.heap_dump";
  /**
   * Set thresholds action id.
   */
  public static String THRESHOLDS_ACTION = "health.thresholds";
  /**
   * Expand all action id.
   */
  public static String EXPAND_ALL_ACTION = "health.expand";
  /**
   * Collapse action id.
   */
  public static String COLLAPSE_ALL_ACTION = "health.collapse";
  /**
   * Select drivers action id.
   */
  public static String SELECT_DRIVERS_ACTION = "health.select_drivers";
  /**
   * Select nodes action id.
   */
  public static String SELECT_NODES_ACTION = "health.select_nodes";
  /**
   * Select all action id.
   */
  public static String SELECT_ALL_ACTION = "health.select_all";
}
