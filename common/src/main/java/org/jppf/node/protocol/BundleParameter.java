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

package org.jppf.node.protocol;

/**
 * Constants used when a client sends an admin command to a server.
 * @author Laurent Cohen
 * @exclude
 */
public enum BundleParameter {
  /**
   * To determine whether a node connection is for a peer driver or an actual execution node.
   */
  IS_PEER,
  /**
   * Parameter for an eventual exception that prevented the tasks execution in the node.
   */
  NODE_EXCEPTION_PARAM,
  /**
   * Parameter for the host the RMI registry for the node is running on.
   */
  NODE_MANAGEMENT_HOST_PARAM,
  /**
   * Parameter for the RMI port used by JMX in the node.
   */
  NODE_MANAGEMENT_PORT_PARAM,
  /**
   * Parameter for the node's available system information.
   */
  SYSTEM_INFO_PARAM,
  /**
   * Parameter for the node's uuid.
   */
  NODE_UUID_PARAM,
  /**
   * Job requeue indicator.
   */
  JOB_REQUEUE,
  /**
   * Job pending indicator, determines whether the job is waiting for its scheduled time to start.
   */
  JOB_PENDING,
  /**
   * Job expired indicator, determines whether the job is should be cancelled.
   */
  JOB_EXPIRED,
  /**
   * Parameter for the driver's uuid.
   */
  DRIVER_UUID_PARAM,
  /**
   * Parameter for the non-secure JMX server port.
   */
  DRIVER_MANAGEMENT_PORT,
  /**
   * Parameter for the secure JMX server port.
   */
  DRIVER_MANAGEMENT_PORT_SSL,
  /**
   * Parameter the total accumulated task execution elapsed time in a bundle.
   */
  NODE_BUNDLE_ELAPSED_PARAM,
  /**
   * Maxium number of concurrent jobs.
   */
  NODE_MAX_JOBS,
  /**
   * Flag indicating whether the node is in offline mode.
   */
  NODE_OFFLINE,
  /**
   * Request sent by the offline node to the driver upon reconnection to notify that the results are ready for a specified NODE_BUNDLE_ID.
   */
  NODE_OFFLINE_OPEN_REQUEST,
  /**
   * Id of the task bundle sent to the node or local client executor.
   */
  TASK_BUNDLE_ID,
  /**
   * Uuid of a job executed offline.
   */
  JOB_UUID,
  /**
   * Marker to indicate whether a node is a master node for the provisioning features.
   */
  NODE_PROVISIONING_MASTER,
  /**
   * Marker to indicate whether a node is a slave node for the provisioning features.
   */
  NODE_PROVISIONING_SLAVE,
  /**
   * An array of boolean flags indicating whther the task raised an error during serilaization or deserialization.
   */
  ERROR_MARKERS,
  /**
   * An array of ints that holds the positions of the tasks such as they are in the initial job submiited by the client.
   */
  TASK_POSITIONS,
  /**
   * An array of ints holding the max number of resubmits for the tasks to send.
   */
  TASK_MAX_RESUBMITS,
  /**
   * An array of ints that holds the positions of the tasks that are marked as having to be resubmitted.
   */
  RESUBMIT_TASK_POSITIONS,
  /**
   * An indicator from the remote peer that the channel should be closed.
   */
  CLOSE_COMMAND,
  /**
   * Whther the job was submitted directly by a client or loaded from a persistence store.
   */
  FROM_PERSISTENCE,
  /**
   * Datasources definitions passed on to a node from a server.
   */
  DATASOURCE_DEFINITIONS,
  /**
   * Whether a job has already been persisted.
   */
  ALREADY_PERSISTED,
  /**
   * Whether a job has already been persisted in another driver.
   */
  ALREADY_PERSISTED_P2P,
  /**
   * UUID of a connection to a driver.
   */
  CONNECTION_UUID,
  /**
   * UUID of the master node if the current node is a slave.
   */
  NODE_PROVISIONING_MASTER_UUID,
  /**
   * Id of the client-side bundle.
   */
  CLIENT_BUNDLE_ID,
  /**
   * Type of notification emitted by a node.
   */
  NODE_NOTIFICATION_TYPE,
  /**
   * Whether a node accepts new jobs.
   */
  NODE_ACCEPTS_NEW_JOBS,
  /**
   * Entry holding the dependency graph of the tasks in a job, if any.
   */
  JOB_TASK_GRAPH,
  /**
   * Info on task dependencies within a task bundle.
   */
  JOB_TASK_GRAPH_INFO,
  /**
   * Whether a job graph is already being handled by a driver.
   */
  JOB_GRAPH_ALREADY_HANDLED
}
