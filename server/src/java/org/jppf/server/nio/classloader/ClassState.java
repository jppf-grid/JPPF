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
package org.jppf.server.nio.classloader;

/**
 * Enumeration of the possible states for a class server channel.
 * @author Laurent Cohen
 */
public enum ClassState
{
	/**
	 * State of determining the type of a channel.
	 */
	DEFINING_TYPE,
	/**
	 * State of sending the initial information to a client.
	 */
	SENDING_INITIAL_PROVIDER_RESPONSE,
	/**
	 * State of sending the initial information to a node classloader.
	 */
	SENDING_INITIAL_NODE_RESPONSE,
	/**
	 * State of waiting for a request from a node classloader.
	 */
	WAITING_NODE_REQUEST,
	/**
	 * State of waiting for a response to a node classloader.
	 */
	SENDING_NODE_RESPONSE,
	/**
	 * State of waiting for a response form a resource provider.
	 */
	WAITING_PROVIDER_RESPONSE,
	/**
	 * State of waiting for a response from a resource provider.
	 */
	SENDING_PROVIDER_REQUEST,
	/**
	 * State of doing nothing for a resource provider.
	 */
	IDLE_PROVIDER,
	/**
	 * State of doing nothing for a resource provider.
	 */
	IDLE_NODE,
	/**
	 * Waiting for a provider (client) to provide a class or resource definition.
	 */
	NODE_WAITING_PROVIDER_RESPONSE
}
