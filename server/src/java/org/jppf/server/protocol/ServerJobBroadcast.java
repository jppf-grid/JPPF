/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
import java.util.concurrent.locks.Lock;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 *
 * @author Martin JANDA
 * @exclude
 */
public class ServerJobBroadcast extends ServerJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerJobBroadcast.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Instance of parent broadcast job.
   */
  private transient ServerJobBroadcast parentJob;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID = null;
  /**
   * Map of all dispatched broadcast jobs.
   */
  private final Map<String, ServerJobBroadcast> broadcastMap;
  /**
   * Map of all pending broadcast jobs.
   */
  private final Set<ServerJobBroadcast> broadcastSet = new LinkedHashSet<>();
  /**
   * Number of remaining tasks that have not completed.
   */
  protected int pendingTasksCount = 0;

  /**
   * Initialized broadcast job with task bundle and data provider.
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an <code>ChangeListener</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public ServerJobBroadcast(final Lock lock, final ServerJobChangeListener notificationEmitter, final TaskBundle job, final DataLocation dataProvider) {
    this(lock, notificationEmitter, job, dataProvider, null, null);
  }

  /**
   * Initialized broadcast job with task bundle and data provider.
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an <code>ChangeListener</code> instance that fires job notifications.
   * @param job   underlying task bundle.
   * @param dataProvider the data location of the data provider.
   * @param parentJob instance of parent broadcast job.
   * @param broadcastUUID the broadcast UUID.
   */
  protected ServerJobBroadcast(final Lock lock, final ServerJobChangeListener notificationEmitter, final TaskBundle job, final DataLocation dataProvider, final ServerJobBroadcast parentJob, final String broadcastUUID) {
    super(lock, notificationEmitter, job, dataProvider);
    if (!job.getSLA().isBroadcastJob()) throw new IllegalStateException("Not broadcast job");

    this.parentJob = parentJob;
    this.broadcastUUID = broadcastUUID;
    if (broadcastUUID == null) {
      this.broadcastMap = new LinkedHashMap<>();
    } else {
      this.broadcastMap = Collections.emptyMap();
    }
  }

  @Override
  public String getBroadcastUUID() {
    return broadcastUUID;
  }

  /**
   * Make a copy of this client job wrapper.
   * @param broadcastUUID the broadcast UUID.
   * @return a new <code>ServerJob</code> instance.
   */
  public ServerJobBroadcast createBroadcastJob(final String broadcastUUID) {
    if (broadcastUUID == null || broadcastUUID.isEmpty()) throw new IllegalArgumentException("broadcastUUID is blank");
    ServerJobBroadcast broadcastJob;
    lock.lock();
    try {
      broadcastJob = new ServerJobBroadcast(lock, notificationEmitter, job, getDataProvider(), this, broadcastUUID);
      broadcastJob.tasks.addAll(tasks);
      broadcastJob.pendingTasksCount = tasks.size();
      broadcastSet.add(broadcastJob);
    } finally {
      lock.unlock();
    }
    return broadcastJob;
  }

  @Override
  public void jobDispatched(final ServerTaskBundleNode bundle) {
    super.jobDispatched(bundle);
    if (parentJob != null) parentJob.broadcastDispatched(this);
  }

  /**
   * Called when all or part of broadcast job is dispatched to a driver.
   * @param broadcastJob    the dispatched job.
   */
  protected void broadcastDispatched(final ServerJobBroadcast broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    if (debugEnabled) log.debug("dispatched broadcast {}", broadcastJob);
    boolean empty;
    lock.lock();
    try {
      broadcastSet.remove(broadcastJob);
      empty = broadcastMap.isEmpty();
      broadcastMap.put(broadcastJob.getBroadcastUUID(), broadcastJob);
      if (empty) {
        updateStatus(ServerJobStatus.NEW, ServerJobStatus.EXECUTING);
        setSubmissionStatus(SubmissionStatus.EXECUTING);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Called to notify that the execution of broadcast job has completed.
   * @param broadcastJob    the completed job.
   */
  protected void broadcastCompleted(final ServerJobBroadcast broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    if (debugEnabled) log.debug("received broadcast results {}", broadcastJob);
    lock.lock();
    try {
      if ((broadcastMap.remove(broadcastJob.getBroadcastUUID()) != broadcastJob) && !broadcastSet.contains(broadcastJob)) throw new IllegalStateException("broadcast job not found");
      if (broadcastMap.isEmpty()) jobEnded();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Called when the entire broadcast job is complete.
   */
  public void jobEnded() {
    if (debugEnabled) log.debug("broadcast job ended {}", this);
    setSubmissionStatus(SubmissionStatus.ENDED);
    CollectionMap<ServerTaskBundleClient, ServerTask> clientMap = new SetIdentityMap<>();
    for (ServerTask task: tasks) {
      if (!task.isDone()) {
        task.broadcastResultReceived();
        clientMap.putValue(task.getBundle(), task);
      }
    }
    for (Map.Entry<ServerTaskBundleClient, Collection<ServerTask>> entry: clientMap.entrySet()) {
      entry.getKey().resultReceived(entry.getValue());
    }
    tasks.clear();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void resultsReceived(final ServerTaskBundleNode bundle, final List<DataLocation> results) {
    if (debugEnabled) log.debug("received results for {}", this);
    pendingTasksCount -= bundle.getTaskCount();
    taskCompleted(bundle, null);
    if (pendingTasksCount <= 0) parentJob.broadcastCompleted(this);
  }

  @Override
  public void resultsReceived(final ServerTaskBundleNode bundle, final Throwable throwable) {
    pendingTasksCount -= bundle.getTaskCount();
    taskCompleted(bundle, throwable);
    if (pendingTasksCount <= 0) parentJob.broadcastCompleted(this);
  }

  @Override
  public boolean addBundle(final ServerTaskBundleClient clientBundle) {
    lock.lock();
    try {
      if (parentJob == null) {
        boolean b = super.addBundle(clientBundle);
        List<ServerJobBroadcast> list = new ArrayList<>(broadcastSet.size() + broadcastMap.size());
        list.addAll(broadcastMap.values());
        list.addAll(broadcastSet);
        for (ServerJobBroadcast broadcastJob: list) addBundle(broadcastJob, clientBundle);
        return b;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Add a bundle to a child broadcast job.
   * @param broadcastJob the broadcast job to which to add the bundle.
   * @param bundle the client bundle to add.
   */
  private void addBundle(final ServerJobBroadcast broadcastJob, final ServerTaskBundleClient bundle) {
    if (broadcastJob.getSubmissionStatus() == SubmissionStatus.COMPLETE) {
      if (broadcastJob.completionBundles == null) broadcastJob.completionBundles = new ArrayList<>();
      broadcastJob.completionBundles.add(bundle);
    } else if (broadcastJob.getSubmissionStatus() == SubmissionStatus.ENDED) throw new IllegalStateException("Job ENDED");
    else {
      broadcastJob.clientBundles.add(bundle);
      broadcastJob.tasks.addAll(bundle.getTaskList());
      fireJobUpdated();
      broadcastJob.pendingTasksCount += bundle.getTaskCount();
    }
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("request to cancel " + this);
    lock.lock();
    try {
      if (parentJob == null) {
        if (!setCancelled(mayInterruptIfRunning)) return false;
        List<ServerJobBroadcast> list = new ArrayList<>(broadcastSet.size() + broadcastMap.size());
        list.addAll(broadcastMap.values());
        list.addAll(broadcastSet);
        broadcastSet.clear();
        for (ServerJobBroadcast broadcastJob : list) broadcastJob.cancel(false);
        jobEnded();
        return true;
      } else {
        return super.cancel(mayInterruptIfRunning);
      }
    } finally {
      lock.unlock();
    }
  }
}
