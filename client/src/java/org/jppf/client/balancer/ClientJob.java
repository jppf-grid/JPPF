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

package org.jppf.client.balancer;

import org.jppf.client.JPPFJob;
import org.jppf.client.balancer.utils.AbstractClientJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.client.event.TaskResultListener;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.client.submission.SubmissionStatusHandler;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;

/**
 * @author Martin JANDA
 */
public class ClientJob extends AbstractClientJob
{
  /**
   * State for task indicating whether result or execption was received.
   */
  protected static enum TaskState {
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
  private static final Logger log = LoggerFactory.getLogger(ClientJob.class);
  /**
   * The list of of the tasks.
   */
  private final List<JPPFTask> tasks;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID = null;
  /**
   * Map of all futures in this job.
   */
  private final Map<ClientTaskBundle, Future> bundleMap = new LinkedHashMap<ClientTaskBundle, Future>();
  /**
   * The status of this submission.
   */
  private SubmissionStatus submissionStatus;
  /**
   * The listener that receives notifications of completed tasks.
   */
  private TaskResultListener resultsListener;
  /**
   * Instance of parent broadcast job.
   */
  private transient ClientJob parentJob;
  /**
   * Map of all dispatched broadcast jobs.
   */
  private final Map<String, ClientJob> broadcastMap;
  /**
   * Map of all pending broadcast jobs.
   */
  private final Set<ClientJob> broadcastSet = new LinkedHashSet<ClientJob>();
  /**
   * Indicator whether this job is executing.
   */
  private boolean executing = false;
  /**
   * The requeue handler.
   */
  private Runnable onRequeue = null;
  /**
   * State map for tasks on which resultReceived was called.
   */
  private final SortedMap<Integer, TaskState> taskStateMap = new TreeMap<Integer, TaskState>();

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   */
  public ClientJob(final JPPFJob job, final List<JPPFTask> tasks)
  {
    this(job, tasks, null, null);
  }

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   * @param parentJob instance of parent broadcast job.
   * @param broadcastUUID the broadcast UUID.
   */
  protected ClientJob(final JPPFJob job, final List<JPPFTask> tasks, final ClientJob parentJob, final String broadcastUUID)
  {
    super(job);
    if (tasks == null) throw new IllegalArgumentException("tasks is null");

    this.parentJob = parentJob;
    this.broadcastUUID = broadcastUUID;

    if (broadcastUUID == null) {
      if (job.getSLA().isBroadcastJob())
        this.broadcastMap = new LinkedHashMap<String, ClientJob>();
      else
        this.broadcastMap = Collections.emptyMap();
      this.resultsListener = this.job.getResultListener();
    } else {
      this.broadcastMap = Collections.emptyMap();
      this.resultsListener = null;
    }

    if (this.job.getResultListener() instanceof SubmissionStatusHandler)
      this.submissionStatus = ((SubmissionStatusHandler) this.job.getResultListener()).getStatus();
    else
      this.submissionStatus = SubmissionStatus.SUBMITTED;

    this.tasks = new ArrayList<JPPFTask>(tasks);
  }

  /**
   * Sets indicator whether is job is executing. Job start or job end is notified when state changes.
   * @param executing <code>true</code> when this client job is executing. <code>false</code> otherwise.
   */
  protected void setExecuting(final boolean executing) {
    synchronized (tasks)
    {
      if (this.executing == executing) return;
      this.executing = executing;
    }
    if (getBroadcastUUID() == null) {
      if (executing)
        job.fireJobEvent(JobEvent.Type.JOB_START);
      else
        job.fireJobEvent(JobEvent.Type.JOB_END);
    }
  }

  /**
   * Get the current number of tasks in the job.
   * @return the number of tasks as an int.
   */
  public int getTaskCount()
  {
    synchronized (tasks)
    {
      return tasks.size();
    }
  }

  /**
   * Get the list of of the tasks.
   * @return a list of <code>JPPFTask</code> instances.
   */
  public List<JPPFTask> getTasks()
  {
    synchronized (tasks)
    {
      return Collections.unmodifiableList(new ArrayList<JPPFTask>(tasks));
    }
  }

  /**
   * Make a copy of this client job wrapper.
   * @param broadcastUUID the broadcast UUID.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientJob createBroadcastJob(final String broadcastUUID)
  {
    if (broadcastUUID == null || broadcastUUID.isEmpty()) throw new IllegalArgumentException("broadcastUUID is blank");

    ClientJob clientJob;
    synchronized (tasks)
    {
      clientJob = new ClientJob(job, this.tasks, this, broadcastUUID);
    }
    synchronized (bundleMap)
    {
      broadcastSet.add(clientJob);
    }
    return clientJob;
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientTaskBundle copy(final int nbTasks)
  {
    synchronized (tasks)
    {
      if (nbTasks >= this.tasks.size())
      {
        try {
          return new ClientTaskBundle(this, this.tasks);
        } finally {
          this.tasks.clear();
        }
      }
      else
      {
        List<JPPFTask> subList = this.tasks.subList(0, nbTasks);
        try
        {
          return new ClientTaskBundle(this, subList);
        }
        finally
        {
          subList.clear();
        }
      }
    }
  }

  /**
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @param after determines whether the tasks from other should be added first or last.
   * @return <code>true</code> when this client job needs to be requeued.
   */
  protected boolean merge(final List<JPPFTask> taskList, final boolean after)
  {
    synchronized (tasks)
    {
      boolean requeue = this.tasks.isEmpty() && !taskList.isEmpty();
      if (!after) this.tasks.addAll(0, taskList);
      if (after) this.tasks.addAll(taskList);
      return requeue;
    }
  }

  /**
   * Get the listener that receives notifications of completed tasks.
   * @return a <code>TaskCompletionListener</code> instance.
   */
  public TaskResultListener getResultListener()
  {
    return resultsListener;
  }

  /**
   * Set the listener that receives notifications of completed tasks.
   * @param resultsListener a <code>TaskCompletionListener</code> instance.
   */
  public void setResultListener(final TaskResultListener resultsListener)
  {
    this.resultsListener = resultsListener;
  }

  /**
   * Get the broadcast UUID.
   * @return an <code>String</code> instance.
   */
  public String getBroadcastUUID()
  {
    return broadcastUUID;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param bundle  the dispatched job.
   * @param channel the node to which the job is dispatched.
   * @param future  future assigned to bundle execution.
   */
  public void jobDispatched(final ClientTaskBundle bundle, final ChannelWrapper<?> channel, final Future<?> future)
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (future == null) throw new IllegalArgumentException("future is null");

    boolean empty;
    synchronized (bundleMap)
    {
      empty = bundleMap.isEmpty();
      bundleMap.put(bundle, future);
    }
    if (empty)
    {
      updateStatus(NEW, EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
      setExecuting(true);
    }
    if (parentJob != null) parentJob.broadcastDispatched(this);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final List<JPPFTask> results)
  {
    if (results.isEmpty()) return;

    synchronized (tasks)
    {
      for (JPPFTask task : results) {
        taskStateMap.put(task.getPosition(), TaskState.RESULT);
      }
    }
    TaskResultListener listener = resultsListener;
    if (listener != null)
    {
      synchronized (listener)
      {
        listener.resultsReceived(new TaskResultEvent(results));
      }
    }
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final Throwable throwable)
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    synchronized (tasks)
    {
      for (JPPFTask task : bundle.getTasksL()) {
        TaskState oldState = taskStateMap.get(task.getPosition());
        if (oldState != TaskState.RESULT) taskStateMap.put(task.getPosition(), TaskState.EXCEPTION);
      }
    }
    TaskResultListener listener = resultsListener;
    if (listener != null)
    {
      synchronized (listener)
      {
        listener.resultsReceived(new TaskResultEvent(throwable));
      }
    }
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param bundle    the completed task.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final ClientTaskBundle bundle, final Exception exception)
  {
    boolean empty;
    synchronized (bundleMap)
    {
      Future future = bundleMap.remove(bundle);
      if (bundle != null && future == null) throw new IllegalStateException("future already removed");
      empty = bundleMap.isEmpty() && broadcastMap.isEmpty();
    }
    boolean requeue = false;
    if (getSLA().isBroadcastJob()) {
      List<JPPFTask> list = new ArrayList<JPPFTask>();
      synchronized (tasks)
      {
        if (bundle != null) {
          for (JPPFTask task : bundle.getTasksL()) {
            if (taskStateMap.put(task.getPosition(), TaskState.RESULT) != TaskState.RESULT) list.add(task);
          }
        }
        if (isCancelled() || getBroadcastUUID() == null) {
          list.addAll(this.tasks);
          this.tasks.clear();
        }
      }
      resultsReceived(bundle, list);
    } else if (bundle == null) {
      if (isCancelled()) {
        List<JPPFTask> list = new ArrayList<JPPFTask>();
        synchronized (tasks)
        {
          list.addAll(this.tasks);
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
    } else {
      if (bundle.isCancelled()) {
        List<JPPFTask> list = new ArrayList<JPPFTask>();
        synchronized (tasks)
        {
          for (JPPFTask task : bundle.getTasksL()) {
            if (taskStateMap.get(task.getPosition()) != TaskState.RESULT) list.add(task);
          }
          list.addAll(this.tasks);
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
      if (bundle.isRequeued()) {
        List<JPPFTask> list = new ArrayList<JPPFTask>();
        synchronized (tasks)
        {
          for (JPPFTask task : bundle.getTasksL()) {
            if (taskStateMap.get(task.getPosition()) != TaskState.RESULT) list.add(task);
          }

          requeue = merge(list, false);
        }
      }
    }

    if (hasPending()) {
      if (exception != null) setSubmissionStatus(SubmissionStatus.FAILED);
      if (empty) setExecuting(false);

      if (requeue && onRequeue != null) onRequeue.run();
    } else {
      boolean callDone = updateStatus(EXECUTING, DONE);
      if (empty) setExecuting(false);
      setSubmissionStatus(SubmissionStatus.COMPLETE);

      try {
        if (callDone) done();
      } finally {
        if (parentJob != null) parentJob.broadcastCompleted(this);
      }
    }
  }

  /**
   * Get indicator whether job has pending tasks.
   * @return <code>true</code> when job has some penging tasks.
   */
  protected boolean hasPending() {
    synchronized (tasks)
    {
      if (tasks.isEmpty() && taskStateMap.size() >= job.getTasks().size())
      {
        for (TaskState state : taskStateMap.values())
        {
          if (state == TaskState.EXCEPTION) return true;
        }
        return false;
      } else
        return true;
    }
  }

  /**
   * Get the status of this submission.
   * @return a {@link SubmissionStatus} enumerated value.
   */
  public SubmissionStatus getSubmissionStatus()
  {
    return submissionStatus;
  }

  /**
   * Set the status of this submission.
   * @param submissionStatus a {@link SubmissionStatus} enumerated value.
   */
  public void setSubmissionStatus(final SubmissionStatus submissionStatus)
  {
    if (this.submissionStatus == submissionStatus) return;
    this.submissionStatus = submissionStatus;
    if (resultsListener instanceof SubmissionStatusHandler) ((SubmissionStatusHandler) resultsListener).setStatus(this.submissionStatus);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancel(final boolean mayInterruptIfRunning)
  {
    if (super.cancel(mayInterruptIfRunning)) {
      done();
      List<ClientJob> list;
      List<Future>   futureList;
      synchronized (bundleMap)
      {
        list = new ArrayList<ClientJob>(broadcastSet.size() + broadcastMap.size());
        list.addAll(broadcastMap.values());
        list.addAll(broadcastSet);

        futureList = new ArrayList<Future>(bundleMap.size());
        futureList.addAll(bundleMap.values());
      }
      for (ClientJob broadcastJob : list)
      {
        broadcastJob.cancel(mayInterruptIfRunning);
      }
      for (Future future : futureList)
      {
        try
        {
          if (!future.isDone()) future.cancel(false);
        }
        catch (Exception e)
        {
          log.error("Error cancelling job " + this, e);
        }
      }

      boolean empty;
      synchronized (bundleMap)
      {
        broadcastSet.clear();
        empty = bundleMap.isEmpty() && broadcastMap.isEmpty();
      }
      if (empty) taskCompleted(null, null);
      return true;
    }
    else
      return false;
  }

  /**
   * Called when all or part of broadcast job is dispatched to a driver.
   * @param broadcastJob    the dispatched job.
   */
  protected void broadcastDispatched(final ClientJob broadcastJob)
  {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");

    boolean empty;
    synchronized (bundleMap)
    {
      broadcastSet.remove(broadcastJob);
      empty = broadcastMap.isEmpty();
      broadcastMap.put(broadcastJob.getBroadcastUUID(), broadcastJob);
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
      setExecuting(true);
    }
  }

  /**
   * Called to notify that the execution of broadcasted job has completed.
   * @param broadcastJob    the completed job.
   */
  protected void broadcastCompleted(final ClientJob broadcastJob)
  {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");

    //    if (debugEnabled) log.debug("received " + n + " tasks for node uuid=" + uuid);
    boolean empty;
    synchronized (bundleMap) {
      if (broadcastMap.remove(broadcastJob.getBroadcastUUID()) != broadcastJob && !broadcastSet.contains(broadcastJob)) throw new IllegalStateException("broadcast job not found");
      empty = broadcastMap.isEmpty();
    }
    if (empty) taskCompleted(null, null);
  }

  /**
   * Set the reuque handler.
   * @param onRequeue {@link Runnable} executed on requeue.
   */
  public void setOnRequeue(final Runnable onRequeue)
  {
    if (getSLA().isBroadcastJob()) return; // broadcast jobs cannot be requeud
    this.onRequeue = onRequeue;
  }
}
