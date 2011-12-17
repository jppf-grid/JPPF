/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.JobSLA;
import org.jppf.scheduling.JPPFSchedule;

/**
 * This class represents the Service Level Agreement Between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 */
public class JPPFJobSLA implements JobSLA
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The tasks execution policy.
   */
  private ExecutionPolicy executionPolicy = null;
  /**
   * The priority of this job, used by the server to prioritize queued jobs.
   */
  private int priority = 0;
  /**
   * The maximum number of nodes this job can run on.
   */
  private int maxNodes = Integer.MAX_VALUE;
  /**
   * Determines whether this job is initially suspended.
   * If it is, it will have to be resumed, using either the admin console or the JMX APIs.
   */
  private boolean suspended = false;
  /**
   * The job start schedule configuration.
   */
  private JPPFSchedule jobSchedule = null;
  /**
   * The job expiration schedule configuration.
   */
  private JPPFSchedule jobExpirationSchedule = null;
  /**
   * Specifies whether the job is a broadcast job.
   */
  private boolean broadcastJob = false;
  /**
   * Determines whether the job should be canceled by the driver if the client gets disconnected.
   */
  private boolean cancelUponClientDisconnect = true;

  /**
   * Default constructor.
   */
  public JPPFJobSLA()
  {
  }

  /**
   * Initialize this job SLA with the specified execution policy.
   * @param policy the tasks execution policy.
   */
  public JPPFJobSLA(final ExecutionPolicy policy)
  {
    this(policy, 0, Integer.MAX_VALUE, false);
  }

  /**
   * Initialize this job SLA with the specified execution policy and priority.
   * @param policy the tasks execution policy.
   * @param priority the priority of this job.
   */
  public JPPFJobSLA(final ExecutionPolicy policy, final int priority)
  {
    this(policy, priority, Integer.MAX_VALUE, false);
  }

  /**
   * Initialize this job SLA with the specified execution policy, priority, max number of nodes and suspended indicator.
   * @param policy the tasks execution policy.
   * @param priority the priority of this job.
   * @param maxNodes the maximum number of nodes this job can run on. A value <= 0 means no limit on the number of nodes.
   * @param suspended determines whether this job is initially suspended.
   */
  public JPPFJobSLA(final ExecutionPolicy policy, final int priority, final int maxNodes, final boolean suspended)
  {
    this.executionPolicy = policy;
    this.priority = priority;
    this.maxNodes = maxNodes > 0 ? maxNodes : Integer.MAX_VALUE;
    this.suspended = suspended;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionPolicy getExecutionPolicy()
  {
    return executionPolicy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExecutionPolicy(final ExecutionPolicy executionPolicy)
  {
    this.executionPolicy = executionPolicy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getPriority()
  {
    return priority;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPriority(final int priority)
  {
    this.priority = priority;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxNodes()
  {
    return maxNodes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMaxNodes(final int maxNodes)
  {
    this.maxNodes = maxNodes > 0 ? maxNodes : Integer.MAX_VALUE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSuspended()
  {
    return suspended;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSuspended(final boolean suspended)
  {
    this.suspended = suspended;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFSchedule getJobSchedule()
  {
    return jobSchedule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setJobSchedule(final JPPFSchedule jobSchedule)
  {
    this.jobSchedule = jobSchedule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFSchedule getJobExpirationSchedule()
  {
    return jobExpirationSchedule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setJobExpirationSchedule(final JPPFSchedule jobExpirationSchedule)
  {
    this.jobExpirationSchedule = jobExpirationSchedule;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isBroadcastJob()
  {
    return broadcastJob;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setBroadcastJob(final boolean broadcastJob)
  {
    this.broadcastJob = broadcastJob;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCancelUponClientDisconnect()
  {
    return cancelUponClientDisconnect;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCancelUponClientDisconnect(final boolean cancelUponClientDisconnect)
  {
    this.cancelUponClientDisconnect = cancelUponClientDisconnect;
  }

  /**
   * Create a copy of this job SLA.
   * @return a {@link JPPFJobSLA} instance.
   */
  public JPPFJobSLA copy()
  {
    JPPFJobSLA sla = new JPPFJobSLA();
    sla.setBroadcastJob(broadcastJob);
    sla.setExecutionPolicy(executionPolicy);
    sla.setJobExpirationSchedule(jobExpirationSchedule);
    sla.setJobSchedule(jobSchedule);
    sla.setMaxNodes(maxNodes);
    sla.setPriority(priority);
    sla.setSuspended(suspended);
    sla.setCancelUponClientDisconnect(cancelUponClientDisconnect);
    return sla;
  }
}
