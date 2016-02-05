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
 * A convenience class for subclasses that wish to subscribe to job monitoring events
 * without having to implement all the methods of the {@link JobMonitoringListener} interface.
 * This class provides an empty implementation for all the methods of the interface.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JobMonitoringListenerAdapter implements JobMonitoringListener {
  @Override
  public void driverAdded(final JobMonitoringEvent event) {
  }

  @Override
  public void driverRemoved(final JobMonitoringEvent event) {
  }

  @Override
  public void jobAdded(final JobMonitoringEvent event) {
  }

  @Override
  public void jobRemoved(final JobMonitoringEvent event) {
  }

  @Override
  public void jobUpdated(final JobMonitoringEvent event) {
  }

  @Override
  public void jobDispatchAdded(final JobMonitoringEvent event) {
  }

  @Override
  public void jobDispatchRemoved(final JobMonitoringEvent event) {
  }
}
