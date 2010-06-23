/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.server.protocol;

/**
 * Constants used when a client sends an admin command to a server.
 * @author Laurent Cohen
 */
public enum  BundleParameter
{
	/**
	 * Parameter to determine whether the tasks bundle size is determined manually or automatically.
	 */
	BUNDLE_TUNING_TYPE_PARAM,
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
	 * Parameter for the RMI port used by JMX in the node.
	 */
	NODE_MANAGEMENT_ID_PARAM,
	/**
	 * Parameter for the node's available system information.
	 */
	NODE_SYSTEM_INFO_PARAM,
	/**
	 * Parameter for the node's uuid.
	 */
	NODE_UUID_PARAM,
	/**
	 * The SLA between job and server.
	 */
	JOB_SLA,
	/**
	 * The user-defined id of a job.
	 */
	JOB_ID,
	/**
	 * The unique universal id of a job.
	 */
	JOB_UUID,
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
	 * Key for the job metadata Map in the JPPFTaskBundle.
	 */
	JOB_METADATA,
	/**
	 * Time at which the job is received on the server side.
	 */
	JOB_RECEIVED_TIME_MILLIS
}
