/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.server.nio.classloader;

/**
 * Enumeration of the possible state transitions for a class server channel.
 * @author Laurent Cohen
 */
public enum ClassTransition
{
	/**
	 * Transition to the DEFINING_TYPE state.
	 */
	TO_DEFINING_TYPE,
	/**
	 * Transition to the TO_SENDING_INITIAL_PROVIDER_RESPONSE state.
	 */
	TO_SENDING_INITIAL_PROVIDER_RESPONSE,
	/**
	 * Transition to the SENDING_INITIAL_RESPONSE state.
	 */
	TO_SENDING_INITIAL_RESPONSE,
	/**
	 * Transition to the WAITING_NODE_REQUEST state.
	 */
	TO_WAITING_NODE_REQUEST,
	/**
	 * Transition to the SENDING_NODE_RESPONSE state.
	 */
	TO_SENDING_NODE_RESPONSE,
	/**
	 * Transition to the SENDING_PROVIDER_REQUEST state.
	 */
	TO_SENDING_PROVIDER_REQUEST,
	/**
	 * Transition to the WAITING_PROVIDER_RESPONSE state.
	 */
	TO_WAITING_PROVIDER_RESPONSE,
	/**
	 * Transition to the IDLE_NODE state in idle mode.
	 */
	TO_IDLE_NODE,
	/**
	 * Transition to the IDLE_PROVIDER state in idle mode.
	 */
	TO_IDLE_PROVIDER;
}
