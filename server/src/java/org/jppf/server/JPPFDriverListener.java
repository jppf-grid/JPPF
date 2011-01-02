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
package org.jppf.server;

import java.util.EventListener;


/**
 * Instances of this class are used to collect statistics on the JPPF server.
 * @author Laurent Cohen
 */
public interface JPPFDriverListener extends EventListener
{
	/**
	 * Called to notify that a new client is connected to he JPPF server.
	 */
	void newClientConnection();

	/**
	 * Called to notify that a new client has disconnected from he JPPF server.
	 */
	void clientConnectionClosed();

	/**
	 * Called to notify that a new node is connected to he JPPF server.
	 */
	void newNodeConnection();

	/**
	 * Called to notify that a new node is connected to he JPPF server.
	 */
	void nodeConnectionClosed();

	/**
	 * Called to notify that a task was added to the queue.
	 * @param count the number of tasks that have been added to the queue.
	 */
	void taskInQueue(int count);

	/**
	 * Called to notify that a task was removed from the queue.
	 * @param count the number of tasks that have been removed from the queue.
	 * @param time the time the task remained in the queue.
	 */
	void taskOutOfQueue(int count, long time);
	
	/**
	 * Called when a task execution has completed.
	 * @param count the number of tasks that have been executed.
	 * @param time the time it took to execute the task, including transport to and from the node.
	 * @param remoteTime the time it took to execute the in the node only.
	 * @param size the size in bytes of the bundle that was sent to the node.
	 */
	void taskExecuted(int count, long time, long remoteTime, long size);
}
