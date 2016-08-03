/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.io.*;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobEvent.Type;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * @author Martin JANDA
 * @author Laurent Cohen
 */
public class ClientJob extends AbstractClientJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientJob.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The list of the tasks.
   */
  private final List<Task<?>> tasks;
  /**
   * The broadcast UUID, i.e. the uuid of the connection the job is broadcast to.
   */
  private transient String broadcastUUID = null;
  /**
   * Map of all futures in this job.
   */
  private final Map<ClientTaskBundle, ChannelWrapper> bundleMap = new LinkedHashMap<>();
  /**
   * The status of this job.
   */
  private JobStatus jobStatus;
  /**
   * The listener that receives notifications of completed tasks.
   */
  private final JPPFResultCollector resultCollector;
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
  public ClientJob(final JPPFJob job, final List<Task<?>> tasks) {
    this(job, tasks, null, null);
  }

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   * @param parentJob instance of parent broadcast job.
   * @param broadcastUUID the broadcast UUID, i.e. the uuid of the connection the job is broadcast to.
   */
  protected ClientJob(final JPPFJob job, final List<Task<?>> tasks, final ClientJob parentJob, final String broadcastUUID) {
    super(job);
    if (tasks == null) throw new IllegalArgumentException("tasks is null");
    this.parentJob = parentJob;
    this.broadcastUUID = broadcastUUID;
    this.initialTaskCount = tasks.size();
    if (broadcastUUID == null) {
      if (job.getSLA().isBroadcastJob()) this.broadcastMap = new LinkedHashMap<>();
      else this.broadcastMap = Collections.emptyMap();
      this.resultCollector = this.job.getResultCollector();
    } else {
      this.broadcastMap = Collections.emptyMap();
      this.resultCollector = null;
    }
    JobStatus s = job.getStatus();
    this.jobStatus = s == null ? JobStatus.SUBMITTED : s;
    this.tasks = new ArrayList<>(tasks);
    for (Task<?> result : job.getResults().getAllResults()) {
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
  public List<Task<?>> getTasks() {
    synchronized (tasks) {
      return Collections.unmodifiableList(new ArrayList<>(tasks));
    }
  }

  /**
   * Make a copy of this client job wrapper.
   * @param broadcastUUID the broadcast UUID, i.e. the uuid of the connection the job is broadcast to.
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
    List<Task<?>> list = (nbTasks >= this.tasks.size()) ? this.tasks : this.tasks.subList(0, nbTasks);
    ClientTaskBundle bundle = new ClientTaskBundle(this, list); // constructor makes a copy of the list
    list.clear();
    return bundle;
  }

  /**
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @param after determines whether the tasks from other should be added first or last.
   * @return <code>true</code> when this client job needs to be requeued.
   */
  protected boolean merge(final List<Task<?>> taskList, final boolean after) {
    synchronized (tasks) {
      boolean requeue = this.tasks.isEmpty() && !taskList.isEmpty();
      if (!after) this.tasks.addAll(0, taskList);
      if (after) this.tasks.addAll(taskList);
      return requeue;
    }
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
   */
  public void jobDispatched(final ClientTaskBundle bundle, final ChannelWrapper channel) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (channel == null) throw new IllegalArgumentException("channel is null");
    boolean empty;
    synchronized (bundleMap) {
      empty = bundleMap.isEmpty();
      bundleMap.put(bundle, channel);
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setJobStatus(JobStatus.EXECUTING);
      setExecuting(true);
    }
    if (!isParentBroadcastJob()) job.fireJobEvent(JobEvent.Type.JOB_DISPATCH, channel, bundle.getTasksL());
    if (parentJob != null) parentJob.broadcastDispatched(this);
  }

  /**
   * Notify that this job was requeued.
   */
  public void jobRequeued() {
    updateStatus(EXECUTING, NEW);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final List<Task<?>> results) {
    if (debugEnabled) log.debug("received " + results.size() + " results for bundle " + bundle);
    if (results.isEmpty()) return;
    synchronized (tasks) {
      for (int i=0; i<results.size(); i++) {
        Task<?> task = results.get(i);
        taskStateMap.put(task.getPosition(), TaskState.RESULT);
        if (task instanceof JPPFExceptionResult) {
          Throwable t = null;
          Task<?> originalTask = job.getJobTasks().get(task.getPosition());
          if (task instanceof JPPFExceptionResultEx) {
            JPPFExceptionResultEx result = (JPPFExceptionResultEx) task;
            String message = String.format("[%s: %s]", result.getThrowableClassName(), result.getThrowableMessage());
            t = new JPPFTaskSerializationException(message, result.getThrowableStackTrace());
          }
          else t = task.getThrowable();
          originalTask.setThrowable(t);
          results.set(i, originalTask);
        }
      }
    }
    callResultListener(results, null);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final Throwable throwable) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (debugEnabled) log.debug("received  throwable " + throwable + " for bundle " + bundle);
    boolean ioe = (throwable instanceof IOException) && !(throwable instanceof NotSerializableException);
    Exception e = throwable instanceof Exception ? (Exception) throwable : new JPPFException(throwable);
    synchronized (tasks) {
      for (Task<?> task : bundle.getTasksL()) {
        TaskState oldState = taskStateMap.get(task.getPosition());
        if (!ioe && (oldState != TaskState.RESULT)) {
          taskStateMap.put(task.getPosition(), TaskState.EXCEPTION);
          task.setThrowable(e);
        }
      }
    }
    callResultListener(bundle.getTasksL(), throwable);
  }

  /**
   * Invoke the job's {@link TaskResultListener} callback and log any exception that may result from the invocation.
   * @param results the tasks to provide as results.
   * @param throwable an eventual {@link Throwable} that may have been raised while the tasks were executing.
   * @see <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-257">JPPF-257 Better exception handling for overriden or custom TaskResultListener implementations</a>
   */
  private void callResultListener(final List<Task<?>> results, final Throwable throwable) {
    JPPFResultCollector listener = resultCollector;
    if (listener != null) {
      try {
        synchronized (listener) {
          listener.resultsReceived(results, throwable, !isParentBroadcastJob());
        }
      } catch(Exception e) {
        log.error("error while calling the TaskResultListener for job [name={}, uuid={}] : {}", new Object[] {job.getName(), job.getUuid(), ExceptionUtils.getStackTrace(e)});
      }
    } else if (isChildBroadcastJob()) {
      job.fireJobEvent(JobEvent.Type.JOB_RETURN, null, results);
    } else log.warn("<null> result collector for job {}", this.job);
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param bundle    the completed task.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final ClientTaskBundle bundle, final Exception exception) {
    if (debugEnabled) log.debug("bundle=" + bundle + ", exception=" + exception + " for " + this);
    boolean empty;
    synchronized (bundleMap) {
      ChannelWrapper channel = bundleMap.remove(bundle);
      if ((bundle != null) && (channel == null)) throw new IllegalStateException("future already removed");
      empty = bundleMap.isEmpty() && broadcastMap.isEmpty();
    }
    boolean requeue = false;
    if (getSLA().isBroadcastJob()) {
      List<Task<?>> list = new ArrayList<>();
      synchronized (tasks) {
        if (bundle != null) {
          for (Task<?> task : bundle.getTasksL()) {
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
        List<Task<?>> list = new ArrayList<>();
        synchronized (tasks) {
          list.addAll(this.tasks);
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
    } else {
      if (bundle.isCancelled()) {
        List<Task<?>> list = new ArrayList<>();
        synchronized (tasks) {
          for (Task<?> task : bundle.getTasksL()) {
            if (taskStateMap.get(task.getPosition()) != TaskState.RESULT) list.add(task);
          }
          list.addAll(this.tasks);
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
      if (bundle.isRequeued()) {
        List<Task<?>> list = new ArrayList<>();
        synchronized (tasks) {
          for (Task<?> task : bundle.getTasksL()) {
            if (taskStateMap.get(task.getPosition()) != TaskState.RESULT) list.add(task);
          }
          requeue = merge(list, false);
        }
      }
    }
    if (hasPending()) {
      if (exception != null) setJobStatus(exception instanceof NotSerializableException ? JobStatus.COMPLETE : JobStatus.FAILED);
      if (empty) setExecuting(false);
      if (requeue && onRequeue != null) {
        onRequeue.run();
        updateStatus(NEW, EXECUTING);
      }
    } else {
      boolean callDone = updateStatus(isCancelled() ? CANCELLED : EXECUTING, DONE);
      if (empty) setExecuting(false);
      try {
        if (callDone) done();
      } finally {
        if (parentJob != null) parentJob.broadcastCompleted(this);
      }
      setJobStatus(JobStatus.COMPLETE);
    }
  }

  /**
   * Get indicator whether job has pending tasks.
   * @return <code>true</code> when job has some pending tasks.
   */
  protected boolean hasPending() {
    synchronized (tasks) {
      if (tasks.isEmpty() && taskStateMap.size() >= job.getJobTasks().size()) return taskStateMap.getStateCount(TaskState.EXCEPTION) > 0;
      else return true;
    }
  }

  /**
   * Get the status of this job.
   * @return a {@link JobStatus} enumerated value.
   */
  public JobStatus getJobStatus() {
    return jobStatus;
  }

  /**
   * Set the status of this job.
   * @param jobStatus a {@link JobStatus} enumerated value.
   */
  public void setJobStatus(final JobStatus jobStatus) {
    if (this.jobStatus == jobStatus) return;
    this.jobStatus = jobStatus;
    if (resultCollector != null) ((JobStatusHandler) resultCollector).setStatus(this.jobStatus);
    else if (((jobStatus == JobStatus.COMPLETE) || (jobStatus == JobStatus.FAILED)) && isChildBroadcastJob()) job.fireJobEvent(Type.JOB_END, null, tasks);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("requesting cancel of jobId=" + this.getUuid());
    if (super.cancel(mayInterruptIfRunning)) {
      job.getCancelledFlag().set(true);
      List<ClientJob> list;
      Map<ClientTaskBundle, ChannelWrapper> map = null;
      synchronized (bundleMap) {
        list = new ArrayList<>(broadcastSet.size() + broadcastMap.size());
        list.addAll(broadcastMap.values());
        list.addAll(broadcastSet);
        map = new HashMap<>(bundleMap);
      }
      for (ClientJob broadcastJob : list) broadcastJob.cancel(mayInterruptIfRunning);
      Set<String> uuids = new HashSet<>();
      for (Map.Entry<ClientTaskBundle, ChannelWrapper> entry: map.entrySet()) {
        try {
          ChannelWrapper wrapper = entry.getValue();
          wrapper.cancel(entry.getKey());
          if (wrapper instanceof ChannelWrapperRemote) {
            JPPFConnectionPool pool = ((ChannelWrapperRemote) wrapper).getChannel().getConnectionPool();
            String driverUuid = pool.getDriverUuid();
            if (!uuids.contains(driverUuid)) {
              uuids.add(driverUuid);
              try {
                if (debugEnabled) log.debug("sending cancel request for jobId={} to driver={}", this.getUuid(), driverUuid);
                pool.getJmxConnection().cancelJob(this.getUuid());
              } catch(Exception e) {
                if (debugEnabled) log.debug(e.getMessage(), e);
                else log.warn(ExceptionUtils.getMessage(e));
              }
            }
          }
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
      if (debugEnabled) log.debug("setting cancelled flag on job {}", job);
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
      setJobStatus(JobStatus.EXECUTING);
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
      if ((broadcastMap.remove(broadcastJob.getBroadcastUUID()) != broadcastJob) && !broadcastSet.contains(broadcastJob)) {
        if (debugEnabled) log.debug("broadcast job not found: " + broadcastJob);
      }
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
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(job.getUuid());
    sb.append(", jobName=").append(job.getName());
    sb.append(", jobStatus=").append(jobStatus);
    sb.append(", broadcastUUID=").append(broadcastUUID);
    sb.append(", executing=").append(executing);
    sb.append(", nbTasks=").append(tasks.size());
    //sb.append(", taskStateMap=").append(taskStateMap);
    sb.append(']');
    return sb.toString();
  }
}
