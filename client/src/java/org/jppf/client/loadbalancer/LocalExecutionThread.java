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
package org.jppf.client.loadbalancer;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * Instances of this class are intended to perform local task executions concurrently.
 */
class LocalExecutionThread extends ExecutionThread
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(LocalExecutionThread.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Initialize this execution thread for local execution.
	 * @param tasks the tasks to execute.
	 * @param job the execution to perform.
	 * @param loadBalancer the load balancer for which this thread is working.
	 */
	public LocalExecutionThread(final List<JPPFTask> tasks, final JPPFJob job, final LoadBalancer loadBalancer)
	{
		super(tasks, job, loadBalancer);
		setName("LocalExecution");
	}

	/**
	 * Perform the execution.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		try
		{
			long accTimeNanos = getAccTime();
			int accSize = JPPFConfiguration.getProperties().getInt("jppf.local.execution.accumulation.size", Integer.MAX_VALUE);
			long start = System.nanoTime();
			LinkedList<Future<JPPFTask>> futures = new LinkedList<Future<JPPFTask>>();
			for (JPPFTask task: tasks)
			{
				task.setDataProvider(job.getDataProvider());
				futures.add(loadBalancer.getThreadPool().submit(new TaskWrapper(task), task));
			}
			int count = 0;
			List<JPPFTask> results = new LinkedList<JPPFTask>();
			while (!futures.isEmpty())
			{
				Future<JPPFTask> f = futures.peek();
				while ((f != null) && f.isDone() &&
						((count == 0) || ((System.nanoTime() - start < accTimeNanos) && (count < accSize))))
				{
					results.add(futures.poll().get());
					count++;
					f = futures.peek();
				}
				if (count > 0)
				{
					if (debugEnabled) log.debug("received " + count + " tasks from local executor" + ", first position=" + results.get(0).getPosition());
					TaskResultListener listener = job.getResultListener();
					if (listener != null)
					{
						synchronized(listener)
						{
							listener.resultsReceived(new TaskResultEvent(results));
						}
					}
					double elapsed = System.nanoTime() - start;
					loadBalancer.getBundlers()[LoadBalancer.LOCAL].feedback(count, elapsed);
					start = System.nanoTime();
					results.clear();
					count = 0;
				}
				else if (f != null) f.get();
			}
		}
		catch(Throwable t)
		{
			if (debugEnabled) log.debug(t.getMessage(), t);
			exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
            if (job.getResultListener() != null)
            {
                synchronized(job.getResultListener())
                {
                    job.getResultListener().resultsReceived(new TaskResultEvent(t));
                }
            }
		}
		finally
		{
			loadBalancer.setLocallyExecuting(false);
		}
	}

	/**
	 * Retrieve the accumulation time and convert it to nanoseconds.
	 * @return the accumulation time in nanoseconds.
	 */
	private long getAccTime()
	{
		long time = JPPFConfiguration.getProperties().getLong("jppf.local.execution.accumulation.time", Long.MAX_VALUE);
		char unitChar = JPPFConfiguration.getProperties().getChar("jppf.local.execution.accumulation.time.unit", 'n');
		TimeUnit unit;
		switch(unitChar)
		{
			case 'n':
				unit = TimeUnit.NANOSECONDS;
				break;
			case 'm':
				unit = TimeUnit.MILLISECONDS;
				break;
			case 's':
				unit = TimeUnit.SECONDS;
				break;
			case 'M':
				unit = TimeUnit.MINUTES;
				break;
			case 'h':
				unit = TimeUnit.HOURS;
				break;
			case 'd':
				unit = TimeUnit.DAYS;
				break;
			default:
				unit = TimeUnit.NANOSECONDS;
				break;
		}
		return unit.toNanos(time);
	}
}