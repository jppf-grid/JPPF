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

package org.jppf.node.connection;

/**
 * This enum lists the possible high-level reasons for a node reconnection request.
 * @author Laurent Cohen
 * @since 4.1
 */
public enum ConnectionReason {
  /**
   * Indicates the first connection attempt when the node starts up.
   */
  INITIAL_CONNECTION_REQUEST,
  /**
   * A reconnection was requested via the management APIs or admin console.
   */
  MANAGEMENT_REQUEST,
  /**
   * An error occurred while initializing the class loader connection.
   */
  CLASSLOADER_INIT_ERROR,
  /**
   * An error occurred while processing a class loader request.
   */
  CLASSLOADER_PROCESSING_ERROR,
  /**
   * An error occurred during the job channel initialization.
   */
  JOB_CHANNEL_INIT_ERROR,
  /**
   * An error occurred on the job channel while processing a job.
   */
  JOB_CHANNEL_PROCESSING_ERROR,
  /**
   * The heartbeat mechanism failed to receive a message from the server in a configured time frame.
   */
  HEARTBEAT_FAILURE
}
