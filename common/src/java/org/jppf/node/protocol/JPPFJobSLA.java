/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
public class JPPFJobSLA extends AbstractCommonSLA implements JobSLA {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The maximum number of nodes this job can run on.
   */
  private int maxNodes = Integer.MAX_VALUE;
  /**
   * The maximum number of groups of master/slave nodes the job can be executed on at any given time.
   * <p>This setting means that the job can only be executed on at most {@code maxMasterNodeGroups} master nodes and all their slaves.
   */
  private int maxNodeProvisioningGroups = Integer.MAX_VALUE;
  /**
   * The priority of this job, used by the server to prioritize queued jobs.
   */
  private int priority = 0;
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
  private JPPFSchedule dispatchExpirationSchedule;
  /**
   * The number of times a dispatched task can expire before it is finally cancelled.
   */
  private int maxDispatchExpirations = 0;
  /**
   * Maximum number of times a task can rsubmit itself via {@link org.jppf.node.protocol.AbstractTask#setResubmit(boolean) AbstractTask.setResubmit(boolean)}.
   */
  private int maxTaskResubmits = 1;
  /**
   * Whether the max resubmits limit for tasks is also applied when tasks are resubmitted due to a node error.
   */
  private boolean applyMaxResubmitsUponNodeError = false;
  /**
   * Whether remote class loading is enabled for the job.
   */
  private boolean remoteClassLoadingEnabled = true;
  /**
   * The global execution policy which applies to the driver only.
   * @since 5.2
   */
  private ExecutionPolicy gridExecutionPolicy;
  /**
   * The configuration of the node(s) this job should be executed on.
   * @since 5.2
   */
  private JPPFNodeConfigSpec nodeConfigurationSpec;

  /**
   * Default constructor.
   */
  public JPPFJobSLA() {
  }

  @Override
  public int getPriority() {
    return priority;
  }

  @Override
  public JobSLA setPriority(final int priority) {
    this.priority = priority;
    return this;
  }

  @Override
  public int getMaxNodes() {
    return maxNodes;
  }

  @Override
  public JobSLA setMaxNodes(final int maxNodes) {
    this.maxNodes = maxNodes > 0 ? maxNodes : Integer.MAX_VALUE;
    return this;
  }

  @Override
  public int getMaxNodeProvisioningGroupss() {
    return maxNodeProvisioningGroups;
  }

  @Override
  public JobSLA setMaxNodeProvisioningGroups(final int maxNodeProvisioningGroups) {
    if (maxNodeProvisioningGroups > 0) this.maxNodeProvisioningGroups = maxNodeProvisioningGroups;
    return this;
  }

  @Override
  public boolean isSuspended() {
    return suspended;
  }

  @Override
  public JobSLA setSuspended(final boolean suspended) {
    this.suspended = suspended;
    return this;
  }

  @Override
  public boolean isBroadcastJob() {
    return broadcastJob;
  }

  @Override
  public JobSLA setBroadcastJob(final boolean broadcastJob) {
    this.broadcastJob = broadcastJob;
    return this;
  }

  @Override
  public boolean isCancelUponClientDisconnect() {
    return cancelUponClientDisconnect;
  }

  @Override
  public JobSLA setCancelUponClientDisconnect(final boolean cancelUponClientDisconnect) {
    this.cancelUponClientDisconnect = cancelUponClientDisconnect;
    return this;
  }

  /**
   * Create a copy of this job SLA.
   * @return a {@link JPPFJobSLA} instance.
   */
  public JPPFJobSLA copy() {
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
  public String getResultsStrategy() {
    return resultsStrategy;
  }

  @Override
  public JobSLA setResultsStrategy(final String name) {
    this.resultsStrategy = name;
    return this;
  }

  @Override
  public ClassPath getClassPath() {
    return classPath;
  }

  @Override
  public JobSLA setClassPath(final ClassPath classpath) {
    if (classpath == null) throw new IllegalArgumentException("classpath cannot be null");
    this.classPath = classpath;
    return this;
  }

  @Override
  public JPPFSchedule getDispatchExpirationSchedule() {
    return dispatchExpirationSchedule;
  }

  @Override
  public JobSLA setDispatchExpirationSchedule(final JPPFSchedule dispatchExpirationSchedule) {
    this.dispatchExpirationSchedule = dispatchExpirationSchedule;
    return this;
  }

  @Override
  public int getMaxDispatchExpirations() {
    return maxDispatchExpirations;
  }

  @Override
  public JobSLA setMaxDispatchExpirations(final int maxDispatchExpirations) {
    this.maxDispatchExpirations = maxDispatchExpirations;
    return this;
  }

  @Override
  public int getMaxTaskResubmits() {
    return maxTaskResubmits;
  }

  @Override
  public JobSLA setMaxTaskResubmits(final int maxResubmits) {
    this.maxTaskResubmits = maxResubmits;
    return this;
  }

  @Override
  public boolean isApplyMaxResubmitsUponNodeError() {
    return applyMaxResubmitsUponNodeError;
  }

  @Override
  public JobSLA setApplyMaxResubmitsUponNodeError(final boolean applyMaxResubmitsUponNodeError) {
    this.applyMaxResubmitsUponNodeError = applyMaxResubmitsUponNodeError;
    return this;
  }

  @Override
  public boolean isRemoteClassLoadingEnabled() {
    return remoteClassLoadingEnabled;
  }

  @Override
  public JobSLA setRemoteClassLoadingEnabled(final boolean enabled) {
    this.remoteClassLoadingEnabled = enabled;
    return this;
  }

  @Override
  public ExecutionPolicy getGridExecutionPolicy() {
    return gridExecutionPolicy;
  }

  @Override
  public JobSLA setGridExecutionPolicy(final ExecutionPolicy gridExecutionPolicy) {
    this.gridExecutionPolicy = gridExecutionPolicy;
    return this;
  }

  @Override
  public JPPFNodeConfigSpec getDesiredNodeConfiguration() {
    return nodeConfigurationSpec;
  }

  @Override
  public JobSLA setDesiredNodeConfiguration(final JPPFNodeConfigSpec nodeConfigurationSpec) {
    this.nodeConfigurationSpec = nodeConfigurationSpec;
    return this;
  }

  @Override
  public JobSLA setExecutionPolicy(final ExecutionPolicy executionPolicy) {
    return (JobSLA) super.setExecutionPolicy(executionPolicy);
  }

  @Override
  public JobSLA setJobSchedule(final JPPFSchedule jobSchedule) {
    return (JobSLA) super.setJobSchedule(jobSchedule);
  }

  @Override
  public JobSLA setJobExpirationSchedule(final JPPFSchedule jobExpirationSchedule) {
    return (JobSLA) super.setJobExpirationSchedule(jobExpirationSchedule);
  }
}
