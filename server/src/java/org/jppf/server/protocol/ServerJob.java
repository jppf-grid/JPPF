/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.job.management.NodeJobInformation;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 *
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an <code>ChangeListener</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public ServerJob(final Lock lock, final ServerJobChangeListener notificationEmitter, final TaskBundle job, final DataLocation dataProvider) {
    super(lock, notificationEmitter, job, dataProvider);
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ServerJob</code> instance.
   */
  public ServerTaskBundleNode copy(final int nbTasks) {
    final TaskBundle newTaskBundle;
    lock.lock();
    try {
      final int taskCount = (nbTasks > this.tasks.size()) ? this.tasks.size() : nbTasks;
      final List<ServerTask> subList = this.tasks.subList(0, taskCount);
      try {
        if (job.getCurrentTaskCount() > taskCount) {
          final int newSize = job.getCurrentTaskCount() - taskCount;
          newTaskBundle = job.copy();
          newTaskBundle.setTaskCount(taskCount);
          newTaskBundle.setCurrentTaskCount(taskCount);
          job.setCurrentTaskCount(newSize);
        } else {
          newTaskBundle = job.copy();
          job.setCurrentTaskCount(0);
        }
        return new ServerTaskBundleNode(this, newTaskBundle, subList);
      } finally {
        subList.clear();
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
    lock.lock();
    try {
      final List<ServerTask> bundleTasks = (bundle == null) ? new ArrayList<>(tasks) : bundle.getTaskList();
      if (isJobExpired() || isCancelled() || (bundle.isExpired() && bundle.isOffline())) {
        for (final ServerTask task : bundleTasks) map.putValue(task.getBundle(), task);
      } else if (results != null) {
        for (int i=0; i<bundleTasks.size(); i++) {
          final ServerTask task = bundleTasks.get(i);
          if (task.getState() == TaskState.RESUBMIT) {
            if (traceEnabled) log.trace("task to resubmit: {}", task);
            task.setState(TaskState.PENDING);
          } else {
            final DataLocation location = results.get(i);
            task.resultReceived(location);
            map.putValue(task.getBundle(), task);
          }
        }
      } else {
        if (debugEnabled) log.debug("results are null, job is neither expired nor cancelled, node bundle not expired: {}", bundle);
      }
    } finally {
      lock.unlock();
    }
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
      for (final ServerTask task : bundle.getTaskList()) {
        task.resultReceived(throwable);
        map.putValue(task.getBundle(), task);
      }
    } finally {
      lock.unlock();
    }
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
    for (final Map.Entry<ServerTaskBundleClient, Collection<ServerTask>> entry: map.entrySet()) {
      if (throwable == null) entry.getKey().resultReceived(entry.getValue());
      else entry.getKey().resultReceived(entry.getValue(), throwable);
      ((JPPFJobManager) notificationEmitter).jobResultsReceived(bundle.getChannel(), this, entry.getValue());
    }
    taskCompleted(bundle, throwable);
    if (getJob().getParameter(BundleParameter.FROM_PERSISTENCE, false)) {
      for (final Map.Entry<ServerTaskBundleClient, Collection<ServerTask>> entry: map.entrySet()) {
        final ServerTaskBundleClient clientBundle = entry.getKey();
        if (clientBundle.getPendingTasksCount() <= 0) clientBundle.bundleEnded();
      }
    }
  }

  /**
   * Utility method - extract DataLocation from list of server tasks and add them to list.
   * @param dst destination list of <code>DataLocation</code>.
   * @param src source list of <code>ServerTask</code> objects.
   */
  private static void addAll(final List<DataLocation> dst, final List<ServerTask> src) {
    for (final ServerTask item : src) dst.add(item.getInitialTask());
  }

  /**
   * Utility method - extract DataLocation from list of server tasks filtered by their state (exclusive) and add them to list.
   * @param dst destination list of <code>DataLocation</code>.
   * @param src source list of <code>ServerTask</code> objects.
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
   * @param throwable the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final ServerTaskBundleNode bundle, final Throwable throwable) {
    boolean requeue = false;
    final List<DataLocation> list = new ArrayList<>();
    lock.lock();
    try {
      if (getSLA().isBroadcastJob()) {
        if (bundle != null) addExcluded(list, bundle.getTaskList(), TaskState.RESULT);
        if (isCancelled() || getBroadcastUUID() == null) addAll(list, this.tasks);
      } else {
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
    if (hasPending()) {
      if (throwable != null) setSubmissionStatus(SubmissionStatus.FAILED);
      if (!isCancelled() && requeue && (onRequeue != null)) onRequeue.run();
    } else {
      setSubmissionStatus(SubmissionStatus.COMPLETE);
      updateStatus(ServerJobStatus.EXECUTING, ServerJobStatus.DONE);
    }
    if (clientBundles.isEmpty() && tasks.isEmpty()) setSubmissionStatus(SubmissionStatus.ENDED);
    if (debugEnabled) log.debug("submission status = {} for {}", getSubmissionStatus(), this);
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
    for (final Map.Entry<Long, ServerTaskBundleNode> entry: map.entrySet()) {
      try {
        final ServerTaskBundleNode nodeBundle = entry.getValue();
        final Future<?> future = nodeBundle.getFuture();
        if (!future.isDone()) {
          future.cancel(false);
          nodeBundle.resultsReceived((List<DataLocation>) null);
        }
      } catch (final Exception e) {
        log.error("Error cancelling job " + this, e);
      }
    }
  }

  /**
   * Perform the necessary actions for when this job has been cancelled.
   */
  private void handleCancelledTasks() {
    if (debugEnabled) log.debug("cancelling tasks for {}", this);
    final CollectionMap<ServerTaskBundleClient, ServerTask> clientMap = new SetIdentityMap<>();
    for (final ServerTask task: tasks) {
      if (!task.isDone()) {
        task.cancel();
        clientMap.putValue(task.getBundle(), task);
      }
    }
    for (Map.Entry<ServerTaskBundleClient, Collection<ServerTask>> entry: clientMap.entrySet()) {
      entry.getKey().resultReceived(entry.getValue());
    }
  }

  /**
   * Cancel this job.
   * @param mayInterruptIfRunning <code>true</code> if the job may be interrupted.
   * @return <code>true</code> if the job was effectively cncelled, <code>false</code> if it was already cancelled previously.
   */
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("request to cancel {}", this);
    lock.lock();
    try {
      if (setCancelled(mayInterruptIfRunning)) {
        handleCancelledStatus();
        if (!getSLA().isBroadcastJob()) handleCancelledTasks();
        setSubmissionStatus(SubmissionStatus.COMPLETE);
        //taskCompleted(null, null);
        JPPFDriver.getInstance().getNodeNioServer().getNodeReservationHandler().onJobCancelled(this);
        return true;
      }
      else return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @return array of <code>NodeManagementInfo</code> instances.
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
   * @param sla the SLA to update with.
   * @param metadata the metadata to update with.
   */
  public void update(final JobSLA sla, final JobMetadata metadata) {
    if (debugEnabled) log.debug("request to update {}", this);
    boolean updated = false;
    lock.lock();
    try {
      if (sla != null) {
        this.sla = sla;
        job.setSLA(sla);
        JPPFDriver.getInstance().getQueue().updateSchedules(this);
        updated = true;
      }
      if (metadata != null) {
        this.metadata = metadata;
        job.setMetadata(metadata);
        updated = true;
      }
    } finally {
      lock.unlock();
    }
    if (updated) fireJobUpdated(true);
  }
}
