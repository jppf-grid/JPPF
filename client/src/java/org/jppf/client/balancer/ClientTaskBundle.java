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
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.protocol.BundleParameter;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.task.storage.DataProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 */
public class ClientTaskBundle extends JPPFTaskBundle
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The job to execute.
   */
  private final ClientJob job;
  /**
   * The shared data provider for this task bundle.
   */
  private transient DataProvider dataProvider = null;
  /**
   * The tasks to be executed by the node.
   */
  private transient List<JPPFTask> tasks = null;
  /**
   * The tasks execution policy.
   */
  private transient ExecutionPolicy localExecutionPolicy = null;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID = null;
  /**
   * The requeue handler.
   */
  private Runnable onRequeue = null;

  /**
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param tasks the tasks to execute.
   */
  public ClientTaskBundle(final ClientJob job, final List<JPPFTask> tasks)
  {
    if (job == null) throw new IllegalArgumentException("job us null");

    this.job = job;
    this.tasks = new ArrayList<JPPFTask>(tasks);
  }

  /**
   * Get the job this submission is for.
   * @return a {@link JPPFJob} instance.
   */
  public JPPFJob getJob()
  {
    return job.getJob();
  }

  /**
   * Get shared data provider for this task.
   * @return a <code>DataProvider</code> instance.
   */
  public DataProvider getDataProviderL()
  {
    return dataProvider;
  }

  /**
   * Set shared data provider for this task.
   * @param dataProvider a <code>DataProvider</code> instance.
   */
  public void setDataProviderL(final DataProvider dataProvider)
  {
    this.dataProvider = dataProvider;
  }

  /**
   * Get the tasks to be executed by the node.
   * @return the tasks as a <code>List</code> of arrays of bytes.
   */
  public List<JPPFTask> getTasksL()
  {
    return tasks;
  }

  /**
   * Set the tasks to be executed by the node.
   * @param tasks the tasks as a <code>List</code> of arrays of bytes.
   */
  public void setTasksL(final List<JPPFTask> tasks)
  {
    this.tasks = tasks;
  }

  /**
   * Make a copy of this bundle.
   * @return a new <code>ClientTaskBundle</code> instance.
   */
  @Override
  public ClientTaskBundle copy()
  {
    throw new UnsupportedOperationException();
//    ClientTaskBundle bundle = new ClientTaskBundle(getJob(), tasks);
//    bundle.setUuidPath(getUuidPath());
//    bundle.setRequestUuid(getRequestUuid());
//    bundle.setUuid(getUuid());
//    bundle.setName(getName());
//    bundle.setTaskCount(getTaskCount());
//    bundle.setDataProvider(getDataProvider());
//    synchronized (bundle.getParametersMap())
//    {
//      for (Map.Entry<Object, Object> entry : getParametersMap().entrySet())
//        bundle.setParameter(entry.getKey(), entry.getValue());
//    }
//    bundle.setQueueEntryTime(getQueueEntryTime());
//    bundle.setCompletionListener(getCompletionListener());
//    bundle.setSLA(getSLA());
//    bundle.setLocalExecutionPolicy(localExecutionPolicy);
//    bundle.setBroadcastUUID(broadcastUUID);
//    //bundle.setParameter(BundleParameter.JOB_METADATA, getJobMetadata());
//
//    return bundle;
  }

  /**
   * Make a copy of this bundle containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientTaskBundle</code> instance.
   */
  @Override
  public ClientTaskBundle copy(final int nbTasks)
  {
    throw new UnsupportedOperationException();
//    ClientTaskBundle bundle = copy();
//    bundle.setTaskCount(nbTasks);
//    taskCount -= nbTasks;
//    return bundle;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder("[");
    sb.append("jobId=").append(getName());
    sb.append(", jobUuid=").append(getUuid());
    sb.append(", initialTaskCount=").append(getInitialTaskCount());
    sb.append(", taskCount=").append(getTaskCount());
    sb.append(", requeue=").append(getParametersMap() == null ? null : getParameter(BundleParameter.JOB_REQUEUE));
    sb.append(']');
    return sb.toString();
  }


  /**
   * Get the tasks execution policy.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  public ExecutionPolicy getExecutionPolicy()
  {
    ExecutionPolicy slaPolicy;
    if (getSLA() == null)
    {
      slaPolicy = null;
    }
    else
    {
      slaPolicy = getSLA().getExecutionPolicy();
    }

    if (localExecutionPolicy == null)
    {
      return slaPolicy;
    }
    else if (slaPolicy == null)
    {
      return localExecutionPolicy;
    }
    else
    {
      return localExecutionPolicy.and(slaPolicy);
    }
  }

  /**
   * Get the tasks local execution policy.
   * @return an <code>ExecutionPolicy</code> instance.
   */
  public ExecutionPolicy getLocalExecutionPolicy()
  {
    return localExecutionPolicy;
  }

  /**
   * Set the tasks local execution policy.
   * @param localExecutionPolicy an <code>ExecutionPolicy</code> instance.
   */
  public void setLocalExecutionPolicy(final ExecutionPolicy localExecutionPolicy)
  {
    this.localExecutionPolicy = localExecutionPolicy;
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
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final List<JPPFTask> results)
  {
    job.resultsReceived(this, results);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final Throwable throwable)
  {
    job.resultsReceived(this, throwable);
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final Exception exception)
  {
    job.taskCompleted(this, exception);
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param channel the node to which the job is dispatched.
   */
  public void jobDispatched(final ChannelWrapper<?> channel)
  {
    job.jobDispatched(this, channel);
  }

  /**
   * Called when this task bundle should be resubmitted
   */
  public void resubmit()
  {
    if(onRequeue != null) onRequeue.run();
  }

  /**
   * Get the requeue handler.
   * @return an <code>Runnable</code> instance.
   */
  public Runnable getOnRequeue()
  {
    return onRequeue;
  }

  /**
   * Set the reuque handler.
   * @param onRequeue {@Link Runnable} executed on requeue.
   */
  public void setOnRequeue(final Runnable onRequeue)
  {
    this.onRequeue = onRequeue;
  }
}
