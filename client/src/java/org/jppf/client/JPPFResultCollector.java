/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.client;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.client.event.*;
import org.jppf.client.event.JobEvent.Type;
import org.jppf.client.persistence.*;
import org.jppf.client.submission.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Implementation of the {@link org.jppf.client.event.TaskResultListener TaskResultListener} interface
 * that can be used &quot;as is&quot; to collect the results of an asynchronous job submission.
 * @deprecated {@code JPPFResultCollector} and its inheritance hierarchy are no longer exposed as public APIs.
 * {@code JobListener} should be used instead, with the {@code JPPFJob.addJobListener(JobListener)} and {@code JPPFJob.removeJobListener(JobListener)} methods.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
@SuppressWarnings("deprecation")
public class JPPFResultCollector implements TaskResultListener, SubmissionStatusHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFResultCollector.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A map containing the resulting tasks, ordered by ascending position in the
   * submitted list of tasks.
   * @exclude
   */
  protected JobResults jobResults;
  /**
   * The job whose results this object is collecting.
   */
  protected JPPFJob job = null;
  /**
   * The status of this submission.
   */
  private AtomicReference<SubmissionStatus> status = new AtomicReference<>(SubmissionStatus.SUBMITTED);
  /**
   * List of listeners registered to receive this submission's status change notifications.
   */
  private final List<SubmissionStatusListener> listeners = new ArrayList<>();

  /**
   * Default constructor, provided as a convenience for subclasses.
   * @exclude
   */
  protected JPPFResultCollector() {
  }

  /**
   * Initialize this collector with the specified job.
   * @param job the job to execute.
   */
  public JPPFResultCollector(final JPPFJob job) {
    this.job = job;
    this.jobResults = job.getResults();
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param event a notification of completion for a set of submitted tasks.
   * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
   */
  @Override
  @SuppressWarnings("unchecked")
  public synchronized void resultsReceived(final TaskResultEvent event) {
    Throwable t = event.getThrowable();
    List<Task<?>> tasks = event.getTasks();
    if (tasks != null) {
      jobResults.addResults(tasks);
      if (debugEnabled) log.debug("Received results for {} tasks, pendingCount={}, count={}, jobResults={}", new Object[] {tasks.size(), job.unexecutedTaskCount(), job.getJobTasks().size(), jobResults});
      JobPersistence pm = job.getPersistenceManager();
      if ((job != null) && (pm != null)) {
        try {
          pm.storeJob(pm.computeKey(job), job, tasks);
        } catch (JobPersistenceException e) {
          log.error(e.getMessage(), e);
        }
      }
    } else {
      if (debugEnabled) log.debug("received throwable '{}'", ExceptionUtils.getMessage(t));
    }
    job.fireJobEvent(JobEvent.Type.JOB_RETURN, null, tasks);
    if (job.unexecutedTaskCount() <= 0) job.fireJobEvent(Type.JOB_END, null, tasks);
    jobResults.wakeUp();
    notifyAll();
  }

  /**
   * Wait until all results of a request have been collected.
   * @return the list of resulting tasks.
   * @deprecated use {@link JPPFJob#awaitResults()} instead.
   */
  @Deprecated
  public List<JPPFTask> waitForResults() {
    return waitForResults(Long.MAX_VALUE);
  }

  /**
   * Wait until all results of a request have been collected, or the timeout has expired,
   * whichever happens first.
   * @param millis the maximum time to wait, zero meaning an indefinite wait.
   * @return the list of resulting tasks.
   * @deprecated use {@link JPPFJob#awaitResults(long)} instead.
   */
  @Deprecated
  public synchronized List<JPPFTask> waitForResults(final long millis) {
    awaitResults(millis);
    return getResults();
  }

  /**
   * Wait until all results of a request have been collected.
   * @return the list of resulting tasks.
   * @deprecated use {@link JPPFJob#awaitResults()} instead.
   */
  public List<Task<?>> awaitResults() {
    return awaitResults(Long.MAX_VALUE);
  }

  /**
   * Wait until all results of a request have been collected, or the timeout has expired,
   * whichever happens first.
   * @param millis the maximum time to wait, zero meaning an indefinite wait.
   * @return the list of resulting tasks, or {@code null} if the timeout expired before all results were received.
   * @deprecated use {@link JPPFJob#awaitResults(long)} instead.
   */
  public synchronized List<Task<?>> awaitResults(final long millis) {
    if (millis < 0L) throw new IllegalArgumentException("wait time cannot be negative");
    if (log.isTraceEnabled()) log.trace("timeout = " + millis + ", pendingCount = " + job.unexecutedTaskCount());
    long timeout = millis > 0 ? millis : Long.MAX_VALUE;
    long start = System.currentTimeMillis();
    long elapsed = 0L;
    int size = job.getJobTasks().size();
    while ((elapsed < timeout) && (jobResults.size() < size)) {
      try {
        if (elapsed >= timeout) return null;
        wait(timeout - elapsed);
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
      elapsed = System.currentTimeMillis() - start;
      if (log.isTraceEnabled()) log.trace("elapsed = " + elapsed + ", millis = " + timeout);
    }
    if (log.isTraceEnabled()) log.trace("elapsed = " + elapsed);
    return jobResults.getResultsList();
  }

  /**
   * Get the list of final results.
   * @return a list of results as tasks, or null if not all tasks have been executed.
   * @deprecated use {@link JPPFJob#getAllResults()} instead.
   */
  @Deprecated
  public List<JPPFTask> getResults() {
    List<Task<?>> results = jobResults.getResultsList();
    List<JPPFTask> list = new ArrayList<>(results.size());
    for (Task<?> task: results) list.add((JPPFTask) task);
    return list;
  }

  /**
   * Get the list of final results.
   * @return a list of results as tasks, or null if not all tasks have been executed.
   * @deprecated use {@link JPPFJob#getAllResults()} instead.
   */
  public List<Task<?>> getAllResults() {
    return jobResults.getResultsList();
  }

  @Override
  public SubmissionStatus getStatus() {
    return status.get();
  }

  @Override
  public void setStatus(final SubmissionStatus newStatus) {
    if (status.get() != newStatus) {
      if (debugEnabled) log.debug("submission [" + getId() + "] status changing from '" + this.status + "' to '" + newStatus + "'");
      this.status.set(newStatus);
      try {
        if (newStatus == SubmissionStatus.COMPLETE) {
          //if (debugEnabled) log.debug("call stack for COMPLETE: {}", ExceptionUtils.getStackTrace(new Exception("call stack")));
          onComplete();
        }
      } finally {
        fireStatusChangeEvent(newStatus);
      }
    }
  }

  /**
   * Called when status is changed to <code>COMPLETE</code>.
   */
  protected void onComplete() {
  }

  /**
   * Get the unique id of this submission.
   * @return the id as a string.
   * @exclude
   */
  public String getId() {
    return job == null ? "no-id" : job.getUuid();
  }

  /**
   * Add a listener to the list of status listeners.
   * @param listener the listener to add.
   */
  public void addSubmissionStatusListener(final SubmissionStatusListener listener) {
    synchronized(listeners) {
      if (debugEnabled) log.debug("submission [" + getId() + "] adding status listener " + listener);
      if (listener != null) listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of status listeners.
   * @param listener the listener to remove.
   */
  public void removeSubmissionStatusListener(final SubmissionStatusListener listener) {
    synchronized(listeners) {
      if (debugEnabled) log.debug("submission [" + getId() + "] removing status listener " + listener);
      if (listener != null) listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners of a change of status for this submission.
   * @param newStatus the status for submission event.
   * @exclude
   */
  protected void fireStatusChangeEvent(final SubmissionStatus newStatus) {
    synchronized(listeners) {
      if (debugEnabled) log.debug("submission [" + getId() + "] fire status changed event for '" + newStatus + "'");
      if (!listeners.isEmpty()) {
        SubmissionStatusEvent event = new SubmissionStatusEvent(getId(), newStatus);
        for (SubmissionStatusListener listener: listeners) listener.submissionStatusChanged(event);
      }
    }
  }
}
