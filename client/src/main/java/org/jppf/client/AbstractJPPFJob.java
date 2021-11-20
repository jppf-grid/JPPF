/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.client.event.*;
import org.jppf.client.persistence.JobPersistence;
import org.jppf.node.protocol.*;
import org.jppf.utils.JPPFUuid;
import org.slf4j.*;

/**
 * Instances of this class represent a JPPF job and hold all the required elements:
 * tasks, execution policy, task listener, data provider, priority, blocking indicator.<br>
 * @param <J> the type of job.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFJob<J extends AbstractJPPFJob<J>> implements Serializable, JPPFDistributedJob, JobStatusHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFJob.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
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
  DataProvider dataProvider;
  /**
   * Determines whether the execution of this job is blocking on the client side.
   * @deprecated a job should be submissible either synchronously or asynchronously, regardless of its state.
   * The way it is submitted is the user's choice at the time of submission, using one of {@link JPPFClient#submit(JPPFJob)} or {@link JPPFClient#submitAsync(JPPFJob)}. 
   */
  boolean blocking = true;
  /**
   * The user-defined display name for this job.
   */
  String name;
  /**
   * The universal unique id for this job.
   */
  final String uuid;
  /**
   * The service level agreement between the job and the server.
   */
  JobSLA jobSLA = new JobSLA();
  /**
   * The service level agreement on the client side.
   */
  JobClientSLA jobClientSLA = new JobClientSLA();
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
  transient JobPersistence<?> persistenceManager;
  /**
   * The client that submitted this job.
   */
  transient JPPFClient client;
  /**
   * Whether this job has been cancelled.
   */
  transient AtomicBoolean cancelled = new AtomicBoolean(false);
  /**
   * Whether this job is being cancelled.
   */
  transient AtomicBoolean cancelling = new AtomicBoolean(false);
  /**
   * The status of this job.
   */
  private AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.SUBMITTED);
  /**
   * List of listeners registered to receive this job's status change notifications.
   */
  private transient List<JobStatusListener> statusListeners = new ArrayList<>();
  /**
   * Whether the tasks form a graph.
   */
  boolean taskGraph;

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
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Set the user-defined display name for this job.
   * @param name the display name as a string.
   * @return this job, for method chaining.
   */
  @SuppressWarnings("unchecked")
  public J setName(final String name) {
    this.name = name;
    results.setJobName(name);
    return (J) this;
  }

  @Override
  public int getTaskCount() {
    return tasks.size();
  }

  @Override
  public int hashCode() {
    return 31 + (uuid == null ? 0 : uuid.hashCode());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AbstractJPPFJob)) return false;
    @SuppressWarnings("unchecked")
    final AbstractJPPFJob<J> other = (AbstractJPPFJob<J>) obj;
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
   * Get the count of the tasks in this job that haven completed.
   * @return the number of executed tasks in this job.
   * @since 4.2
   */
  public int executedTaskCount() {
    return results.size();
  }

  /**
   * Get the count of the tasks in this job that haven't yet been executed.
   * @return the number of unexecuted tasks in this job.
   * @since 4.2
   */
  public int unexecutedTaskCount() {
    return tasks.size() - results.size();
  }

  /**
   * Get the container for data shared between tasks.
   * @return a <code>DataProvider</code> instance.
   */
  public DataProvider getDataProvider() {
    return dataProvider;
  }

  /**
   * Set the container for data shared between tasks.
   * @param dataProvider a <code>DataProvider</code> instance.
   * @return this job, for method chaining.
   */
  @SuppressWarnings("unchecked")
  public J setDataProvider(final DataProvider dataProvider) {
    this.dataProvider = dataProvider;
    return (J) this;
  }

  /**
   * Determine whether the execution of this job is blocking on the client side.
   * @return true if the execution is blocking, false otherwise.
   * @deprecated a job should be submittable either synchronously or asynchronously, regardless of its state.
   * The submission mode is the user's choice at the time of submission, using one of {@link JPPFClient#submit(JPPFJob) JPPFClient.submit()} or {@link JPPFClient#submitAsync(JPPFJob) JPPFClient.submitAsync()}. 
   */
  public boolean isBlocking() {
    return blocking;
  }

  /**
   * Specify whether the execution of this job is blocking on the client side.
   * @param blocking true if the execution is blocking, false otherwise.
   * @return this job, for method chaining.
   * @deprecated a job should be submittable either synchronously or asynchronously, regardless of its state.
   * The submission mode is the user's choice at the time of submission, using one of {@link JPPFClient#submit(JPPFJob) JPPFClient.submit()} or {@link JPPFClient#submitAsync(JPPFJob) JPPFClient.submitAsync()}. 
   */
  @SuppressWarnings("unchecked")
  public J setBlocking(final boolean blocking) {
    this.blocking = blocking;
    return (J) this;
  }

  @Override
  public JobSLA getSLA() {
    return jobSLA;
  }

  /**
   * Get the job SLA for the client side.
   * @return an instance of <code>JobSLA</code>.
   */
  public JobClientSLA getClientSLA() {
    return jobClientSLA;
  }

  @Override
  public JobMetadata getMetadata() {
    return jobMetadata;
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
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", uuid=").append(uuid);
    sb.append(", blocking=").append(blocking);
    sb.append(", nbTasks=").append(tasks.size());
    sb.append(", nbResults=").append(results.size());
    sb.append(", hasGraph=").append(taskGraph);
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


  @Override
  public JobStatus getStatus() {
    return status.get();
  }

  @Override
  public void setStatus(final JobStatus newStatus) {
    if (status.get() != newStatus) {
      if (debugEnabled) log.debug("job [" + uuid + "] status changing from '" + this.status + "' to '" + newStatus + "'");
      this.status.set(newStatus);
      fireStatusChangeEvent(newStatus);
    }
  }

  /**
   * Add a listener to the list of status listeners.
   * @param listener the listener to add.
   * @exclude
   */
  public void addJobStatusListener(final JobStatusListener listener) {
    synchronized(statusListeners) {
      if (debugEnabled) log.debug("job [" + uuid + "] adding status listener " + listener);
      if (listener != null) statusListeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of status listeners.
   * @param listener the listener to remove.
   * @exclude
   */
  public void removeJobStatusListener(final JobStatusListener listener) {
    synchronized(statusListeners) {
      if (debugEnabled) log.debug("job [" + uuid + "] removing status listener " + listener);
      if (listener != null) statusListeners.remove(listener);
    }
  }

  /**
   * Notify all listeners of a change of status for this job.
   * @param newStatus the status for job event.
   * @exclude
   */
  protected void fireStatusChangeEvent(final JobStatus newStatus) {
    synchronized(statusListeners) {
      if (debugEnabled) log.debug("job [" + uuid + "] fire status changed event for '" + newStatus + "'");
      if (!statusListeners.isEmpty()) {
        final JobStatusEvent event = new JobStatusEvent(uuid, newStatus);
        for (final JobStatusListener listener: statusListeners) listener.jobStatusChanged(event);
      }
    }
    results.wakeUp();
  }

  /**
   * Get the flag that determines whether this job is being cancelled.
   * @return an {@code AtomicBoolean} instance.
   * @exclude
   */
  public AtomicBoolean getCancellingFlag() {
    return cancelling;
  }

  /**
   * Wait until the job is complete or the timeout expires, whichever happens first.
   * @param timeout the maximum time to wait for the job completion.
   * @param raiseTimeoutException whether to raise a {@link TimeoutException} when the timeout expires.
   * @throws TimeoutException if the timeout expired and {@code raiseTimeoutException == true}.
   * @throws InterruptedException if the current thread is interrupted while wating for the results.
   */
  void await(final long timeout, final boolean raiseTimeoutException) throws TimeoutException, InterruptedException {
    final long start = System.nanoTime();
    long elapsed;
    final int nbTasks = tasks.size();
    synchronized(results) {
      while (((elapsed = (System.nanoTime() - start) / 1_000_000L) < timeout) && ((results.size() < nbTasks) || !getStatus().isDone())) {
        results.wait(timeout - elapsed);
      }
      if (!getStatus().isDone() && raiseTimeoutException) throw new TimeoutException("timeout expired");
    }
  }

  /**
   * Save the state of the {@code AbstractJPPFJob} instance to a stream (i.e.,serialize it).
   * @param out the output stream to which to write the job. 
   * @throws IOException if any I/O error occurs.
   */
  @SuppressWarnings("static-method")
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  /**
   * Reconstitute the {@code AbstractJPPFJob} instance from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the job. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph can not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    statusListeners = new ArrayList<>();
    listeners = new CopyOnWriteArrayList<>();
    cancelled = new AtomicBoolean(false);
    cancelling = new AtomicBoolean(false);
  }
}
