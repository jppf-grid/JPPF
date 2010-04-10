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

package org.jppf.client.concurrent;

import java.util.concurrent.*;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.DateTimeUtils;

/**
 * Implementation of a future handled by a {@link JPPFExecutorService}.
 * @param <V> the type of the result for the future.
 * @author Laurent Cohen
 */
public class JPPFTaskFuture<V> extends AbstractJPPFFuture<V>
{
	/**
	 * The collector that receives the results from the server.
	 */
	private FutureResultCollector collector = null;
	/**
	 * The position of the task in the job.
	 */
	private int position = -1;
	/**
	 * The id of the job the task is a part of.
	 */
	private String jobId = null;
	/**
	 * JMX connection to the server to which the job was sent.
	 */
	private JMXDriverConnectionWrapper jmx = null;

	/**
	 * Initialize this future with the specified parameters.
	 * @param collector the collector that receives the results from the server.
	 * @param position the position of the task in the job.
	 */
	public JPPFTaskFuture(FutureResultCollector collector, int position)
	{
		this(collector, position, null, null);
	}

	/**
	 * Initialize this future with the specified parameters.
	 * @param collector the collector that receives the results from the server.
	 * @param position the position of the task in the job.
	 * @param jobId the id of the job the task is a part of.
	 * @param jmx JMX connection to the server to which the job was sent.
	 */
	public JPPFTaskFuture(FutureResultCollector collector, int position, String jobId, JMXDriverConnectionWrapper jmx)
	{
		this.collector = collector;
		this.position = position;
		this.jobId = jobId;
		this.jmx = jmx;
	}

	/**
	 * Returns true if this task completed. Completion may be due to normal termination,
	 * an exception, or cancellation. In all of these cases, this method will return true. 
	 * @return true if the task completed.
	 * @see org.jppf.client.concurrent.AbstractJPPFFuture#isDone()
	 */
	public boolean isDone()
	{
		return done.compareAndSet(false, collector.isTaskReceived(position));
	}

	/**
	 * Waits if necessary for the computation to complete, and then retrieves its result.
	 * @return the computed result.
	 * @throws InterruptedException if the current thread was interrupted while waiting.
	 * @throws ExecutionException if the computation threw an exception.
	 * @see java.util.concurrent.Future#get()
	 */
	public V get() throws InterruptedException, ExecutionException
	{
		return (V) collector.waitForTask(position).getResult();
	}

	/**
   * Waits if necessary for at most the given time for the computation
   * to complete, and then retrieves its result, if available.
	 * @param timeout the maximum time to wait.
	 * @param unit the time unit of the timeout argument.
	 * @return the computed result.
	 * @throws InterruptedException if the current thread was interrupted while waiting.
	 * @throws ExecutionException if the computation threw an exception.
	 * @throws TimeoutException if the wait timed out.
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		long millis = DateTimeUtils.toMillis(timeout, unit);
		return (V) collector.waitForTask(position, millis).getResult();
	}

	/**
	 * Mark the task as done.
	 */
	void setDone()
	{
		done.set(true);
	}

	/**
	 * Mark the task as cancelled.
	 */
	void setCancelled()
	{
		cancelled.set(true);
	}
}
