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

import java.util.*;

import org.jppf.job.JobInformation;

/**
 * Instanes of this class represent a job as seen by a driver.
 * @author Laurent Cohen
 * @since 5.1
 */
public class Job extends AbstractJobComponent {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Information on the job.
   */
  private transient JobInformation jobInfo;

  /**
   * Initialize this job with the supplied information.
   * @param jobInfo information on the job.
   */
  Job(final JobInformation jobInfo) {
    super(jobInfo.getJobUuid());
    this.jobInfo = jobInfo;
  }

  /**
   * Set the information on the job.
   * @param jobInfo an instance of {@link JobInformation}.
   */
  synchronized void setJobInformation(final JobInformation jobInfo) {
    this.jobInfo = jobInfo;
  }

  /**
   * Get the information on the job.
   * @return an instance of {@link JobInformation}.
   */
  public synchronized JobInformation getJobInformation() {
    return jobInfo;
  }

  /**
   * Get the driver that holds this job.
   * @return a {@link JobDriver} instance.
   */
  public JobDriver getJobDriver() {
    return (JobDriver) getParent();
  }

  /**
   * Get the dispatch with the specified node uuid for this job.
   * @param nodeUuid the uuid of the node of the dispatch to retrieve.
   * @return a {@link JobDispatch} instance, or {@code null} if thiere is nu such job dispatch.
   */
  public JobDispatch getJobDispatch(final String nodeUuid) {
    return (JobDispatch) getChild(nodeUuid);
  }

  /**
   * Get the job dispatches for this job.
   * @return a list of {@link JobDispatch} instances, possibly empty.
   */
  public List<JobDispatch> getJobDispatches() {
    final List<JobDispatch> list = new ArrayList<>(getChildCount());
    for (final AbstractJobComponent child: getChildren()) list.add((JobDispatch) child);
    return list;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", jobInfo=").append(jobInfo);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public String getDisplayName() {
    return jobInfo != null ? jobInfo.getJobName() : "" + uuid;
  }
}
