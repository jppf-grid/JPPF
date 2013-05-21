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
import org.jppf.job.JobInformation;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.server.job.management.NodeJobInformation;
import org.jppf.server.protocol.utils.*;
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
  private final ServerTaskBundleClient.CompletionListener bundleCompletionListener = new BundleCompletionListener();
  /**
   * Set of all dispatched bundles in this job.
   */
  private final Set<ServerTaskBundleNode> dispatchSet = new LinkedHashSet<ServerTaskBundleNode>();
  /**
   * The requeue handler.
   */
  private Runnable onRequeue = null;
  /**
   * The data location of the data provider.
   */
  private final DataLocation dataProvider;

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an <code>ChangeListener</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public ServerJob(final Lock lock, final ServerJobChangeListener notificationEmitter, final JPPFTaskBundle job, final DataLocation dataProvider) {
    super(lock, job);

    this.notificationEmitter = notificationEmitter;
    this.dataProvider = dataProvider;
  }

  /**
   * Get list of bundles received from client.
   * @return list of bundles received from client.
   */
  public List<ServerTaskBundleClient> getBundleList() {
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
      else this.tasks.addAll(taskList);
      return requeue;
    } finally {
      lock.unlock();
    }
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
      updateStatus(ServerJobStatus.NEW, ServerJobStatus.EXECUTING);
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
    fireJobReturned(bundle.getChannel(), bundle);
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
    CollectionMap<ServerTaskBundleClient, Pair<Integer, DataLocation>> map = new SetIdentityMap<ServerTaskBundleClient, Pair<Integer, DataLocation>>();
    lock.lock();
    try {
      List<ServerTask> bundleTasks = (bundle == null) ? new ArrayList<ServerTask>(tasks) : bundle.getTaskList();
      if (isJobExpired() || isCancelled()) {
        for (ServerTask task : bundleTasks) map.putValue(task.getBundle(), new Pair(task.getPosition(), task.getDataLocation()));
      } else {
        for (int index = 0; index < bundleTasks.size(); index++) {
          ServerTask task = bundleTasks.get(index);
          DataLocation location = results.get(index);
          map.putValue(task.getBundle(), new Pair(task.getPosition(), location));
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
    CollectionMap<ServerTaskBundleClient, ServerTask> map = new SetIdentityMap<ServerTaskBundleClient, ServerTask>();
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
          for (ServerTaskBundleNode item : dispatchSet) futureList.add(item.getFuture());
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
        if (bundle != null) addExcluded(list, bundle.getTaskList(), TaskState.RESULT);
        if (isCancelled() || getBroadcastUUID() == null) addAll(list, this.tasks);
      } else if (bundle == null) {
        if (isCancelled()) addAll(list, this.tasks);
      } else {
        if (bundle.isCancelled()) {
          addExcluded(list, bundle.getTaskList(), TaskState.RESULT);
          addAll(list, this.tasks);
        }
        if (bundle.isRequeued()) {
          List<ServerTask> taskList = new ArrayList<ServerTask>();
          for (ServerTask task : bundle.getTaskList()) {
            if (task.getState() != TaskState.RESULT) taskList.add(task);
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
        setSubmissionStatus(SubmissionStatus.COMPLETE);
        fireTaskCompleted(this);
      }
      if (bundleList.isEmpty() && tasks.isEmpty()) setSubmissionStatus(SubmissionStatus.ENDED);
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
    } finally {
      lock.unlock();
    }
    return false;
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
    synchronized (dispatchSet) {
      return dispatchSet.size();
    }
  }

  /**
   * Get the dispatch set. Used for debugging purposes.
   * @return a set of {@link ServerTaskBundleNode} instances.
   */
  public Set<ServerTaskBundleNode> getDispatchSet() {
    synchronized (dispatchSet) {
      return new LinkedHashSet<ServerTaskBundleNode>(dispatchSet);
    }
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
    for (ServerTaskBundleNode nodeBundle : entries) {
      JPPFManagementInfo nodeInfo = nodeBundle.getChannel().getManagementInfo();
      JPPFTaskBundle bundle = nodeBundle.getJob();
      boolean pending = Boolean.TRUE.equals(bundle.getParameter(BundleParameter.JOB_PENDING));
      JobInformation jobInfo = new JobInformation(getUuid(), bundle.getName(), bundle.getTaskCount(),
        bundle.getInitialTaskCount(), bundle.getSLA().getPriority(), bundle.getSLA().isSuspended(), pending);
      jobInfo.setMaxNodes(bundle.getSLA().getMaxNodes());
      result[i++] = new NodeJobInformation(nodeInfo, jobInfo);
    }
    return result;
  }

  /**
   * Add received bundle to this server job.
   * @param bundle the bundle to add.
   * @return <code>true</code> when bundle was added to job. <code>false</code> when job is <code>COMPLETE</code>.
   */
  public boolean addBundle(final ServerTaskBundleClient bundle) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    lock.lock();
    try {
      if (getSubmissionStatus() == SubmissionStatus.COMPLETE) {
        if (completionBundles == null) completionBundles = new ArrayList<ServerTaskBundleClient>();
        completionBundles.add(bundle);
        return false;
      } else if (getSubmissionStatus() == SubmissionStatus.ENDED) throw new IllegalStateException("Job ENDED");
      else {
        bundleList.add(bundle);
        this.tasks.addAll(bundle.getTaskList());
        bundle.addCompletionListener(bundleCompletionListener);
        fireJobUpdated();
        return true;
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("id=").append(id);
    sb.append(", job uuid=").append(uuid);
    sb.append(", name=").append(name);
    sb.append(", status=").append(status);
    if (lock.tryLock()) {
      try {
        sb.append(", taskCount=").append(tasks.size());
      } finally {
        lock.unlock();
      }
    }
    sb.append(", nbBundles=").append(getNbBundles()); 
    //sb.append(", nbChannels=").append(getNbChannels());
    sb.append(']');
    sb.append(", jobExpired=").append(jobExpired); 
    sb.append(", pending=").append(pending); 
    sb.append(", suspended=").append(isSuspended()); 
    return sb.toString();
  }

  /**
   * Get the number of client bundles.
   * @return the number of bundles as an int.
   */
  public int getNbBundles() {
    return bundleList.size();
  }

  /**
   * Listener for handling completed bundles.
   */
  private class BundleCompletionListener implements ServerTaskBundleClient.CompletionListener {
    @Override
    public void taskCompleted(final ServerTaskBundleClient bundle, final List<ServerTask> results) {
      if (bundle == null) throw new IllegalArgumentException("bundle is null");
      if (bundle.isCancelled()) cancel(false);
    }

    @Override
    public void bundleEnded(final ServerTaskBundleClient bundle) {
      if (bundle == null) throw new IllegalArgumentException("bundle is null");
      lock.lock();
      try {
        bundle.removeCompletionListener(this);
        bundleList.remove(bundle);
        tasks.removeAll(bundle.getTaskList());
        if (completionBundles != null) completionBundles.remove(bundle);

        if (bundleList.isEmpty() && tasks.isEmpty() && getSubmissionStatus() == SubmissionStatus.COMPLETE) setSubmissionStatus(SubmissionStatus.ENDED);
      } finally {
        lock.unlock();
      }
    }
  }
}
