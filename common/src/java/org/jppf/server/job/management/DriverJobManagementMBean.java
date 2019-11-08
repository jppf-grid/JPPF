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

package org.jppf.server.job.management;

import java.util.*;

import javax.management.NotificationEmitter;

import org.jppf.job.*;
import org.jppf.management.doc.*;
import org.jppf.node.protocol.*;


/**
 * This is the job management MBean interface.
 * @author Laurent Cohen
 */
@MBeanDescription("the server-side job management interface")
@MBeanNotif(description = "notification on the status of a job", notifClass = JobNotification.class)
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
  @MBeanDescription("cancel the job with the specified uuid")
  void cancelJob(@MBeanParamName("jobUuid") String jobUuid) throws Exception;

  /**
   * Cancel the jobs specified with a given job selector.
   * @param selector determines which jobs to cancel.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  @MBeanDescription("cancel the selected jobs")
  void cancelJobs(@MBeanParamName("jobSelector") JobSelector selector) throws Exception;

  /**
   * Suspend the job with the specified uuid.
   * @param jobUuid the uuid of the job to suspend.
   * @param requeue {@code true} if the sub-jobs running on each node should be canceled and requeued, {@code false} if they should be left to execute until completion.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("suspend the job with the specified uuid")
  void suspendJob(@MBeanParamName("jobUuid") String jobUuid, @MBeanParamName("requeue") Boolean requeue) throws Exception;

  /**
   * Suspend the jobs specified with a given job selector.
   * @param selector determines which jobs to suspend.
   * @param requeue {@code true} if the sub-jobs running on each node should be canceled and requeued, {@code false} if they should be left to execute until completion.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  @MBeanDescription("suspend the selected jobs")
  void suspendJobs(@MBeanParamName("jobSelector") JobSelector selector, @MBeanParamName("requeue") Boolean requeue) throws Exception;

  /**
   * Resume the job with the specified uuid.
   * @param jobUuid the uuid of the job to resume.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("resume the job with the specified uuid")
  void resumeJob(@MBeanParamName("jobUuid") String jobUuid) throws Exception;

  /**
   * Resume the jobs specified with a given job selector.
   * @param selector determines which jobs to resume.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  @MBeanDescription("resume the selected jobs")
  void resumeJobs(@MBeanParamName("jobSelector") JobSelector selector) throws Exception;

  /**
   * Update the maximum number of nodes the job with the specified uuid can run on.
   * @param jobUuid the uuid of the job to update.
   * @param maxNodes the new maximum number of nodes for the job.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("update the maximum number of nodes the job with the specified uuid can run on")
  void updateMaxNodes(@MBeanParamName("jobUuid") String jobUuid, @MBeanParamName("maxNodes") Integer maxNodes) throws Exception;

  /**
   * Update the maximum number of nodes the specified jobs can run on.
   * @param selector determines for which jobs to change the maximum number of nodes.
   * @param maxNodes the new maximum number of nodes for the job.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  @MBeanDescription("update the maximum number of nodes the selected jobs can run on")
  void updateMaxNodes(@MBeanParamName("jobSelector") JobSelector selector, @MBeanParamName("maxNodes") Integer maxNodes) throws Exception;

  /**
   * Get the set of uuids for all the jobs currently queued or executing.
   * @return an array of uuids as strings.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  @MBeanDescription("the set of uuids for all the jobs currently queued or executing")
  String[] getAllJobUuids() throws Exception;

  /**
   * Get an object describing the job with the specified uuid.
   * @param jobUuid the uuid of the job to get information about.
   * @return an instance of <code>JobInformation</code>.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get an object describing the job with the specified uuid")
  JobInformation getJobInformation(@MBeanParamName("jobUuid") String jobUuid) throws Exception;

  /**
   * Get the job information for all the jobs accepted by the specified selector.
   * @param selector determines for which jobs to return information.
   * @return an array of {@link JobInformation} objects, possibly empty if no job matches the selector.
   * @throws Exception if any error occurs.
   * @since 5.1
   */
  @MBeanDescription("get the job information for the selected jobs")
  JobInformation[] getJobInformation(@MBeanParamName("jobSelector") JobSelector selector) throws Exception;

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @param jobUuid the uuid of the job for which to find node information.
   * @return an array of <code>NodeManagementInfo</code>, <code>JobInformation</code> instances.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get a list of objects describing the nodes to which the whole or part of a job was dispatched")
  NodeJobInformation[] getNodeInformation(@MBeanParamName("jobUuid") String jobUuid) throws Exception;

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched, for all the jobs specified by a job selector.
   * @param selector determines for which jobs to return the node disptaches.
   * @return a mapping of job uuids to a corresponding array of {@link NodeJobInformation} instances.
   * @throws Exception if any error occurs.
   */
  @MBeanDescription("get, for the selected jobs, a mapping of job uuid to an object describing the nodes to which the whole or part of each job was dispatched")
  @MBeanElementType(type = Map.class, parameters = { "java.lang.String", "[Lorg.jppf.server.job.management.NodeJobInformation;" })
  Map<String, NodeJobInformation[]> getNodeInformation(@MBeanParamName("jobSelector") JobSelector selector) throws Exception;

  /**
   * Update the priority of the job with the specified uuid.
   * @param jobUuid the uuid of the job to update.
   * @param newPriority the job's new priority value.
   */
  @MBeanDescription("update the priority of the job with the specified uuid")
  void updatePriority(@MBeanParamName("jobUuid") String jobUuid, @MBeanParamName("newPriority") Integer newPriority);

  /**
   * Update the priority of the jobs specified by a job selector.
   * @param selector determines for which jobs to change the maximum number of nodes.
   * @param newPriority the jobs new priority value.
   */
  @MBeanDescription("update the priority of the selected jobs")
  void updatePriority(@MBeanParamName("jobSelector") JobSelector selector, @MBeanParamName("newPriority") Integer newPriority);

  /**
   * Update the SLA and/or metadata of the specified jobs.
   * @param selector specifies which jobs to update.
   * @param sla the new job SLA; if {@code null} then the sla is not changed.
   * @param metadata the new job metadata; if {@code null} then the metadata is not changed.
   * @since 5.1
   */
  @MBeanDescription("update the SLA and/or metadata of the selected jobs")
  void updateJobs(@MBeanParamName("jobSelector") JobSelector selector, @MBeanParamName("sla") JobSLA sla, @MBeanParamName("metadata") JobMetadata metadata);
}
