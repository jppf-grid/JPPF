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

package org.jppf.client.monitoring.jobs;

/**
 * Enumeration of the possible ways job updates are published as events by the job monitor.
 * @since 5.1
 */
public enum JobMonitorUpdateMode {
  /**
   * Updates are computed by polling information on the jobs from the drivers at regular intervals.
   */
  POLLING,
  /**
   * Updates are computed from JMX notifications and pushed immediately as job monitor events.
   * This means one event for each jmx notification.
   */
  IMMEDIATE_NOTIFICATIONS,
  /**
   * Updates are computed from JMX notifications and published periodically as job monitor events.
   * Notifications are merged/aggregated in the interval between publications.
   */
  DEFERRED_NOTIFICATIONS
}