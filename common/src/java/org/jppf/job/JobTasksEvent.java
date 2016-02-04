/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.List;

import org.jppf.management.JPPFManagementInfo;

/**
 * Instances of this class encapsulate information on job dispatches to nodes.
 * @author Laurent Cohen
 * @since 5.1
 */
@SuppressWarnings("deprecation")
public class JobTasksEvent extends TaskReturnEvent {
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
    super(jobUuid, jobName, serverTasks, returnReason, nodeInfo);
  }

  @Override
  public String getJobUuid() {
    return super.getJobUuid();
  }

  @Override
  public String getJobName() {
    return super.getJobName();
  }

  /**
   * Get the tasks dispatched to or received from the node. 
   * @return a list of {@link ServerTaskInformation} objects.
   */
  public List<ServerTaskInformation> getTasks() {
    return super.getReturnedTasks();
  }

  @Override
  public JobReturnReason getReturnReason() {
    return super.getReturnReason();
  }

  @Override
  public JPPFManagementInfo getNodeInfo() {
    return super.getNodeInfo();
  }
}
