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

import java.io.*;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobEvent.Type;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.*;
import org.jppf.node.protocol.graph.*;
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
  final Map<Integer, Task<?>> tasks;
  //private final List<Task<?>> tasks;
  /**
   * The broadcast UUID, i.e. the uuid of the connection the job is broadcast to.
   */
  private transient String broadcastUUID;
  /**
   * Mapping of task bundles to the channel they are sent to.
   */
  private final Map<ClientTaskBundle, ChannelWrapper> bundleMap = new LinkedHashMap<>();
  /**
   * The status of this job.
   */
  private JobStatus jobStatus;
  /**
   * Map of all dispatched broadcast jobs.
   */
  private final Map<String, ClientJob> broadcastMap;
  /**
   * Map of all pending broadcast jobs.
   */
  private final Set<ClientJob> broadcastSet = new LinkedHashSet<>();
  /**
   * The requeue handler.
   */
  private Runnable onRequeue;
  /**
   * State map for tasks on which resultReceived was called.
   */
  private final TaskStateMap taskStateMap = new TaskStateMap();
  /**
   * The original number of tasks in the job.
   */
  protected final int initialTaskCount;
  /**
   * The graph of tasks in the job, if any.
   */
  private final TaskGraph taskGraph;
  /**
   * Position of taskss sent tot he server.
   */
  private final Set<Integer> dispatchedTasks = new HashSet<>();

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   */
  public ClientJob(final JPPFJob job, final Collection<Task<?>> tasks) {
    this(job, tasks, null, null);
  }

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   * @param parentJob instance of parent broadcast job.
   * @param broadcastUUID the broadcast UUID, i.e. the uuid of the connection the job is broadcast to.
   */
  protected ClientJob(final JPPFJob job, final Collection<Task<?>> tasks, final ClientJob parentJob, final String broadcastUUID) {
    super(job);
    if (tasks == null) throw new IllegalArgumentException("tasks is null");
    this.parentJob = parentJob;
    this.broadcastUUID = broadcastUUID;
    this.initialTaskCount = tasks.size();
    if ((broadcastUUID == null) && job.getSLA().isBroadcastJob()) this.broadcastMap = new LinkedHashMap<>();
    else this.broadcastMap = Collections.emptyMap();
    final JobStatus s = job.getStatus();
    this.jobStatus = s == null ? JobStatus.SUBMITTED : s;
    this.tasks = new TreeMap<>();
    for (final Task<?> task: tasks) this.tasks.put(task.getPosition(), task);
    for (final Task<?> result : job.getResults().getAllResults()) {
      if (result != null) taskStateMap.put(result.getPosition(), TaskState.RESULT);
    }
    this.taskGraph = job.hasTaskGraph() ? TaskGraphHelper.graphOf(tasks) : null;
    if (debugEnabled && (taskGraph != null)) log.debug("taskGraph = {}", taskGraph);
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
   * Make a copy of this client job wrapper.
   * @param broadcastUUID the broadcast UUID, i.e. the uuid of the connection the job is broadcast to.
   * @return a new {@code ClientJob} instance.
   */
  public ClientJob createBroadcastJob(final String broadcastUUID) {
    if (broadcastUUID == null || broadcastUUID.isEmpty()) throw new IllegalArgumentException("broadcastUUID is blank");
    final ClientJob clientJob;
    synchronized (tasks) {
      clientJob = new ClientJob(job, tasks.values(), this, broadcastUUID);
    }
    synchronized (bundleMap) {
      broadcastSet.add(clientJob);
    }
    return clientJob;
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new {@code ClientJob} instance.
   */
  public ClientTaskBundle copy(final int nbTasks) {
    Collection<Task<?>> list = null;
    synchronized (tasks) {
      if ((taskGraph == null) || !getJob().getClientSLA().isGraphTraversalInClient()) {
        if ((nbTasks >= tasks.size()) || (taskGraph != null)) {
          list = new ArrayList<>(tasks.values());
        } else {
          list = new ArrayList<>(nbTasks);
          final Iterator<Map.Entry<Integer, Task<?>>> it = tasks.entrySet().iterator();
          for (int i=0; i<nbTasks; i++) {
            final Map.Entry<Integer, Task<?>> entry = it.next();
            list.add(entry.getValue());
          }
        }
        if (taskGraph != null) {
          for (final Task<?> task: list) dispatchedTasks.add(task.getPosition());
        }
      } else {
        final Set<Integer> availablePos = taskGraph.getAvailableNodes();
        final int effectiveNbTasks = Math.min(nbTasks, availablePos.size());
        final Iterator<Integer> it = availablePos.iterator();
        list = new ArrayList<>(effectiveNbTasks);
        for (int i=0; i<effectiveNbTasks; i++) {
          final int pos = it.next();
          if (!dispatchedTasks.contains(pos)) {
            dispatchedTasks.add(pos);
            list.add(tasks.get(pos));
          }
        }
        if (debugEnabled) log.debug("taskGraph = {}, sentTasks = {}", taskGraph, dispatchedTasks);
      }
      final ClientTaskBundle bundle = new ClientTaskBundle(this, list);
      for (final Task<?> task: list) tasks.remove(task.getPosition());
      return bundle;
    }
  }

  /**
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @return {@code true} when this client job needs to be requeued.
   */
  protected boolean merge(final List<Task<?>> taskList) {
    synchronized (tasks) {
      final boolean requeue = this.tasks.isEmpty() && !taskList.isEmpty();
      for (final Task<?> task: taskList) tasks.put(task.getPosition(), task);
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
    final boolean empty;
    synchronized (bundleMap) {
      empty = bundleMap.isEmpty();
      if (debugEnabled) log.debug("adding channel {} to bundleMap of {}", channel, this);
      bundleMap.put(bundle, channel);
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setJobStatus(JobStatus.EXECUTING);
    }
    if (!isParentBroadcastJob()) job.fireJobEvent(JobEvent.Type.JOB_DISPATCH, channel, bundle.getTasksL());
    if (parentJob != null) parentJob.broadcastDispatched(this);
  }

  /**
   * Notify that this job was requeued.
   */
  public void jobRequeued() {
    synchronized (tasks) {
      dispatchedTasks.clear();
    }
    updateStatus(EXECUTING, NEW);
    if (debugEnabled) log.debug("job requeued: {}", this);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final List<Task<?>> results) {
    if (debugEnabled) log.debug("received {} results for bundle {}", results.size(), bundle);
    if (results.isEmpty()) return;
    synchronized (tasks) {
      for (int i=0; i<results.size(); i++) {
        final Task<?> task = results.get(i);
        final int position = task.getPosition();
        taskStateMap.put(position, TaskState.RESULT);
        if (taskGraph != null) {
          dispatchedTasks.remove(position);
          taskGraph.nodeDone(position);
        }
        if (task instanceof JPPFExceptionResult) {
          Throwable t = null;
          final Task<?> originalTask = job.getJobTasks().get(position);
          if (task instanceof JPPFExceptionResultEx) {
            final JPPFExceptionResultEx result = (JPPFExceptionResultEx) task;
            final String message = String.format("[%s: %s]", result.getThrowableClassName(), result.getThrowableMessage());
            t = new JPPFTaskSerializationException(message, result.getThrowableStackTrace());
          }
          else t = task.getThrowable();
          originalTask.setThrowable(t);
          results.set(i, originalTask);
        }
      }
      if (debugEnabled && (taskGraph != null)) log.debug("taskGraph = {}, sentTasks = {}", taskGraph, dispatchedTasks);
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
    if (debugEnabled) log.debug("received  throwable {} for bundle ", throwable, bundle);
    final boolean ioe = (throwable instanceof IOException) && !(throwable instanceof NotSerializableException);
    final Exception e = throwable instanceof Exception ? (Exception) throwable : new JPPFException(throwable);
    synchronized (tasks) {
      for (final Task<?> task : bundle.getTasksL()) {
        final int position = task.getPosition();
        if (taskGraph != null) {
          dispatchedTasks.remove(position);
          taskGraph.nodeDone(position);
        }
        final TaskState oldState = taskStateMap.get(position);
        if (!ioe && (oldState != TaskState.RESULT)) {
          taskStateMap.put(task.getPosition(), TaskState.EXCEPTION);
          task.setThrowable(e);
        }
      }
      if (debugEnabled && (taskGraph != null)) log.debug("taskGraph = {}, sentTasks = {}", taskGraph, dispatchedTasks);
    }
    callResultListener(bundle.getTasksL(), throwable);
  }

  /**
   * Invoke the job's {@link TaskResultListener} callback and log any exception that may result from the invocation.
   * @param results the tasks to provide as results.
   * @param throwable an eventual {@link Throwable} that may have been raised while the tasks were executing.
   */
  private void callResultListener(final List<Task<?>> results, final Throwable throwable) {
    if (job != null) {
      try {
        job.resultsReceived(results, throwable, !isParentBroadcastJob());
      } catch(final Exception e) {
        log.error("error while calling the TaskResultListener for job [name={}, uuid={}] : {}", job.getName(), job.getUuid(), ExceptionUtils.getStackTrace(e));
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
    if (debugEnabled) log.debug("bundle={}, exception={} for {}", bundle, exception, this);
    synchronized (bundleMap) {
      final ChannelWrapper channel = bundleMap.remove(bundle);
      if (debugEnabled) log.debug("removed channel {} from bundleMap of {}", channel, this);
    }
    boolean requeue = false;
    if (getSLA().isBroadcastJob()) {
      if (debugEnabled) log.debug("processing broadcast job {}", this);
      final List<Task<?>> list = new ArrayList<>();
      synchronized (tasks) {
        if (bundle != null) {
          for (final Task<?> task : bundle.getTasksL()) {
            if (taskStateMap.put(task.getPosition(), TaskState.RESULT) != TaskState.RESULT) list.add(task);
          }
        }
        if (isCancelled() || getBroadcastUUID() == null) {
          list.addAll(tasks.values());
          this.tasks.clear();
        }
      }
      resultsReceived(bundle, list);
    } else if (bundle == null) {
      if (debugEnabled) log.debug("processing null bundle for job {}", this);
      if (isCancelled()) {
        final List<Task<?>> list = new ArrayList<>();
        synchronized (tasks) {
          list.addAll(tasks.values());
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
    } else {
      if (bundle.isCancelled()) {
        if (debugEnabled) log.debug("processing cancelled job {}", this);
        final List<Task<?>> list = new ArrayList<>();
        synchronized (tasks) {
          for (final Task<?> task : bundle.getTasksL()) {
            if (taskStateMap.get(task.getPosition()) != TaskState.RESULT) list.add(task);
          }
          list.addAll(tasks.values());
          this.tasks.clear();
        }
        resultsReceived(bundle, list);
      }
      if (bundle.isRequeued()) {
        if (debugEnabled) log.debug("processing requeued job {}", this);
        final List<Task<?>> list = new ArrayList<>();
        synchronized (tasks) {
          for (final Task<?> task : bundle.getTasksL()) {
            if (taskStateMap.get(task.getPosition()) != TaskState.RESULT) list.add(task);
          }
          requeue = merge(list);
          if (debugEnabled) log.debug("requeue = {}, resubmit list = {}", requeue, list.size());
        }
      }
    }
    if (hasPending()) {
      if (debugEnabled) log.debug("processing hasPending for {}", this);
      if (exception != null) setJobStatus(exception instanceof NotSerializableException ? JobStatus.COMPLETE : JobStatus.FAILED);
      if (requeue && onRequeue != null) {
        onRequeue.run();
        updateStatus(NEW, EXECUTING);
      }
    } else {
      if (debugEnabled) log.debug("processing cancelled or done for job {}", this);
      final boolean callDone = updateStatus(isCancelled() ? CANCELLED : EXECUTING, DONE);
      try {
        if (callDone) done();
      } finally {
        if (parentJob != null) parentJob.broadcastCompleted(this);
      }
      setJobStatus(JobStatus.COMPLETE);
    }
    if (debugEnabled) log.debug("finished taskCOmpleted() for {}", this);
  }

  /**
   * Get indicator whether job has pending tasks.
   * @return {@code true} when job has some pending tasks.
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
    if (job != null) job.setStatus(this.jobStatus);
    else if (((jobStatus == JobStatus.COMPLETE) || (jobStatus == JobStatus.FAILED)) && isChildBroadcastJob()) job.fireJobEvent(Type.JOB_END, null, new ArrayList<>(tasks.values()));
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("requesting cancel of jobId=" + this.getUuid());
    if (super.cancel(mayInterruptIfRunning)) {
      job.getCancelledFlag().set(true);
      final List<ClientJob> list;
      Map<ClientTaskBundle, ChannelWrapper> map = null;
      synchronized (bundleMap) {
        list = new ArrayList<>(broadcastSet.size() + broadcastMap.size());
        list.addAll(broadcastMap.values());
        list.addAll(broadcastSet);
        map = new HashMap<>(bundleMap);
      }
      for (final ClientJob broadcastJob : list) broadcastJob.cancel(mayInterruptIfRunning);
      final Set<String> uuids = new HashSet<>();
      for (final Map.Entry<ClientTaskBundle, ChannelWrapper> entry: map.entrySet()) {
        try {
          final ChannelWrapper wrapper = entry.getValue();
          wrapper.cancel(entry.getKey());
          if (!wrapper.isLocal()) {
            final JPPFConnectionPool pool = ((AbstractChannelWrapperRemote) wrapper).getChannel().getConnectionPool();
            final String driverUuid = pool.getDriverUuid();
            if (!uuids.contains(driverUuid)) {
              uuids.add(driverUuid);
              try {
                if (debugEnabled) log.debug("sending cancel request for jobId={} to driver={}", this.getUuid(), driverUuid);
                final JMXDriverConnectionWrapper jmx = pool.getJmxConnection();
                if (jmx != null) jmx.cancelJob(this.getUuid());
              } catch(final Exception e) {
                if (debugEnabled) log.debug(e.getMessage(), e);
                else log.warn(ExceptionUtils.getMessage(e));
              }
            }
          }
        } catch (final Exception e) {
          log.error("Error cancelling job " + this, e);
        }
      }
      final boolean empty;
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
   * @param broadcastJob the dispatched job.
   */
  protected void broadcastDispatched(final ClientJob broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    final boolean empty;
    synchronized (bundleMap) {
      broadcastSet.remove(broadcastJob);
      empty = broadcastMap.isEmpty();
      broadcastMap.put(broadcastJob.getBroadcastUUID(), broadcastJob);
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setJobStatus(JobStatus.EXECUTING);
    }
  }

  /**
   * Called to notify that the execution of broadcast job has completed.
   * @param broadcastJob the completed job.
   */
  protected void broadcastCompleted(final ClientJob broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    final boolean empty;
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
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("uuid=").append(job.getUuid())
      .append(", jobName=").append(job.getName())
      .append(", jobStatus=").append(jobStatus)
      .append(", broadcastUUID=").append(broadcastUUID)
      .append(", nbTasks=").append(tasks.size())
      .append(", taskGraph=").append(taskGraph)
      .append(']').toString();
  }

  /**
   * @return the graph of tasks in the job, if any.
   */
  public TaskGraph getTaskGraph() {
    return taskGraph;
  }

  /**
   * @return {@code true} if there is a graph and an available node in the graph, {@code false} otherwise.
   */
  public boolean hasAvvailableGraphNode() {
    synchronized (tasks) {
      if (taskGraph == null) return false;
      return taskGraph.getAvailableNodes().size() - dispatchedTasks.size() > 0;
    }
  }

  /**
   * @return the number of nodes int he graph that can be executed.
   */
  public int getAvailableGraphNodeCount() {
    synchronized (tasks) {
      return (taskGraph != null) ? taskGraph.getAvailableNodes().size() - dispatchedTasks.size() : -1;
    }
  }
}
