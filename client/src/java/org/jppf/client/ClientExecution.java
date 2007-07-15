/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.client;

import java.util.List;

import org.jppf.client.event.TaskResultListener;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;

/**
 * Instances of this class encapsulate
 * @author Laurent Cohen
 */
public class ClientExecution
{
	/**
	 * List of tasks for this execution.
	 */
	public List<JPPFTask> tasks = null;
	/**
	 * Data provider for this execution.
	 */
	public DataProvider dataProvider = null;
	/**
	 * Flag to determine whether the execution is blocking or not.
	 */
	public boolean isBlocking = true;
	/**
	 * Listener to notify whenever a set of results have been received.
	 */
	public TaskResultListener listener = null;

	/**
	 * Initialize this execution with the specified parameters.
	 * @param tasks list of tasks for this execution.
	 * @param dataProvider data provider for this execution.
	 * @param isBlocking flag to determine whether the execution is blocking or not.
	 * @param listener listener to notify whenever a set of results have been received.
	 */
	public ClientExecution(List<JPPFTask> tasks, DataProvider dataProvider, boolean isBlocking, TaskResultListener listener)
	{
		this.tasks = tasks;
		this.dataProvider = dataProvider;
		this.isBlocking = isBlocking;
		this.listener = listener;
	}
}
