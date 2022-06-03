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

package org.jppf.client.balancer;

import static org.jppf.client.balancer.ClientJobStatus.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.execute.ExecutorChannel;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.*;
import org.slf4j.*;

/**
 * Abstract class that support job state management.
 * @author Martin JANDA
 */
public abstract class AbstractClientJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractClientJob.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Instance count.
   */
  private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);
  /**
   * The job status.
   */
  private volatile ClientJobStatus status = NEW;
  /**
   * List of all runnables called on job completion.
   */
  private final List<Runnable> onDoneList = new ArrayList<>();
  /**
   * Time at which the job is received on the server side. In milliseconds since January 1, 1970 UTC.
   */
  private long jobReceivedTime;
  /**
   * The time at which this wrapper was added to the queue.
   */
  private transient long queueEntryTime;
  /**
   * Instance of parent broadcast job.
   */
  protected ClientJob parentJob;
  /**
   * The underlying task bundle.
   */
  protected final JPPFJob job;
  /**
   * The universal unique id for this job.
   */
  private String uuid;
  /**
   * The user-defined display name for this job.
   */
  private String name;
  /**
   * The service level agreement between the job and the server.
   */
  JobSLA sla;
  /**
   * The service level agreement on the client side.
   */
  private JobClientSLA clientSla;
  /**
   * The job metadata.
   */
  private JobMetadata metadata;
  /**
   * Job expired indicator, determines whether the job is should be cancelled.
   */
  private boolean jobExpired;
  /**
   * Job pending indicator, determines whether the job is waiting for its scheduled time to start.
   */
  private boolean pending;
  /**
   * Count of channels used by this job.
   */
  private final AtomicInteger dispatchCount = new AtomicInteger(0);
  /**
   * Counts the number of times this job is dispatched to each channel. 
   */
  private final Map<String, AtomicInteger> channelCounts = new HashMap<>();

  /**
   * Initialized abstract client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   */
  protected AbstractClientJob(final JPPFJob job) {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (debugEnabled) log.debug("creating ClientJob #" + INSTANCE_COUNT.incrementAndGet());
    this.job = job;
    this.uuid = this.job.getUuid();
    this.name = this.job.getName();
    this.sla = this.job.getSLA();
    this.clientSla = this.job.getClientSLA();
    this.metadata = this.job.getMetadata();
  }

  /**
   * Get the underlying task bundle.
   * @return a <code>ClientTaskBundle</code> instance.
   */
  public JPPFJob getJob() {
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
   * Get the service level agreement between the job and the client.
   * @return an instance of {@link org.jppf.node.protocol.JobSLA}.
   */
  public JobClientSLA getClientSLA() {
    return clientSla;
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
   * Get the service level agreement between the job and the client.
   * @param clientSla an instance of <code>JobClientSLA</code>.
   */
  public void setClientSLA(final JobClientSLA clientSla) {
    this.clientSla = clientSla;
  }

  /**
   * Get the job expired indicator.
   * @return <code>true</code> if job has expired, <code>false</code> otherwise.
   */
  public boolean isJobExpired() {
    return jobExpired;
  }

  /**
   * Notifies that job has expired.
   */
  public void jobExpired() {
    this.jobExpired = true;
    cancel(true);
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
    this.pending = pending;
  }

  /**
   * Updates status to new value if old value is equal to expect.
   * @param expect the expected value.
   * @param newStatus the new value.
   * @return <code>true</code> if new status was set.
   */
  protected final boolean updateStatus(final ClientJobStatus expect, final ClientJobStatus newStatus) {
    if (status == expect) {
      if ((newStatus == EXECUTING) && (status != newStatus) && !isParentBroadcastJob()) job.fireJobEvent(JobEvent.Type.JOB_START, null, null);
      status = newStatus;
      return true;
    }
    else return false;
  }

  /**
   * @return <code>true</code> when job is cancelled or finished normally.
   */
  public boolean isDone() {
    return status.compareTo(EXECUTING) >= 0;
  }

  /**
   * @return <code>true</code> when job was cancelled.
   */
  public boolean isCancelled() {
    return status.compareTo(CANCELLED) >= 0;
  }

  /**
   * Cancels this job.
   * @param mayInterruptIfRunning true if the thread executing this task should be interrupted.
   * @return whether cancellation was successful.
   */
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (status.compareTo(EXECUTING) > 0) return false;
    status = CANCELLED;
    return true;
  }

  /**
   * Called when task was cancelled or finished.
   */
  protected void done() {
    if (debugEnabled) log.debug("job done: {}", this);
    final Runnable[] runnables;
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
    if(runnable == null) throw new IllegalArgumentException("runnable is null");
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
   * Add a channel to this job.
   * @param channel the channel to add.
   */
  public void addChannel(final ExecutorChannel<?> channel) {
    dispatchCount.incrementAndGet();
    final String uuid = channel.getUuid();
    synchronized(channelCounts) {
      final AtomicInteger n = channelCounts.get(uuid);
      if (n == null) channelCounts.put(uuid, new AtomicInteger(1));
      else n.incrementAndGet();
    }
  }

  /**
   * Add a channel to this job.
   * @param channel the channel to add.
   */
  public void removeChannel(final ExecutorChannel<?> channel) {
    dispatchCount.decrementAndGet();
    final String uuid = channel.getUuid();
    synchronized(channelCounts) {
      final AtomicInteger n = channelCounts.get(uuid);
      if (n != null) {
        final int count = n.decrementAndGet();
        if (count <= 0) channelCounts.remove(uuid);
      }
    }
  }

  /**
   * Get the number of times this job is dispatched to the specified channel.
   * @param uuid the uuid of the channel to check.
   * @return the number of dispatches of this job to the channel.
   */
  final int getChannelDispatchCount(final String uuid) {
    synchronized(channelCounts) {
      final AtomicInteger n = channelCounts.get(uuid);
      return (n == null) ? 0 : n.get();
    }
  }

  /**
   * Get the number of channels this job is dispatched to.
   * @return the number of dispatches of this job to the channel.
   */
  int getChannelCount() {
    synchronized(channelCounts) {
      return channelCounts.size();
    }
  }

  /**
   * Check whether the job is already dispatched to the specfied channel.
   * @param uuid the uuid of the channel to check.
   * @return {@code true} f this job is already dispatched to the channel, {@code false} otherwise.
   */
  boolean hasChannel(final String uuid) {
    synchronized(channelCounts) {
      return channelCounts.containsKey(uuid);
    }
  }

  /**
   * Determine whether this job can be sent to the specified channel.
   * Currently this method only accepts a single remote channel, and it has to always be the same for the same job.
   * See {@link #remoteChannel}.
   * @param channel the channel to check for acceptance.
   * @return {@code true} if the channel is accepted, {@code false} otherwise.
   */
  public boolean acceptsChannel(final ExecutorChannel<?> channel) {
    final int channelCount = getChannelCount();
    if (traceEnabled) log.trace(String.format("job '%s' : cancelled=%b, cancelling=%b, pending=%b, expired=%b, nb dispatches=%d, nb channels=%d, max channels=%d",
      job.getName(), isCancelled(), isCancelling(), isPending(), isJobExpired(), dispatchCount.get(), channelCount, clientSla.getMaxChannels()));
    if (isCancelling() || isCancelled() || isPending() || isJobExpired()) return false;
    if (!hasChannel(channel.getUuid()) && (channelCount >= clientSla.getMaxChannels())) return false;
    if (!clientSla.isAllowMultipleDispatchesToSameChannel()) {
      final int n = getChannelDispatchCount(channel.getUuid());
      if (n > 0) return false;
    }
    final ExecutionPolicy policy = clientSla.getExecutionPolicy();
    boolean b = true;
    if (policy != null) {
      final JPPFSystemInformation info = channel.getSystemInformation();
      preparePolicy(policy);
      b = policy.evaluate(info);
      if (traceEnabled) log.trace("policy result = " + b);
    }
    return b;
  }

  /**
   * Set the parameters needed as bounded variables fro scripted execution policies. 
   * @param policy the root policy to explore.
   */
  public void preparePolicy(final ExecutionPolicy policy) {
    if (policy == null) return;
    policy.setContext(sla, clientSla, metadata, dispatchCount.get(), null);
  }

  /**
   * Clear the channels used to dispatch this job.
   */
  public void clearChannels() {
    dispatchCount.set(0);
    synchronized(channelCounts) {
      channelCounts.clear();
    }
  }

  /**
   * Whether this is the master/parent broadcast job.
   * @return {@code true} if this job is the parent broadcast job, {@code false} otherwise.
   */
  boolean isParentBroadcastJob() {
    return sla.isBroadcastJob() && (parentJob == null);
  }

  /**
   * Whether this is the master/parent broadcast job.
   * @return {@code true} if this job is the parent broadcast job, {@code false} otherwise.
   */
  boolean isChildBroadcastJob() {
    return sla.isBroadcastJob() && (parentJob != null);
  }

  /**
   * Whether this job is being cancelled.
   * @return {@code true} a cancellation request is being processed, {@code false} otherwise.
   */
  public boolean isCancelling() {
    return (job == null) ? false : job.getCancellingFlag().get();
  }

  /**
   * Whether this job is being cancelled or has already been cancelled.
   * @return {@code true} a cancellation request is being or has been processed, {@code false} otherwise.
   */
  public boolean isCancellingOrCancelled() {
    return isCancelled() || isCancelling();
  }
}
