/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.server.nio.multiplexer;


/**
 * Enumeration of the possible state transitions for a multiplexer channel.
 * @author Laurent Cohen
 */
public enum MultiplexerTransition
{
	/**
	 * Transition from a state to SENDING_OR_RECEIVING.
	 */
	TO_SENDING_OR_RECEIVING,
	/**
	 * Transition from a state to SENDING.
	 */
	TO_SENDING, 
	/**
	 * Transition from a state to RECEIVING.
	 */
	TO_RECEIVING,
	/**
	 * Transition from a state to IDENTIFYING_INBOUND_CHANNEL.
	 */
	TO_IDENTIFYING_INBOUND_CHANNEL,
	/**
	 * Transition from a state to SENDING_MULTIPLEXING_INFO.
	 */
	TO_SENDING_MULTIPLEXING_INFO,
	/**
	 * Transition from a state to SENDING in idle mode.
	 */
	TO_IDLE;
}
