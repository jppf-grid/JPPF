/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.management.debug;

import java.io.Serializable;

/**
 * MBean interface for server debugging utilities.
 * @author Laurent Cohen
 */
public interface ServerDebugMBean  extends Serializable
{
	/**
	 * The object name for this mbean.
	 */
	String MBEAN_NAME = "org.jppf:name=server.debug,type=driver";
	/**
	 * Get a list of all idle nodes.
	 * @return an array of string representations of each node connection.
	 */
	String[] allIdleNodes();
	/**
	 * Get the list of all keys being processed for the task server.
	 * @return an array of string representations of each task server key.
	 */
	String[] allTaskProcessingKeys();
	/**
	 * Get the list of all keys handled by the task server's NIO selector.
	 * @return an array of string representations of each task server key.
	 */
	String[] allTaskServerKeys();
}
