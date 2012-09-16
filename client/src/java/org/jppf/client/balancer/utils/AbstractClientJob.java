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

package org.jppf.client.balancer.utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.client.balancer.*;
import org.jppf.node.protocol.*;
import org.slf4j.*;

/**
 * Abstract class that support job state management.
 * @author Martin JANDA
 */
public abstract class AbstractClientJob
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientJob.class);
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
  protected final JPPFJob job;
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
   * The local channel eventually used for this job.
   */
  private ChannelWrapper localChannel = null;
  /**
   * The remote channels eventually used for this job.
   * In the next version, this should be declared as a set of channels,
   * so job dispatching through multiple remote channels can be enabled.
   */
  private ChannelWrapper remoteChannel = null;

  /**
   * Initialized abstract client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   */
  protected AbstractClientJob(final JPPFJob job)
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
  public JPPFJob getJob()
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
   * Notifies that job has expired.
   */
  public void jobExpired()
  {
    this.jobExpired = true;
    cancel(true);
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
    this.pending = pending;
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
    for (Runnable runnable : runnables)
    {
      runnable.run();
    }
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
   * Add a channel to this job.
   * See {@link #remoteChannel}.
   * @param channel the channel to add.
   */
  public void addChannel(final ChannelWrapper channel)
  {
    if (channel.isLocal()) localChannel = channel;
    else remoteChannel = channel;
  }

  /**
   * Add a channel to this job.
   * See {@link #remoteChannel}.
   * @param channel the channel to add.
   */
  public void removeChannel(final ChannelWrapper channel)
  {
    if (channel.isLocal()) localChannel = null;
    else remoteChannel = null;
  }

  /**
   * Determine whether this job can be sent to the specified channel.
   * Currently this method only accepts a single remote channel, and it has to always be the same for the same job.
   * See {@link #remoteChannel}.
   * @param channel the channel to check for acceptance.
   * @return <code>true</code> if the channel is accepted, <code>false</code> otherwise.
   */
  public boolean acceptsChannel(final ChannelWrapper channel)
  {
    if (channel.isLocal()) return true;
    checkRemoteChannel();
    // we accept a single channel, always the same
    if ((remoteChannel == null) || (remoteChannel == channel)) return true;
    return false;
  }

  /**
   * Clear the channels used to duispatch this job.
   * See {@link #remoteChannel}.
   */
  public void clearChannels()
  {
    localChannel = remoteChannel = null;
  }

  /**
   * Check if the remote channel is available.
   * @return <code>true</code> if the remote channel is available, <code>false</code> otherwise.
   */
  private boolean checkRemoteChannel()
  {
    boolean b = checkChannel(remoteChannel);
    if (!b) remoteChannel = null;
    return b;
  }

  /**
   * Check if the specified channel is available.
   * @param channel the channel to check.
   * @return <code>true</code> if the channel is available, <code>false</code> otherwise.
   */
  private boolean checkChannel(final ChannelWrapper channel)
  {
    if (channel == null) return true;
    JPPFClientConnectionStatus status = channel.getStatus();
    return (status == JPPFClientConnectionStatus.ACTIVE) || (status == JPPFClientConnectionStatus.EXECUTING);
  }
}
