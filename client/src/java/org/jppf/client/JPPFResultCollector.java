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

package org.jppf.client;

import java.util.*;

import org.jppf.client.event.*;
import org.jppf.client.persistence.*;
import org.jppf.client.submission.*;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * Implementation of the {@link org.jppf.client.event.TaskResultListener TaskResultListener} interface
 * that can be used &quot;as is&quot; to collect the results of an asynchronous job submission.
 * @see org.jppf.client.JPPFClient#submitNonBlocking(List, org.jppf.task.storage.DataProvider, TaskResultListener)
 * @author Laurent Cohen
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
   */
  protected int count;
  /**
   * Count of results not yet received.
   */
  protected int pendingCount = 0;
  /**
   * A map containing the resulting tasks, ordered by ascending position in the
   * submitted list of tasks.
   */
  protected Map<Integer, JPPFTask> resultMap = null;
  /**
   * The list of final resulting tasks.
   */
  protected List<JPPFTask> results = null;
  /**
   * 
   */
  protected JPPFJob job = null;
  /**
   * The status of this submission.
   */
  private SubmissionStatus status = SubmissionStatus.SUBMITTED;
  /**
   * List of listeners registered to receive this submission's status change notifications.
   */
  private final List<SubmissionStatusListener> listeners = new ArrayList<SubmissionStatusListener>();

  /**
   * Default constructor, provided as a convenience for subclasses.
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
    count = job.getTasks().size() - job.getResults().size();
    pendingCount = count;
  }

  /**
   * Initialize this collector with a specified number of tasks.
   * @param count the count of submitted tasks.
   * @deprecated use {@link #JPPFResultCollector(JPPFJob) JPPFResultCollector(JPPFJob)} instead.
   */
  public JPPFResultCollector(final int count)
  {
    this.count = count;
    this.pendingCount = count;
    resultMap = new TreeMap<Integer, JPPFTask>();
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param event a notification of completion for a set of submitted tasks.
   * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
   */
  @Override
  public synchronized void resultsReceived(final TaskResultEvent event)
  {
    if (event.getThrowable() == null)
    {
      List<JPPFTask> tasks = event.getTaskList();
      if (job == null) for (JPPFTask task: tasks) resultMap.put(task.getPosition(), task);
      else job.getResults().putResults(tasks);
      pendingCount -= tasks.size();
      if (debugEnabled) log.debug("Received results for " + tasks.size() + " tasks, pendingCount = " + pendingCount);
      if (pendingCount <= 0)
      {
        buildResults();
        setStatus(SubmissionStatus.COMPLETE);
      }
      notifyAll();
      if ((job != null) && (job.getPersistenceManager() != null))
      {
        JobPersistence pm = job.getPersistenceManager();
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
      Throwable t = event.getThrowable();
      if (debugEnabled) log.debug("received throwable '" + t.getClass().getName() + ": " + t.getMessage() + "', resetting this result collector");
      // reset this object's state to prepare for job resubmission
      if (job != null) count = job.getTasks().size() - job.getResults().size();
      pendingCount = count;
      resultMap = new TreeMap<Integer, JPPFTask>();
      results = null;
    }
  }

  /**
   * Wait until all results of a request have been collected.
   * @return the list of resulting tasks.
   */
  public synchronized List<JPPFTask> waitForResults()
  {
    return waitForResults(Long.MAX_VALUE);
  }

  /**
   * Wait until all results of a request have been collected, or the timeout has expired,
   * whichever happens first.
   * @param millis the maximum time to wait, zero meaning an indefinite wait.
   * @return the list of resulting tasks.
   */
  public synchronized List<JPPFTask> waitForResults(final long millis)
  {
    if (millis < 0) throw new IllegalArgumentException("wait time cannot be negative");
    if (log.isTraceEnabled()) log.trace("timeout = " + millis + ", pendingCount = " + pendingCount);
    long start = System.currentTimeMillis();
    long elapsed = 0;
    while ((elapsed < millis) && (pendingCount > 0))
    {
      try
      {
        if (elapsed >= millis) return null;
        wait(millis - elapsed);
      }
      catch(InterruptedException e)
      {
        log.error(e.getMessage(), e);
      }
      elapsed = System.currentTimeMillis() - start;
      if (log.isTraceEnabled()) log.trace("elapsed = " + elapsed + ", millis = " + millis);
    }
    //if (pendingCount <= 0) buildResults();
    if (log.isTraceEnabled()) log.trace("elapsed = " + elapsed);
    return results;
  }

  /**
   * Get the list of final results.
   * @return a list of results as tasks, or null if not all tasks have been executed.
   */
  public List<JPPFTask> getResults()
  {
    return results;
  }

  /**
   * Build the results list based on a map of executed tasks.
   */
  protected void buildResults()
  {
    if (job == null) results = new ArrayList<JPPFTask>(resultMap.values());
    else results = new ArrayList<JPPFTask>(job.getResults().getAll());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized SubmissionStatus getStatus()
  {
    return status;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void setStatus(final SubmissionStatus newStatus)
  {
    if (debugEnabled) log.debug("submission [" + getId() + "] status changing from '" + this.status + "' to '" + newStatus + "'");
    this.status = newStatus;
    fireStatusChangeEvent(newStatus);
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
   */
  protected void fireStatusChangeEvent(final SubmissionStatus newStatus)
  {
    synchronized(listeners)
    {
      if (!this.status.equals(newStatus))
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
}
