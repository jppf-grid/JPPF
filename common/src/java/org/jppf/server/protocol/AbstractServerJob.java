/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
package org.jppf.server.protocol;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

import org.jppf.execute.ExecutorChannel;
import org.jppf.node.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.slf4j.*;

/**
 * Abstract class that support job state management.
 * @author Martin JANDA
 */
public abstract class AbstractServerJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerJob.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Count of instances of this class.
   */
  private static final AtomicLong INSTANCE_COUNT = new AtomicLong(0L);
  /**
   * A unique id for this client bundle.
   */
  protected final long id = INSTANCE_COUNT.incrementAndGet();
  /**
   * The job status.
   */
  protected volatile ServerJobStatus status = ServerJobStatus.NEW;
  /**
   * List of all runnables called on job completion.
   */
  protected final List<Runnable> onDoneList = new ArrayList<>();
  /**
   * Time at which the job is received on the server side. In milliseconds since January 1, 1970 UTC.
   */
  protected long jobReceivedTime = 0L;
  /**
   * The time at which this wrapper was added to the queue.
   */
  protected transient long queueEntryTime = 0L;
  /**
   * The underlying task bundle.
   */
  protected final TaskBundle job;
  /**
   * The universal unique id for this job.
   */
  protected String uuid = null;
  /**
   * The user-defined display name for this job.
   */
  protected String name = null;
  /**
   * The service level agreement between the job and the server.
   */
  protected JobSLA sla = null;
  /**
   * The job metadata.
   */
  protected JobMetadata metadata = null;
  /**
   * Job expired indicator, determines whether the job is should be cancelled.
   */
  protected boolean jobExpired = false;
  /**
   * Job pending indicator, determines whether the job is waiting for its scheduled time to start.
   */
  protected boolean pending = false;
  /**
   * Used for synchronized access to job.
   */
  protected final Lock lock;
  /**
   * The status of this submission.
   */
  protected SubmissionStatus submissionStatus;
  /**
   * Handler for job state notifications.
   */
  protected ServerJobChangeListener notificationEmitter;
  /**
   * List of bundles added after submission status set to <code>COMPLETE</code>.
   */
  protected List<ServerTaskBundleClient> completionBundles = null;

  /**
   * Initialized abstract client job with task bundle and list of tasks to execute.
   * @param lock used to synchronized access to job.
   * @param job  underlying task bundle.
   */
  protected AbstractServerJob(final Lock lock, final TaskBundle job) {
    if (lock == null) throw new IllegalArgumentException("lock is null");
    if (job == null) throw new IllegalArgumentException("job is null");
    if (debugEnabled) log.debug("creating ClientJob #" + id);
    this.lock = lock;
    this.job = job;
    this.uuid = this.job.getUuid();
    this.name = this.job.getName();
    this.sla = this.job.getSLA();
    this.metadata = this.job.getMetadata();
    this.submissionStatus = SubmissionStatus.SUBMITTED;
  }

  /**
   * Get the underlying task bundle.
   * @return a <code>ClientTaskBundle</code> instance.
   */
  public TaskBundle getJob() {
    return job;
  }

  /**
   * Get the universal unique id for this job.
   * @return the uuid as a string.
   * @exclude
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Set the universal unique id for this job.
   * @param uuid the universal unique id.
   */
  public void setUuid(final String uuid) {
    this.uuid = uuid;
  }

  /**
   * Get the user-defined display name for this job. This is the name displayed in the administration console.
   * @return the name as a string.
   */
  public String getName() {
    return name;
  }

  /**
   * Set the user-defined display name for this job.
   * @param name the display name as a string.
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @return an instance of {@link org.jppf.node.protocol.JobSLA}.
   */
  public JobSLA getSLA() {
    return sla;
  }

  /**
   * Get the job metadata.
   * @return an instance of {@link JobMetadata}.
   */
  public JobMetadata getMetadata() {
    return metadata;
  }

  /**
   * Set the job metadata.
   * @param metadata an instance of {@link JobMetadata}.
   */
  public void setMetadata(final JobMetadata metadata) {
    this.metadata = metadata;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param sla an instance of <code>JobSLA</code>.
   */
  public void setSLA(final JobSLA sla) {
    this.sla = sla;
  }

  /**
   * Get the job expired indicator.
   * @return <code>true</code> if job has expired, <code>false</code> otherwise.
   */
  public boolean isJobExpired() {
    return jobExpired;
  }

  /**
   * Sets and notifies that job has expired.
   */
  public void jobExpired() {
    setJobExpired(true);
  }

  /**
   * Set the job expired indicator.
   * @param jobExpired <code>true</code> to indicate that job has expired. <code>false</code> otherwise.
   */
  public void setJobExpired(final boolean jobExpired) {
    this.jobExpired = jobExpired;
    if (this.jobExpired && !isDone()) setCancelled(true);
  }

  /**
   * Get the job pending indicator.
   * @return <code>true</code> if job is pending, <code>false</code> otherwise.
   */
  public boolean isPending() {
    return pending;
  }

  /**
   * Set the job pending indicator.
   * @param pending <code>true</code> to indicate that job is pending, <code>false</code> otherwise
   */
  public void setPending(final boolean pending) {
    boolean oldValue = isPending();
    this.pending = pending;
    boolean newValue = isPending();
    if (oldValue != newValue) fireJobUpdated();
  }

  /**
   * Get the job suspended indicator.
   * @return <code>true</code> if job is suspended, <code>false</code> otherwise.
   */
  public boolean isSuspended() {
    return getJob().getSLA().isSuspended();
  }
  
  /**
   * Set the job suspended indicator.
   * @param suspended <code>true</code> to indicate that job is suspended, <code>false</code> otherwise.
   * @param requeue <code>true</code> to indicate that job should be requeued, <code>false</code> otherwise.
   */
  public void setSuspended(final boolean suspended, final boolean requeue) {
    JobSLA sla = getJob().getSLA();
    if (sla.isSuspended() == suspended) return;
    sla.setSuspended(suspended);
    fireJobUpdated();
  }

  /**
   * Set the maximum number of nodes this job can run on.
   * @param maxNodes the number of nodes as an int value. A value <= 0 means no limit on the number of nodes.
   */
  public void setMaxNodes(final int maxNodes) {
    if (maxNodes <= 0) return;
    getJob().getSLA().setMaxNodes(maxNodes);
    fireJobUpdated();
  }

  /**
   * Updates status to new value if old value is equal to expect.
   * @param expect the expected value.
   * @param newStatus the new value.
   * @return <code>true</code> if new status was set.
   */
  protected final boolean updateStatus(final ServerJobStatus expect, final ServerJobStatus newStatus) {
    if (status == expect) {
      status = newStatus;
      return true;
    }
    else return false;
  }

  /**
   * @return <code>true</code> when job is cancelled or finished normally.
   */
  public boolean isDone() {
    return status.compareTo(ServerJobStatus.EXECUTING) >= 0;
  }

  /**
   * @return <code>true</code> when job was cancelled.
   */
  public boolean isCancelled() {
    return status.compareTo(ServerJobStatus.CANCELLED) >= 0;
  }

  /**
   * Cancels this job.
   * @param mayInterruptIfRunning true if the thread executing this task should be interrupted.
   * @return whether cancellation was successful.
   */
  public boolean setCancelled(final boolean mayInterruptIfRunning) {
    if (!isSuspended() && (status.ordinal() > ServerJobStatus.EXECUTING.ordinal())) return false;
    status = ServerJobStatus.CANCELLED;
    return true;
  }

  /**
   * Called when task was cancelled or finished.
   */
  protected void done() {
    Runnable[] runnables;
    synchronized (onDoneList) {
      runnables = onDoneList.toArray(new Runnable[onDoneList.size()]);
    }
    for (Runnable runnable : runnables) runnable.run();
  }

  /**
   * Registers instance to be called on job finish.
   * @param runnable {@link Runnable} to be called on job finish.
   */
  public void addOnDone(final Runnable runnable) {
    if (runnable == null) throw new IllegalArgumentException("runnable is null");
    synchronized (onDoneList) {
      onDoneList.add(runnable);
    }
  }

  /**
   * Deregisters instance to be called on job finish.
   * @param runnable {@link Runnable} to be called on job finish.
   */
  public void removeOnDone(final Runnable runnable) {
    if (runnable == null) throw new IllegalArgumentException("runnable is null");
    synchronized (onDoneList) {
      onDoneList.remove(runnable);
    }
  }

  /**
   * Get the job received time.
   * @return the time in milliseconds as a long value.
   */
  public long getJobReceivedTime() {
    return jobReceivedTime;
  }

  /**
   * Set the job received time.
   * @param jobReceivedTime the time in milliseconds as a long value.
   */
  public void setJobReceivedTime(final long jobReceivedTime) {
    this.jobReceivedTime = jobReceivedTime;
  }

  /**
   * Get the time at which this wrapper was added to the queue.
   * @return the time in milliseconds as a long value.
   */
  public long getQueueEntryTime() {
    return queueEntryTime;
  }

  /**
   * Set the time at which this wrapper was added to the queue.
   * @param queueEntryTime the time in milliseconds as a long value.
   */
  public void setQueueEntryTime(final long queueEntryTime) {
    this.queueEntryTime = queueEntryTime;
  }

    /**
    * Get the current number of tasks in the job.
    * @return the number of tasks as an int.
    */
  public abstract int getTaskCount();

  /**
   * Get the initial task count.
   * @return the count as an int.
   */
  public int getInitialTaskCount() {
    return job.getInitialTaskCount();
  }

  /**
   * Get the status of this submission.
   * @return a {@link SubmissionStatus} enumerated value.
   */
  public SubmissionStatus getSubmissionStatus() {
    lock.lock();
    try {
      return submissionStatus;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Set the status of this submission.
   * @param submissionStatus a {@link SubmissionStatus} enumerated value.
   */
  public void setSubmissionStatus(final SubmissionStatus submissionStatus) {
    lock.lock();
    try {
      if (this.submissionStatus == submissionStatus) return;
      SubmissionStatus oldValue = this.submissionStatus;
      this.submissionStatus = submissionStatus;
      fireStatusChanged(oldValue, this.submissionStatus);
      if (submissionStatus == SubmissionStatus.ENDED) done();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Notify that job submission status has changed.
   * @param oldValue value before change.
   * @param newValue value after change.
   */
  protected void fireStatusChanged(final SubmissionStatus oldValue, final SubmissionStatus newValue) {
    if (notificationEmitter != null) notificationEmitter.jobStatusChanged(this, oldValue, newValue);
  }

  /**
   * Get the broadcast UUID.
   * @return an <code>String</code> instance.
   */
  public String getBroadcastUUID() {
    return null;
  }

  /**
   * Get list of bundles added after job completion.
   * @return list of bundles added after job completion.
   */
  public List<ServerTaskBundleClient> getCompletionBundles() {
    lock.lock();
    try {
      if(completionBundles == null) return Collections.emptyList();
      else return new ArrayList<>(completionBundles);
    } finally {
      lock.unlock();
    }
  }

  /**
   * The current number of tasks in a job was updated.
   */
  public void fireJobUpdated() {
    if (notificationEmitter != null) notificationEmitter.jobUpdated(this);
  }

  /**
   * A sub-job was dispatched to a node.
   * @param channel the node to which the job is dispatched.
   * @param bundleNode the bundle for job event.
   */
  protected void fireJobDispatched(final ExecutorChannel channel, final ServerTaskBundleNode bundleNode) {
    if (notificationEmitter != null) notificationEmitter.jobDispatched(this, channel, bundleNode);
  }

  /**
   * A sub-job returned from a node.
   * @param channel the node from which the job is returned.
   * @param bundleNode the bundle for job event.
   */
  protected void fireJobReturned(final ExecutorChannel channel, final ServerTaskBundleNode bundleNode) {
    if (notificationEmitter != null) notificationEmitter.jobReturned(this, channel, bundleNode);
  }
}
