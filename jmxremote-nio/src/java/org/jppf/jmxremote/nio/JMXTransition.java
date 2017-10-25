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

package org.jppf.jmxremote.nio;

/**
 * Enumeration of the possible state transitions for a client job channel.
 * @author Laurent Cohen
 */
public enum JMXTransition {
  /**
   * Transition from a state to RECEIVING_MESSAGE.
   */
  TO_RECEIVING_MESSAGE,
  /**
   * Transition from a state to SENDING_MESSAGE.
   */
  TO_SENDING_MESSAGE,
  /**
   * Transition from a state to IDLE.
   */
  TO_IDLE
}
