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

package org.jppf.client;

import java.io.Serializable;
import java.util.*;

import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class hold and manage the results of a job.
 * @author Laurent Cohen
 */
public class JobResults implements Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * A map containing the tasks that have been successfully executed,
	 * ordered by ascending position in the submitted list of tasks.
	 */
	private final SortedMap<Integer, JPPFTask> resultMap = new TreeMap<Integer, JPPFTask>();

	/**
	 * Get the current number of received results.
	 * @return the number of results as an int.
	 */
	public synchronized int size()
	{
		return resultMap.size();
	}

	/**
	 * Determine whether this job received a result for the task at the specified position.
	 * @param position the task position to check.
	 * @return <code>true</code> if a result was received, <code>false</code> otherwise.
	 */
	public synchronized boolean hasResult(final int position)
	{
		return resultMap.containsKey(position);
	}

	/**
	 * Add the specified results to this job.
	 * @param tasks the list of tasks for which results were received.
	 */
	public synchronized void putResults(final List<JPPFTask> tasks)
	{
		for (JPPFTask task: tasks) resultMap.put(task.getPosition(), task);
	}

	/**
	 * Get all the tasks received as results for this job.
	 * @return a collection of {@link JPPFTask} instances.
	 */
	public synchronized Collection<JPPFTask> getAll()
	{
		return Collections.unmodifiableCollection(resultMap.values());
	}
}
