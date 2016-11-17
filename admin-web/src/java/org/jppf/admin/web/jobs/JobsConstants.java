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

package org.jppf.admin.web.jobs;

/**
 * 
 * @author Laurent Cohen
 */
public class JobsConstants {
  /**
   * Terminate job action id.
   */
  public static String CANCEL_ACTION = "jobs.terminate";
  /**
   * Suspend job action id.
   */
  public static String SUSPEND_ACTION = "jobs.suspend";
  /**
   * Suspend/requeue job action id.
   */
  public static String SUSPEND_REQUEUE_ACTION = "jobs.suspend_requeue";
  /**
   * Resume job action id.
   */
  public static String RESUME_ACTION = "jobs.resume";
  /**
   * Update job max nodes action id.
   */
  public static String UPDATE_MAX_NODES_ACTION = "jobs.update_max_nodes";
  /**
   * Update job priority action id.
   */
  public static String UPDATE_PRIORITY_ACTION = "jobs.update_priority";
  /**
   * Select jobs action id.
   */
  public static String SELECT_JOBS_ACTION = "jobs.select_jobs";
  /**
   * Expand all action id.
   */
  public static String EXPAND_ALL_ACTION = "jobs.expand";
  /**
   * Collapse action id.
   */
  public static String COLLAPSE_ALL_ACTION = "jobs.collapse";
}
