/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.execute.ExecutorChannel;
import org.jppf.io.DataLocation;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class group tasks for the same node channel together.
 * @author Martin JANDA
 * @exclude
 */
public class ServerTaskBundleNode {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerTaskBundleNode.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Flag indicating whether collection of debug information is available via JMX.
   */
  private static final boolean JPPF_DEBUG = JPPFConfiguration.getProperties().getBoolean("jppf.debug.enabled", false);
  /**
   * Count of instances of this class.
   */
  private static final AtomicLong INSTANCE_COUNT = new AtomicLong(0L);
  /**
   * A unique id for this node bundle.
   */
  private final long id = INSTANCE_COUNT.incrementAndGet();
  /**
   * The job to execute.
   */
  private final ServerJob job;
  /**
   * The shared data provider for this task bundle.
   */
  private transient final DataLocation dataProvider;
  /**
   * The tasks to be executed by the node.
   */
  private transient final List<ServerTask> taskList;
  /**
   * Job requeue indicator.
   */
  private boolean requeued = false;
  /**
   * Job cancel indicator.
   */
  private boolean cancelled  = false;
  /**
   * Job dispatch expiration indicator.
   */
  private boolean expired  = false;
  /**
   * The job this submission is for.
   */
  private TaskBundle taskBundle;
  /**
   * Channel to which is this bundle dispatched.
   */
  private ExecutorChannel channel = null;
  /**
   * The future from channel dispatch.
   */
  private Future<?> future = null;
  /**
   * The number of tasks in this node bundle.
   */
  private final int taskCount;

  /**
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param taskBundle the job.
   * @param taskList the tasks to execute.
   */
  public ServerTaskBundleNode(final ServerJob job, final TaskBundle taskBundle, final List<ServerTask> taskList) {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (taskBundle == null) throw new IllegalArgumentException("taskBundle is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");

    this.job = job;
    this.taskBundle = taskBundle;
    this.taskList = new ArrayList<>(taskList);
    int size = this.taskList.size();
    this.taskBundle.setTaskCount(size);
    this.taskBundle.setCurrentTaskCount(size);
    this.dataProvider = job.getDataProvider();
    this.taskCount = size;
    if (JPPF_DEBUG) this.taskBundle.setParameter("node.bundle.id", this.id);
    checkTaskCount();
  }

  /**
   * Get the job this submission is for.
   * @return a {@link JPPFTaskBundle} instance.
   */
  public TaskBundle getJob() {
    return taskBundle;
  }

  /**
   * Get the client job this submission is for
   * @return a {@link ServerJob} instance.
   */
  public ServerJob getClientJob() {
    return job;
  }

  /**
   * Get shared data provider for this task.
   * @return a <code>DataProvider</code> instance.
   */
  public DataLocation getDataProvider() {
    return dataProvider;
  }

  /**
   * Get the tasks to be executed by the node.
   * @return the tasks as a <code>List</code> of arrays of bytes.
   */
  public List<ServerTask> getTaskList() {
    return taskList;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param channel the node to which the job is dispatched.
   * @param future  future assigned to bundle execution.
   */
  public void jobDispatched(final ExecutorChannel channel, final Future<?> future) {
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (future == null) throw new IllegalArgumentException("future is null");
    this.channel = channel;
    this.future  = future;
    job.jobDispatched(this);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final List<DataLocation> results) {
    job.resultsReceived(this, results);
    taskCompleted(null);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final Throwable throwable) {
    job.resultsReceived(this, throwable);
    taskCompleted(throwable);
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final Throwable exception) {
    if (debugEnabled && (exception != null)) log.debug("received exception for " + this + " : " + ExceptionUtils.getStackTrace(exception));
    try {
      job.jobReturned(this);
    } finally {
      //job.taskCompleted(this, exception);
      this.channel = null;
      this.future = null;
    }
  }

  /**
   * Called when this task bundle should be resubmitted
   */
  public void resubmit() {
    if (getJob().getSLA().isBroadcastJob()) return; // broadcast jobs cannot be resubmitted.
    synchronized(this) {
      requeued = true;
      for (ServerTask task: taskList) task.resubmit();
    }
  }

  /**
   * Called when this task bundle should be resubmitted
   */
  public void expire() {
    if (getJob().getSLA().isBroadcastJob()) return; // broadcast jobs cannot expire.
    int max = job.getSLA().getMaxDispatchExpirations();
    synchronized(this) {
      for (ServerTask task: taskList) {
        if (task.incExpirationCount() > max) task.cancel();
        else task.resubmit();
      }
      expired = true;
    }
  }

  /**
   * Get the requeued indicator.
   * @return <code>true</code> if job is requeued, <code>false</code> otherwise.
   */
  public synchronized boolean isRequeued() {
    return requeued;
  }

  /**
   * Called when this task bundle is cancelled.
   */
  public synchronized void cancel() {
    this.cancelled = true;
    for (ServerTask task: taskList) task.cancel();
  }

  /**
   * Get the cancelled indicator.
   * @return <code>true</code> if job is cancelled, <code>false</code> otherwise.
   */
  public synchronized boolean isCancelled() {
    return cancelled;
  }

  /**
   * Get the expired indicator.
   * @return <code>true</code> if job dispatch is expired, <code>false</code> otherwise.
   */
  public synchronized boolean isExpired() {
    return expired;
  }

  /**
   * Get the channel to which the job is dispatched.
   * @return an <code>ExecutorChannel</code> instance.
   */
  public ExecutorChannel getChannel() {
    return channel;
  }

  /**
   * Get the future corresponding to the channel dispatch.
   * @return a <code>Future</code> instance.
   */
  public Future<?> getFuture() {
    return future;
  }

  /**
   * Check the task count in this node bundle is equal to the one in its <code>JPPFTaskBundle</code>.
   */
  public void checkTaskCount() {
    if (taskCount != taskBundle.getTaskCount()) throw new IllegalStateException("task counts do not match");
  }

  /**
   * Get the number of tasks in this node bundle.
   * @return the number of tasks as an int.
   */
  public int getTaskCount()
  {
    return taskCount;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("id=").append(id);
    sb.append(", name=").append(job.getName());
    sb.append(", uuid=").append(job.getUuid());
    sb.append(", initialTaskCount=").append(job.getInitialTaskCount());
    sb.append(", taskCount=").append(taskCount);
    sb.append(", cancelled=").append(cancelled);
    sb.append(", requeued=").append(requeued);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ServerTaskBundleNode other = (ServerTaskBundleNode) obj;
    return id == other.id;
  }

  /**
   * Get the unique id for this node bundle.
   * @return the id as a long value.
   */
  public long getId()
  {
    return id;
  }

  /**
   * Build a unique key for the specified node task bundle.
   * @param bundle a {@link ServerTaskBundleNode} instance.
   * @return a unique key as a string.
   */
  public static String makeKey(final ServerTaskBundleNode bundle)
  {
    return makeKey(bundle.getJob().getUuid(), bundle.getId());
  }

  /**
   * Build a unique key for a node task bundle with the specified job uuid and node id.
   * @param jobUuid the uuid of the job to which the bundle belongs.
   * @param bundleId the id of the bundle.
   * @return a unique key as a string.
   */
  public static String makeKey(final String jobUuid, final long bundleId)
  {
    return new StringBuilder(jobUuid).append('|').append(bundleId).toString();
  }
}
