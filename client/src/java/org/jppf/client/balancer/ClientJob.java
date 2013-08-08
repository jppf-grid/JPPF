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

package org.jppf.client.balancer;

import java.io.*;
import java.util.*;
import java.util.concurrent.Future;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.client.balancer.utils.AbstractClientJob;
import org.jppf.client.event.*;
import org.jppf.client.submission.*;
import org.jppf.execute.ExecutorChannel;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * @author Martin JANDA
 */
public class ClientJob extends AbstractClientJob
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientJob.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
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
  private final Map<ClientTaskBundle, Future> bundleMap = new LinkedHashMap<>();
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
  private final Set<ClientJob> broadcastSet = new LinkedHashSet<>();
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
  private final TaskStateMap taskStateMap = new TaskStateMap();
  /**
   * The original number of tasks in the job.
   */
  protected final int initialTaskCount;

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   */
  public ClientJob(final JPPFJob job, final List<JPPFTask> tasks) {
    this(job, tasks, null, null);
  }

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   * @param parentJob instance of parent broadcast job.
   * @param broadcastUUID the broadcast UUID.
   */
  protected ClientJob(final JPPFJob job, final List<JPPFTask> tasks, final ClientJob parentJob, final String broadcastUUID) {
    super(job);
    if (tasks == null) throw new IllegalArgumentException("tasks is null");
    this.parentJob = parentJob;
    this.broadcastUUID = broadcastUUID;
    this.initialTaskCount = tasks.size();

    if (broadcastUUID == null) {
      if (job.getSLA().isBroadcastJob())
        this.broadcastMap = new LinkedHashMap<>();
      else
        this.broadcastMap = Collections.emptyMap();
      this.resultsListener = this.job.getResultListener();
    } else {
      this.broadcastMap = Collections.emptyMap();
      this.resultsListener = null;
    }

    if (this.job.getResultListener() instanceof SubmissionStatusHandler) this.submissionStatus = ((SubmissionStatusHandler) this.job.getResultListener()).getStatus();
    else this.submissionStatus = SubmissionStatus.SUBMITTED;
    this.tasks = new ArrayList<>(tasks);

    for (JPPFTask result : job.getResults().getAll()) {
      if (result != null) taskStateMap.put(result.getPosition(), TaskState.RESULT);
    }
  }

  /**
   * Sets indicator whether is job is executing. Job start or job end is notified when state changes.
   * @param executing <code>true</code> when this client job is executing. <code>false</code> otherwise.
   */
  protected void setExecuting(final boolean executing) {
    synchronized (tasks) {
      if (this.executing == executing) return;
      this.executing = executing;
    }
    //if (getBroadcastUUID() == null) job.fireJobEvent(executing ? JobEvent.Type.JOB_START: JobEvent.Type.JOB_END);
  }

  /**
   * Get the current number of tasks in the job.
   * @return the number of tasks as an int.
   */
  public int getTaskCount() {
    synchronized (tasks) {
      return tasks.size();
    }
  }

  /**
   * Get the list of of the tasks.
   * @return a list of <code>JPPFTask</code> instances.
   */
  public List<JPPFTask> getTasks() {
    synchronized (tasks) {
      return Collections.unmodifiableList(new ArrayList<>(tasks));
    }
  }

  /**
   * Make a copy of this client job wrapper.
   * @param broadcastUUID the broadcast UUID.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientJob createBroadcastJob(final String broadcastUUID) {
    if (broadcastUUID == null || broadcastUUID.isEmpty()) throw new IllegalArgumentException("broadcastUUID is blank");

    ClientJob clientJob;
    synchronized (tasks) {
      clientJob = new ClientJob(job, this.tasks, this, broadcastUUID);
    }
    synchronized (bundleMap) {
      broadcastSet.add(clientJob);
    }
    return clientJob;
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientTaskBundle copy(final int nbTasks) {
    synchronized (tasks) {
      if (nbTasks >= this.tasks.size()) {
        try {
          return new ClientTaskBundle(this, this.tasks);
        } finally {
          this.tasks.clear();
        }
      } else {
        List<JPPFTask> subList = this.tasks.subList(0, nbTasks);
        try {
          return new ClientTaskBundle(this, subList);
        } finally {
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
  protected boolean merge(final List<JPPFTask> taskList, final boolean after) {
    synchronized (tasks) {
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
  public TaskResultListener getResultListener() {
    return resultsListener;
  }

  /**
   * Set the listener that receives notifications of completed tasks.
   * @param resultsListener a <code>TaskCompletionListener</code> instance.
   */
  public void setResultListener(final TaskResultListener resultsListener) {
    this.resultsListener = resultsListener;
  }

  /**
   * Get the broadcast UUID.
   * @return an <code>String</code> instance.
   */
  public String getBroadcastUUID() {
    return broadcastUUID;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param bundle  the dispatched job.
   * @param channel the node to which the job is dispatched.
   * @param future  future assigned to bundle execution.
   */
  public void jobDispatched(final ClientTaskBundle bundle, final ExecutorChannel channel, final Future<?> future) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (future == null) throw new IllegalArgumentException("future is null");

    boolean empty;
    synchronized (bundleMap) {
      empty = bundleMap.isEmpty();
      bundleMap.put(bundle, future);
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
      setExecuting(true);
    }
    job.fireJobEvent(JobEvent.Type.JOB_DISPATCH, channel, bundle.getTasksL());
    if (parentJob != null) parentJob.broadcastDispatched(this);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final List<JPPFTask> results) {
    if (debugEnabled) log.debug("received " + results.size() + " results for bundle " + bundle);
    if (results.isEmpty()) return;

    synchronized (tasks) {
      for (JPPFTask task : results) taskStateMap.put(task.getPosition(), TaskState.RESULT);
    }
    TaskResultListener listener = resultsListener;
    if (listener != null) {
      synchronized (listener) {
        listener.resultsReceived(new TaskResultEvent(results, null));
      }
    }
    job.fireJobEvent(JobEvent.Type.JOB_RETURN, null, results);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final Throwable throwable) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (debugEnabled) log.debug("received  throwable " + throwable + " for bundle " + bundle);
    boolean ioe = isIOException(throwable);
    Exception e = throwable instanceof Exception ? (Exception) throwable : new JPPFException(throwable);
    synchronized (tasks) {
      for (JPPFTask task : bundle.getTasksL()) {
        TaskState oldState = taskStateMap.get(task.getPosition());
        if (!ioe && (oldState != TaskState.RESULT)) {
          taskStateMap.put(task.getPosition(), TaskState.EXCEPTION);
          task.setException(e);
        }
      }
    }
    TaskResultListener listener = resultsListener;
    if (listener != null) {
      synchronized (listener) {
        listener.resultsReceived(new TaskResultEvent(bundle.getTasksL(), throwable));
      }
    }
    job.fireJobEvent(JobEvent.Type.JOB_RETURN, null, bundle.getTasksL());
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param bundle    the completed task.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final ClientTaskBundle bundle, final Exception exception)
  {
    if (debugEnabled) log.debug("bundle=" + bundle + ", exception=" + exception + " for " + this);
    boolean empty;
    synchronized (bundleMap) {
      Future future = bundleMap.remove(bundle);
      if (bundle != null && future == null) throw new IllegalStateException("future already removed");
      empty = bundleMap.isEmpty() && broadcastMap.isEmpty();
    }
    //if (empty) clearChannels();
    boolean requeue = false;
    if (getSLA().isBroadcastJob()) {
      List<JPPFTask> list = new ArrayList<>();
      synchronized (tasks) {
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
        List<JPPFTask> list = new ArrayList<>();
        synchronized (tasks) {
          list.addAll(this.tasks);
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
    } else {
      if (bundle.isCancelled()) {
        List<JPPFTask> list = new ArrayList<>();
        synchronized (tasks) {
          for (JPPFTask task : bundle.getTasksL()) {
            if (taskStateMap.get(task.getPosition()) != TaskState.RESULT) list.add(task);
          }
          list.addAll(this.tasks);
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
      if (bundle.isRequeued()) {
        List<JPPFTask> list = new ArrayList<>();
        synchronized (tasks) {
          for (JPPFTask task : bundle.getTasksL()) {
            if (taskStateMap.get(task.getPosition()) != TaskState.RESULT) list.add(task);
          }
          requeue = merge(list, false);
        }
      }
    }

    if (hasPending()) {
      if (exception != null) {
        setSubmissionStatus(exception instanceof NotSerializableException ? SubmissionStatus.COMPLETE : SubmissionStatus.FAILED);
      }
      if (empty) setExecuting(false);
      if (requeue && onRequeue != null) onRequeue.run();
    } else {
      boolean callDone = updateStatus(isCancelled() ? CANCELLED : EXECUTING, DONE);
      if (empty) setExecuting(false);
      try {
        if (callDone) done();
      } finally {
        if (parentJob != null) parentJob.broadcastCompleted(this);
      }
      setSubmissionStatus(SubmissionStatus.COMPLETE);
    }
  }

  /**
   * Get indicator whether job has pending tasks.
   * @return <code>true</code> when job has some pending tasks.
   */
  protected boolean hasPending() {
    synchronized (tasks) {
      if (tasks.isEmpty() && taskStateMap.size() >= job.getTasks().size()) {
        return taskStateMap.getStateCount(TaskState.EXCEPTION) > 0;
      } else return true;
    }
  }

  /**
   * Get the status of this submission.
   * @return a {@link SubmissionStatus} enumerated value.
   */
  public SubmissionStatus getSubmissionStatus() {
    return submissionStatus;
  }

  /**
   * Set the status of this submission.
   * @param submissionStatus a {@link SubmissionStatus} enumerated value.
   */
  public void setSubmissionStatus(final SubmissionStatus submissionStatus) {
    if (this.submissionStatus == submissionStatus) return;
    this.submissionStatus = submissionStatus;
    if (resultsListener instanceof SubmissionStatusHandler) ((SubmissionStatusHandler) resultsListener).setStatus(this.submissionStatus);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("requesting cancel of jobId=" + this.getUuid());
    if (super.cancel(mayInterruptIfRunning)) {
      done();
      List<ClientJob> list;
      List<Future>   futureList;
      synchronized (bundleMap) {
        list = new ArrayList<>(broadcastSet.size() + broadcastMap.size());
        list.addAll(broadcastMap.values());
        list.addAll(broadcastSet);

        futureList = new ArrayList<>(bundleMap.size());
        futureList.addAll(bundleMap.values());
      }
      for (ClientJob broadcastJob : list) broadcastJob.cancel(mayInterruptIfRunning);
      for (Future future : futureList) {
        try {
          if (!future.isDone()) future.cancel(false);
        } catch (Exception e) {
          log.error("Error cancelling job " + this, e);
        }
      }

      boolean empty;
      synchronized (bundleMap) {
        broadcastSet.clear();
        empty = bundleMap.isEmpty() && broadcastMap.isEmpty();
      }
      if (empty) taskCompleted(null, null);
      return true;
    }
    else return false;
  }

  /**
   * Called when all or part of broadcast job is dispatched to a driver.
   * @param broadcastJob    the dispatched job.
   */
  protected void broadcastDispatched(final ClientJob broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    boolean empty;
    synchronized (bundleMap) {
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
   * Called to notify that the execution of broadcast job has completed.
   * @param broadcastJob    the completed job.
   */
  protected void broadcastCompleted(final ClientJob broadcastJob) {
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
   * Set the requeue handler.
   * @param onRequeue {@link Runnable} executed on requeue.
   */
  public void setOnRequeue(final Runnable onRequeue) {
    if (getSLA().isBroadcastJob()) return; // broadcast jobs cannot be requeued
    this.onRequeue = onRequeue;
  }

  /**
   * Get count of channels on which this job is executed.
   * @return the number used for job execution.
   */
  public int getNbChannels() {
    synchronized (bundleMap) {
      return bundleMap.size();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("job=").append(job);
    sb.append(", submissionStatus=").append(submissionStatus);
    sb.append(", broadcastUUID=").append(broadcastUUID);
    sb.append(", executing=").append(executing);
    sb.append(", nbTasks=").append(tasks.size());
    //sb.append(", taskStateMap=").append(taskStateMap);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Determine if a throwable is an IOException causing a recoonection to the driver.
   * @param t th throwable to check.
   * @return .
   */
  private boolean isIOException(final Throwable t)
  {
    return (t instanceof IOException) && !(t instanceof NotSerializableException);
  }
}
