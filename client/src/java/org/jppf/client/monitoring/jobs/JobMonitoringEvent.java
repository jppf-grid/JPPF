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

package org.jppf.client.monitoring.jobs;

import java.util.EventObject;

import org.jppf.client.monitoring.AbstractComponent;

/**
 * This class represents an event emitteed by a {@link JobMonitor}.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JobMonitoringEvent extends EventObject {
  /**
   * The job driver where this event originates from.
   */
  private final JobDriver jobDriver;
  /**
   * The related job, if any.
   */
  private final Job job;
  /**
   * The related job dispatch, if any.
   */
  private final JobDispatch jobDispatch;

  /**
   * The possible types of events.
   * @exclude
   */
  enum Type {
    /**
     * A driver was added.
     */
    DRIVER_ADDED,
    /**
     * A driver was removed.
     */
    DRIVER_REMOVED,
    /**
     * A job was added.
     */
    JOB_ADDED,
    /**
     * A job was removed.
     */
    JOB_REMOVED,
    /**
     * A job was updated.
     */
    JOB_UPDATED,
    /**
     * A job dispatch was added.
     */
    DISPATCH_ADDED,
    /**
     * A job dispatch was removed.
     */
    DISPATCH_REMOVED
  };

  /**
   * Initialize this job event.
   * @param jobMonitor the job monitor which emitted this event.
   * @param jobDriver the job driver from which this event originates.
   * @param job the related job, if any.
   * @param jobDuspatch the related job dispatch, if any.
   */
  JobMonitoringEvent(final JobMonitor jobMonitor, final JobDriver jobDriver, final Job job, final JobDispatch jobDuspatch) {
    super(jobMonitor);
    this.jobDriver = jobDriver;
    this.job = job;
    this.jobDispatch = jobDuspatch;
  }

  /**
   * Get the job monitor which emitted this event.
   * @return a {@link JobMonitor} instance.
   */
  public JobMonitor getJobMonitor() {
    return (JobMonitor) getSource();
  }

  /**
   * Get the job driver from which this event originates.
   * @return a {@link JobDriver} instance.
   */
  public JobDriver getJobDriver() {
    return jobDriver;
  }

  /**
   * Get the related job, if any.
   * @return a {@link Job} instance, or {@code null} if this event does not relate to a job.
   */
  public Job getJob() {
    return job;
  }

  /**
   * Get the related job dispatch, if any.
   * @return a {@link JobDispatch} instance, or {@code null} if this event does not relate to a job dispatch.
   */
  public JobDispatch getJobDispatch() {
    return jobDispatch;
  }

  @Override
  public String toString() {
    return "JobMonitoringEvent[jobDriver=" + name(jobDriver) + ", job=" + name(job) + ", jobDispatch=" + name(jobDispatch) + "]";
  }

  /**
   * Get a string representing the specified component.
   * @param comp the component whose name to get.
   * @return a string representing the component.
   */
  private String name(final AbstractComponent<?> comp) {
    return (comp == null) ? "none" : comp.getDisplayName();
  }
}
