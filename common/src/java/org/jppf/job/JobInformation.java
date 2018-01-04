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
package org.jppf.job;

import java.io.Serializable;

import org.jppf.node.protocol.TaskBundle;

/**
 * Instances of this class provide information about the state of a job being processed by a driver.
 * @author Laurent Cohen
 */
public class JobInformation implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The unique identifier for the job.
   */
  private String jobUuid = null;
  /**
   * The user-defined name for the job.
   */
  private String jobName = null;
  /**
   * The current number of tasks in the job.
   */
  private int taskCount = 0;
  /**
   * The initial number of tasks in the job.
   */
  private int initialTaskCount = 0;
  /**
   * The priority of this job.
   */
  private int priority = 0;
  /**
   * Determines whether the job is in suspended state.
   */
  private boolean suspended = false;
  /**
   * Determines whether the job is waiting to reach its scheduled execution date.
   */
  private boolean pending = false;
  /**
   * The maximum number of nodes this job can run on.
   */
  private int maxNodes = Integer.MAX_VALUE;

  /**
   * Initialize this object.
   */
  public JobInformation() {
  }

  /**
   * Initialize this object with the specified parameters.
   * @param bundle the task bundle from which to get the job information.
   */
  public JobInformation(final TaskBundle bundle) {
    this(bundle.getUuid(), bundle.getName(), bundle.getCurrentTaskCount(), bundle.getInitialTaskCount(), bundle.getSLA().getPriority(), bundle.getSLA().isSuspended(), bundle.isPending());
  }

  /**
   * Initialize this object with the specified parameters.
   * @param jobUuid the universal unique id of this job.
   * @param jobId the user-defined id of this job.
   * @param taskCount the number of tasks in this job.
   * @param initialTaskCount the initial number of tasks in the job submitted by the JPPF client.
   * @param priority the priority of this job.
   * @param suspended determines whether the job is in suspended state.
   * @param pending determines whether the job is waiting to reach its scheduled execution date.
   */
  public JobInformation(final String jobUuid, final String jobId, final int taskCount, final int initialTaskCount, final int priority, final boolean suspended, final boolean pending) {
    this.jobUuid = jobUuid;
    this.jobName = jobId;
    this.taskCount = taskCount;
    this.initialTaskCount = initialTaskCount;
    this.priority = priority;
    this.suspended = suspended;
    this.pending = pending;
  }

  /**
   * Get the user-defined name for the job.
   * @return the job name as a string.
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * Set the user-defined name for the job.
   * @param name the name as a string.
   * @deprecated the field is initialized in a constructor, this setter is never used in JPPF code and should not be used in client code.
   * @exclude
   */
  @Deprecated
  public void setJobName(final String name) {
    this.jobName = name;
  }

  /**
   * Get the current number of tasks in the job.
   * @return the number of tasks as an int.
   */
  public int getTaskCount() {
    return taskCount;
  }

  /**
   * Set the current number of tasks in the job.
   * @param taskCount the number of tasks as an int.
   * @deprecated the field is initialized in a constructor, this setter is never used in JPPF code and should not be used in client code.
   * @exclude
   */
  @Deprecated
  public void setTaskCount(final int taskCount) {
    this.taskCount = taskCount;
  }

  /**
   * Get the priority of the job.
   * @return the priority as an int.
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Set the priority of the job.
   * @param priority the priority as an int.
   * @deprecated the field is initialized in a constructor, this setter is never used in JPPF code and should not be used in client code.
   * @exclude
   */
  @Deprecated
  public void setPriority(final int priority) {
    this.priority = priority;
  }

  /**
   * Get the initial task count of the job, that is, the number of tasks it contained when it was received by the driver.
   * @return the task count as an int.
   */
  public int getInitialTaskCount() {
    return initialTaskCount;
  }

  /**
   * Set the initial task count of the job.
   * @param initialTaskCount the task count as an int.
   * @deprecated the field is initialized in a constructor, this setter is never used in JPPF code and should not be used in client code.
   * @exclude
   */
  @Deprecated
  public void setInitialTaskCount(final int initialTaskCount) {
    this.initialTaskCount = initialTaskCount;
  }

  /**
   * Determine whether the job is in suspended state.
   * @return true if the job is suspended, false otherwise.
   */
  public boolean isSuspended() {
    return suspended;
  }

  /**
   * Specify whether the job is in suspended state.
   * @param suspended true if the job is suspended, false otherwise.
   * @deprecated the field is initialized in a constructor, this setter is never used in JPPF code and should not be used in client code.
   * @exclude
   */
  @Deprecated
  public void setSuspended(final boolean suspended) {
    this.suspended = suspended;
  }

  /**
   * Get the maximum number of nodes this job can run on.
   * @return the number of nodes as an int value.
   */
  public int getMaxNodes() {
    return maxNodes;
  }

  /**
   * Get the maximum number of nodes this job can run on.
   * @param maxNodes the number of nodes as an int value.
   * @exclude
   */
  public void setMaxNodes(final int maxNodes) {
    this.maxNodes = maxNodes;
  }

  /**
   * Get the pending state of the job.
   * A job is pending if its scheduled execution date/time has not yet been reached.
   * @return determines whether the job is waiting to reach its scheduled execution date.
   */
  public boolean isPending() {
    return pending;
  }

  /**
   * Set the pending state of the job.
   * @param pending specifies whether the job is waiting to reach its scheduled execution date.
   * @deprecated the field is initialized in a constructor, this setter is never used in JPPF code and should not be used in client code.
   * @exclude
   */
  @Deprecated
  public void setPending(final boolean pending) {
    this.pending = pending;
  }

  /**
   * Get the unique identifier for the job.
   * @return the uuid as a string.
   * @exclude
   */
  public String getJobUuid() {
    return jobUuid;
  }

  /**
   * Set the unique identifier for the job.
   * @param jobUuid the uuid as a string.
   * @deprecated the field is initialized in a constructor, this setter is never used in JPPF code and should not be used in client code.
   * @exclude
   */
  @Deprecated
  public void setJobUuid(final String jobUuid) {
    this.jobUuid = jobUuid;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("jobUuid=").append(jobUuid);
    sb.append(", jobName=").append(jobName);
    sb.append(", taskCount=").append(taskCount);
    sb.append(", initialTaskCount=").append(initialTaskCount);
    sb.append(", priority=").append(priority);
    sb.append(", suspended=").append(suspended);
    sb.append(", pending=").append(pending);
    sb.append(", maxNodes=").append(maxNodes);
    sb.append(']');
    return sb.toString();
  }
}
