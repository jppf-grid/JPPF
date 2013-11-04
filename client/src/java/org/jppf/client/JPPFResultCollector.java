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

package org.jppf.client;

import java.util.*;

import org.jppf.client.event.*;
import org.jppf.client.persistence.*;
import org.jppf.client.submission.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * Implementation of the {@link org.jppf.client.event.TaskResultListener TaskResultListener} interface
 * that can be used &quot;as is&quot; to collect the results of an asynchronous job submission.
 * @see org.jppf.client.JPPFClient#submit(org.jppf.client.JPPFJob)
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class JPPFResultCollector implements TaskResultListener, SubmissionStatusHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFResultCollector.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The initial task count in the job.
   * @exclude
   */
  protected int count;
  /**
   * A map containing the resulting tasks, ordered by ascending position in the
   * submitted list of tasks.
   * @exclude
   */
  protected JobResults jobResults;
  /**
   * The list of final resulting tasks.
   * @exclude
   */
  protected List<Task<?>> results = null;
  /**
   * The job whose results this object is collecting.
   */
  protected JPPFJob job = null;
  /**
   * The status of this submission.
   */
  private SubmissionStatus status = SubmissionStatus.SUBMITTED;
  /**
   * List of listeners registered to receive this submission's status change notifications.
   */
  private final List<SubmissionStatusListener> listeners = new ArrayList<>();

  /**
   * Default constructor, provided as a convenience for subclasses.
   * @exclude
   */
  protected JPPFResultCollector()
  {
  }

  /**
   * Initialize this collector with the specified job.
   * @param job the job to execute.
   */
  public JPPFResultCollector(final JPPFJob job)
  {
    this.job = job;
    count = job.getJobTasks().size();
    this.jobResults = job.getResults();
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param event a notification of completion for a set of submitted tasks.
   * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
   */
  @Override
  @SuppressWarnings("unchecked")
  public synchronized void resultsReceived(final TaskResultEvent event)
  {
    Throwable t = event.getThrowable();
    List<Task<?>> tasks = event.getTasks();
    if (tasks != null)
    {
      List<Integer> positions = new ArrayList<>(tasks.size());
      for (Task<?> task: tasks) positions.add(task.getPosition());
      if (debugEnabled) log.debug("before putResults(): jobResults={}, positions={}", jobResults, positions);
      jobResults.addResults(tasks);
      if (debugEnabled) log.debug("Received results for " + tasks.size() + " tasks, pendingCount = " + (count - jobResults.size()) + 
          ", count=" + count + ", jobResults=" + jobResults);
      JobPersistence pm = job.getPersistenceManager();
      if ((job != null) && (pm != null))
      {
        try
        {
          pm.storeJob(pm.computeKey(job), job, tasks);
        }
        catch (JobPersistenceException e)
        {
          log.error(e.getMessage(), e);
        }
      }
    }
    else
    {
      if (debugEnabled) log.debug("received throwable '" + t.getClass().getName() + ": " + t.getMessage() + "', resetting this result collector");
    }
    notifyAll();
  }

  /**
   * Wait until all results of a request have been collected.
   * @return the list of resulting tasks.
   * @deprecated use {@link #awaitResults()} instead.
   */
  @Deprecated
  public synchronized List<JPPFTask> waitForResults()
  {
    return waitForResults(Long.MAX_VALUE);
  }

  /**
   * Wait until all results of a request have been collected, or the timeout has expired,
   * whichever happens first.
   * @param millis the maximum time to wait, zero meaning an indefinite wait.
   * @return the list of resulting tasks.
   * @deprecated use {@link #awaitResults(long)} instead.
   */
  @Deprecated
  public synchronized List<JPPFTask> waitForResults(final long millis)
  {
    awaitResults(millis);
    return getResults();
  }

  /**
   * Wait until all results of a request have been collected.
   * @return the list of resulting tasks.
   */
  public List<Task<?>> awaitResults()
  {
    return awaitResults(Long.MAX_VALUE);
  }

  /**
   * Wait until all results of a request have been collected, or the timeout has expired,
   * whichever happens first.
   * @param millis the maximum time to wait, zero meaning an indefinite wait.
   * @return the list of resulting tasks.
   */
  public synchronized List<Task<?>> awaitResults(final long millis)
  {
    if (millis < 0L) throw new IllegalArgumentException("wait time cannot be negative");
    if (log.isTraceEnabled()) log.trace("timeout = " + millis + ", pendingCount = " + (count - jobResults.size()));
    long timeout = millis > 0 ? millis : Long.MAX_VALUE;
    long start = System.currentTimeMillis();
    long elapsed = 0L;
    while ((elapsed < timeout) && (getStatus() != SubmissionStatus.COMPLETE))
    {
      try
      {
        if (elapsed >= timeout) return null;
        wait(timeout - elapsed);
      }
      catch(InterruptedException e)
      {
        log.error(e.getMessage(), e);
      }
      elapsed = System.currentTimeMillis() - start;
      if (log.isTraceEnabled()) log.trace("elapsed = " + elapsed + ", millis = " + timeout);
    }
    if (log.isTraceEnabled()) log.trace("elapsed = " + elapsed);
    return results;
  }

  /**
   * Get the list of final results.
   * @return a list of results as tasks, or null if not all tasks have been executed.
   * @deprecated use {@link #getAllResults()} instead.
   */
  @Deprecated
  public List<JPPFTask> getResults()
  {
    List<JPPFTask> list = new ArrayList<>(results.size());
    for (Task<?> task: results) list.add((JPPFTask) task);
    return list;
  }

  /**
   * Get the list of final results.
   * @return a list of results as tasks, or null if not all tasks have been executed.
   */
  public List<Task<?>> getAllResults()
  {
    return results;
  }

  /**
   * Build the results list based on a map of executed tasks.
   */
  protected void buildResults()
  {
    results = new ArrayList<>(jobResults.getAllResults());
  }

  @Override
  public synchronized SubmissionStatus getStatus()
  {
    return status;
  }

  @Override
  public synchronized void setStatus(final SubmissionStatus newStatus)
  {
    if (newStatus == this.status) return;
    if (debugEnabled) log.debug("submission [" + getId() + "] status changing from '" + this.status + "' to '" + newStatus + "'");
    this.status = newStatus;
    try {
      if (newStatus == SubmissionStatus.COMPLETE)
      {
        buildResults();
        onComplete();
      }
    } finally {
      notifyAll();
      fireStatusChangeEvent(newStatus);
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
   */
  public String getId()
  {
    return job == null ? "no-id" : job.getUuid();
  }

  /**
   * Add a listener to the list of status listeners.
   * @param listener the listener to add.
   */
  public void addSubmissionStatusListener(final SubmissionStatusListener listener)
  {
    synchronized(listeners)
    {
      if (debugEnabled) log.debug("submission [" + getId() + "] adding status listener " + listener);
      if (listener != null) listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of status listeners.
   * @param listener the listener to remove.
   */
  public void removeSubmissionStatusListener(final SubmissionStatusListener listener)
  {
    synchronized(listeners)
    {
      if (debugEnabled) log.debug("submission [" + getId() + "] removing status listener " + listener);
      if (listener != null) listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners of a change of status for this submission.
   * @param newStatus the status for submission event.
   * @exclude
   */
  protected void fireStatusChangeEvent(final SubmissionStatus newStatus)
  {
    synchronized(listeners)
    {
      if (debugEnabled) log.debug("submission [" + getId() + "] fire status changed event for '" + newStatus + "'");
      if (!listeners.isEmpty())
      {
        SubmissionStatusEvent event = new SubmissionStatusEvent(getId(), newStatus);
        for (SubmissionStatusListener listener: listeners)
        {
          listener.submissionStatusChanged(event);
        }
      }
    }
  }
}
