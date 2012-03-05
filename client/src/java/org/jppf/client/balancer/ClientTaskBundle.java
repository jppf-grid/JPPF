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

import java.util.Map;

/**
 * Instances of this class group tasks from the same client together, so they are sent to the same node,
 * avoiding unnecessary transport overhead.<br>
 * The goal is to provide a performance enhancement through an adaptive bundling of tasks originating from the same client.
 * The bundle size is computed dynamically, depending on the number of nodes connected to the server, and other factors.
 * @author Laurent Cohen
 */
public class ClientTaskBundle extends ClientTaskBundleBase
{
  /**
   * The tasks execution policy.
   */
  private transient ExecutionPolicy localExecutionPolicy = null;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID = null;

  /**
   * Initialize this task bundle and set its build number.
   * @param job the job to execute.
   */
  public ClientTaskBundle(final JPPFJob job)
  {
    super(job);
  }


  /**
   * Make a copy of this bundle.
   * @return a new <code>ClientTaskBundle</code> instance.
   */
  public ClientTaskBundle copy()
  {
    ClientTaskBundle bundle = new ClientTaskBundle(getJob());
    bundle.setUuidPath(getUuidPath());
    bundle.setRequestUuid(getRequestUuid());
    bundle.setUuid(getUuid());
    bundle.setName(getName());
    bundle.setTaskCount(getTaskCount());
    bundle.setDataProvider(getDataProvider());
    synchronized (bundle.getParametersMap())
    {
      for (Map.Entry<Object, Object> entry : getParametersMap().entrySet())
        bundle.setParameter(entry.getKey(), entry.getValue());
    }
    bundle.setQueueEntryTime(getQueueEntryTime());
    bundle.setCompletionListener(getCompletionListener());
    bundle.setSLA(getSLA());
    bundle.setLocalExecutionPolicy(localExecutionPolicy);
    bundle.setBroadcastUUID(broadcastUUID);
    //bundle.setParameter(BundleParameter.JOB_METADATA, getJobMetadata());

    return bundle;
  }

  /**
   * Make a copy of this bundle containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientTaskBundle</code> instance.
   */
  public ClientTaskBundle copy(final int nbTasks)
  {
    ClientTaskBundle bundle = copy();
    bundle.setTaskCount(nbTasks);
    taskCount -= nbTasks;
    return bundle;
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
}
