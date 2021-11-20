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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.execute.ExecutorChannel;
import org.jppf.io.DataLocation;
import org.jppf.job.JobReturnReason;
import org.jppf.node.protocol.*;
import org.jppf.node.protocol.graph.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Instances of this class group tasks for the same node channel together.
 * @author Martin JANDA
 * @exclude
 */
public class ServerTaskBundleNode {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerTaskBundleNode.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
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
  private boolean requeued;
  /**
   * Job cancel indicator.
   */
  private boolean cancelled;
  /**
   * Job dispatch expiration indicator.
   */
  private boolean expired;
  /**
   * The job this submission is for.
   */
  private TaskBundle taskBundle;
  /**
   * Channel to which is this bundle dispatched.
   */
  private ExecutorChannel<?> channel;
  /**
   * The future from channel dispatch.
   */
  private Future<?> future;
  /**
   * The number of tasks in this node bundle.
   */
  private final int taskCount;
  /**
   * The reason why the task results are reeived.
   */
  private JobReturnReason jobReturnReason;
  /**
   * Offline node indicator.
   */
  private boolean offline;
  /**
   * The time at which this dispatch started.
   */
  private final long dispatchStartTime;
  /**
   * Info on the dependenencies of the tasks in this bundle.
   */
  private TaskGraphInfo graphInfo;

  /**
   * Initialize this task bundle and set its build number.
   * @param job the job to execute.
   * @param taskBundle the job.
   * @param taskList the tasks to execute.
   */
  public ServerTaskBundleNode(final ServerJob job, final TaskBundle taskBundle, final Collection<ServerTask> taskList) {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (taskBundle == null) throw new IllegalArgumentException("taskBundle is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");

    this.job = job;
    this.taskBundle = taskBundle;
    this.taskList = new ArrayList<>(taskList);
    final int size = this.taskList.size();
    this.taskBundle.setTaskCount(size);
    this.taskBundle.setCurrentTaskCount(size);
    this.dataProvider = job.getDataProvider();
    this.taskCount = size;
    this.dispatchStartTime = System.currentTimeMillis();
    this.taskBundle.setParameter("node.bundle.id", this.id);
    checkTaskCount();
    resolveDependencies();
  }

  /**
   * Get the task bundle this node bundle is for.
   * @return a {@link TaskBundle} instance.
   */
  public TaskBundle getJob() {
    return taskBundle;
  }

  /**
   * Get the job this node bundle is a dispatch of.
   * @return a {@link ServerJob} instance.
   */
  public ServerJob getServerJob() {
    return job;
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
   * Set the node channel onto this job dispatch.
   * @param channel the node to which the job is dispatched.
   */
  public void setChannel(final ExecutorChannel<?> channel) {
    if (channel == null) throw new IllegalArgumentException("channel is null for " + this);
    this.channel = channel;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param channel the node to which the job is dispatched.
   * @param future future assigned to bundle execution.
   */
  public void jobDispatched(final ExecutorChannel<?> channel, final Future<?> future) {
    if (channel == null) throw new IllegalArgumentException("channel is null for " + this);
    if (future == null) throw new IllegalArgumentException("future is null for " + this);
    this.channel = channel;
    this.future = future;
    job.jobDispatched(this);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final List<DataLocation> results) {
    taskCompleted(null);
    job.resultsReceived(this, results);
    this.channel = null;
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final Throwable throwable) {
    job.resultsReceived(this, throwable);
    taskCompleted(throwable);
    this.channel = null;
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final Throwable exception) {
    if (debugEnabled && (exception != null)) log.debug("received exception for {} :\n{}\ncall stack:\n{}", this, ExceptionUtils.getStackTrace(exception), ExceptionUtils.getCallStack());
    try {
      job.jobReturned(this);
    } finally {
      this.future = null;
    }
  }

  /**
   * Called when this task bundle should be resubmitted
   */
  public void resubmit() {
    if (getJob().getSLA().isBroadcastJob()) return; // broadcast jobs cannot be resubmitted.
    synchronized (this) {
      requeued = true;
      for (final ServerTask task: taskList) task.resubmit();
    }
  }

  /**
   * Called when this task bundle should be resubmitted
   */
  public void expire() {
    if (getJob().getSLA().isBroadcastJob()) return; // broadcast jobs cannot expire.
    final int max = job.getSLA().getMaxDispatchExpirations();
    synchronized (this) {
      for (final ServerTask task: taskList) {
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
  public ExecutorChannel<?> getChannel() {
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
  public int getTaskCount() {
    return taskCount;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("id=").append(id);
    sb.append(", name=").append(job.getName());
    sb.append(", uuid=").append(job.getUuid());
    sb.append(", initialTaskCount=").append(job.getInitialTaskCount());
    sb.append(", taskCount=").append(taskCount);
    sb.append(", cancelled=").append(cancelled);
    sb.append(", requeued=").append(requeued);
    sb.append(", dependencies=").append(graphInfo == null ? 0 : graphInfo.getNbDependencies());
    sb.append(", channel=").append(channel);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final ServerTaskBundleNode other = (ServerTaskBundleNode) obj;
    return id == other.id;
  }

  /**
   * Get the unique id for this node bundle.
   * @return the id as a long value.
   */
  public long getId() {
    return id;
  }

  /**
   * Build a unique key for the specified node task bundle.
   * @param bundle a {@link ServerTaskBundleNode} instance.
   * @return a unique key as a string.
   */
  public static String makeKey(final ServerTaskBundleNode bundle) {
    return makeKey(bundle.getJob().getUuid(), bundle.getId());
  }

  /**
   * Build a unique key for a node task bundle with the specified job uuid and node id.
   * @param jobUuid the uuid of the job to which the bundle belongs.
   * @param bundleId the id of the bundle.
   * @return a unique key as a string.
   */
  public static String makeKey(final String jobUuid, final long bundleId) {
    return new StringBuilder(jobUuid).append('|').append(bundleId).toString();
  }

  /**
   * Get the reason why the task results are reeived.
   * @return a {@link JobReturnReason} enum value.
   */
  public JobReturnReason getJobReturnReason() {
    return jobReturnReason;
  }

  /**
   * Set the reason why the task results are reeived.
   * @param jobReturnReason a {@link JobReturnReason} enum value.
   */
  public void setJobReturnReason(final JobReturnReason jobReturnReason) {
    this.jobReturnReason = jobReturnReason;
  }

  /**
   * Get the offline node indicator.
   * @return {@code true} if the corresponding node is offline, {@code false} otherwise.
   */
  public boolean isOffline() {
    return offline;
  }

  /**
   * Set the offline node indicator.
   * @param offline {@code true} if the corresponding node is offline, {@code false} otherwise.
   */
  public void setOffline(final boolean offline) {
    this.offline = offline;
  }

  /**
   * @return the time at which this dispatch started.
   */
  public long getDispatchStartTime() {
    return dispatchStartTime;
  }

  /**
   * Find and capture a canonical set of direct dependencies for all the tasks in this dispatch bundle.
   */
  private void resolveDependencies() {
    final TaskGraph graph = job.getTaskGraph();
    job.lock.lock();
    try {
      final Set<Integer> depsPositions = new HashSet<>();
      final Set<ServerTask> dependencies = new HashSet<>();
      final CollectionMap<Integer, Integer> dependenciesMap = new ArrayListHashMap<>();
      for (final ServerTask task: taskList) {
        if (graph != null) { 
          final TaskGraph.Node node = graph.nodeAt(task.getPosition());
          if (node == null) continue;
          if (traceEnabled) log.trace("found node in graph for {}", task);
          final List<TaskGraph.Node> deps = node.getDependencies();
          if ((deps != null) && !deps.isEmpty()) {
            for (final TaskGraph.Node dep: deps) {
              ServerTask depTask = job.tasks.get(dep.getPosition());
              if (depTask == null) depTask = job.dependendedOnTasks.get(dep.getPosition());
              if (depTask != null) {
                dependencies.add(depTask);
                dependenciesMap.putValue(task.getPosition(), dep.getPosition());
              }
            }
          }
        } else {
          final ServerTaskBundleClient clientBundle = task.getBundle();
          final TaskGraphInfo clientGraphInfo = clientBundle.getTaskGraphInfo();
          if (clientGraphInfo != null) {
            if (traceEnabled) log.trace("found graph info for {}", task);
            final Collection<Integer> positions = clientGraphInfo.getDependenciesMap().getValues(task.getPosition());
            if (positions != null) {
              for (final int position: positions) {
                final ServerTask dep = (ServerTask) clientGraphInfo.getDependencyAt(position);
                if (dep != null) {
                  dependencies.add(dep);
                  depsPositions.add(position);
                  dependenciesMap.putValue(task.getPosition(), dep.getPosition());
                }
              }
            }
          } else {
            if (traceEnabled) log.trace("no graph info found for {}", task);
          }
        }
      }
      if (!dependencies.isEmpty()) {
        final List<ServerTask> depsList = new ArrayList<>(dependencies);
        final int[] positions = new int[depsList.size()];
        int count = 0;
        for (final ServerTask task: depsList) positions[count++] = task.getPosition();
        graphInfo = new TaskGraphInfo(depsList.size(), dependenciesMap, positions);
        graphInfo.setDependencies(depsList);
        if (debugEnabled) log.debug("there are {} dependencies in {}", graphInfo.getNbDependencies(), this);
      } else {
        if (debugEnabled) log.debug("there are no dependencies in {}", this);
      }
    } finally {
      job.lock.unlock();
    }
  }

  /**
   * Get the dependencies of the tasks in this bundle.
   * @return the list of dependencies.
   */
  public TaskGraphInfo getTaskGraphInfo() {
    return graphInfo;
  }
}
