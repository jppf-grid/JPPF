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
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.server.job.management.NodeJobInformation;
import org.jppf.server.protocol.utils.AbstractServerJob;
import org.jppf.server.submission.*;
import org.jppf.utils.Pair;
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
   * The list of the incomming bundles.
   */
  private final List<ServerTaskBundleClient> bundleList = new ArrayList<ServerTaskBundleClient>();
  /**
   * Listener for handling completed bundles.
   */
  private final ServerTaskBundleClient.CompletionListener bundleCompletionListener = new ServerTaskBundleClient.CompletionListener() {
    @Override
    public void taskCompleted(final ServerTaskBundleClient bundle, final List<ServerTask> results) {
      if(bundle == null) throw new IllegalArgumentException("bundlerWrapper is null");

      if (bundle.isCancelled()) cancel(false);
    }

    @Override
    public void bundleDone(final ServerTaskBundleClient bundle) {
      if(bundle == null) throw new IllegalArgumentException("bundlerWrapper is null");

      bundle.removeCompletionListener(this);
      synchronized (tasks) {
        bundleList.remove(bundle);
        tasks.removeAll(bundle.getTaskList());
      }
    }
  };

  /**
   * Map of all futures in this job.
   */
  private final Map<ServerTaskBundleNode, Pair<ExecutorChannel, Future>> bundleMap = new LinkedHashMap<ServerTaskBundleNode, Pair<ExecutorChannel, Future>>();
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
   * State map for tasks on which resultReceived was called.
   */
  private final Map<ServerTask, Pair<DataLocation, TaskState>> taskStateMap = new IdentityHashMap<ServerTask, Pair<DataLocation, TaskState>>();
  /**
   * The data location of the data provider.
   */
  private final DataLocation dataProvider;
  /**
   * Handler for job state notifications.
   */
  protected final JobNotificationEmitter notificationEmitter;

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param notificationEmitter an <code>JobNotificationEmitter</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public ServerJob(final JobNotificationEmitter notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider) {
    super(job);
    this.notificationEmitter = notificationEmitter;
    this.dataProvider = dataProvider;
    this.submissionStatus = SubmissionStatus.SUBMITTED;
  }

  /**
   * Get list of bundles received from client.
   * @return list of bundles received from client.
   */
  protected List<ServerTaskBundleClient> getBundleList() {
    synchronized (tasks) {
      return new ArrayList<ServerTaskBundleClient>(bundleList);
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
    synchronized (tasks) {
      return tasks.size();
    }
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ServerJob</code> instance.
   */
  public ServerTaskBundleNode copy(final int nbTasks) {
    JPPFTaskBundle taskBundle = getJob();
    synchronized (tasks) {
      int taskCount;
      if(nbTasks > this.tasks.size()) taskCount = this.tasks.size();
      else taskCount = nbTasks;
      List<ServerTask> subList = this.tasks.subList(0, taskCount);
      try {
        if(taskBundle.getTaskCount() != taskCount) {
          int newSize = taskBundle.getCurrentTaskCount() - taskCount;
          taskBundle = taskBundle.copy();
          taskBundle.setTaskCount(taskCount);
          getJob().setCurrentTaskCount(newSize);
        }
        return new ServerTaskBundleNode(this, taskBundle, subList);
      } finally {
        subList.clear();
      }
    }
  }

  /**
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @param after determines whether the tasks from other should be added first or last.
   * @return <code>true</code> when this client job needs to be requeued.
   */
  protected boolean merge(final List<ServerTask> taskList, final boolean after) {
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
   * @param channel the node to which the job is dispatched.
   * @param future  future assigned to bundle execution.
   */
  public void jobDispatched(final ServerTaskBundleNode bundle, final ExecutorChannel channel, final Future<?> future) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (future == null) throw new IllegalArgumentException("future is null");
    boolean empty;
    synchronized (bundleMap) {
      empty = bundleMap.isEmpty();
      bundleMap.put(bundle, new Pair<ExecutorChannel, Future>(channel, future));
    }
    if (empty) {
      updateStatus(NEW, EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
    }
    fireJobDispatched(channel, bundle);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ServerTaskBundleNode bundle, final List<DataLocation> results) {
    if (results.isEmpty()) return;
    synchronized (tasks) {
      List<ServerTask> bundleTasks =  bundle == null ? tasks : bundle.getTaskList();
      if(isJobExpired() || isCancelled()) {
        for (int index = 0; index < bundleTasks.size(); index++) {
          ServerTask location = bundleTasks.get(index);
          DataLocation task = location.getDataLocation();
          taskStateMap.put(location, new Pair<DataLocation, TaskState>(task, TaskState.RESULT));
          location.getBundle().resultReceived(location.getPosition(), location.getDataLocation());
        }
      } else {
        for (int index = 0; index < bundleTasks.size(); index++) {
          ServerTask location = bundleTasks.get(index);
          DataLocation task = results.get(index);
          taskStateMap.put(location, new Pair<DataLocation, TaskState>(task, TaskState.RESULT));
          location.getBundle().resultReceived(location.getPosition(), task);
        }
      }
    }
    ExecutorChannel channel;
    synchronized (bundleMap) {
      Pair<ExecutorChannel, Future> pair = bundleMap.get(bundle);
      channel = pair == null ? null : pair.first();
    }
    fireJobReturned(channel, bundle);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ServerTaskBundleNode bundle, final Throwable throwable) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    synchronized (tasks) {
      for (ServerTask task : bundle.getTaskList()) {
        Pair<DataLocation, TaskState> oldPair = taskStateMap.get(task);
        TaskState oldState = oldPair == null ? null : oldPair.second();
        if (oldState != TaskState.RESULT) taskStateMap.put(task, new Pair<DataLocation, TaskState>(null, TaskState.EXCEPTION));
        task.getBundle().resultReceived(task.getPosition(), throwable);
      }
    }
    ExecutorChannel channel;
    synchronized (bundleMap) {
      Pair<ExecutorChannel, Future> pair = bundleMap.get(bundle);
      channel = pair == null ? null : pair.first();
    }
    fireJobReturned(channel, bundle);
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
    if(isCancelled()) {
      List<Future>   futureList;
      synchronized (bundleMap) {
        futureList = new ArrayList<Future>(bundleMap.size());
        for (Pair<ExecutorChannel, Future> pair : bundleMap.values()) {
          futureList.add(pair.second());
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
    synchronized (bundleMap) {
      Pair<ExecutorChannel, Future> pair = bundleMap.remove(bundle);
      if (bundle != null && (pair == null || pair.second() == null)) throw new IllegalStateException("future already removed");
    }
    boolean requeue = false;
    List<DataLocation> list = new ArrayList<DataLocation>();
    synchronized (tasks)
    {
      if (getSLA().isBroadcastJob()) {
        if (bundle != null) {
          for (ServerTask task : bundle.getTaskList()) {
            Pair<DataLocation, TaskState> pair = taskStateMap.put(task, new Pair<DataLocation, TaskState>(task.getDataLocation(), TaskState.RESULT));
            if (pair == null || pair.second() != TaskState.RESULT) list.add(task.getDataLocation());
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
            Pair<DataLocation, TaskState> pair = taskStateMap.get(task);
            if (pair == null || pair.second() != TaskState.RESULT) list.add(task.getDataLocation());
          }
          addAll(list, this.tasks);
        }
        if (bundle.isRequeued()) {
          List<ServerTask> taskList = new ArrayList<ServerTask>();
          for (ServerTask task : bundle.getTaskList()) {
            Pair<DataLocation, TaskState> pair = taskStateMap.get(task);
            if (pair != null && pair.second() != TaskState.RESULT) taskList.add(task);
          }
          requeue = merge(taskList, false);
        }
      }
    }
    if(!list.isEmpty()) {
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
    synchronized (tasks) {
      for (ServerTaskBundleClient bundle : bundleList) {
        if (bundle.getPendingTasksCount() > 0) return true;
      }
    }
    return false;
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
    if (debugEnabled) log.debug("request to cancel " + this);
    if (super.cancel(mayInterruptIfRunning)) {
      done();
      taskCompleted(null, null);
      return true;
    }
    else return false;
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
    synchronized (bundleMap)
    {
      return bundleMap.size();
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
    Map.Entry<ServerTaskBundleNode, Pair<ExecutorChannel, Future>>[] entries;
    synchronized (bundleMap) {
      entries = bundleMap.entrySet().toArray(new Map.Entry[bundleMap.size()]);
    }
    if (entries.length == 0) return NodeJobInformation.EMPTY_ARRAY;

    NodeJobInformation[] result = new NodeJobInformation[entries.length];
    int i = 0;
    for (Map.Entry<ServerTaskBundleNode, Pair<ExecutorChannel, Future>> entry : entries) {
      ExecutorChannel channel = entry.getValue().first();
      JPPFManagementInfo nodeInfo = channel.getManagementInfo();
      ServerTaskBundleNode bundle = entry.getKey();
      Boolean pending = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING);
      JobInformation jobInfo = new JobInformation(getUuid(), bundle.getName(),
              bundle.getTaskCount(), bundle.getInitialTaskCount(), bundle.getSLA().getPriority(),
              bundle.getSLA().isSuspended(), (pending != null) && pending);
      jobInfo.setMaxNodes(bundle.getSLA().getMaxNodes());
      result[i++] = new NodeJobInformation(nodeInfo, jobInfo);
    }
    return result;
  }

  /**
   * Add received bundle to this server job.
   * @param bundle the bundle to add.
   */
  public void addBundle(final ServerTaskBundleClient bundle) {
    if(bundle == null) throw new IllegalArgumentException("bundle is null");
    bundle.addCompletionListener(bundleCompletionListener);
    synchronized (tasks) {
      bundleList.add(bundle);
      this.tasks.addAll(bundle.getTaskList());
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append('{');
    sb.append("uuid=").append(getUuid());
    if(getSLA().isBroadcastJob()) sb.append("broadcastID=").append(getBroadcastUUID());
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
