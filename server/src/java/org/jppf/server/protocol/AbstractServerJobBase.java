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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class AbstractServerJobBase extends AbstractServerJob {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractServerJobBase.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The list of the tasks.
   */
  protected final Map<Integer, ServerTask> tasks = new TreeMap<>();
  /**
   * The list of the incoming bundles.
   */
  protected final List<ServerTaskBundleClient> clientBundles = new ArrayList<>();
  /**
   * Set of all dispatched bundles in this job.
   */
  protected final Map<Long, ServerTaskBundleNode> dispatchSet = new LinkedHashMap<>();
  /**
   * Set of all nodes to which this job is dispatched.
   */
  protected final Map<String, Integer> channelSet = new HashMap<>();
  /**
   * The requeue handler.
   */
  protected Runnable onRequeue;
  /**
   * The data location of the data provider.
   */
  protected final DataLocation dataProvider;
  /**
   * Total number of dispatches in this job.
   */
  protected final AtomicInteger totalDispatches = new AtomicInteger(0);
  /**
   * The list of the tasks.
   */
  protected final Map<Integer, ServerTask> dependendedOnTasks = new TreeMap<>();

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param lock used to synchronized access to job.
   * @param notificationEmitter an <code>ChangeListener</code> instance that fires job notifications.
   * @param job  underlying task bundle.
   * @param dataProvider the data location of the data provider.
   */
  public AbstractServerJobBase(final Lock lock, final ServerJobChangeListener notificationEmitter, final TaskBundle job, final DataLocation dataProvider) {
    super(lock, job);
    this.notificationEmitter = notificationEmitter;
    this.dataProvider = dataProvider;
  }

  /**
   * Get list of bundles received from client.
   * @return list of bundles received from client.
   */
  public List<ServerTaskBundleClient> getClientBundles() {
    lock.lock();
    try {
      return new ArrayList<>(clientBundles);
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
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @param after determines whether the tasks from other should be added first or last.
   * @return <code>true</code> when this client job needs to be requeued.
   */
  protected boolean merge(final List<ServerTask> taskList, final boolean after) {
    lock.lock();
    try {
      final boolean requeue = this.tasks.isEmpty() && !taskList.isEmpty();
      for (final ServerTask task: taskList) tasks.put(task.getPosition(), task);
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
    if (bundle == null) throw new IllegalArgumentException("bundle is null for " + this);
    final boolean empty;
    synchronized (dispatchSet) {
      empty = dispatchSet.isEmpty();
      dispatchSet.put(bundle.getId(), bundle);
      final String uuid = bundle.getJob().getUuid();
      final Integer n = channelSet.get(uuid);
      channelSet.put(uuid, (n == null) ? 1 : n + 1);
    }
    totalDispatches.incrementAndGet();
    if (debugEnabled) log.debug("added to dispatch set: {}", bundle);
    if (empty) {
      updateStatus(ServerJobStatus.NEW, ServerJobStatus.EXECUTING);
      setSubmissionStatus(SubmissionStatus.EXECUTING);
    }
    fireJobDispatched(bundle.getChannel(), bundle);
  }

  /**
   * Called when all or part of a job is returned from node.
   * @param bundle the returned job.
   */
  public void jobReturned(final ServerTaskBundleNode bundle) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    synchronized (dispatchSet) {
      dispatchSet.remove(bundle.getId());
      final String uuid = bundle.getJob().getUuid();
      final Integer n = channelSet.get(uuid);
      if (n != null) {
        if (n > 1) channelSet.put(uuid, n - 1);
        else channelSet.remove(uuid);
      }
    }
    if (debugEnabled) log.debug("removed from dispatch set: {}", bundle);
    fireJobReturned(bundle.getChannel(), bundle);
  }

  /**
   * Get indicator whether job has pending tasks.
   * @return <code>true</code> when job has some pending tasks.
   */
  protected boolean hasPending() {
    lock.lock();
    try {
      for (final ServerTaskBundleClient bundle: clientBundles) {
        if (bundle.getPendingTasksCount() > 0) return true;
      }
    } finally {
      lock.unlock();
    }
    return false;
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
      return channelSet.size();
    }
  }

  /**
   * Get count of dispatches executed on the specified channel.
   * @param uuid uuid of the channel to check.
   * @return the number of dispatches to the channel.
   */
  public int getNbDispatches(final String uuid) {
    final Integer n;
    synchronized (dispatchSet) {
      n = channelSet.get(uuid);
    }
    return (n == null) ? 0 : n;
  }

  /**
   * Get count of dispatches being executed.
   * @return the number of dipatches.
   */
  public int getNbDispatches() {
    synchronized (dispatchSet) {
      return dispatchSet.size();
    }
  }

  /**
   * @return the total number of dispatches.
   */
  public int getTotalDispatches() {
    return totalDispatches.get();
  }

  /**
   * Get the node bundle with the specified id from the dispatch set.
   * @param id the id of the bundle to find.
   * @return a {@link ServerTaskBundleNode} instance.
   */
  public ServerTaskBundleNode getNodeBundle(final long id) {
    synchronized (dispatchSet) {
      return dispatchSet.get(id);
    }
  }

  /**
   * Get the dispatch set. Used for debugging purposes.
   * @return a set of {@link ServerTaskBundleNode} instances.
   */
  public Set<ServerTaskBundleNode> getDispatchSet() {
    synchronized (dispatchSet) {
      return new LinkedHashSet<>(dispatchSet.values());
    }
  }

  /**
   * Add received bundle to this server job.
   * @param bundle the bundle to add.
   * @return {@code true} when bundle was added to job. {@code false} when job is {@code COMPLETE}.
   * @throws JPPFJobEndedException if the job is already {@code ENDED}.
   */
  public boolean addBundle(final ServerTaskBundleClient bundle) throws JPPFJobEndedException {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    lock.lock();
    try {
      final SubmissionStatus submissionStatus = getSubmissionStatus();
      if (debugEnabled) log.debug("submissionStatus={}, adding {} to {}", submissionStatus, bundle, this);
      if (hasCompleted()) {
        throw new JPPFJobEndedException("Job " + submissionStatus);
      } else {
        if (log.isTraceEnabled()) logTasksPositions(bundle);
        clientBundles.add(bundle);
        for (final ServerTask task: bundle.getTaskList()) {
          final int pos = task.getPosition();
          if (tasks.containsKey(pos)) throw new IllegalStateException(String.format("position %d already in task map for %s, client bundle = %s", pos, this, bundle));
          tasks.put(pos, task);
        }
        bundle.addCompletionListener(new BundleCompletionListener(this));
        fireJobUpdated(false);
        return true;
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Log the positions of the tasks in a client bundle.
   * @param bundle the bundle whose tasks to log.
   */
  private static void logTasksPositions(final ServerTaskBundleClient bundle) {
    final StringBuilder sb = new StringBuilder();
    int count = 0;
    for (final ServerTask task: bundle.getTaskList()) {
      if (count > 0) sb.append(", ");
      sb.append(task.getPosition());
      count++;
    }
    log.trace("tasks positions in client bundle: {}", sb);
  }

  /**
   * DFetermine whether this job is in COMPLETED or ENDED state.
   * @return {@code true} if this job has completed, {@code false} otherwise.
   */
  public boolean hasCompleted() {
    lock.lock();
    try {
      final SubmissionStatus submissionStatus = getSubmissionStatus();
      return (submissionStatus == SubmissionStatus.COMPLETE) || (submissionStatus == SubmissionStatus.ENDED);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("id=").append(id);
    sb.append(", job uuid=").append(uuid);
    sb.append(", name=").append(name);
    sb.append(", submissionStatus=").append(submissionStatus.get());
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
    sb.append(", jobExpired=").append(jobExpired);
    sb.append(", pending=").append(pending);
    sb.append(", suspended=").append(isSuspended());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the number of client bundles.
   * @return the number of bundles as an int.
   */
  public int getNbBundles() {
    return clientBundles.size();
  }

  /**
   * @return {@code true} if the job graph is already handled in another driver that dispatched the job, {@code false} otherwise.
   */
  public boolean isJobGraphAlreadyHandled() {
    return job.getParameter(BundleParameter.JOB_GRAPH_ALREADY_HANDLED, false);
  }
}
