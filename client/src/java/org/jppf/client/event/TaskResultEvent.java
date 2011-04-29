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
package org.jppf.client.event;

import java.util.*;

import org.jppf.server.protocol.JPPFTask;

/**
 * Event object used to notify interested listeners that a list of task results
 * have been received from the server.
 * @author Laurent Cohen
 */
public class TaskResultEvent extends EventObject
{
	/**
	 * Index of the first task in the list, relative to the initial execution request.
	 * @deprecated use {@link org.jppf.server.protocol.JPPFTask#getPosition() JPPFTask.getPosition()} instead.
	 */
	private int startIndex = -1;

	/**
	 * Initialize this event with a specified list of tasks.
	 * @param taskList the list of tasks whose results have been received from the server.
	 */
	public TaskResultEvent(List<JPPFTask> taskList)
	{
		super(taskList);
	}

	/**
	 * Initialize this event with a specified list of tasks and start index.
	 * @param taskList the list of tasks whose results have been received from the server.
	 * @param startIndex index of the first task in the list, relative to the initial execution
	 * request. Used to enable proper ordering of the results.
	 * @deprecated the startIndex is not used any more to determine each task's position.
	 * Use {@link org.jppf.server.protocol.JPPFTask#getPosition() JPPFTask.getPosition()} instead.
	 */
	public TaskResultEvent(List<JPPFTask> taskList, int startIndex)
	{
		super(taskList);
		this.startIndex = startIndex;
	}

	/**
	 * Get the list of tasks whose results have been received from the server.
	 * To properly order the results, developers should use {@link org.jppf.server.protocol.JPPFTask#getPosition() JPPFTask.getPosition()} for each task.
	 * @return a list of <code>JPPFTask</code> instances.
	 */
  @SuppressWarnings("unchecked")
	public List<JPPFTask> getTaskList()
	{
		return (List<JPPFTask>) getSource();
	}

	/**
	 * Get the index of the first task in the list, relative to the initial execution request.
	 * @return the index as an int value.
	 * @deprecated use {@link org.jppf.server.protocol.JPPFTask#getPosition() JPPFTask.getPosition()} instead.
	 */
	public int getStartIndex()
	{
		return startIndex;
	}
}
