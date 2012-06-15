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

package org.jppf.client.balancer;

import org.jppf.client.JPPFJob;
import org.jppf.client.balancer.utils.AbstractClientJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.client.event.TaskResultListener;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.client.submission.SubmissionStatusHandler;
import org.jppf.node.protocol.JobSLA;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Martin JANDA
 */
public class ClientJob extends AbstractClientJob
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientJob.class);
  /**
   * The underlying task bundle.
   */
  private final JPPFJob job;
  /**
   * The list of of the tasks.
   */
  private final List<JPPFTask> tasks;
  /**
   * Job expired indicator, determines whether the job is should be cancelled.
   */
  private boolean jobExpired = false;
  /**
   * Job pending indicator, determines whether the job is waiting for its scheduled time to start.
   */
  private boolean pending = false;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID = null;
  /**
   * The universal unique id for this job.
   */
  private String uuid = null;
  /**
   * The user-defined display name for this job.
   */
  private String name = null;
  /**
   * The service level agreement between the job and the server.
   */
  private JobSLA sla = null;
  /**
   * Map of all futures in this job.
   */
  private final Map<ClientTaskBundle, Future> bundleMap = new LinkedHashMap<ClientTaskBundle, Future>();
  /**
   * The status of this submission.
   */
  private SubmissionStatus submissionStatus;
  /**
   * Number of tasks that hav completed.
   */
  private int completedCount = 0;
  /**
   * The listener that receives notifications of completed tasks.
   */
  private TaskResultListener resultsListener;
  /**
   * Instance of parent broadcast job.
   */
  private transient ClientJob parentJob;
  /**
   * Map of all dispatched broadcast jobs.
   */
  private final Map<String, ClientJob> broadcastMap;

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   */
  public ClientJob(final JPPFJob job, final List<JPPFTask> tasks)
  {
    this(job, tasks, null, null);
  }

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   * @param parentJob instance of parent broadcast job.
   * @param broadcastUUID the broadcast UUID.
   */
  protected ClientJob(final JPPFJob job, final List<JPPFTask> tasks, final ClientJob parentJob, final String broadcastUUID)
  {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (tasks == null) throw new IllegalArgumentException("tasks is null");

    this.job = job;
    this.parentJob = parentJob;
    this.broadcastUUID = broadcastUUID;

    if(broadcastUUID == null) {
      if(job.getSLA().isBroadcastJob())
        this.broadcastMap = new LinkedHashMap<String, ClientJob>();
      else
        this.broadcastMap = Collections.emptyMap();
      this.resultsListener = this.job.getResultListener();
    } else {
      this.broadcastMap = Collections.emptyMap();
      this.resultsListener = null;
    }

    if (this.job.getResultListener() instanceof SubmissionStatusHandler)
      this.submissionStatus = ((SubmissionStatusHandler) this.job.getResultListener()).getStatus();
    else
      this.submissionStatus = SubmissionStatus.SUBMITTED;

    this.tasks = new ArrayList<JPPFTask>(tasks);
    this.uuid = this.job.getUuid();
    this.name = this.job.getName();
    this.sla = this.job.getSLA();
  }

  /**
   * Get the universal unique id for this job.
   * @return the uuid as a string.
   * @exclude
   */
  public String getUuid()
  {
    return uuid;
  }

  /**
   * Set the universal unique id for this job.
   * @param uuid the universal unique id.
   */
  public void setUuid(final String uuid)
  {
    this.uuid = uuid;
  }

  /**
   * Get the user-defined display name for this job. This is the name displayed in the administration console.
   * @return the name as a string.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Set the user-defined display name for this job.
   * @param name the display name as a string.
   */
  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @return an instance of {@link JobSLA}.
   */
  public JobSLA getSLA()
  {
    return sla;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @param sla an instance of <code>JobSLA</code>.
   */
  public void setSLA(final JobSLA sla)
  {
    this.sla = sla;
  }

  /**
   * Get the current number of tasks in the job.
   * @return the number of tasks as an int.
   */
  public int getTaskCount()
  {
    return tasks.size();
  }

  /**
   * Get the underlying task bundle.
   * @return a <code>ClientTaskBundle</code> instance.
   */
  public JPPFJob getJob()
  {
    return job;
  }

  /**
   * Get the list of of the tasks.
   * @return a list of <code>JPPFTask</code> instances.
   */
  public List<JPPFTask> getTasks()
  {
    return Collections.unmodifiableList(tasks);
  }

  /**
   * Make a copy of this client job wrapper.
   * @param broadcastUUID the broadcast UUID.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientJob createBroadcastJob(final String broadcastUUID)
  {
    if (broadcastUUID == null || broadcastUUID.isEmpty()) throw new IllegalArgumentException("broadcastUUID is blank");

    return new ClientJob(job, this.tasks, this, broadcastUUID);
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientTaskBundle copy(final int nbTasks)
  {
    if (nbTasks >= this.tasks.size())
    {
      try {
        return new ClientTaskBundle(this, this.tasks);
      } finally {
        this.tasks.clear();
      }
    }
    else
    {
      List<JPPFTask> subList = this.tasks.subList(0, nbTasks);
      try
      {
        return new ClientTaskBundle(this, subList);
      }
      finally
      {
        subList.clear();
      }
    }
  }

  /**
   * Merge this client job wrapper with another.
   * @param taskList list of tasks to merge.
   * @param after determines whether the tasks from other should be added first or last.
   */
  public void merge(final List<JPPFTask> taskList, final boolean after)
  {
    if (!after) this.tasks.addAll(0, taskList);
    if (after) this.tasks.addAll(taskList);
  }

  /**
   * Get the listener that receives notifications of completed tasks.
   * @return a <code>TaskCompletionListener</code> instance.
   */
  public TaskResultListener getResultListener()
  {
    return resultsListener;
  }

  /**
   * Set the listener that receives notifications of completed tasks.
   * @param resultsListener a <code>TaskCompletionListener</code> instance.
   */
  public void setResultListener(final TaskResultListener resultsListener)
  {
    this.resultsListener = resultsListener;
  }

  /**
   * Get the job expired indicator.
   * @return <code>true</code> if job has expired, <code>false</code> otherwise.
   */
  public boolean isJobExpired()
  {
    return jobExpired;
  }

  /**
   * Notifies that job has expired.
   */
  public void jobExpired()
  {
    this.jobExpired = true;
    for (ClientJob broadcastJob : getBroadcastJobs())
    {
      broadcastJob.jobExpired();
    }
    cancel(true);
  }

  /**
   * Get the job pending indicator.
   * @return <code>true</code> if job is pending, <code>false</code> otherwise.
   */
  public boolean isPending()
  {
    return pending;
  }

  /**
   * Set the job pending indicator.
   * @param pending <code>true</code> to indicate that job is pending, <code>false</code> otherwise
   */
  public void setPending(final boolean pending)
  {
    this.pending = pending;
  }

  /**
   * Get the broadcast UUID.
   * @return an <code>String</code> instance.
   */
  public String getBroadcastUUID()
  {
    return broadcastUUID;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param bundle  the dispatched job.
   * @param channel the node to which the job is dispatched.
   * @param future  future assigned to bundle execution.
   */
  public void jobDispatched(final ClientTaskBundle bundle, final ChannelWrapper<?> channel, final Future<?> future)
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (future == null) throw new IllegalArgumentException("future is null");

    boolean empty = bundleMap.isEmpty();
    bundleMap.put(bundle, future);
    if (updateStatus(NEW, EXECUTING) || empty)
    {
      setSubmissionStatus(SubmissionStatus.EXECUTING);
    }
    if(empty && getBroadcastUUID() == null) job.fireJobEvent(JobEvent.Type.JOB_START);
    if(parentJob != null) parentJob.broadcastDispatched(this);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final List<JPPFTask> results)
  {
//    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    completedCount += results.size();
//    if(completedCount > dispatchedCount) throw new IllegalStateException("completedCount > dispatchedCount");

    TaskResultListener listener = resultsListener;
    if (listener != null)
    {
      synchronized (listener)
      {
        listener.resultsReceived(new TaskResultEvent(results));
      }
    }
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param bundle    the finished job.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final Throwable throwable)
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    TaskResultListener listener = resultsListener;
    if (listener != null)
    {
      synchronized (listener)
      {
        listener.resultsReceived(new TaskResultEvent(throwable));
      }
    }
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param bundle    the completed task.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final ClientTaskBundle bundle, final Exception exception)
  {
//    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    Future future = bundleMap.remove(bundle);
    if(bundle != null && future == null) throw new IllegalStateException("future already removed");

    if(bundle == null) resultsReceived(null, job.getTasks());

    boolean fire = false;
    try {
      if(exception != null)
      {
        setSubmissionStatus(SubmissionStatus.FAILED);
      }

      if(completedCount == job.getTasks().size() && submissionStatus != SubmissionStatus.FAILED)
      {
        fire = true;
        done();
      } else if(completedCount > job.getTasks().size())
        throw new IllegalStateException("completedCount > job.tasks.size");
    } finally {
      if(bundleMap.isEmpty() && getBroadcastUUID() == null) job.fireJobEvent(JobEvent.Type.JOB_END);
      if(fire) setSubmissionStatus(SubmissionStatus.COMPLETE);
    }
  }

  /**
   * Get the status of this submission.
   * @return a {@link SubmissionStatus} enumerated value.
   */
  public SubmissionStatus getSubmissionStatus()
  {
    return submissionStatus;
  }

  /**
   * Set the status of this submission.
   * @param submissionStatus a {@link SubmissionStatus} enumerated value.
   */
  public void setSubmissionStatus(final SubmissionStatus submissionStatus)
  {
    this.submissionStatus = submissionStatus;
    if (resultsListener instanceof SubmissionStatusHandler) ((SubmissionStatusHandler) resultsListener).setStatus(this.submissionStatus);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancel(final boolean mayInterruptIfRunning)
  {
    if(super.cancel(mayInterruptIfRunning)) {
      try
      {
        for (ClientJob broadcastJob : getBroadcastJobs())
        {
          broadcastJob.cancel(mayInterruptIfRunning);
        }
        for (Future future : bundleMap.values())
        {
          try
          {
            if(!future.isDone()) future.cancel(false);
          }
          catch (Exception e)
          {
            log.error("Error cancelling job " + this, e);
          }
        }
      }
      finally
      {
        done();
      }
      return true;
    }
    else
      return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void done()
  {
    try {
      super.done();
    } finally {
      if (job.getSLA().isBroadcastJob()) {
        TaskResultListener listener = resultsListener;
        if (listener != null)
        {
          synchronized (listener)
          {
            listener.resultsReceived(new TaskResultEvent(job.getTasks()));
          }
        }
      }
      if (parentJob != null) parentJob.broadcastCompleted(this);
    }
  }

  /**
   * Get the array of dispatched broadcast jobs.
   * @return an array of {@link ClientJob} instances.
   */
  protected ClientJob[] getBroadcastJobs()
  {
    synchronized (broadcastMap) {
      return broadcastMap.values().toArray(new ClientJob[broadcastMap.size()]);
    }
  }

  /**
   * Called when all or part of broadcast job is dispatched to a driver.
   * @param broadcastJob    the dispatched job.
   */
  protected void broadcastDispatched(final ClientJob broadcastJob)
  {
    if(broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");

    boolean empty;
    synchronized (broadcastMap)
    {
      empty = broadcastMap.isEmpty();
      broadcastMap.put(broadcastJob.getBroadcastUUID(), broadcastJob);
    }

    if (updateStatus(NEW, EXECUTING) || empty) setSubmissionStatus(SubmissionStatus.EXECUTING);
    if(empty && getBroadcastUUID() == null) job.fireJobEvent(JobEvent.Type.JOB_START);
  }

  /**
   * Called to notify that the execution of broadcasted job has completed.
   * @param broadcastJob    the completed job.
   */
  protected void broadcastCompleted(final ClientJob broadcastJob)
  {
    if(broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");

    //    if (debugEnabled) log.debug("received " + n + " tasks for node uuid=" + uuid);
    boolean empty;
    synchronized (broadcastMap) {
      if(broadcastMap.remove(broadcastJob.getBroadcastUUID()) != broadcastJob) throw new IllegalStateException("broadcast job not found");
      empty = broadcastMap.isEmpty();
    }

    if(empty) taskCompleted(null, null);
  }
}
