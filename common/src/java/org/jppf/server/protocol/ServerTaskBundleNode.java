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
package org.jppf.server.protocol;

import java.util.*;
import java.util.concurrent.Future;

import org.jppf.execute.ExecutorChannel;
import org.jppf.io.DataLocation;

/**
 * Instances of this class group tasks for the same node channel together.
 * @author Martin JANDA
 */
public class ServerTaskBundleNode extends JPPFTaskBundle {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

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
   * Job cancel indicator
   */
  private boolean cancelled  = false;
  /**
   * The job this submission is for
   */
  private JPPFTaskBundle taskBundle;

  /**
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param taskList the tasks to execute.
   */
  public ServerTaskBundleNode(final ServerJob job, final List<ServerTask> taskList) {
    this(job, null, taskList);
  }

  /**
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param taskBundle the job.
   * @param taskList the tasks to execute.
   */
  public ServerTaskBundleNode(final ServerJob job, final JPPFTaskBundle taskBundle, final List<ServerTask> taskList)
  {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (taskBundle == null) throw new IllegalArgumentException("taskBundle is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");

    this.job = job;
    this.taskBundle = taskBundle;
//    if (getState() == State.INITIAL_BUNDLE)
//      this.taskBundle = this.job.getJob();
//    else
//      this.taskBundle = this.job.getJob().copy(tasks.size());
    this.setSLA(job.getSLA());
    this.setMetadata(job.getJob().getMetadata());
    this.taskList = Collections.unmodifiableList(new ArrayList<ServerTask>(taskList));
    this.setName(job.getJob().getName());
    this.dataProvider = job.getDataProvider();
    setTaskCount(this.taskList.size());
  }

  /**
   * Get the job this submission is for.
   * @return a {@link JPPFTaskBundle} instance.
   */
  public JPPFTaskBundle getJob()
  {
    return taskBundle;
  }

  /**
   * Get the client job this submission is for
   * @return a {@link ServerJob} instance.
   */
  public ServerJob getClientJob()
  {
    return job;
  }

  /**
   * Get shared data provider for this task.
   * @return a <code>DataProvider</code> instance.
   */
  public DataLocation getDataProviderL()
  {
    return dataProvider;
  }

  /**
   * Get the tasks to be executed by the node.
   * @return the tasks as a <code>List</code> of arrays of bytes.
   */
  public List<ServerTask> getTaskList()
  {
    return taskList;
  }

  /**
   * Make a copy of this bundle.
   * @return a new <code>ClientTaskBundle</code> instance.
   */
  @Override
  public ServerTaskBundleNode copy()
  {
    throw new UnsupportedOperationException();
//    ClientTaskBundle bundle = new ClientTaskBundle(getJob(), tasks);
//    bundle.setUuidPath(getUuidPath());
//    bundle.setRequestUuid(getRequestUuid());
//    bundle.setUuid(getUuid());
//    bundle.setName(getName());
//    bundle.setTaskCount(getTaskCount());
//    bundle.setDataProvider(getDataProvider());
//    synchronized (bundle.getParametersMap())
//    {
//      for (Map.Entry<Object, Object> entry : getParametersMap().entrySet())
//        bundle.setParameter(entry.getKey(), entry.getValue());
//    }
//    bundle.setQueueEntryTime(getQueueEntryTime());
//    bundle.setCompletionListener(getCompletionListener());
//    bundle.setSLA(getSLA());
//    bundle.setLocalExecutionPolicy(localExecutionPolicy);
//    bundle.setBroadcastUUID(broadcastUUID);
//    //bundle.setParameter(BundleParameter.JOB_METADATA, getJobMetadata());
//
//    return bundle;
  }

  /**
   * Make a copy of this bundle containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientTaskBundle</code> instance.
   */
  @Override
  public ServerTaskBundleNode copy(final int nbTasks)
  {
    throw new UnsupportedOperationException();
//    ClientTaskBundle bundle = copy();
//    bundle.setTaskCount(nbTasks);
//    taskCount -= nbTasks;
//    return bundle;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param channel the node to which the job is dispatched.
   * @param future  future assigned to bundle execution.
   */
  public void jobDispatched(final ExecutorChannel channel, final Future<?> future)
  {
    job.jobDispatched(this, channel, future);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final List<DataLocation> results)
  {
    job.resultsReceived(this, results);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final Throwable throwable)
  {
    job.resultsReceived(this, throwable);
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final Exception exception)
  {
    job.taskCompleted(this, exception);
  }

  /**
   * Called when this task bundle should be resubmitted
   */
  public synchronized void resubmit()
  {
    if (getSLA().isBroadcastJob()) return; // broadcast jobs cannot be resubmitted.
    requeued = true;
  }

  /**
   * Get the requeued indicator.
   * @return <code>true</code> if job is requeued, <code>false</code> otherwise.
   */
  public synchronized boolean isRequeued()
  {
    return requeued;
  }

  /**
   * Called when this task bundle is cancelled.
   */
  public synchronized void cancel()
  {
    this.cancelled = true;
  }

  /**
   * Get the cancelled indicator.
   * @return <code>true</code> if job is cancelled, <code>false</code> otherwise.
   */
  public synchronized boolean isCancelled()
  {
    return cancelled;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder("[");
    sb.append("jobId=").append(getName());
    sb.append(", jobUuid=").append(getUuid());
    sb.append(", initialTaskCount=").append(getInitialTaskCount());
    sb.append(", taskCount=").append(getTaskCount());
    sb.append(", requeue=").append(isRequeued());
    sb.append(", cancelled=").append(isCancelled());
    sb.append(']');
    return sb.toString();
  }
}