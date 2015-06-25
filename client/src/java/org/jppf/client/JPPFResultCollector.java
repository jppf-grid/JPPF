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

package org.jppf.client;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.jppf.client.event.*;
import org.jppf.client.event.JobEvent.Type;
import org.jppf.client.persistence.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Implementation of the {@link org.jppf.client.event.TaskResultListener TaskResultListener} interface
 * that can be used &quot;as is&quot; to collect the results of an asynchronous job submission.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class JPPFResultCollector implements JobStatusHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFResultCollector.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A map containing the resulting tasks, ordered by ascending position in the submitted list of tasks.
   */
  private final JobResults jobResults;
  /**
   * The job whose results this object is collecting.
   */
  private final JPPFJob job;
  /**
   * The status of this job.
   */
  private AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.SUBMITTED);
  /**
   * List of listeners registered to receive this job's status change notifications.
   */
  private final List<JobStatusListener> listeners = new ArrayList<>();

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
   * @param tasks the list of tasks whose results have been received from the server.
   * @param throwable the throwable that was raised while receiving the results.
   */
  @SuppressWarnings("unchecked")
  public synchronized void resultsReceived(final List<Task<?>> tasks, final Throwable throwable) {
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
      if (debugEnabled) log.debug("received throwable '{}'", ExceptionUtils.getMessage(throwable));
    }
    job.fireJobEvent(JobEvent.Type.JOB_RETURN, null, tasks);
    if (job.unexecutedTaskCount() <= 0) job.fireJobEvent(Type.JOB_END, null, tasks);
    job.client.unregisterClassLoaders(job.getUuid());
    jobResults.wakeUp();
    notifyAll();
  }

  @Override
  public JobStatus getStatus() {
    return status.get();
  }

  @Override
  public void setStatus(final JobStatus newStatus) {
    if (status.get() != newStatus) {
      if (debugEnabled) log.debug("job [" + job.getUuid() + "] status changing from '" + this.status + "' to '" + newStatus + "'");
      this.status.set(newStatus);
      fireStatusChangeEvent(newStatus);
    }
  }

  /**
   * Add a listener to the list of status listeners.
   * @param listener the listener to add.
   */
  public void addJobStatusListener(final JobStatusListener listener) {
    synchronized(listeners) {
      if (debugEnabled) log.debug("job [" + job.getUuid() + "] adding status listener " + listener);
      if (listener != null) listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of status listeners.
   * @param listener the listener to remove.
   */
  public void removeJobStatusListener(final JobStatusListener listener) {
    synchronized(listeners) {
      if (debugEnabled) log.debug("job [" + job.getUuid() + "] removing status listener " + listener);
      if (listener != null) listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners of a change of status for this job.
   * @param newStatus the status for job event.
   * @exclude
   */
  protected void fireStatusChangeEvent(final JobStatus newStatus) {
    synchronized(listeners) {
      if (debugEnabled) log.debug("job [" + job.getUuid() + "] fire status changed event for '" + newStatus + "'");
      if (!listeners.isEmpty()) {
        JobStatusEvent event = new JobStatusEvent(job.getUuid(), newStatus);
        for (JobStatusListener listener: listeners) listener.jobStatusChanged(event);
      }
    }
  }
}
