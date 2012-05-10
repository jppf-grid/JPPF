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
import java.util.List;
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
  private List<JPPFTask> tasks;
  /**
   * The task completion listener to notify, once the execution of this task has completed.
   */
  private ClientCompletionListener completionListener = null;
  /**
   * Time at which the job is received on the server side. In milliseconds since January 1, 1970 UTC.
   */
  private long jobReceivedTime = 0L;
  /**
   * Job expired indicator, determines whether the job is should be cancelled.
   */
  private boolean jobExpired = false;
  /**
   * Job pending indicator, determines whether the job is waiting for its scheduled time to start.
   */
  private boolean pending = false;
  /**
   * Job requeue indicator.
   */
  private boolean requeued = false;
  /**
   * The time at which this wrapper was added to the queue.
   */
  private transient long queueEntryTime = 0L;
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
   * List of all futures in this job.
   */
  private final List<Future> futureList = new ArrayList<Future>();
  /**
   * The status of this submission.
   */
  private SubmissionStatus submissionStatus;
  /**
   * Number of tasks that have been dispatched to the executor.
   */
  private int dispatchedCount = 0;
  /**
   * Number of tasks that hav completed.
   */
  private int completedCount = 0;

  /**
   * Initialized client job with task bundle and list of tasks to execute.
   * @param job   underlying task bundle.
   * @param tasks list of tasks to execute.
   */
  public ClientJob(final JPPFJob job, final List<JPPFTask> tasks)
  {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (tasks == null) throw new IllegalArgumentException("tasks is null");

    this.job = job;
    if (this.job.getResultListener() instanceof SubmissionStatusHandler)
      this.submissionStatus = ((SubmissionStatusHandler) this.job.getResultListener()).getStatus();
    else
      this.submissionStatus = SubmissionStatus.SUBMITTED;

    this.tasks = new ArrayList<JPPFTask>(tasks);
    this.uuid = getJob().getUuid();
    this.name = getJob().getName();
    this.sla = getJob().getSLA();
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
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientJob copy()
  {
    return new ClientJob(job, this.tasks);
  }

  /**
   * Make a copy of this client job wrapper containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientJob</code> instance.
   */
  public ClientTaskBundle copy(final int nbTasks)
  {
    if (nbTasks == this.tasks.size())
    {
      return new ClientTaskBundle(this, this.tasks);
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
   * @param that  the wrapper to merge with.
   * @param after determines whether the tasks from other should be added first or last.
   */
  public void merge(final ClientJob that, final boolean after)
  {
    List<JPPFTask> list = new ArrayList<JPPFTask>(this.tasks.size() + that.tasks.size());
    if (!after) list.addAll(that.tasks);
    list.addAll(this.tasks);
    if (after) list.addAll(that.tasks);
    this.tasks = list;
  }

  /**
   * Get the task completion listener to notify, once the execution of this task has completed.
   * @return a <code>TaskCompletionListener</code> instance.
   */
  public ClientCompletionListener getCompletionListener()
  {
    return completionListener;
  }

  /**
   * Set the task completion listener to notify, once the execution of this task has completed.
   * @param completionListener a <code>TaskCompletionListener</code> instance.
   */
  public void setCompletionListener(final ClientCompletionListener completionListener)
  {
    this.completionListener = completionListener;
  }

  /**
   * Notifies that execution of this task has completed.
   */
  public void fireTaskCompleted()
  {
    if (this.completionListener != null) this.completionListener.taskCompleted(this);
  }

  /**
   * Get the job received time.
   * @return the time in milliseconds as a long value.
   */
  public long getJobReceivedTime()
  {
    return jobReceivedTime;
  }

  /**
   * Set the job received time.
   * @param jobReceivedTime the time in milliseconds as a long value.
   */
  public void setJobReceivedTime(final long jobReceivedTime)
  {
    this.jobReceivedTime = jobReceivedTime;
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
   * Set the job expired indicator.
   * @param jobExpired <code>true</code> to indicate that job has expired, <code>false</code> otherwise
   */
  public void setJobExpired(final boolean jobExpired)
  {
    this.jobExpired = jobExpired;
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
   * Notifies that job was cancelled.
   */
  public void jobCancelled()
  {
    cancel();
  }

  /**
   * Notifies that job has expired.
   */
  public void jobExpired()
  {
    jobCancelled();
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
   * Set the broadcast UUID.
   * @param broadcastUUID the broadcast UUID.
   */
  public void setBroadcastUUID(final String broadcastUUID)
  {
    this.broadcastUUID = broadcastUUID;
  }

  /**
   * Get the requeued indicator.
   * @return <code>true</code> if job is requeued, <code>false</code> otherwise.
   */
  public boolean isRequeued()
  {
    return requeued;
  }

  /**
   * Set the requeued indicator.
   * @param requeued <code>true</code> to indicate that job was requeued, <code>false</code> otherwise
   */
  public void setRequeued(final boolean requeued)
  {
    this.requeued = requeued;
  }

  /**
   * Get the time at which this wrapper was added to the queue.
   * @return the time in milliseconds as a long value.
   */
  public long getQueueEntryTime()
  {
    return queueEntryTime;
  }

  /**
   * Set the time at which this wrapper was added to the queue.
   * @param queueEntryTime the time in milliseconds as a long value.
   */
  public void setQueueEntryTime(final long queueEntryTime)
  {
    this.queueEntryTime = queueEntryTime;
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param bundle  the executing job.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final ClientTaskBundle bundle, final List<JPPFTask> results)
  {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    TaskResultListener listener = getJob().getResultListener();
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

    TaskResultListener listener = getJob().getResultListener();
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
    if (bundle == null) throw new IllegalArgumentException("bundle is null");

    completedCount += bundle.getTasksL().size();

    if(completedCount > dispatchedCount) throw new IllegalStateException("completedCount > dispatchedCount");

    if(exception != null) {
      if(submissionStatus == SubmissionStatus.EXECUTING) job.fireJobEvent(JobEvent.Type.JOB_END);
      setSubmissionStatus(SubmissionStatus.FAILED);
    }

    if(completedCount == job.getTasks().size() && submissionStatus == SubmissionStatus.EXECUTING)
    {
      job.fireJobEvent(JobEvent.Type.JOB_END);
      setSubmissionStatus(SubmissionStatus.COMPLETE);
    } else if(completedCount >= job.getTasks().size())
      throw new IllegalStateException("completedCount > job.tasks.size");
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

    dispatchedCount += bundle.getTasksL().size();
    futureList.add(future);
    if (updateStatus(NEW, EXECUTING))
    {
      setSubmissionStatus(SubmissionStatus.EXECUTING);
      job.fireJobEvent(JobEvent.Type.JOB_START);
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
    if (getJob().getResultListener() instanceof SubmissionStatusHandler)
    {
      ((SubmissionStatusHandler) getJob().getResultListener()).setStatus(this.submissionStatus);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancel()
  {
    if(super.cancel()) {
      try
      {
        for (Future future : futureList)
        {
          try
          {
            future.cancel(false);
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
    super.done();
    fireTaskCompleted();
  }
}
