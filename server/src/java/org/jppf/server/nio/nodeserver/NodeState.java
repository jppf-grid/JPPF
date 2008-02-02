/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.server.nio.nodeserver;


/**
 * Enumeration of the possible states for a class server channel.
 * @author Laurent Cohen
 */
public enum NodeState
{
	/**
	 * State of sending the initial bundle to the node.
	 */
	SEND_INITIAL_BUNDLE, 
	/**
	 * State of waiting for the initial bundle from the node.
	 */
	WAIT_INITIAL_BUNDLE, 
	/**
	 * State of waiting for something to do / sending a task bundle to the node.
	 */
	SENDING_BUNDLE, 
	/**
	 * State of waiting for the execution result of a task bundle.
	 */
	WAITING_RESULTS;
}
