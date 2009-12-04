/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.client;

/**
 * Connection status of a JPPFClientConnection instance.
 * @author Laurent Cohen
 */
public enum JPPFClientConnectionStatus
{
	/**
	 * Indicates a new connection.
	 */
	NEW,
	/**
	 * Indicates that the connection instance is disconnected from the driver.
	 */
	DISCONNECTED,
	/**
	 * Indicates that the connection instance is currently attempting to connect to the driver.
	 */
	CONNECTING,
	/**
	 * Indicates that the connection instance has successfully connected to the driver.
	 */
	ACTIVE,
	/**
	 * Indicates that the connection instance has is currently executing tasks.
	 */
	EXECUTING,
	/**
	 * Indicates that the connection instance has failed to connect to the driver.
	 */
	FAILED
}
