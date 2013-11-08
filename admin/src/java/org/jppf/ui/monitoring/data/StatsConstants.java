/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.ui.monitoring.data;

import static org.jppf.ui.monitoring.data.Fields.*;

import org.jppf.utils.CollectionUtils;

/**
 * Constants for the JPPF statistics collected form the server.
 * @author Laurent Cohen
 */
public interface StatsConstants
{
  /**
   * List of stats properties related to network connections.
   */
  Fields[] CONNECTION_PROPS = {
      NB_NODES, MAX_NODES, NB_IDLE_NODES, NB_CLIENTS, MAX_CLIENTS
  };
  /**
   * List of stats properties related to queue operations.
   */
  Fields[] QUEUE_PROPS = {
      LATEST_QUEUE_TIME, TOTAL_QUEUE_TIME, MIN_QUEUE_TIME, MAX_QUEUE_TIME, AVG_QUEUE_TIME, TOTAL_QUEUED, QUEUE_SIZE,
      MAX_QUEUE_SIZE
  };
  /**
   * List of stats properties related to tasks execution.
   */
  Fields[] EXECUTION_PROPS = {
      TOTAL_TASKS_EXECUTED, TOTAL_EXECUTION_TIME, LATEST_EXECUTION_TIME, MIN_EXECUTION_TIME, MAX_EXECUTION_TIME,
      AVG_EXECUTION_TIME
  };

  /**
   * List of stats properties related to tasks execution.
   */
  Fields[] NODE_EXECUTION_PROPS = {
      TOTAL_NODE_EXECUTION_TIME, LATEST_NODE_EXECUTION_TIME, MIN_NODE_EXECUTION_TIME, MAX_NODE_EXECUTION_TIME,
      AVG_NODE_EXECUTION_TIME
  };
  /**
   * List of stats properties related to tasks execution.
   */
  Fields[] TRANSPORT_PROPS = {
      TOTAL_TRANSPORT_TIME, LATEST_TRANSPORT_TIME, MIN_TRANSPORT_TIME, MAX_TRANSPORT_TIME, AVG_TRANSPORT_TIME
  };
  /**
   * List of stats properties related to job execution.
   */
  Fields[] JOB_PROPS = {
      JOBS_TOTAL, JOBS_LATEST, JOBS_MAX, JOBS_LATEST_TIME, JOBS_MIN_TIME, JOBS_MAX_TIME, JOBS_AVG_TIME, JOBS_MIN_TASKS, JOBS_MAX_TASKS, JOBS_AVG_TASKS
  };
  /**
   * List of stats properties related to class loading requests to the clients.
   */
  Fields[] CLIENT_CL_REQUEST_TIME_PROPS = {
      CLIENT_TOTAL_CL_REQUEST_COUNT, CLIENT_AVG_CL_REQUEST_TIME, CLIENT_MIN_CL_REQUEST_TIME, CLIENT_MAX_CL_REQUEST_TIME, CLIENT_LATEST_CL_REQUEST_TIME
  };
  /**
   * List of stats properties related to class loading requests from the nodes.
   */
  Fields[] NODE_CL_REQUEST_TIME_PROPS = {
      NODE_TOTAL_CL_REQUEST_COUNT, NODE_AVG_CL_REQUEST_TIME, NODE_MIN_CL_REQUEST_TIME, NODE_MAX_CL_REQUEST_TIME, NODE_LATEST_CL_REQUEST_TIME
  };
  /**
   * List of all fields.
   */
  Fields[] ALL_FIELDS =
    CollectionUtils.concatArrays(EXECUTION_PROPS, NODE_EXECUTION_PROPS, TRANSPORT_PROPS, JOB_PROPS, QUEUE_PROPS, CONNECTION_PROPS, NODE_CL_REQUEST_TIME_PROPS);
}
