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

package org.jppf.client;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.event.JobListener;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.node.protocol.*;
import org.jppf.utils.JPPFUuid;

/**
 * Instances of this class represent a JPPF job and hold all the required elements:
 * tasks, execution policy, task listener, data provider, priority, blocking indicator.<br>
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFJob implements Serializable, JPPFDistributedJob {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The list of tasks to execute.
   */
  final List<Task<?>> tasks = new ArrayList<>();
  /**
   * The container for data shared between tasks.
   * The data provider should be considered read-only, i.e. no modification will be returned back to the client application.
   */
  DataProvider dataProvider = null;
  /**
   * Determines whether the execution of this job is blocking on the client side.
   */
  boolean blocking = true;
  /**
   * The user-defined display name for this job.
   */
  String name = null;
  /**
   * The universal unique id for this job.
   */
  final String uuid;
  /**
   * The service level agreement between the job and the server.
   */
  JobSLA jobSLA = new JPPFJobSLA();
  /**
   * The service level agreement on the client side.
   */
  JobClientSLA jobClientSLA = new JPPFJobClientSLA();
  /**
   * The user-defined metadata associated with this job.
   */
  JobMetadata jobMetadata = new JPPFJobMetadata();
  /**
   * The object that holds the results of executed tasks.
   */
  final JobResults results = new JobResults();
  /**
   * The list of listeners registered with this job.
   */
  transient List<JobListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * The persistence manager that enables saving and restoring the state of this job.
   */
  transient JobPersistence<?> persistenceManager = null;
  /**
   * The client that submitted this job.
   */
  transient JPPFClient client;
  /**
   * Whether this job has been cancelled.
   */
  transient final AtomicBoolean cancelled = new AtomicBoolean(false);

  /**
   * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
   * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
   */
  public AbstractJPPFJob() {
    this(JPPFUuid.normalUUID());
  }

  /**
   * Default constructor, creates a blocking job with no data provider, default SLA values and a priority of 0.
   * This constructor generates a pseudo-random id as a string of 32 hexadecimal characters.
   * @param jobUuid the uuid to assign to this job.
   */
  public AbstractJPPFJob(final String jobUuid) {
    this.uuid = (jobUuid == null) ? JPPFUuid.normalUUID() : jobUuid;
    name = this.uuid;
  }

  @Override
  public int hashCode() {
    return 31 + (uuid == null ? 0 : uuid.hashCode());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AbstractJPPFJob)) return false;
    AbstractJPPFJob other = (AbstractJPPFJob) obj;
    return (uuid == null) ? other.uuid == null : uuid.equals(other.uuid);
  }

  /**
   * Get the object that holds the results of executed tasks.
   * @return a {@link JobResults} instance.
   */
  public JobResults getResults() {
    return results;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobSLA an instance of <code>JobSLA</code>.
   * @exclude
   */
  public void setSLA(final JobSLA jobSLA) {
    this.jobSLA = jobSLA;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param jobClientSLA an instance of <code>JobSLA</code>.
   * @exclude
   */
  public void setClientSLA(final JobClientSLA jobClientSLA) {
    this.jobClientSLA = jobClientSLA;
  }

  /**
   * Set this job's metadata.
   * @param jobMetadata a {@link JobMetadata} instance.
   * @exclude
   */
  public void setMetadata(final JobMetadata jobMetadata) {
    this.jobMetadata = jobMetadata;
  }

  /**
   * Resolve this instance after deserialization.
   * @return an instance of {@link Object}.
   * @exclude
   */
  protected Object readResolve() {
    listeners = new LinkedList<>();
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", uuid=").append(uuid);
    sb.append(", blocking=").append(blocking);
    sb.append(", nbTasks=").append(tasks.size());
    sb.append(", nbResults=").append(results.size());
    sb.append(", jobSLA=").append(jobSLA);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the flag that determines whether this job has been cancelled.
   * @return an {@code AtomicBoolean} instance.
   * @exclude
   */
  public AtomicBoolean getCancelledFlag() {
    return cancelled;
  }
}
