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

package sample.test.resubmit;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.server.protocol.JPPFTask;

/**
 * Result collector that removes the completed tasks from a job
 * in order to not execute them again in case of a resubmission.
 * @author Laurent Cohen
 */
public class MyResultCollector extends JPPFResultCollector
{
	/**
	 * The job to execute.
	 */
	private JPPFJob job;

	/**
	 * Initialize this collector with the specified task count and job.
	 * @param taskCount the number of tasks in the job.
	 * @param job the job to execute.
	 */
	public MyResultCollector(int taskCount, JPPFJob job)
	{
		super(taskCount);
		this.job = job;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void resultsReceived(TaskResultEvent event)
	{
		super.resultsReceived(event);
		List<JPPFTask> tasks = event.getTaskList();
		for (Iterator<JPPFTask> it = job.getTasks().iterator(); it.hasNext(); )
		{
			for (JPPFTask t: tasks)
			{
				JPPFTask jobTask = it.next();
				if (jobTask.getPosition() == t.getPosition())
				{
					System.out.println("received results for task at position " + t.getPosition() + " removing it from the job");
					it.remove();
				}
			}
		}
	}
}
