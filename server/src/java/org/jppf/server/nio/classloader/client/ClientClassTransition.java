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
package org.jppf.server.nio.classloader.client;

/**
 * Enumeration of the possible state transitions for a class server channel.
 * @author Laurent Cohen
 */
public enum ClientClassTransition
{
  /**
   * Transition to the WAITING_INITIAL_PROVIDER_REQUEST state.
   */
  TO_WAITING_INITIAL_PROVIDER_REQUEST,
  /**
   * Transition to the TO_SENDING_INITIAL_PROVIDER_RESPONSE state.
   */
  TO_SENDING_INITIAL_PROVIDER_RESPONSE,
  /**
   * Transition to the SENDING_PROVIDER_REQUEST state.
   */
  TO_SENDING_PROVIDER_REQUEST,
  /**
   * Transition to the WAITING_PROVIDER_RESPONSE state.
   */
  TO_WAITING_PROVIDER_RESPONSE,
  /**
   * Transition to the IDLE_PROVIDER state in idle mode.
   */
  TO_IDLE_PROVIDER,
  /**
   * Transition to the IDLE_PROVIDER state in idle mode for a peer server connection.
   */
  TO_IDLE_PEER_PROVIDER,
  /**
   * Sending of the initial request by a peer server.
   */
  TO_SENDING_PEER_CHANNEL_IDENTIFIER,
  /**
   * Sending of the initial request by a peer server.
   */
  TO_SENDING_PEER_INITIATION_REQUEST,
  /**
   * Waiting for the initial response from a peer server
   */
  TO_WAITING_PEER_INITIATION_RESPONSE
}
