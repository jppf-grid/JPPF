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

package org.jppf.server.protocol;

import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.JobSLA;

/**
 * This class represents the Service Level Agreement Between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 */
public class JPPFJobSLA extends AbstractCommonSLA implements JobSLA
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
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

  @Override
  public int getMaxNodes()
  {
    return maxNodes;
  }

  @Override
  public void setMaxNodes(final int maxNodes)
  {
    this.maxNodes = maxNodes > 0 ? maxNodes : Integer.MAX_VALUE;
  }

  @Override
  public boolean isSuspended()
  {
    return suspended;
  }

  @Override
  public void setSuspended(final boolean suspended)
  {
    this.suspended = suspended;
  }

  @Override
  public boolean isBroadcastJob()
  {
    return broadcastJob;
  }

  @Override
  public void setBroadcastJob(final boolean broadcastJob)
  {
    this.broadcastJob = broadcastJob;
  }

  @Override
  public boolean isCancelUponClientDisconnect()
  {
    return cancelUponClientDisconnect;
  }

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
    copyTo(sla);
    sla.setBroadcastJob(broadcastJob);
    sla.setMaxNodes(maxNodes);
    sla.setSuspended(suspended);
    sla.setCancelUponClientDisconnect(cancelUponClientDisconnect);
    return sla;
  }
}
