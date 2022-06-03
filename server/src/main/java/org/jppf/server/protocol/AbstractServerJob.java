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
package org.jppf.server.protocol;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Lock;

import org.jppf.execute.ExecutorChannel;
import org.jppf.io.*;
import org.jppf.node.protocol.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * Abstract class that support job state management.
 * @author Martin JANDA
 */
public abstract class AbstractServerJob implements JPPFDistributedJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractServerJob.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
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
  protected AtomicReference<ServerJobStatus> status = new AtomicReference<>(ServerJobStatus.NEW);
  /**
   * List of all runnables called on job completion.
   */
  protected final List<Runnable> onDoneList = new ArrayList<>();
  /**
   * Time at which the job is received on the server side. In milliseconds since January 1, 1970 UTC.
   */
  protected long jobReceivedTime;
  /**
   * The time at which this wrapper was added to the queue.
   */
  protected transient long queueEntryTime;
  /**
   * The underlying task bundle.
   */
  protected final TaskBundle job;
  /**
   * The universal unique id for this job.
   */
  protected String uuid;
  /**
   * The user-defined display name for this job.
   */
  protected String name;
  /**
   * Job expired indicator, determines whether the job is should be cancelled.
   */
  protected boolean jobExpired;
  /**
   * Job pending indicator, determines whether the job is waiting for its scheduled time to start.
   */
  protected boolean pending;
  /**
   * Used for synchronized access to job.
   */
  protected final Lock lock;
  /**
   * Condition signalled when this job is removed from the queue.
   */
  protected final ThreadSynchronization removalCondition = new ThreadSynchronization();
  /**
   * The status of this submission.
   */
  protected final SynchronizedReference<SubmissionStatus> submissionStatus = new SynchronizedReference<>(SubmissionStatus.SUBMITTED);
  /**
   * Handler for job state notifications.
   */
  protected ServerJobChangeListener notificationEmitter;
  /**
   * List of bundles added after submission status set to <code>COMPLETE</code>.
   */
  protected List<ServerTaskBundleClient> completionBundles;
  /**
   * The serialized job header.
   */
  DataLocation jobDataLocation;

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
  }

  /**
   * Get the underlying task bundle.
   * @return a {@code ClientTaskBundle} instance.
   */
  public TaskBundle getJob() {
    return job;
  }

  /**
   * Get the universal unique id for this job.
   * @return the uuid as a string.
   * @exclude
   */
  @Override
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
  @Override
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
  @Override
  public JobSLA getSLA() {
    return job.getSLA();
  }

  /**
   * Get the job metadata.
   * @return an instance of {@link JobMetadata}.
   */
  @Override
  public JobMetadata getMetadata() {
    return job.getMetadata();
  }

  /**
   * Set the job metadata.
   * @param metadata an instance of {@link JobMetadata}.
   */
  public void setMetadata(final JobMetadata metadata) {
    job.setMetadata(metadata);
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param sla an instance of <code>JobSLA</code>.
   */
  public void setSLA(final JobSLA sla) {
    job.setSLA(sla);
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
    this.jobExpired = true;
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
    final boolean oldValue = isPending();
    this.pending = pending;
    final boolean newValue = isPending();
    if (oldValue != newValue) fireJobUpdated(true);
  }

  /**
   * Get the job suspended indicator.
   * @return <code>true</code> if job is suspended, <code>false</code> otherwise.
   */
  public boolean isSuspended() {
    return getSLA().isSuspended();
  }
  
  /**
   * Set the job suspended indicator.
   * @param suspended <code>true</code> to indicate that job is suspended, <code>false</code> otherwise.
   * @param requeue <code>true</code> to indicate that job should be requeued, <code>false</code> otherwise.
   */
  public void setSuspended(final boolean suspended, final boolean requeue) {
    if (getSLA().isSuspended() == suspended) return;
    getSLA().setSuspended(suspended);
    fireJobUpdated(true);
  }

  /**
   * Set the maximum number of nodes this job can run on.
   * @param maxNodes the number of nodes as an int value. A value <= 0 means no limit on the number of nodes.
   */
  public void setMaxNodes(final int maxNodes) {
    if (maxNodes <= 0) return;
    if (getSLA().getMaxNodes() == maxNodes) return;
    getSLA().setMaxNodes(maxNodes);
    fireJobUpdated(true);
  }

  /**
   * Updates status to new value if old value is equal to expect.
   * @param expect the expected value.
   * @param newStatus the new value.
   * @return <code>true</code> if new status was set.
   */
  protected final boolean updateStatus(final ServerJobStatus expect, final ServerJobStatus newStatus) {
    return status.compareAndSet(expect, newStatus);
  }

  /**
   * @return {@code true} when job was cancelled.
   */
  public boolean isCancelled() {
    return status.get() == ServerJobStatus.CANCELLED;
  }

  /**
   * Cancels this job.
   * @param mayInterruptIfRunning true if the thread executing this task should be interrupted.
   * @return whether cancellation was successful.
   */
  public boolean setCancelled(final boolean mayInterruptIfRunning) {
    final ServerJobStatus current = status.get();
    if (!isSuspended() && (current.ordinal() > ServerJobStatus.EXECUTING.ordinal())) return false;
    status.set(ServerJobStatus.CANCELLED);
    return true;
  }

  /**
   * Called when task was cancelled or finished.
   */
  protected void done() {
    final Runnable[] runnables;
    synchronized (onDoneList) {
      runnables = onDoneList.toArray(new Runnable[onDoneList.size()]);
    }
    GlobalExecutor.getGlobalexecutor().execute(() -> { 
      for (final Runnable runnable : runnables) runnable.run();
    });
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

  @Override
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
    return submissionStatus.get();
  }

  /**
   * Set the status of this submission.
   * @param newStatus a {@link SubmissionStatus} enumerated value.
   */
  public void setSubmissionStatus(final SubmissionStatus newStatus) {
    if (!submissionStatus.setIfDifferent(newStatus)) return;
    if (newStatus == SubmissionStatus.ENDED) done();
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
   * @param headerUpdated whether the job header(a {@link org.jppf.node.protocol.TaskBundle TaskBundle} instance) has been updated.
   */
  public void fireJobUpdated(final boolean headerUpdated) {
    if (headerUpdated) updateJobDataLocation();
    if (notificationEmitter != null) notificationEmitter.jobUpdated(this, headerUpdated);
  }

  /**
   * A sub-job was dispatched to a node.
   * @param channel the node to which the job is dispatched.
   * @param bundleNode the bundle for job event.
   */
  protected void fireJobDispatched(final ExecutorChannel<?> channel, final ServerTaskBundleNode bundleNode) {
    if (notificationEmitter != null) notificationEmitter.jobDispatched(this, channel, bundleNode);
  }

  /**
   * A sub-job returned from a node.
   * @param channel the node from which the job is returned.
   * @param bundleNode the bundle for job event.
   */
  protected void fireJobReturned(final ExecutorChannel<?> channel, final ServerTaskBundleNode bundleNode) {
    if (notificationEmitter != null) notificationEmitter.jobReturned(this, channel, bundleNode);
  }

  /**
   * Get the lock used for synchronized access to job.
   * @return a {@link Lock} object.
   */
  public Lock getLock() {
    return lock;
  }

  /**
   * Serialize the updated job header and cache the resulting serialized representation.
   */
  public void updateJobDataLocation() {
    if (!isPersistent()) return;
    DataLocation dl = null;
    try {
      lock.lock();
      final int size = (jobDataLocation == null) ? -1 : jobDataLocation.getSize();
      final ObjectSerializer ser = IOHelper.getDefaultserializer();
      if (size <= 0) dl = IOHelper.serializeData(job, ser);
      else {
        if (IOHelper.fitsInMemory(size)) dl = IOHelper.serializeDataToMemory(job, ser);
        else dl = IOHelper.serializeDataToFile(job, ser);
      }
      jobDataLocation = dl;
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * @return whether this job is persisted in the driver.
   */
  public boolean isPersistent() {
    return getSLA().getPersistenceSpec().isPersistent();
  }

  /**
   * @return a Condition that is signalled when this job is removed from the queue
   */
  public ThreadSynchronization getRemovalCondition() {
    return removalCondition;
  }
}
