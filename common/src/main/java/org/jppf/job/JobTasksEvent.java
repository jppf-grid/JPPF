/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.job;

import java.util.*;

import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.*;

/**
 * Instances of this class encapsulate information on job dispatches to nodes.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JobTasksEvent extends EventObject {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The name of the job whose tasks were dispatched.
   */
  private final String jobName;
  /**
   * The list of tasks that were disptached.
   */
  private final List<ServerTaskInformation> serverTasks;
  /**
   * The reason why the set of tasks wass returned by a node.
   */
  private final JobReturnReason returnReason;
  /**
   * Info on the node to which the job was dispatched.
   */
  private final JPPFManagementInfo nodeInfo;
  /**
   * The job SLA.
   */
  private final JobSLA jobSLA;
  /**
   * The job metadata.
   */
  private final JobMetadata jobMetadata;

  /**
   * Intialize this job dispatch event with the specified information.
   * @param jobUuid the uuid of the job whose tasks were dispatched.
   * @param jobName the name of the job whose tasks were dispatched.
   * @param serverTasks the list of tasks that were disptached.
   * @param returnReason the reason why the set of tasks wass returned by a node.
   * @param nodeInfo info on the node to which the job was dispatched.
   * @exclude
   */
  public JobTasksEvent(final String jobUuid, final String jobName, final List<ServerTaskInformation> serverTasks, final JobReturnReason returnReason, final JPPFManagementInfo nodeInfo) {
    this(jobUuid, jobName, null, null, serverTasks, returnReason, nodeInfo);
  }

  /**
   * Initialize this job dispatch event with the specified information.
   * @param jobUuid the uuid of the job whose tasks were dispatched.
   * @param jobName the name of the job whose tasks were dispatched.
   * @param jobSLA the job SLA.
   * @param jobMetadata the job metadata.
   * @param serverTasks the list of tasks that were disptached.
   * @param returnReason the reason why the set of tasks wass returned by a node.
   * @param nodeInfo info on the node to which the job was dispatched.
   * @exclude
   */
  public JobTasksEvent(final String jobUuid, final String jobName, final JobSLA jobSLA, final JobMetadata jobMetadata,
    final List<ServerTaskInformation> serverTasks, final JobReturnReason returnReason, final JPPFManagementInfo nodeInfo) {
    super(jobUuid);
    this.jobName = jobName;
    this.serverTasks = serverTasks;
    this.returnReason = returnReason;
    this.nodeInfo = nodeInfo;
    this.jobSLA = jobSLA;
    this.jobMetadata = jobMetadata;
  }

  /**
   * Get the uuid of the job to which the tasks belong.
   * @return the uuid as a {@code String}.
   */
  public String getJobUuid() {
    return (String) getSource();
  }

  /**
   * Get the name of the job to which the tasks belong.
   * @return the name as a {@code String}.
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * Get the list of tasks that were disptached.
   * @return a {@code List} of {@code ServerTaskInformation} objects.
   */
  public List<ServerTaskInformation> getReturnedTasks() {
    return serverTasks;
  }

  /**
   * Get the list of tasks that were disptached.
   * @return a {@code List} of {@code ServerTaskInformation} objects.
   */
  public List<ServerTaskInformation> getTasks() {
    return serverTasks;
  }

  /**
   * Get the reason why the set of tasks was returned by a node.
   * @return the return reason as a {@code JobReturnReason} element.
   */
  public JobReturnReason getReturnReason() {
    return returnReason;
  }

  /**
   * Get the information on the node to which the job was dispatched.
   * @return a {@link JPPFManagementInfo} object.
   */
  public JPPFManagementInfo getNodeInfo() {
    return nodeInfo;
  }

  /**
   * Get the job SLA from this event.
   * @return an instance of {@link JobSLA}.
   */
  public JobSLA getJobSLA() {
    return jobSLA;
  }

  /**
   * Get the job metadata from this event.
   * @return an instance of {@link JobMetadata}.
   */
  public JobMetadata getJobMetadata() {
    return jobMetadata;
  }
}
