/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package org.jppf.server.protocol.utils;

import org.jppf.execute.ExecutorChannel;
import org.jppf.job.JobEventType;
import org.jppf.job.JobInformation;
import org.jppf.job.JobNotification;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.JobMetadata;
import org.jppf.node.protocol.JobSLA;
import org.jppf.server.protocol.BundleParameter;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.server.protocol.ServerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract class that support job state management.
 * @author Martin JANDA
 */
public abstract class AbstractServerJob {
  /**
   * State for task indicating whether result or exception was received.
   */
  protected static enum TaskState
  {
    /**
     * Result was received for task.
     */
    RESULT,
    /**
     * Exception was received for task.
     */
    EXCEPTION
  }

  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerJob.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Instance count.
   */
  private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);
  /**
   * Job status is new (just submitted).
   */
  protected static final int NEW = 0;
  /**
   * Job status is executing.
   */
  protected static final int EXECUTING = 1;
  /**
   * Job status is done/complete.
   */
  protected static final int DONE = 2;
  /**
   * Job status is cancelled.
   */
  protected static final int CANCELLED = 3;
  /**
   * The job status.
   */
  private volatile int status = NEW;
  /**
   * List of all runnables called on job completion.
   */
  private final List<Runnable> onDoneList = new ArrayList<Runnable>();
  /**
   * Time at which the job is received on the server side. In milliseconds since January 1, 1970 UTC.
   */
  private long jobReceivedTime = 0L;
  /**
   * The time at which this wrapper was added to the queue.
   */
  private transient long queueEntryTime = 0L;
  /**
   * The underlying task bundle.
   */
  protected final JPPFTaskBundle job;
  /**
   * The universal unique id for this job.
   */
  private String uuid = null;
  /**
   * The user-defined display name for this job.
   */
  private String name = null;
  /**
   * The service level agreement between the job and the server.
   */
  private JobSLA sla = null;
  /**
   * The job metadata.
   */
  private JobMetadata metadata = null;
  /**
   * Job expired indicator, determines whether the job is should be cancelled.
   */
  private boolean jobExpired = false;
  /**
   * Job pending indicator, determines whether the job is waiting for its scheduled time to start.
   */
  private boolean pending = false;

  /**
   * Initialized abstract client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   */
  protected AbstractServerJob(final JPPFTaskBundle job)
  {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (debugEnabled) log.debug("creating ClientJob #" + INSTANCE_COUNT.incrementAndGet());
    this.job = job;
    this.uuid = this.job.getUuid();
    this.name = this.job.getName();
    this.sla = this.job.getSLA();
    this.metadata = this.job.getMetadata();
  }

  /**
   * Get the underlying task bundle.
   * @return a <code>ClientTaskBundle</code> instance.
   */
  public JPPFTaskBundle getJob()
  {
    return job;
  }

  /**
   * Get the universal unique id for this job.
   * @return the uuid as a string.
   * @exclude
   */
  public String getUuid()
  {
    return uuid;
  }

  /**
   * Set the universal unique id for this job.
   * @param uuid the universal unique id.
   */
  public void setUuid(final String uuid)
  {
    this.uuid = uuid;
  }

  /**
   * Get the user-defined display name for this job. This is the name displayed in the administration console.
   * @return the name as a string.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Set the user-defined display name for this job.
   * @param name the display name as a string.
   */
  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @return an instance of {@link org.jppf.node.protocol.JobSLA}.
   */
  public JobSLA getSLA()
  {
    return sla;
  }

  /**
   * Get the job metadata.
   * @return an instance of {@link JobMetadata}.
   */
  public JobMetadata getMetadata()
  {
    return metadata;
  }

  /**
   * Set the job metadata.
   * @param metadata an instance of {@link JobMetadata}.
   */
  public void setMetadata(final JobMetadata metadata)
  {
    this.metadata = metadata;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param sla an instance of <code>JobSLA</code>.
   */
  public void setSLA(final JobSLA sla)
  {
    this.sla = sla;
  }

  /**
   * Get the job expired indicator.
   * @return <code>true</code> if job has expired, <code>false</code> otherwise.
   */
  public boolean isJobExpired()
  {
    return jobExpired;
  }

  /**
   * Sets and notifies that job has expired.
   */
  public void jobExpired()
  {
    setJobExpired(true);
  }

  /**
   * Set the job expired indicator.
   * @param jobExpired <code>true</code> to indicate that job has expired. <code>false</code> otherwise.
   */
  public void setJobExpired(final boolean jobExpired)
  {
    this.jobExpired = jobExpired;
    if (this.jobExpired && !isDone()) cancel(true);
  }

  /**
   * Get the job pending indicator.
   * @return <code>true</code> if job is pending, <code>false</code> otherwise.
   */
  public boolean isPending()
  {
    return pending;
  }

  /**
   * Set the job pending indicator.
   * @param pending <code>true</code> to indicate that job is pending, <code>false</code> otherwise
   */
  public void setPending(final boolean pending)
  {
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
  public void setSuspended(final boolean suspended, final boolean requeue)
  {
    JobSLA sla = getJob().getSLA();
    if (sla.isSuspended() == suspended) return;
    sla.setSuspended(suspended);
    fireJobUpdated();
  }

  /**
   * Set the maximum number of nodes this job can run on.
   * @param maxNodes the number of nodes as an int value. A value <= 0 means no limit on the number of nodes.
   */
  public void setMaxNodes(final int maxNodes)
  {
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
  protected final boolean updateStatus(final int expect, final int newStatus)
  {
    if(status == expect)
    {
      status = newStatus;
      return true;
    }
    else return false;
  }

  /**
   * @return <code>true</code> when job is cancelled or finished normally.
   */
  public boolean isDone()
  {
    return status >= EXECUTING;
  }

  /**
   * @return <code>true</code> when job was cancelled.
   */
  public boolean isCancelled()
  {
    return status >= CANCELLED;
  }

  /**
   * Cancels this job.
   * @param mayInterruptIfRunning true if the thread executing this task should be interrupted.
   * @return whether cancellation was successful.
   */
  public boolean cancel(final boolean mayInterruptIfRunning)
  {
    if (status > EXECUTING) return false;
    status = CANCELLED;
    return true;
  }

  /**
   * Called when task was cancelled or finished.
   */
  protected void done()
  {
    Runnable[] runnables;
    synchronized (onDoneList)
    {
      runnables = onDoneList.toArray(new Runnable[onDoneList.size()]);
    }
    for (Runnable runnable : runnables) runnable.run();
  }

  /**
   * Registers instance to be called on job finish.
   * @param runnable {@link Runnable} to be called on job finish.
   */
  public void addOnDone(final Runnable runnable)
  {
    if(runnable == null) throw new IllegalArgumentException("runnable is null");
    synchronized (onDoneList)
    {
      onDoneList.add(runnable);
    }
  }

  /**
   * Deregisters instance to be called on job finish.
   * @param runnable {@link Runnable} to be called on job finish.
   */
  public void removeOnDone(final Runnable runnable)
  {
    if(runnable == null) throw new IllegalArgumentException("runnable is null");
    synchronized (onDoneList)
    {
      onDoneList.remove(runnable);
    }
  }

  /**
   * Get the job received time.
   * @return the time in milliseconds as a long value.
   */
  public long getJobReceivedTime()
  {
    return jobReceivedTime;
  }

  /**
   * Set the job received time.
   * @param jobReceivedTime the time in milliseconds as a long value.
   */
  public void setJobReceivedTime(final long jobReceivedTime)
  {
    this.jobReceivedTime = jobReceivedTime;
  }

  /**
   * Get the time at which this wrapper was added to the queue.
   * @return the time in milliseconds as a long value.
   */
  public long getQueueEntryTime()
  {
    return queueEntryTime;
  }

  /**
   * Set the time at which this wrapper was added to the queue.
   * @param queueEntryTime the time in milliseconds as a long value.
   */
  public void setQueueEntryTime(final long queueEntryTime)
  {
    this.queueEntryTime = queueEntryTime;
  }

  /**
   * The current number of tasks in a job was updated.
   */
  public void fireJobUpdated() {
    fireJobNotification(createJobNotification(JobEventType.JOB_UPDATED, null));
  }

  /**
   * A new job was submitted to the JPPF driver queue.
   */
  public void fireJobQueued() {
    fireJobNotification(createJobNotification(JobEventType.JOB_QUEUED, null));
  }

  /**
   * A job was completed and sent back to the client.
   */
  public void fireJobEnded() {
    fireJobNotification(createJobNotification(JobEventType.JOB_ENDED, null));
  }

  /**
   * A sub-job was dispatched to a node.
   * @param channel the node to which the job is dispatched.
   * @param bundle the bundle for job event.
   */
  public void fireJobDispatched(final ExecutorChannel channel, final JPPFTaskBundle bundle) {
    fireJobNotification(createJobNotification(JobEventType.JOB_DISPATCHED, channel, bundle));
  }

  /**
   * A sub-job returned from a node.
   * @param channel the node from which the job is returned.
   * @param bundle the bundle for job event.
   */
  public void fireJobReturned(final ExecutorChannel channel, final JPPFTaskBundle bundle) {
    if(bundle != null) fireJobNotification(createJobNotification(JobEventType.JOB_RETURNED, channel, bundle));
  }

  /**
   * Fire job listener event.
   * @param event the event to be fired.
   */
  protected abstract void fireJobNotification(final JobNotification event);

    /**
    * Get the current number of tasks in the job.
    * @return the number of tasks as an int.
    */
  public abstract int getTaskCount();

  /**
   * Create instance of job notification.
   * @param eventType the type of this job event.
   * @param channel the node to which the job event is created.
   * @param bundle the bundle for created job event.
   * @return {@link JobNotification} instance.
   */
  protected static JobNotification createJobNotification(final JobEventType eventType, final ExecutorChannel channel, final JPPFTaskBundle bundle) {
    JobSLA sla = bundle.getSLA();
    Boolean pending = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING);
    JobInformation jobInfo = new JobInformation(bundle.getUuid(), bundle.getName(), bundle.getTaskCount(),
            bundle.getInitialTaskCount(), sla.getPriority(), sla.isSuspended(), (pending != null) && pending);
    jobInfo.setMaxNodes(sla.getMaxNodes());
    JPPFManagementInfo nodeInfo = (channel == null) ? null : channel.getManagementInfo();
    JobNotification event = new JobNotification(eventType, jobInfo, nodeInfo, System.currentTimeMillis());
    if (eventType == JobEventType.JOB_UPDATED)
    {
      Integer n = (Integer) bundle.getParameter(BundleParameter.REAL_TASK_COUNT);
      if (n != null) jobInfo.setTaskCount(n);
    }
    return event;
  }

  /**
   * Create instance of job notification.
   * @param eventType the type of this job event.
   * @param channel the node to which the job event is created.
   * @return {@link org.jppf.job.JobNotification} instance.
   */
  protected JobNotification createJobNotification(final JobEventType eventType, final ExecutorChannel channel)
  {
    JobSLA sla = getSLA();
    boolean pending = isPending();
    JobInformation jobInfo = new JobInformation(getUuid(), getName(), getTaskCount(),
            getTaskCount(), sla.getPriority(), sla.isSuspended(), pending);
    jobInfo.setMaxNodes(sla.getMaxNodes());
    JPPFManagementInfo nodeInfo = (channel == null) ? null : channel.getManagementInfo();
    JobNotification event = new JobNotification(eventType, jobInfo, nodeInfo, System.currentTimeMillis());
    if (eventType == JobEventType.JOB_UPDATED)
    {
      Integer n = (Integer) getJob().getParameter(BundleParameter.REAL_TASK_COUNT);
      if (n != null) jobInfo.setTaskCount(n);
    }
    return event;
  }
}
