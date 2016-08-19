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

import java.util.EventListener;

/**
 * Listener interface for changes in the jobs of one or more drivers. 
 * @author Laurent Cohen
 * @since 5.1
 */
public interface JobMonitoringListener extends EventListener {
  /**
   * Called when a new driver is added to the topology.
   * @param event the event encapsulating the change information.
   */
  void driverAdded(JobMonitoringEvent event);

  /**
   * Called when a new driver is added to the topology.
   * @param event the event encapsulating the change information.
   */
  void driverRemoved(JobMonitoringEvent event);

  /**
   * Called when a job is added to the driver queue.
   * @param event the event encapsulating the change information.
   */
  void jobAdded(JobMonitoringEvent event);

  /**
   * Called when a job is removed from the driver queue.
   * @param event the event encapsulating the change information.
   */
  void jobRemoved(JobMonitoringEvent event);

  /**
   * Called when the state a job has changed.
   * @param event the event encapsulating the change information.
   */
  void jobUpdated(JobMonitoringEvent event);

  /**
   * Called when a job is dispatched to a node.
   * @param event the event encapsulating the change information.
   */
  void jobDispatchAdded(JobMonitoringEvent event);

  /**
   * Called when a job dispatch returns from a node.
   * @param event the event encapsulating the change information.
   */
  void jobDispatchRemoved(JobMonitoringEvent event);
}
