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

package org.jppf.server.job.management;

import javax.management.NotificationEmitter;

import org.jppf.job.JobInformation;


/**
 * This is the job management MBean interface.
 * @author Laurent Cohen
 */
public interface DriverJobManagementMBean extends NotificationEmitter
{
  /**
   * The name under which this MBean is registered with the MBean server.
   */
  String MBEAN_NAME = "org.jppf:name=jobManagement,type=driver";
  /**
   * Cancel the job with the specified id.
   * @param jobUuid the uuid of the job to cancel.
   * @throws Exception if any error occurs.
   */
  void cancelJob(String jobUuid) throws Exception;
  /**
   * Suspend the job with the specified id.
   * @param jobUuid the uuid of the job to suspend.
   * @param requeue true if the sub-jobs running on each node should be canceled and requeued,
   * false if they should be left to execute until completion.
   * @throws Exception if any error occurs.
   */
  void suspendJob(String jobUuid, Boolean requeue) throws Exception;
  /**
   * Resume the job with the specified id.
   * @param jobUuid the uuid of the job to resume.
   * @throws Exception if any error occurs.
   */
  void resumeJob(String jobUuid) throws Exception;
  /**
   * Update the maximum number of nodes a job can run on.
   * @param jobUuid the uuid of the job to update.
   * @param maxNodes the new maximum number of nodes for the job.
   * @throws Exception if any error occurs.
   */
  void updateMaxNodes(String jobUuid, Integer maxNodes) throws Exception;
  /**
   * Get the set of uuids for all the jobs currently queued or executing.
   * @return an array of uuids as strings.
   * @throws Exception if any error occurs.
   */
  String[] getAllJobIds() throws Exception;
  /**
   * Get an object describing the job with the specified id.
   * @param jobUuid the uuid of the job to get information about.
   * @return an instance of <code>JobInformation</code>.
   * @throws Exception if any error occurs.
   */
  JobInformation getJobInformation(String jobUuid) throws Exception;
  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @param jobUuid the uuid of the job for which to find node information.
   * @return an array of <code>NodeManagementInfo</code>, <code>JobInformation</code> instances.
   * @throws Exception if any error occurs.
   */
  NodeJobInformation[] getNodeInformation(String jobUuid) throws Exception;
  /**
   * Update the priority of a job.
   * @param jobUuid the uuid of the job to update.
   * @param newPriority the job's new priority value.
   */
  void updatePriority(String jobUuid, Integer newPriority);
}
