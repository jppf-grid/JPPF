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

package org.jppf.gigaspaces.test;

import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;

/**
 * Utility class used from a Java Server Page to invoke a JPPF job execution
 * via a JPPF client deployed as a GigaSpaces processing unit.
 * @author Laurent Cohen
 */
public class TestFromJSP
{
	/**
	 * Execute a job and return the result as a string.
	 * @param jobName the name given to the JPPF job.
	 * @param nbTasks the number of tasks in the job.
	 * @param taskDuration the duration in milliseconds of each task in the job.
	 * @return the job result as a string message.
	 */
	public static String testGS(final String jobName, final int nbTasks, final long taskDuration)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<h2>Results for job ").append(jobName).append("</h2>");
		try
		{
			JPPFJob job = GSClient.execute(jobName, nbTasks, taskDuration);
			for (JPPFTask task: job.getTasks())
			{
				sb.append("Task ").append(task.getId()).append(" : ").append(task.getResult()).append("<br/>");
			}
		}
		catch(Exception e)
		{
			sb.append(e.getClass().getName()).append(" : ").append(e.getMessage()).append("<br/>");
			for (StackTraceElement elt: e.getStackTrace())
			{
				sb.append(elt).append("<br/>");
			}
		}
		return sb.toString();
	}
}
