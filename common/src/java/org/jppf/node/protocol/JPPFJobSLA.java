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

package org.jppf.node.protocol;

import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.scheduling.JPPFSchedule;

/**
 * This class represents the Service Level Agreement Between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 * @exclude
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
   * The priority of this job, used by the server to prioritize queued jobs.
   */
  protected int priority = 0;
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
   * Get the name of the strategy used to return the results back to the client.
   */
  private String resultsStrategy = null;
  /**
   * The classpath associated with the job.
   */
  private ClassPath classPath = new ClassPathImpl();
  /**
   * The expiration schedule for any subset of the job dispatched to a node.
   */
  protected JPPFSchedule dispatchExpirationSchedule = null;
  /**
   * The number of times a dispatched task can expire before it is finally cancelled.
   */
  protected int maxDispatchExpirations = 0;
  /**
   * Maximum number of times a task can rsubmit itself via {@link org.jppf.node.protocol.AbstractTask#setResubmit(boolean) AbstractTask.setResubmit(boolean)}.
   */
  protected int maxTaskResubmits = 1;
  /**
   * Whether the max resubmits limit for tasks is also applied when tasks are resubmitted due to a node error.
   */
  private boolean applyMaxResubmitsUponNodeError = false;
  /**
   * Whether remote class loading is enabled for the job.
   */
  private boolean remoteClassLoadingEnabled = true;

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
  public int getPriority()
  {
    return priority;
  }

  @Override
  public void setPriority(final int priority)
  {
    this.priority = priority;
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
    sla.setResultsStrategy(resultsStrategy);
    sla.setClassPath(classPath);
    sla.setDispatchExpirationSchedule(dispatchExpirationSchedule);
    sla.setMaxDispatchExpirations(maxDispatchExpirations);
    return sla;
  }

  @Override
  public String getResultsStrategy()
  {
    return resultsStrategy;
  }

  @Override
  public void setResultsStrategy(final String name)
  {
    this.resultsStrategy = name;
  }

  @Override
  public ClassPath getClassPath()
  {
    return classPath;
  }

  @Override
  public void setClassPath(final ClassPath classpath)
  {
    if (classpath == null) throw new IllegalArgumentException("classpath cannot be null");
    this.classPath = classpath;
  }

  @Override
  public JPPFSchedule getDispatchExpirationSchedule()
  {
    return dispatchExpirationSchedule;
  }

  @Override
  public void setDispatchExpirationSchedule(final JPPFSchedule dispatchExpirationSchedule)
  {
    this.dispatchExpirationSchedule = dispatchExpirationSchedule;
  }

  @Override
  public int getMaxDispatchExpirations()
  {
    return maxDispatchExpirations;
  }

  @Override
  public void setMaxDispatchExpirations(final int maxDispatchExpirations)
  {
    this.maxDispatchExpirations = maxDispatchExpirations;
  }

  @Override
  public int getMaxTaskResubmits() {
    return maxTaskResubmits;
  }

  @Override
  public void setMaxTaskResubmits(final int maxResubmits) {
    this.maxTaskResubmits = maxResubmits;
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public boolean isApplyMaxResubmitsUponNodeError() {
    return applyMaxResubmitsUponNodeError;
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public void setApplyMaxResubmitsUponNodeError(final boolean applyMaxResubmitsUponNodeError) {
    this.applyMaxResubmitsUponNodeError = applyMaxResubmitsUponNodeError;
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public boolean isRemoteClassLoadingEnabled() {
    return remoteClassLoadingEnabled;
  }

  /**
   * {@inheritDoc}
   * @since 4.2
   */
  @Override
  public void setRemoteClassLoadingEnabled(final boolean enabled) {
    this.remoteClassLoadingEnabled = enabled;
  }
}
