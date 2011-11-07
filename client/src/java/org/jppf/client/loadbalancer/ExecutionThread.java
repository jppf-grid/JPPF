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

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class are intended to perform local and remote task executions concurrently.
 */
abstract class ExecutionThread extends Thread
{
	/**
	 * The tasks to execute.
	 */
	protected List<JPPFTask> tasks = null;
	/**
	 * Exception that may result from the execution.
	 */
	protected Exception exception = null;
	/**
	 * The execution to perform.
	 */
	protected JPPFJob job = null;
	/**
	 * The load balancer for which this thread is working.
	 */
	protected LoadBalancer loadBalancer = null;

	/**
	 * Initialize this execution thread for remote execution.
	 * @param tasks the tasks to execute.
	 * @param job the execution to perform.
	 * @param loadBalancer the load balancer for which this thread is working.
	 */
	public ExecutionThread(final List<JPPFTask> tasks, final JPPFJob job, final LoadBalancer loadBalancer)
	{
		this.tasks = tasks;
		this.job = job;
		this.loadBalancer = loadBalancer;
	}

	/**
	 * Perform the execution.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public abstract void run();

	/**
	 * Get the resulting exception.
	 * @return an <code>Exception</code> or null if no exception was raised.
	 */
	public Exception getException()
	{
		return exception;
	}
}