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

package org.jppf.server.queue;

import static org.jppf.utils.collections.CollectionUtils.formatSizeMapInfo;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.*;

import org.jppf.execute.*;
import org.jppf.job.*;
import org.jppf.node.protocol.*;
import org.jppf.queue.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.*;
import org.jppf.server.nio.nodeserver.BaseNodeContext;
import org.jppf.server.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.*;
import org.jppf.utils.collections.LinkedListSortedMap;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * A JPPF queue whose elements are ordered by decreasing priority.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class JPPFPriorityQueue extends AbstractJPPFQueue<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode> implements JobManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFPriorityQueue.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The driver.
   */
  final JPPFDriver driver;
  /**
   * The list of registered job listeners.
   */
  private final List<JobManagerListener> jobListeners = new ArrayList<>();
  /**
   * Manages jobs start and expiration scheduling.
   */
  final ScheduleManager scheduleManager = new ScheduleManager();
  /**
   * The job manager.
   */
  final JPPFJobManager jobManager;
  /**
   * Manages operations on broadcast jobs.
   */
  private final BroadcastManager broadcastManager;
  /**
   * Handles the persistence of jobs.
   */
  final PersistenceHandler persistenceHandler;
  /**
   * 
   */
  private final Map<String, Condition> jobRemovalConditions = new HashMap<>();
  /**
   * The job dependency graph handler.
   */
  private final JobDependenciesHandler dependenciesHandler;

  /**
   * Initialize this queue.
   * @param driver reference to the driver.
   * @param jobManager the job manager.
   */
  public JPPFPriorityQueue(final JPPFDriver driver, final JPPFJobManager jobManager) {
    this.driver = driver;
    this.jobManager = jobManager;
    broadcastManager = new BroadcastManager(this);
    persistenceHandler = new PersistenceHandler(this);
    dependenciesHandler = new JobDependenciesHandler(this);
  }

  @Override
  public ServerJob addBundle(final ServerTaskBundleClient clientBundle) {
    if (debugEnabled) log.debug("adding bundle=" + clientBundle);
    if (clientBundle == null) throw new IllegalArgumentException("clientBundle is null");
    final JobSLA sla = clientBundle.getSLA();
    final String jobUuid = clientBundle.getUuid();
    ServerJob serverJob = null;
    boolean cancel = false;
    lock.lock();
    try {
      if (sla.isBroadcastJob()) {
        if (debugEnabled) log.debug("before processing broadcast job {}", clientBundle.getJob());
        broadcastManager.processBroadcastJob(clientBundle);
      } else {  
        boolean newJob = false;
        boolean done = false;
        boolean added = false;
        while (!done) {
          serverJob = jobMap.get(jobUuid);
          if (serverJob == null) {
            newJob = true;
            serverJob = createServerJob(clientBundle);
            if (debugEnabled) log.debug("created new {}", serverJob);
            jobMap.put(jobUuid, serverJob);
            jobManager.jobQueued(serverJob);
          } else {
            if (debugEnabled) log.debug("job already queued");
            clientBundle.getJob().removeParameter(BundleParameter.JOB_TASK_GRAPH);
          }
          try {
            added = serverJob.addBundle(clientBundle);
            done = true;
          } catch (final JPPFJobEndedException e) {
            if (debugEnabled) log.debug("caught {}, awaiting removal of {}", ExceptionUtils.getMessage(e), serverJob);
            awaitJobRemoved(serverJob);
          }
        }
        if (added) {
          if (!newJob) priorityMap.removeValue(sla.getPriority(), serverJob);
          else cancel = (serverJob.getSLA().getDependencySpec().getId() != null) && dependenciesHandler.jobQueued(serverJob);
        } else return serverJob;
        if (!sla.isBroadcastJob() || serverJob.getBroadcastUUID() != null) {
          priorityMap.putValue(sla.getPriority(), serverJob);
          incrementSizeCount(getSize(serverJob));
        }
        updateLatestMaxSize();
        if (!newJob) driver.getStatistics().addValue(JPPFStatisticsHelper.JOB_TASKS, clientBundle.getTaskCount());
        final TaskBundle header = clientBundle.getJob();
        if (!header.getParameter(BundleParameter.FROM_PERSISTENCE, false) && !header.getParameter(BundleParameter.ALREADY_PERSISTED, false)) {
          header.setParameter(BundleParameter.ALREADY_PERSISTED, true);
          persistenceHandler.storeJob(serverJob, clientBundle, !newJob);
        }
        if (!cancel) fireBundleAdded(new QueueEvent<>(this, serverJob, false));
      }
      if (debugEnabled) log.debug("Maps size information: {}", formatSizeMapInfo("priorityMap", priorityMap));
    } finally {
      lock.unlock();
    }
    driver.getStatistics().addValue(JPPFStatisticsHelper.TASK_QUEUE_TOTAL, clientBundle.getTaskCount());
    driver.getStatistics().addValue(JPPFStatisticsHelper.TASK_QUEUE_COUNT, clientBundle.getTaskCount());
    if (cancel) serverJob.cancel(driver, true);
    return serverJob;
  }

  /**
   * Wait for the specified job to be removed from this queue.
   * @param serverJob the job to remove.
   */
  void awaitJobRemoved(final ServerJob serverJob) {
    if (debugEnabled) log.debug("awaiting removal of {}", serverJob);
    final String uuid = serverJob.getUuid();
    try {
      Condition cond = jobRemovalConditions.get(uuid);
      ServerJob job = null;
      while (((job = jobMap.get(uuid)) != null) && job.hasCompleted()) {
        if (cond == null) {
          cond = lock.newCondition();
          jobRemovalConditions.put(uuid, cond);
        }
        cond.await();
      }
      if (debugEnabled) log.debug("finished waiting for removal of {}", serverJob);
    } catch(final InterruptedException e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Create a {@link ServerJob} object from the specified client bundle.
   * @param clientBundle the bundle to create the job from.
   * @return a newly created job.
   */
  private ServerJob createServerJob(final ServerTaskBundleClient clientBundle) {
    final TaskBundle header = clientBundle.getJob();
    header.setDriverQueueTaskCount(header.getTaskCount());
    final ServerJob serverJob = new ServerJob(new ReentrantLock(), jobManager, header, clientBundle.getDataProvider());
    serverJob.setSubmissionStatus(SubmissionStatus.PENDING);
    serverJob.setQueueEntryTime(System.currentTimeMillis());
    serverJob.setJobReceivedTime(serverJob.getQueueEntryTime());
    serverJob.addOnDone(new RemoveBundleAction(this, serverJob));
    if (!clientBundle.getSLA().isBroadcastJob() || serverJob.getBroadcastUUID() != null) {
      if (debugEnabled) log.debug("adding bundle with {}", clientBundle);
      scheduleManager.handleStartJobSchedule(serverJob);
      scheduleManager.handleExpirationJobSchedule(driver, serverJob);
    }
    return serverJob;
  }

  /**
   * Handle requeue of the specified job.
   * @param job the job to requeue.
   */
  void requeue(final ServerJob job) {
    lock.lock();
    try {
      if (!jobMap.containsKey(job.getUuid())) throw new IllegalStateException("Job " + job + " not managed");
      if (debugEnabled) log.debug("requeuing job {}", job);
      priorityMap.putValue(job.getSLA().getPriority(), job);
      incrementSizeCount(getSize(job));
      fireBundleAdded(new QueueEvent<>(this, job, true));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ServerTaskBundleNode nextBundle(final ServerJob serverJob, final int nbTasks, final ExecutorChannel<ServerTaskBundleNode> channel) {
    final ServerTaskBundleNode result;
    lock.lock();
    try {
      final int taskCount = serverJob.getTaskCount();
      if (debugEnabled) log.debug("requesting bundle with {} tasks, next bundle has {} tasks", nbTasks, taskCount);
      final int size = getSize(serverJob);
      decrementSizeCount(size);
      int effectiveNbTasks = nbTasks;
      if (serverJob.getTaskGraph() != null) effectiveNbTasks = Math.min(nbTasks, serverJob.getAvailableGraphNodeCount());
      if (debugEnabled) log.debug("nbTasks={}, effectiveNbTasks={}", nbTasks, effectiveNbTasks);
      if (effectiveNbTasks >= taskCount) {
        if (taskCount <= 0) throw new IllegalStateException("no task to dispatch for job " + serverJob);
        serverJob.setOnRequeue(new RequeueBundleAction(this, serverJob));
        result = serverJob.createNodeDispatch(taskCount);
        removeBundle(serverJob, false);
      } else {
        if (debugEnabled) log.debug("removing {} tasks from bundle", effectiveNbTasks);
        result = serverJob.createNodeDispatch(effectiveNbTasks);
        incrementSizeCount(size);
        // to ensure that other jobs with same priority are also processed without waiting
        priorityMap.moveToEndOfList(serverJob.getSLA().getPriority(), serverJob);
      }
      updateLatestMaxSize();
      if (debugEnabled) log.debug("Maps size information: {}", formatSizeMapInfo("priorityMap", priorityMap));
    } finally {
      lock.unlock();
    }
    if (debugEnabled) log.debug("found {} tasks in the job, result={}", result.getTaskCount(), result);
    driver.getStatistics().addValue(JPPFStatisticsHelper.TASK_QUEUE_COUNT, -result.getTaskCount());
    driver.getStatistics().addValues(JPPFStatisticsHelper.TASK_QUEUE_TIME, System.currentTimeMillis() - serverJob.getQueueEntryTime(), result.getTaskCount());
    return result;
  }

  @Override
  public ServerJob removeBundle(final ServerJob serverJob) {
    return removeBundle(serverJob, true);
  }

  /**
   * Remove the specified bundle from the queue.
   * @param serverJob the bundle to remove.
   * @param removeFromJobMap flag whether bundle should be removed from job map.
   * @return the removed bundle.
   */
  public ServerJob removeBundle(final ServerJob serverJob, final boolean removeFromJobMap) {
    if (serverJob == null) throw new IllegalArgumentException("serverJob is null");
    lock.lock();
    try {
      if (removeFromJobMap) {
        final String uuid = serverJob.getUuid();
        if (jobMap.remove(uuid) != null) {
          scheduleManager.clearSchedules(serverJob.getUuid());
          if (serverJob.getSLA().getDependencySpec().getId() != null) dependenciesHandler.jobEnded(serverJob);
          jobManager.jobEnded(serverJob);
        } else if (debugEnabled) log.debug("could not remove {}", serverJob);
        final Condition cond = jobRemovalConditions.remove(uuid);
        if (cond != null) cond.signalAll();
      }
      if (debugEnabled) log.debug("removing job from queue, jobName= {}, removeFromJobMap={}", serverJob.getName(), removeFromJobMap);
      if (priorityMap.removeValue(serverJob.getSLA().getPriority(), serverJob)) {
        for (final ServerTaskBundleClient clientBundle : serverJob.getCompletionBundles()) {
          if (debugEnabled) log.debug("adding completion bundle for job={} : {}", serverJob.getName(), clientBundle);
          addBundle(clientBundle);
        }
      }
      fireBundleRemoved(new QueueEvent<>(this, serverJob, false));
    } finally {
      lock.unlock();
    }
    return serverJob;
  }

  @Override
  public void updatePriority(final String jobUuid, final int newPriority) {
    lock.lock();
    try {
      final ServerJob job = jobMap.get(jobUuid);
      if (job == null) return;
      final int oldPriority = job.getSLA().getPriority();
      if (oldPriority != newPriority) {
        job.getSLA().setPriority(newPriority);
        priorityMap.removeValue(oldPriority, job);
        priorityMap.putValue(newPriority, job);
        job.fireJobUpdated(true);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean cancelJob(final String jobId) {
    lock.lock();
    try {
      final ServerJob job = jobMap.get(jobId);
      boolean res = job != null;
      if (res) {
        decrementSizeCount(getSize(job));
        res &= job.cancel(driver, false);
      }
      return res;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Close this queue and all the resources it uses.
   */
  public void close() {
    lock.lock();
    try {
      scheduleManager.close();
      synchronized(queueListeners) {
        queueListeners.clear();
      }
      priorityMap.clear();
      sizeMap.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the job for the jobId.
   * @param jobId the uuid of the job.
   * @return a <code>ServerJob</code> instance.
   */
  public ServerJob getJob(final String jobId) {
    lock.lock();
    try {
      return jobMap.get(jobId);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the job for the jobId.
   * @param jobId the uuid of the job.
   * @return a <code>ServerJob</code> instance.
   */
  public ServerJob getJobFromPriorityMap(final String jobId) {
    lock.lock();
    try {
      for (ServerJob job: priorityMap) {
        if (job.getUuid().equals(jobId)) return job;
      }
      return null;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return a set of ids as strings.
   */
  @Override
  public Set<String> getAllJobIds() {
    lock.lock();
    try {
      return new HashSet<>(jobMap.keySet());
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the set of all the jobs currently queued or executing.
   * @return a list of {@link ServerJob} instances.
   */
  public List<ServerJob> getAllJobs() {
    lock.lock();
    try {
      return new ArrayList<>(jobMap.values());
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return a set of ids as strings.
   */
  public Set<String> getAllJobIdsFromPriorityMap() {
    lock.lock();
    try {
      final Set<String> set = new HashSet<>();
      for (ServerJob job: priorityMap.allValues()) set.add(job.getUuid());
      return set;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get all the jobs in the queue, ordered by priority.
   * @return a list of server-side jobs. The returned list is completely independant from this queue
   * and can be modified without affecting this queue.
   */
  public List<ServerJob> getAllJobsFromPriorityMap() {
    lock.lock();
    try {
      return priorityMap.allValues();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void addJobListener(final JobManagerListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    synchronized (jobListeners) {
      jobListeners.add(listener);
    }
  }

  @Override
  public void removeJobListener(final JobManagerListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    synchronized (jobListeners) {
      jobListeners.remove(listener);
    }
  }

  @Override
  public ServerJob getBundleForJob(final String jobUuid) {
    return getJob(jobUuid);
  }

  @Override
  protected int getSize(final ServerJob job) {
    return job.getJob().getDriverQueueTaskCount();
  }

  /**
   * @return the job map.
   */
  Map<String, ServerJob> getJobMap() {
    return jobMap;
  }

  /**
   * @return the priority map.
   */
  LinkedListSortedMap<Integer, ServerJob> getPriorityMap() {
    return priorityMap;
  }

  /**
   * Set the callable source for all available connections.
   * @param callableAllConnections a {@link Callable} instance.
   */
  public void setCallableAllConnections(final Callable<List<BaseNodeContext>> callableAllConnections) {
    broadcastManager.setCallableAllConnections(callableAllConnections);
  }

  /**
   * Update count of working connections base on status change.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  public void updateWorkingConnections(final ExecutorStatus oldStatus, final ExecutorStatus newStatus) {
    broadcastManager.updateWorkingConnections(oldStatus, newStatus);
  }

  /**
   * Select the jobs specified by a given job selector.
   * @param selector determines for which jobs to return.
   * @return a list of {@link ServerJob} obejcts, possibly empty.
   */
  public List<ServerJob> selectJobs(final JobSelector selector) {
    if ((selector == null) || (selector instanceof AllJobsSelector)) return getAllJobs();
    final List<ServerJob> list = new ArrayList<>();
    lock.lock();
    try {
      for (final ServerJob job: jobMap.values()) {
        if (selector.accepts(job)) list.add(job);
      }
    } finally {
      lock.unlock();
    }
    return list;
  }

  /**
   * Update the start and expiration schedules of an existing job. Normally called when a job's SLA is updated.
   * @param job the job to update.
   */
  public void updateSchedules(final ServerJob job) {
    final JobSLA sla = job.getSLA();
    scheduleManager.clearSchedules(job.getUuid());
    if (sla.getJobSchedule() != null) scheduleManager.handleStartJobSchedule(job);
    if (sla.getJobExpirationSchedule() != null) scheduleManager.handleExpirationJobSchedule(driver, job);
  }

  /**
   * Get the objects wich manages operations on broadcast jobs.
   * @return a {@link BroadcastManager} instance.
   */
  public BroadcastManager getBroadcastManager() {
    return broadcastManager;
  }

  /**
   * @return the persistence handler.
   */
  public PersistenceHandler getPersistenceHandler() {
    return persistenceHandler;
  }

  /**
   * @return the job dependency graph handler.
   */
  public JobDependenciesHandler getDependenciesHandler() {
    return dependenciesHandler;
  }
}
