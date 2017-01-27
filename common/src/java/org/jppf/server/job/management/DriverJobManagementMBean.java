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

package org.jppf.server.job.management;

import java.util.Map;

import javax.management.NotificationEmitter;

import org.jppf.job.*;
import org.jppf.node.protocol.*;


/**
 * This is the job management MBean interface.
 * @author Laurent Cohen
 */
public interface DriverJobManagementMBean extends NotificationEmitter {
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
   * Cancel the jobs specified with a given job selector.
   * @param selector determines which jobs to cancel.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  void cancelJobs(JobSelector selector) throws Exception;

  /**
   * Suspend the job with the specified id.
   * @param jobUuid the uuid of the job to suspend.
   * @param requeue {@code true} if the sub-jobs running on each node should be canceled and requeued, {@code false} if they should be left to execute until completion.
   * @throws Exception if any error occurs.
   */
  void suspendJob(String jobUuid, Boolean requeue) throws Exception;

  /**
   * Suspend the jobs specified with a given job selector.
   * @param selector determines which jobs to suspend.
   * @param requeue {@code true} if the sub-jobs running on each node should be canceled and requeued, {@code false} if they should be left to execute until completion.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  void suspendJobs(JobSelector selector, Boolean requeue) throws Exception;

  /**
   * Resume the job with the specified id.
   * @param jobUuid the uuid of the job to resume.
   * @throws Exception if any error occurs.
   */
  void resumeJob(String jobUuid) throws Exception;

  /**
   * Resume the jobs specified with a given job selector.
   * @param selector determines which jobs to resume.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  void resumeJobs(JobSelector selector) throws Exception;

  /**
   * Update the maximum number of nodes a job can run on.
   * @param jobUuid the uuid of the job to update.
   * @param maxNodes the new maximum number of nodes for the job.
   * @throws Exception if any error occurs.
   */
  void updateMaxNodes(String jobUuid, Integer maxNodes) throws Exception;

  /**
   * Update the maximum number of nodes the specified jobs can run on.
   * @param selector determines for which jobs to change the maximum number of nodes.
   * @param maxNodes the new maximum number of nodes for the job.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  void updateMaxNodes(JobSelector selector, Integer maxNodes) throws Exception;

  /**
   * Get the set of uuids for all the jobs currently queued or executing.
   * @return an array of uuids as strings.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  String[] getAllJobUuids() throws Exception;

  /**
   * Get an object describing the job with the specified id.
   * @param jobUuid the uuid of the job to get information about.
   * @return an instance of <code>JobInformation</code>.
   * @throws Exception if any error occurs.
   */
  JobInformation getJobInformation(String jobUuid) throws Exception;

  /**
   * The job information for all the jobs accepted by the specified selector.
   * @param selector determines for which jobs to return information.
   * @return an array of {@link JobInformation} objects, possibly empty if no job matches the selector.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  JobInformation[] getJobInformation(JobSelector selector) throws Exception;

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @param jobUuid the uuid of the job for which to find node information.
   * @return an array of <code>NodeManagementInfo</code>, <code>JobInformation</code> instances.
   * @throws Exception if any error occurs.
   */
  NodeJobInformation[] getNodeInformation(String jobUuid) throws Exception;

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched, for all the jobs specified by a job selector.
   * @param selector determines for which jobs to return the node disptaches.
   * @return a mapping of job uuids to a corresponding array of {@link NodeJobInformation} instances.
   * @throws Exception if any error occurs.
   */
  Map<String, NodeJobInformation[]> getNodeInformation(JobSelector selector) throws Exception;

  /**
   * Update the priority of a job.
   * @param jobUuid the uuid of the job to update.
   * @param newPriority the job's new priority value.
   */
  void updatePriority(String jobUuid, Integer newPriority);

  /**
   * Update the priority of the jobs specified by a job selector.
   * @param selector determines for which jobs to change the maximum number of nodes.
   * @param newPriority the jobs new priority value.
   */
  void updatePriority(JobSelector selector, Integer newPriority);

  /**
   * Update the SLA and/or metadata of the specified jobs.
   * @param selector specifies which jobs to update.
   * @param sla the new job SLA; if {@code null} then the sla is not changed.
   * @param metadata the new job metadata; if {@code null} then the metadata is not changed.
   * @since 5.1
   */
  void updateJobs(JobSelector selector, JobSLA sla, JobMetadata metadata);
}
