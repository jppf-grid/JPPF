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
import java.util.concurrent.locks.Lock;

import org.jppf.io.DataLocation;
import org.jppf.job.JobInformation;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.*;
import org.jppf.node.protocol.graph.TaskGraph;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.job.management.NodeJobInformation;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Server-side representation of a job.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class ServerJob extends AbstractServerJobBase {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerJob.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
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
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an {@code ChangeListener} instance that fires job notifications.
   * @param job underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public ServerJob(final Lock lock, final ServerJobChangeListener notificationEmitter, final TaskBundle job, final DataLocation dataProvider) {
    super(lock, notificationEmitter, job, dataProvider);
    taskGraph = job.removeParameter(BundleParameter.JOB_TASK_GRAPH);
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new {@code ServerJob} instance.
   */
  public ServerTaskBundleNode createNodeDispatch(final int nbTasks) {
    lock.lock();
    try {
      Collection<ServerTask> list = null;
      try {
        if (taskGraph == null) {
          if ((nbTasks >= tasks.size()) || (taskGraph != null)) {
            list = new ArrayList<>(tasks.values());
          } else {
            list = new ArrayList<>(nbTasks);
            final Iterator<Map.Entry<Integer, ServerTask>> it = tasks.entrySet().iterator();
            for (int i=0; i<nbTasks; i++) {
              final Map.Entry<Integer, ServerTask> entry = it.next();
              list.add(entry.getValue());
            }
          }
          if (debugEnabled) log.debug("requested tasks={}, found tasks={}", nbTasks, list.size());
        } else {
          final Set<Integer> availablePos = taskGraph.getAvailableNodes();
          final int effectiveNbTasks = Math.min(nbTasks, availablePos.size());
          final Iterator<Integer> it = availablePos.iterator();
          list = new ArrayList<>(effectiveNbTasks);
          int count = 0;
          while (it.hasNext() && (count < nbTasks)) {
            final int pos = it.next();
            if (!dispatchedTasks.contains(pos)) {
              dispatchedTasks.add(pos);
              count++;
              list.add(tasks.get(pos));
            }
          }
          if (debugEnabled) log.debug("count={}, nbTasks={}, effectiveNbTasks={}, dispatchedTasks={} for {}", count, nbTasks, effectiveNbTasks, dispatchedTasks.size(), this);
        }
        if (list.isEmpty() && !getJob().isHandshake()) throw new IllegalStateException("list of tasks to dispatch is empty");
        final TaskBundle newTaskBundle;
        final int taskCount = list.size();
        if (job.getCurrentTaskCount() > taskCount) {
          final int newSize = job.getCurrentTaskCount() - taskCount;
          newTaskBundle = job.copy();
          newTaskBundle.removeParameter(BundleParameter.JOB_TASK_GRAPH);
          newTaskBundle.setTaskCount(taskCount);
          newTaskBundle.setCurrentTaskCount(taskCount);
          job.setCurrentTaskCount(newSize);
        } else {
          newTaskBundle = job.copy();
          job.setCurrentTaskCount(0);
        }
        if (!job.isHandshake() && getSLA().getDependencySpec().getId() != null) newTaskBundle.setParameter(BundleParameter.JOB_GRAPH_ALREADY_HANDLED, true);
        return new ServerTaskBundleNode(this, newTaskBundle, list);
      } finally {
        if (list != null) {
          for (final ServerTask task: list) {
            tasks.remove(task.getPosition());
            if ((taskGraph != null) && taskGraph.isDependendOn(task.getPosition())) this.dependendedOnTasks.put(task.getPosition(), task);
          }
        }
        fireJobUpdated(false);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ServerTaskBundleNode bundle, final List<DataLocation> results) {
    if (debugEnabled) log.debug("received {} results from {}", (results == null ? "null" : results.size()), bundle);
    if ((results != null) && results.isEmpty()) return;
    final CollectionMap<ServerTaskBundleClient, ServerTask> map = new SetIdentityMap<>();
    final List<ServerTask> bundleTasks;
    final boolean b;
    lock.lock();
    try {
      bundleTasks = (bundle == null) ? new ArrayList<>(tasks.values()) : bundle.getTaskList();
      b = isJobExpired() || isCancelled() || (bundle.isExpired() && bundle.isOffline());
      if (b) {
        for (final ServerTask task : bundleTasks) map.putValue(task.getBundle(), task);
      } else if (results != null) {
        int nbResubmits = 0, maxPos = 0, minPos = Integer.MAX_VALUE;
        for (int i=0; i<bundleTasks.size(); i++) {
          final ServerTask task = bundleTasks.get(i);
          final int pos = task.getPosition();
          if (task.getState() == TaskState.RESUBMIT) {
            if (traceEnabled) log.trace("task to resubmit: {}", task);
            task.setState(TaskState.PENDING);
            task.setReturnedFromNode(false);
            nbResubmits++;
            if (pos > maxPos) maxPos = pos;
            if (pos < minPos) minPos = pos;
          } else {
            if (taskGraph != null) {
              dispatchedTasks.remove(pos);
              taskGraph.nodeDone(pos);
            }
            final DataLocation location = results.get(i);
            task.resultReceived(location);
            map.putValue(task.getBundle(), task);
          }
        }
        if (debugEnabled && (nbResubmits > 0)) log.debug("got {} tasks to resubmit with minPos={}, maxPos={} for {}", nbResubmits, minPos, maxPos, this); 
      } else {
        if (debugEnabled) log.debug("results are null, job is neither expired nor cancelled, node bundle not expired: {}", bundle);
      }
    } finally {
      lock.unlock();
    }
    if (debugEnabled && (taskGraph != null)) log.debug("taskGraph = {}, sentTasks = {}", taskGraph, dispatchedTasks);
    postResultsReceived(map, bundle, null);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ServerTaskBundleNode bundle, final Throwable throwable) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (debugEnabled) log.debug("*** received exception '{}' from {}", ExceptionUtils.getMessage(throwable), bundle);
    final CollectionMap<ServerTaskBundleClient, ServerTask> map = new SetIdentityMap<>();
    lock.lock();
    try {
      int nbResubmits = 0, maxPos = 0, minPos = Integer.MAX_VALUE;
      for (final ServerTask task : bundle.getTaskList()) {
        final int pos = task.getPosition();
        if (task.getState() == TaskState.RESUBMIT) {
          if (traceEnabled) log.trace("task to resubmit: {}", task);
          task.setState(TaskState.PENDING);
          task.setReturnedFromNode(false);
          nbResubmits++;
          if (pos > maxPos) maxPos = pos;
          if (pos < minPos) minPos = pos;
        } else {
          if (taskGraph != null) {
            dispatchedTasks.remove(pos);
            taskGraph.nodeDone(pos);
          }
          task.resultReceived(throwable);
          if (taskGraph != null) taskGraph.nodeDone(pos);
          map.putValue(task.getBundle(), task);
        }
      }
      if (debugEnabled && (nbResubmits > 0)) log.debug("got {} tasks to resubmit with minPos={}, maxPos={} for {}", nbResubmits, minPos, maxPos, this); 
    } finally {
      lock.unlock();
    }
    if (debugEnabled && (taskGraph != null)) log.debug("taskGraph = {}, sentTasks = {}", taskGraph, dispatchedTasks);
    postResultsReceived(map, bundle, throwable);
  }

  /**
   * 
   * @param map .
   * @param bundle .
   * @param throwable .
   */
  private void postResultsReceived(final CollectionMap<ServerTaskBundleClient, ServerTask> map, final ServerTaskBundleNode bundle, final Throwable throwable) {
    if (debugEnabled) log.debug("client bundle map has {} keys: {}", map.keySet().size(), map.keySet());
    map.forEach((clientBundle, tasks) -> {
      if (throwable == null) clientBundle.resultReceived(tasks);
      else clientBundle.resultReceived(tasks, throwable);
      ((JPPFJobManager) notificationEmitter).jobResultsReceived(bundle.getChannel(), this, tasks);
      if (debugEnabled) log.debug("received results for {}", clientBundle);
    });
    taskCompleted(bundle, throwable);
    if (getJob().getParameter(BundleParameter.FROM_PERSISTENCE, false) || submissionStatus.get() == SubmissionStatus.COMPLETE) {
      map.forEach((clientBundle, tasks) -> {
        if (debugEnabled) log.debug("checking bundleEnded() for {}", clientBundle);
        if (clientBundle.getPendingTasksCount() <= 0) clientBundle.bundleEnded();
      });
    }
  }

  /**
   * Utility method - extract DataLocation from list of server tasks and add them to list.
   * @param dst destination list of {@code DataLocation}.
   * @param src source list of {@code ServerTask} objects.
   */
  private static void addAll(final List<DataLocation> dst, final Collection<ServerTask> src) {
    for (final ServerTask item : src) dst.add(item.getInitialTask());
  }

  /**
   * Utility method - extract DataLocation from list of server tasks filtered by their state (exclusive) and add them to list.
   * @param dst destination list of {@code DataLocation}.
   * @param src source list of {@code ServerTask} objects.
   * @param state the state of the server tasks to add.
   */
  private static void addExcluded(final List<DataLocation> dst, final List<ServerTask> src, final TaskState state) {
    for (final ServerTask item : src) {
      if (item.getState() != state) dst.add(item.getInitialTask());
    }
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param bundle    the completed task.
   * @param throwable the {@link Exception} thrown during job execution or {@code null}.
   */
  public void taskCompleted(final ServerTaskBundleNode bundle, final Throwable throwable) {
    boolean requeue = false;
    final List<DataLocation> list = new ArrayList<>();
    lock.lock();
    try {
      if (getSLA().isBroadcastJob()) {
        if (debugEnabled) log.debug("processing broadcast job");
        if (bundle != null) addExcluded(list, bundle.getTaskList(), TaskState.RESULT);
        if (isCancelled() || getBroadcastUUID() == null) addAll(list, this.tasks.values());
      } else if (bundle != null) {
        if (debugEnabled) log.debug("processing pending tasks");
        final List<ServerTask> taskList = new ArrayList<>();
        for (final ServerTask task : bundle.getTaskList()) {
          if (task.getState() == TaskState.RESUBMIT) task.setState(TaskState.PENDING);
          if (task.getState() == TaskState.PENDING) taskList.add(task);
        }
        requeue = merge(taskList, false);
      }
    } finally {
      lock.unlock();
    }
    if (debugEnabled) log.debug("requeue = {} for bundle {}, job = {}", requeue, bundle, this);
    if (hasPending()) {
      if (debugEnabled) log.debug("processing hasPending=true");
      if ((throwable != null) && !requeue) setSubmissionStatus(SubmissionStatus.FAILED);
      if (!isCancelled() && requeue && (onRequeue != null)) onRequeue.run();
    } else {
      if (debugEnabled) log.debug("processing hasPending=false");
      setSubmissionStatus(SubmissionStatus.COMPLETE);
      updateStatus(ServerJobStatus.EXECUTING, ServerJobStatus.DONE);
    }
    if (clientBundles.isEmpty() && tasks.isEmpty()) setSubmissionStatus(SubmissionStatus.ENDED);
    if (debugEnabled) log.debug("submissionStatus={}, clientBundles={} for {}", getSubmissionStatus(), clientBundles.size(), this);
  }

  /**
   * Perform the necessary actions for when this job has been cancelled.
   */
  private void handleCancelledStatus() {
    final Map<Long, ServerTaskBundleNode> map;
    synchronized (dispatchSet) {
      map = new HashMap<>(dispatchSet);
    }
    if (debugEnabled) log.debug("cancelling {} dispatches for {}", map.size(), this);
    map.forEach((id, nodeBundle) -> cancelDispatch(nodeBundle));
  }

  /**
   * Cancel the specified job dispatch.
   * @param nodeBundle the dispatch to cancel.
   */
  public void cancelDispatch(final ServerTaskBundleNode nodeBundle) {
    try {
      final Future<?> future = nodeBundle.getFuture();
      if ((future != null) && !future.isDone()) future.cancel(false);
      nodeBundle.resultsReceived((List<DataLocation>) null);
    } catch (final Exception e) {
      log.error("Error cancelling job " + this, e);
    }
  }

  /**
   * Perform the necessary actions for when this job has been cancelled.
   * @return a mapping of client bundles to the tasks that belong to them and were cacelled.
   */
  private CollectionMap<ServerTaskBundleClient, ServerTask> handleCancelledTasks() {
    if (debugEnabled) log.debug("cancelling tasks for {}", this);
    final CollectionMap<ServerTaskBundleClient, ServerTask> clientMap = new SetIdentityMap<>();
    for (final ServerTask task: tasks.values()) {
      if (!task.isDone() && !task.isReturnedFromNode()) {
        task.cancel();
        clientMap.putValue(task.getBundle(), task);
      }
    }
    return clientMap;
  }

  /**
   * Cancel this job.
   * @param driver reference to the JPPF driver.
   * @param mayInterruptIfRunning {@code true} if the job may be interrupted.
   * @return {@code true} if the job was effectively cncelled, {@code false} if it was already cancelled previously.
   */
  public boolean cancel(final JPPFDriver driver, final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("request to cancel {}", this);
    boolean result = false;
    CollectionMap<ServerTaskBundleClient, ServerTask> clientMap = null;
    lock.lock();
    try {
      if (setCancelled(mayInterruptIfRunning)) {
        driver.getQueue().getDependenciesHandler().jobCancelled(this);
        handleCancelledStatus();
        if (!getSLA().isBroadcastJob()) clientMap = handleCancelledTasks();
        setSubmissionStatus(SubmissionStatus.COMPLETE);
        driver.getAsyncNodeNioServer().getNodeReservationHandler().onJobCancelled(this);
        result = true;
      }
    } finally {
      lock.unlock();
    }
    if (clientMap != null) clientMap.forEach((clientBundle, tasks) -> clientBundle.resultReceived(tasks));
    if (result) setSubmissionStatus(SubmissionStatus.ENDED);
    return result;
  }

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @return array of {@code NodeManagementInfo} instances.
   */
  public NodeJobInformation[] getNodeJobInformation() {
    final ServerTaskBundleNode[] entries;
    synchronized (dispatchSet) {
      if (dispatchSet.isEmpty()) return NodeJobInformation.EMPTY_ARRAY;
      entries = dispatchSet.values().toArray(new ServerTaskBundleNode[dispatchSet.size()]);
    }
    final NodeJobInformation[] result = new NodeJobInformation[entries.length];
    int i = 0;
    for (final ServerTaskBundleNode nodeBundle : entries) {
      final JPPFManagementInfo nodeInfo = nodeBundle.getChannel().getManagementInfo();
      final TaskBundle bundle = nodeBundle.getJob();
      final JobInformation jobInfo = new JobInformation(bundle);
      jobInfo.setMaxNodes(bundle.getSLA().getMaxNodes());
      result[i++] = new NodeJobInformation(nodeInfo, jobInfo);
    }
    return result;
  }

  /**
   * Update this job with the specified sla and metadata.
   * @param driver reference to the JPPF driver.
   * @param sla the SLA to update with.
   * @param metadata the metadata to update with.
   */
  public void update(final JPPFDriver driver, final JobSLA sla, final JobMetadata metadata) {
    if (debugEnabled) log.debug("request to update {}", this);
    boolean updated = false;
    lock.lock();
    try {
      if (sla != null) {
        job.setSLA(sla);
        driver.getQueue().updateSchedules(this);
        updated = true;
      }
      if (metadata != null) {
        job.setMetadata(metadata);
        updated = true;
      }
    } finally {
      lock.unlock();
    }
    if (updated) fireJobUpdated(true);
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
  public boolean hasAvailableGraphNode() {
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
