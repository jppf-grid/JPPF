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

package org.jppf.jca.work;

import java.util.List;

import javax.resource.spi.work.Work;

/**
 * Instances of this class run a set of tasks rpeatedly, with a delay between each set of executions.
 * @author Laurent Cohen
 */
public class JPPFJcaJob implements Work
{
	/**
	 * The tasks to run periodically.
	 */
	private List<Runnable> tasks = null;
	/**
	 * Length of time between to executions of the job.
	 */
	private long period = 0;
	/**
	 * Determines whether this job should be stopped or not.
	 */
	private boolean stop = false;

	/**
	 * Create a connection iniitializer job with the specified tasks, delay and period.
	 * @param tasks the tasks to run periodically.
	 * @param period length of time between to executions of the job.
	 */
	public JPPFJcaJob(List<Runnable> tasks, long period)
	{
		this.tasks = tasks;
		this.period = period;
	}

	/**
	 * Run all the tasks in sequence. 
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		while (!isStopped())
		{
			for (Runnable r: tasks)
			{
				if (!isStopped()) r.run();
			}
			try
			{
				if (!isStopped()) Thread.sleep(period);
			}
			catch(InterruptedException ignored)
			{
			}
		}
	}

	/**
	 * Stop this job and release the resources it is using.
	 * @see javax.resource.spi.work.Work#release()
	 */
	public void release()
	{
		setStopped(true);
		tasks = null;
	}

	/**
	 * Get the flag that determines whether this job should be stopped or not.
	 * @return true if the job be stopped, false otherwise.
	 */
	private synchronized boolean isStopped()
	{
		return stop;
	}

	/**
	 * Set the flag that determines whether this job should be stopped or not.
	 * @param stop true if the job be stopped, false otherwise.
	 */
	private synchronized void setStopped(boolean stop)
	{
		this.stop = stop;
	}
}
