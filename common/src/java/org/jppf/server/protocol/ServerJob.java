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
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.io.DataLocation;
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.server.job.management.NodeJobInformation;
import org.jppf.server.protocol.utils.AbstractServerJob;
import org.jppf.server.submission.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class ServerJob extends AbstractServerJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerJob.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The list of the tasks.
   */
  private final List<ServerTask> tasks = new ArrayList<ServerTask>();
  /**
   * The list of the incoming bundles.
   */
  private final List<ServerTaskBundleClient> bundleList = new ArrayList<ServerTaskBundleClient>();
  /**
   * Listener for handling completed bundles.
   */
  private final ServerTaskBundleClient.CompletionListener bundleCompletionListener = new ServerTaskBundleClient.CompletionListener() {
    @Override
    public void taskCompleted(final ServerTaskBundleClient bundle, final List<ServerTask> results) {
      if (bundle == null) throw new IllegalArgumentException("bundlerWrapper is null");
      if (bundle.isCancelled()) cancel(false);
    }

    @Override
    public void bundleDone(final ServerTaskBundleClient bundle) {
      if (bundle == null) throw new IllegalArgumentException("bundlerWrapper is null");
      lock.lock();
      try {
        bundle.removeCompletionListener(this);
        bundleList.remove(bundle);
        tasks.removeAll(bundle.getTaskList());
      } finally {
        lock.unlock();
      }
    }
  };

  /**
   * Set of all dispatched bundles in this job.
   */
  private final Set<ServerTaskBundleNode> dispatchSet = new LinkedHashSet<ServerTaskBundleNode>();
  /**
   * The status of this submission.
   */
  private SubmissionStatus submissionStatus;
  /**
   * The listener that receives notifications of completed tasks.
   */
  private Object resultsListener = null;
  /**
   * The requeue handler.
   */
  private Runnable onRequeue = null;
  /**
   * The data location of the data provider.
   */
  private final DataLocation dataProvider;
  /**
   * Handler for job state notifications.
   */
  protected final JobNotificationEmitter notificationEmitter;
  /**
   * Used for synchronized access to job.
   */
  protected final ReentrantLock lock;
  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an <code>JobNotificationEmitter</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public ServerJob(final ReentrantLock lock, final JobNotificationEmitter notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider) {
    super(job);
    if (lock == null) throw new IllegalArgumentException("lock is null");

    this.lock = lock;
    this.notificationEmitter = notificationEmitter;
    this.dataProvider = dataProvider;
    this.submissionStatus = SubmissionStatus.SUBMITTED;
  }

  /**
   * Get list of bundles received from client.
   * @return list of bundles received from client.
   */
  protected List<ServerTaskBundleClient> getBundleList() {
    lock.lock();
    try {
      return new ArrayList<ServerTaskBundleClient>(bundleList);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the location for data shared between tasks.
   * @return a <code>DataLocation</code> instance.
   */
  public DataLocation getDataProvider() {
    return dataProvider;
  }

  @Override
  public int getTaskCount() {
    lock.lock();
    try {
      return tasks.size();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ServerJob</code> instance.
   */
  public ServerTaskBundleNode copy(final int nbTasks) {
    JPPFTaskBundle taskBundle = getJob();
    lock.lock();
    try {
      int taskCount;
      if (nbTasks > this.tasks.size()) taskCount = this.tasks.size();
      else taskCount = nbTasks;
      List<ServerTask> subList = this.tasks.subList(0, taskCount);
      try {
        if (taskBundle.getTaskCount() != taskCount) {
          int newSize = taskBundle.getCurrentTaskCount() - taskCount;
          taskBundle = taskBundle.copy();
          taskBundle.setTaskCount(taskCount);
          getJob().setCurrentTaskCount(newSize);
        }
        return new ServerTaskBundleNode(this, taskBundle, subList);
      } finally {
        subList.clear();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @param after determines whether the tasks from other should be added first or last.
   * @return <code>true</code> when this client job needs to be requeued.
   */
  protected boolean merge(final List<ServerTask> taskList, final boolean after) {
    lock.lock();
    try {
      boolean requeue = this.tasks.isEmpty() && !taskList.isEmpty();
      if (!after) this.tasks.addAll(0, taskList);
      if (after) this.tasks.addAll(taskList);
      return requeue;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the listener that receives notifications of completed tasks.
   * @return a <code>TaskCompletionListener</code> instance.
   */
  public Object getResultListener() {
    return resultsListener;
  }

  /**
   * Set the listener that receives notifications of completed tasks.
   * @param resultsListener a <code>TaskCompletionListener</code> instance.
   */
  public void setResultListener(final Object resultsListener) {
    this.resultsListener = resultsListener;
  }

  /**
   * Get the broadcast UUID.
   * @return an <code>String</code> instance.
   */
  public String getBroadcastUUID() {
    return null;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param bundle  the dispatched job.
   */
  public void jobDispatched(final ServerTaskBundleNode bundle) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    boolean empty;
    synchronized (dispatchSet) {
      empty = dispatchSet.isEmpty();
      dispatchSet.add(bundle);
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
    }
    fireJobDispatched(bundle.getChannel(), bundle);
  }

  /**
   * Called when all or part of a job is returned from node.
   * @param bundle  the returned job.
   */
  public void jobReturned(final ServerTaskBundleNode bundle) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    synchronized (dispatchSet) {
      dispatchSet.remove(bundle);
    }
    fireJobReturned(bundle.getChannel(), bundle.getJob());
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ServerTaskBundleNode bundle, final List<DataLocation> results) {
    if (results.isEmpty()) return;
    lock.lock();
    try {
      List<ServerTask> bundleTasks =  bundle == null ? new ArrayList<ServerTask>(tasks) : bundle.getTaskList();
      if (isJobExpired() || isCancelled()) {
        for (ServerTask location : bundleTasks) {
          location.getBundle().resultReceived(location.getPosition(), location.getDataLocation());
        }
      } else {
        for (int index = 0; index < bundleTasks.size(); index++) {
          ServerTask location = bundleTasks.get(index);
          DataLocation task = results.get(index);
          location.getBundle().resultReceived(location.getPosition(), task);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ServerTaskBundleNode bundle, final Throwable throwable) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    lock.lock();
    try {
      for (ServerTask task : bundle.getTaskList()) {
        task.getBundle().resultReceived(task.getPosition(), throwable);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Utility method - extract DataLocation from list of server tasks and add them to list.
   * @param dst destination list of <code>DataLocation</code>.
   * @param src source list of <code>ServerTask</code> objects.
   */
  private static void addAll(final List<DataLocation> dst, final List<ServerTask> src) {
    for (ServerTask item : src) {
      dst.add(item.getDataLocation());
    }
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param bundle    the completed task.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final ServerTaskBundleNode bundle, final Exception exception) {
    lock.lock();
    try {
      if (isCancelled()) {
        List<Future>   futureList;
        synchronized (dispatchSet) {
          futureList = new ArrayList<Future>(dispatchSet.size());
          for (ServerTaskBundleNode item : dispatchSet) {
            futureList.add(item.getFuture());
          }
        }
        for (Future future : futureList) {
          try {
            if (!future.isDone()) future.cancel(false);
          } catch (Exception e) {
            log.error("Error cancelling job " + this, e);
          }
        }
      }
      boolean requeue = false;
      List<DataLocation> list = new ArrayList<DataLocation>();
      if (getSLA().isBroadcastJob()) {
        if (bundle != null) {
          for (ServerTask task : bundle.getTaskList()) {
            if (task.getState() != ServerTask.State.RESULT) list.add(task.getDataLocation());
          }
        }
        if (isCancelled() || getBroadcastUUID() == null) addAll(list, this.tasks);
      } else if (bundle == null) {
        if (isCancelled()) {
          addAll(list, this.tasks);
        }
      } else {
        if (bundle.isCancelled()) {
          for (ServerTask task : bundle.getTaskList()) {
            if (task.getState() != ServerTask.State.RESULT) list.add(task.getDataLocation());
          }
          addAll(list, this.tasks);
        }
        if (bundle.isRequeued()) {
          List<ServerTask> taskList = new ArrayList<ServerTask>();
          for (ServerTask task : bundle.getTaskList()) {
            if (task.getState() != ServerTask.State.RESULT) taskList.add(task);
          }
          requeue = merge(taskList, false);
        }
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
        boolean callDone = updateStatus(EXECUTING, DONE);
        setSubmissionStatus(SubmissionStatus.COMPLETE);
        try {
          if (callDone) done();
        } finally {
          fireTaskCompleted(this);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Notifies that execution of this task has completed.
   * @param result the result of the task's execution.
   */
  protected void fireTaskCompleted(final ServerJob result) {
    getJob().fireTaskCompleted(result);
  }

  /**
   * Get indicator whether job has pending tasks.
   * @return <code>true</code> when job has some pending tasks.
   */
  protected boolean hasPending() {
    lock.lock();
    try {
      for (ServerTaskBundleClient bundle : bundleList) {
        if (bundle.getPendingTasksCount() > 0) return true;
      }

      if (!tasks.isEmpty()) System.out.println("ServerJob.hasPending: " + tasks.size());
    } finally {
      lock.unlock();
    }
    return false;
  }

  /**
   * Get the status of this submission.
   * @return a {@link SubmissionStatus} enumerated value.
   */
  public SubmissionStatus getSubmissionStatus() {
    lock.lock();
    try {
      return submissionStatus;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Set the status of this submission.
   * @param submissionStatus a {@link SubmissionStatus} enumerated value.
   */
  public void setSubmissionStatus(final SubmissionStatus submissionStatus) {
    lock.lock();
    try {
      if (this.submissionStatus == submissionStatus) return;
      this.submissionStatus = submissionStatus;
    } finally {
      lock.unlock();
    }
    if (resultsListener instanceof SubmissionStatusHandler) ((SubmissionStatusHandler) resultsListener).setStatus(this.submissionStatus);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("request to cancel " + this);
    lock.lock();
    try {
      if (super.cancel(mayInterruptIfRunning)) {
        done();
        taskCompleted(null, null);
        return true;
      }
      else return false;
    } finally {
      lock.unlock();
    }
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
    synchronized (dispatchSet)
    {
      return dispatchSet.size();
    }
  }

  @Override
  protected void fireJobNotification(final JobNotification event) {
    if (event == null) throw new IllegalArgumentException("event is null");
    if (notificationEmitter != null) notificationEmitter.fireJobEvent(event);
  }

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @return array of <code>NodeManagementInfo</code> instances.
   */
  @SuppressWarnings("unchecked")
  public NodeJobInformation[] getNodeJobInformation() {
    ServerTaskBundleNode[] entries;
    synchronized (dispatchSet) {
      entries = dispatchSet.toArray(new ServerTaskBundleNode[dispatchSet.size()]);
    }
    if (entries.length == 0) return NodeJobInformation.EMPTY_ARRAY;

    NodeJobInformation[] result = new NodeJobInformation[entries.length];
    int i = 0;
    for (ServerTaskBundleNode bundle : entries) {
      JPPFManagementInfo nodeInfo = bundle.getChannel().getManagementInfo();
      boolean pending = Boolean.TRUE.equals(bundle.getParameter(BundleParameter.JOB_PENDING));
      JobInformation jobInfo = new JobInformation(getUuid(), bundle.getName(),
              bundle.getTaskCount(), bundle.getInitialTaskCount(), bundle.getSLA().getPriority(),
              bundle.getSLA().isSuspended(), pending);
      jobInfo.setMaxNodes(bundle.getSLA().getMaxNodes());
      result[i++] = new NodeJobInformation(nodeInfo, jobInfo);
    }
    return result;
  }

  /**
   * Add received bundle to this server job.
   * @param bundle the bundle to add.
   * @return <code>true</code> when bundle was added to job. <code>false</code> when job is COMPLETED.
   */
  public boolean addBundle(final ServerTaskBundleClient bundle) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    lock.lock();
    try {
      if (getSubmissionStatus() == SubmissionStatus.COMPLETE) return false;
      bundleList.add(bundle);
      this.tasks.addAll(bundle.getTaskList());
      bundle.addCompletionListener(bundleCompletionListener);
      return true;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append('{');
    sb.append("uuid=").append(getUuid());
    if (getSLA().isBroadcastJob()) sb.append("broadcastID=").append(getBroadcastUUID());
    sb.append(", status=").append(getSubmissionStatus());
    sb.append(", tasks=").append(tasks.size());
    sb.append(", bundlerWrappers=").append(bundleList.size());
    sb.append(", cancelled=").append(isCancelled());
    sb.append(", expired=").append(isJobExpired());
    sb.append(", pending=").append(isPending());
    sb.append(", suspended=").append(isSuspended());
    sb.append('}');
    return sb.toString();
  }
}
