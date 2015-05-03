/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

/**
 * Enumeration of the possible reasons why a set of tasks is returned by a node in a given state.
 * @author Laurent Cohen
 * @since 5.0
 */
public enum JobReturnReason {
  /**
   * The tasks were normally processed by the node.
   */
  RESULTS_RECEIVED,
  /**
   * The processing of the tasks dispatched in the node took longer than the specified dispatch timeout.
   */
  DISPATCH_TIMEOUT,
  /**
   * An error occurred in the node which prevented the normal execution of the tasks.
   */
  NODE_PROCESSING_ERROR,
  /**
   * An error occurred in the driver while processing the results returned by the node.
   */
  DRIVER_PROCESSING_ERROR,
  /**
   * The connection between node and server was severed before the results could be returned.
   */
  NODE_CHANNEL_ERROR
}
