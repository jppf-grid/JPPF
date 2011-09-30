/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.nio.nodeserver;


/**
 * Enumeration of the possible state transitions for a Node server channel.
 * @author Laurent Cohen
 */
public enum NodeTransition
{
	/**
	 * Transition from a state to SENDING_BUNDLE.
	 */
	TO_SENDING, 
	/**
	 * Transition from a state to WAITING_RESULTS.
	 */
	TO_WAITING,
	/**
	 * Transition from a state to SEND_INITIAL_BUNDLE.
	 */
	TO_SEND_INITIAL,
	/**
	 * Transition from a state to WAIT_INITIAL_BUNDLE.
	 */
	TO_WAIT_INITIAL,
	/**
	 * Transition from a state to IDLE.
	 */
	TO_IDLE
}
