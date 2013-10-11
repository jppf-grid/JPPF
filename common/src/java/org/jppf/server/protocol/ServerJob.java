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
import java.util.concurrent.locks.Lock;

import org.jppf.io.DataLocation;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.Pair;
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
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an <code>ChangeListener</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public ServerJob(final Lock lock, final ServerJobChangeListener notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider) {
    super(lock, notificationEmitter, job, dataProvider);
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ServerJob</code> instance.
   */
  public ServerTaskBundleNode copy(final int nbTasks) {
    JPPFTaskBundle newTaskBundle;
    lock.lock();
    try {
      int taskCount = (nbTasks > this.tasks.size()) ? this.tasks.size() : nbTasks;
      List<ServerTask> subList = this.tasks.subList(0, taskCount);
      try {
        if (job.getCurrentTaskCount() > taskCount) {
          int newSize = job.getCurrentTaskCount() - taskCount;
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
        fireJobUpdated();
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
  @SuppressWarnings("unchecked")
  public void resultsReceived(final ServerTaskBundleNode bundle, final List<DataLocation> results) {
    if (debugEnabled) log.debug("*** received " + results.size() + " results from " + bundle);
    if (results.isEmpty()) return;
    CollectionMap<ServerTaskBundleClient, Pair<Integer, DataLocation>> map = new SetIdentityMap<>();
    lock.lock();
    try {
      List<ServerTask> bundleTasks = (bundle == null) ? new ArrayList<>(tasks) : bundle.getTaskList();
      if (isJobExpired() || isCancelled()) {
        for (ServerTask task : bundleTasks) map.putValue(task.getBundle(), new Pair(task.getPosition(), task.getDataLocation()));
      } else {
        for (int index = 0; index < bundleTasks.size(); index++) {
          ServerTask task = bundleTasks.get(index);
          if (task.getState() == TaskState.TIMEOUT_RESUBMIT) {
            if (debugEnabled) log.debug("task to resubmit: {}", task);
            task.setState(TaskState.PENDING);
          } else {
            DataLocation location = results.get(index);
            if (task.getBundle() != null) map.putValue(task.getBundle(), new Pair(task.getPosition(), location));
          }
        }
      }
    } finally {
      lock.unlock();
    }
    for (Map.Entry<ServerTaskBundleClient, Collection<Pair<Integer, DataLocation>>> entry: map.entrySet()) {
      entry.getKey().resultReceived(entry.getValue());
    }
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ServerTaskBundleNode bundle, final Throwable throwable) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    CollectionMap<ServerTaskBundleClient, ServerTask> map = new SetIdentityMap<>();
    lock.lock();
    try {
      for (ServerTask task : bundle.getTaskList()) map.putValue(task.getBundle(), task);
    } finally {
      lock.unlock();
    }
    for (Map.Entry<ServerTaskBundleClient, Collection<ServerTask>> entry: map.entrySet()) {
      entry.getKey().resultReceived(entry.getValue(), throwable);
    }
  }

  /**
   * Utility method - extract DataLocation from list of server tasks and add them to list.
   * @param dst destination list of <code>DataLocation</code>.
   * @param src source list of <code>ServerTask</code> objects.
   */
  private static void addAll(final List<DataLocation> dst, final List<ServerTask> src) {
    for (ServerTask item : src) dst.add(item.getDataLocation());
  }

  /**
   * Utility method - extract DataLocation from list of server tasks filtered by their state (exclusive) and add them to list.
   * @param dst destination list of <code>DataLocation</code>.
   * @param src source list of <code>ServerTask</code> objects.
   * @param state the state of the server tasks to add.
   */
  private static void addExcluded(final List<DataLocation> dst, final List<ServerTask> src, final TaskState state) {
    for (ServerTask item : src) {
      if (item.getState() != state) dst.add(item.getDataLocation());
    }
  }

  /**
   * Utility method - extract DataLocation from list of server tasks filtered by their state (exclusive) and add them to list.
   * @param dst destination list of <code>DataLocation</code>.
   * @param src source list of <code>ServerTask</code> objects.
   * @param state the first state of the server tasks to exclude.
   * @param otherStates the other states of the server tasks to exclude.
   */
  private static void addExcluded(final List<DataLocation> dst, final List<ServerTask> src, final TaskState state, final TaskState...otherStates) {
    EnumSet<TaskState> set = EnumSet.of(state, otherStates);
    for (ServerTask item : src) {
      if (!set.contains(item.getState())) dst.add(item.getDataLocation());
    }
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param bundle    the completed task.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final ServerTaskBundleNode bundle, final Exception exception) {
    boolean requeue = false;
    List<DataLocation> list = new ArrayList<>();
    lock.lock();
    try {
      if (isCancelled()) handleCancelledStatus();
      if (getSLA().isBroadcastJob()) {
        if (bundle != null) addExcluded(list, bundle.getTaskList(), TaskState.RESULT);
        if (isCancelled() || getBroadcastUUID() == null) addAll(list, this.tasks);
      } else if (bundle == null) {
        if (isCancelled()) addAll(list, this.tasks);
      } else {
        if (bundle.isCancelled()) {
          addExcluded(list, bundle.getTaskList(), TaskState.RESULT);
          addAll(list, this.tasks);
        }
        else {
          List<ServerTask> taskList = new ArrayList<>();
          for (ServerTask task : bundle.getTaskList()) {
            if (task.getState() != TaskState.RESULT) taskList.add(task);
          }
          requeue = merge(taskList, false);
        }
      }
    } finally {
      lock.unlock();
    }
    if (!list.isEmpty()) {
      try {
        resultsReceived(bundle, list);
      } finally {
        this.tasks.clear();
      }
    }
    if (hasPending()) {
      if (exception != null) setSubmissionStatus(SubmissionStatus.FAILED);
      if (requeue && onRequeue != null) onRequeue.run();
    } else {
      setSubmissionStatus(SubmissionStatus.COMPLETE);
      fireTaskCompleted(this);
    }
    if (bundleList.isEmpty() && tasks.isEmpty()) setSubmissionStatus(SubmissionStatus.ENDED);
  }

  /**
   * Perform the necessary actions for when this job has been cancelled.
   */
  private void handleCancelledStatus() {
    List<Future>   futureList;
    synchronized (dispatchSet) {
      futureList = new ArrayList<>(dispatchSet.size());
      for (Map.Entry<Long, ServerTaskBundleNode> entry: dispatchSet.entrySet()) futureList.add(entry.getValue().getFuture());
    }
    for (Future future : futureList) {
      try {
        if (!future.isDone()) future.cancel(false);
      } catch (Exception e) {
        log.error("Error cancelling job " + this, e);
      }
    }
  }

  /**
   * Notifies that execution of this task has completed.
   * @param result the result of the task's execution.
   */
  protected void fireTaskCompleted(final ServerJob result) {
    getJob().fireTaskCompleted(result);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("request to cancel " + this);
    lock.lock();
    try {
      if (super.cancel(mayInterruptIfRunning)) {
        taskCompleted(null, null);
        return true;
      }
      else return false;
    } finally {
      lock.unlock();
    }
  }
}
