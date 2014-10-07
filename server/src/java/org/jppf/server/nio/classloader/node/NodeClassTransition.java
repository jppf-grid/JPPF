/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
package org.jppf.server.nio.classloader.node;

/**
 * Enumeration of the possible state transitions for a class server channel.
 * @author Laurent Cohen
 */
public enum NodeClassTransition {
  /**
   * Transition to the WAITING_INITIAL_NODE_REQUEST state.
   */
  TO_WAITING_INITIAL_NODE_REQUEST,
  /**
   * Transition to the SENDING_INITIAL_RESPONSE state.
   */
  TO_SENDING_INITIAL_NODE_RESPONSE,
  /**
   * Transition to the WAITING_NODE_REQUEST state.
   */
  TO_WAITING_NODE_REQUEST,
  /**
   * Transition to the SENDING_NODE_RESPONSE state.
   */
  TO_SENDING_NODE_RESPONSE,
  /**
   * Transition to NODE_WAITING_PROVIDER_RESPONSE state.
   */
  TO_NODE_WAITING_PROVIDER_RESPONSE,
  /**
   * Transition to the IDLE_NODE state in idle mode.
   */
  TO_IDLE_NODE,
}
