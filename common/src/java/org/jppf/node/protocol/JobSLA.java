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
 * This interface represents the Service Level Agreement between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 */
public class JobSLA extends JobCommonSLA<JobSLA> {
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
   */
  private ExecutionPolicy gridExecutionPolicy;
  /**
   * The configuration of the node(s) this job should be executed on.
   */
  private JPPFNodeConfigSpec nodeConfigurationSpec;
  /**
   * Whether this job is persisted by the driver and behavior upon recovery.
   */
  private PersistenceSpec persistenceSpec = new PersistenceSpec();

  /**
   * Default constructor.
   */
  public JobSLA() {
  }

  /**
   * Get the priority of this job.
   * @return the priority as an int.
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Set the priority of this job.
   * @param priority the priority as an int.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setPriority(final int priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Get the maximum number of nodes this job can run on.
   * @return the number of nodes as an int value.
   */
  public int getMaxNodes() {
    return maxNodes;
  }

  /**
   * Set the maximum number of nodes this job can run on.
   * @param maxNodes the number of nodes as an int value. A value <= 0 means no limit on the number of nodes.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setMaxNodes(final int maxNodes) {
    this.maxNodes = maxNodes > 0 ? maxNodes : Integer.MAX_VALUE;
    return this;
  }

  /**
   * Get the maximum number of groups of master/slaves nodes the job can be executed on at any given time.
   * <p>This setting means that the job can only be executed on at most {@code maxMasterNodeGroups} master nodes and all their slaves.
   * @return the number of nodes as an int value.
   * @since 5.1
   */
  public int getMaxNodeProvisioningGroupss() {
    return maxNodeProvisioningGroups;
  }

  /**
   * Set the maximum number of groups of master/slaves nodes the job can be executed on at any given time.
   * <p>This setting means that the job can only be executed on at most {@code maxMasterNodeGroups} master nodes and all their slaves.
   * @param maxNodeProvisioningGroups the number of nodes as an int value. A value <= 0 means no limit on the number of nodes.
   * Any value <= 0 will be ignored.
   * @return this SLA, for mathod chaining.
   * @since 5.1
   */
  public JobSLA setMaxNodeProvisioningGroups(final int maxNodeProvisioningGroups) {
    if (maxNodeProvisioningGroups > 0) this.maxNodeProvisioningGroups = maxNodeProvisioningGroups;
    return this;
  }

  /**
   * Determine whether this job is initially suspended.
   * @return true if the job is suspended, false otherwise.
   */
  public boolean isSuspended() {
    return suspended;
  }

  /**
   * Specify whether this job is initially suspended.
   * @param suspended true if the job is suspended, false otherwise.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setSuspended(final boolean suspended) {
    this.suspended = suspended;
    return this;
  }

  /**
   * Determine whether the job is a broadcast job.
   * @return true for a broadcast job, false otherwise.
   */
  public boolean isBroadcastJob() {
    return broadcastJob;
  }

  /**
   * Specify whether the job is a broadcast job.
   * @param broadcastJob true for a broadcast job, false otherwise.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setBroadcastJob(final boolean broadcastJob) {
    this.broadcastJob = broadcastJob;
    return this;
  }

  /**
   * Determine whether the job should be canceled by the driver if the client gets disconnected.
   * @return <code>true</code> if the job should be canceled (this is the default), <code>false</code> otherwise.
   */
  public boolean isCancelUponClientDisconnect() {
    return cancelUponClientDisconnect;
  }

  /**
   * Specify whether the job should be canceled by the driver if the client gets disconnected.
   * @param cancelUponClientDisconnect <code>true</code> if the job should be canceled, <code>false</code> otherwise.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setCancelUponClientDisconnect(final boolean cancelUponClientDisconnect) {
    this.cancelUponClientDisconnect = cancelUponClientDisconnect;
    return this;
  }

  /**
   * Get the strategy used to return the results back to the client.
   * @return the name of the strategy to use.
   * @exclude
   */
  public String getResultsStrategy() {
    return resultsStrategy;
  }

  /**
   * Set the strategy used to return the results back to the client.
   * @param name the name of the strategy to use.
   * @return this SLA, for mathod chaining.
   * @exclude
   */
  public JobSLA setResultsStrategy(final String name) {
    this.resultsStrategy = name;
    return this;
  }

  /**
   * Get the class path associated with the job.
   * @return an instance of {@link ClassPath}.
   */
  public ClassPath getClassPath() {
    return classPath;
  }

  /**
   * Set the class path associated with the job.
   * @param classpath an instance of {@link ClassPath}.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setClassPath(final ClassPath classpath) {
    if (classpath == null) throw new IllegalArgumentException("classpath cannot be null");
    this.classPath = classpath;
    return this;
  }

  /**
   * Get the expiration schedule for any subset of the job dispatched to a node.
   * @return a {@link JPPFSchedule} instance.
   */
  public JPPFSchedule getDispatchExpirationSchedule() {
    return dispatchExpirationSchedule;
  }

  /**
   * Set the expiration schedule for any subset of the job dispatched to a node.
   * @param schedule a {@link JPPFSchedule} instance.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setDispatchExpirationSchedule(final JPPFSchedule schedule) {
    this.dispatchExpirationSchedule = schedule;
    return this;
  }

  /**
   * Get the number of times a dispatched task can expire before it is finally cancelled.
   * @return the number of expirations as an int.
   */
  public int getMaxDispatchExpirations() {
    return maxDispatchExpirations;
  }

  /**
   * Set the number of times a dispatched task can expire before it is finally cancelled.
   * @param max the number of expirations as an int.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setMaxDispatchExpirations(final int max) {
    this.maxDispatchExpirations = max;
    return this;
  }

  /**
   * Get the naximum number of times a task can resubmit itself via {@link org.jppf.node.protocol.AbstractTask#setResubmit(boolean) AbstractTask.setResubmit(boolean)}.
   * The default value is 1, meaning that a task can resubmit itself at most once.
   * @return the maximum number of resubmits; a value of 0 or less means tasks in the job cannot be resubmitted.
   */
  public int getMaxTaskResubmits() {
    return maxTaskResubmits;
  }

  /**
   * Set the naximum number of times a task can resubmit itself via {@link org.jppf.node.protocol.AbstractTask#setResubmit(boolean) AbstractTask.setResubmit(boolean)}.
   * @param maxResubmits the maximum number of resubmits; a value of 0 or less means tasks in the job cannot be resubmitted.
   * @return this SLA, for mathod chaining.
   */
  public JobSLA setMaxTaskResubmits(final int maxResubmits) {
    this.maxTaskResubmits = maxResubmits;
    return this;
  }

  /**
   * Determine whether the max resubmits limit for tasks is also applied when tasks are resubmitted due to a node error.
   * This flag is false by default.
   * @return {@code true} if the max resubmits count is applied upon node errors, {@code false} otherwise.
   * @since 4.2
   */
  public boolean isApplyMaxResubmitsUponNodeError() {
    return applyMaxResubmitsUponNodeError;
  }

  /**
   * Specify whether the max resubmits limit for tasks should also be applied when tasks are resubmitted due to a node error.
   * @param applyMaxResubmitsUponNodeError {@code true} to specify that the max resubmits count is applied upon node errors, {@code false} otherwise.
   * @return this SLA, for mathod chaining.
   * @since 4.2
   */
  public JobSLA setApplyMaxResubmitsUponNodeError(final boolean applyMaxResubmitsUponNodeError) {
    this.applyMaxResubmitsUponNodeError = applyMaxResubmitsUponNodeError;
    return this;
  }

  /**
   * Determine whether remote class loading is enabled for the job.
   * The default value, when not specified via {@link #setRemoteClassLoadingEnabled(boolean)}, is {@code true}.
   * @return {@code true} is remote class loading is enabled, {@code false} otherwise.
   * @since 4.2
   */
  public boolean isRemoteClassLoadingEnabled() {
    return remoteClassLoadingEnabled;
  }

  /**
   * Specify whether remote class loading is enabled for the job.
   * @param enabled {@code true} to enable remote class loading, {@code false} to disable it.
   * @return this SLA, for mathod chaining.
   * @since 4.2
   */
  public JobSLA setRemoteClassLoadingEnabled(final boolean enabled) {
    this.remoteClassLoadingEnabled = enabled;
    return this;
  }

  /**
   * Get the global grid execution policy (which applies to the driver).
   * @return an {@link ExecutionPolicy} object.
   * @since 5.2
   */
  public ExecutionPolicy getGridExecutionPolicy() {
    return gridExecutionPolicy;
  }

  /**
   * Set the global grid execution policy (which applies to the driver).
   * @param policy an {@link ExecutionPolicy} object.
   * @return this SLA, for mathod chaining.
   * @since 5.2
   */
  public JobSLA setGridExecutionPolicy(final ExecutionPolicy policy) {
    this.gridExecutionPolicy = policy;
    return this;
  }

  /**
   * Get the configuration of the node(s) this job should be executed on,
   * forcing a restart of the node with appropriate configuration overrides if there is no such node.
   * @return the desired configuration as a {@link JPPFNodeConfigSpec} object.
   * @since 5.2
   */
  public JPPFNodeConfigSpec getDesiredNodeConfiguration() {
    return nodeConfigurationSpec;
  }

  /**
   * Set the configuration of the node(s) this job should be executed on,
   * forcing a restart of the node with appropriate configuration overrides if there is no such node.
   * @param nodeConfigurationSpec the desired configuration as a {@link JPPFNodeConfigSpec} object.
   * @return this SLA, for mathod chaining.
   * @since 5.2
   */
  public JobSLA setDesiredNodeConfiguration(final JPPFNodeConfigSpec nodeConfigurationSpec) {
    this.nodeConfigurationSpec = nodeConfigurationSpec;
    return this;
  }

  /**
   * Get the specification of the job persistence in the driver.
   * @return a {@link PersistenceSpec} instance.
   * @since 6.0
   */
  public PersistenceSpec getPersistenceSpec() {
    return persistenceSpec;
  }

  /**
   * Set the specification of the job persistence in the driver.
   * @param persistenceSpec a {@link PersistenceSpec} instance.
   */
  void setPersistenceSpec(final PersistenceSpec persistenceSpec) {
    this.persistenceSpec = persistenceSpec;
  }

  /**
   * Create a copy of this job SLA.
   * @return a {@link JobSLA} instance.
   */
  public JobSLA copy() {
    JobSLA sla = new JobSLA();
    copyTo(sla);
    sla.setApplyMaxResubmitsUponNodeError(applyMaxResubmitsUponNodeError);
    sla.setBroadcastJob(broadcastJob);
    sla.setCancelUponClientDisconnect(cancelUponClientDisconnect);
    sla.setClassPath(classPath);
    sla.setDesiredNodeConfiguration(nodeConfigurationSpec);
    sla.setDispatchExpirationSchedule(dispatchExpirationSchedule);
    sla.setGridExecutionPolicy(gridExecutionPolicy);
    sla.setMaxDispatchExpirations(maxDispatchExpirations);
    sla.setMaxNodeProvisioningGroups(maxNodeProvisioningGroups);
    sla.setMaxNodes(maxNodes);
    sla.setMaxTaskResubmits(maxTaskResubmits);
    sla.setPersistenceSpec(persistenceSpec);
    sla.setPriority(priority);
    sla.setRemoteClassLoadingEnabled(remoteClassLoadingEnabled);
    sla.setResultsStrategy(resultsStrategy);
    sla.setSuspended(suspended);
    return sla;
  }
}
