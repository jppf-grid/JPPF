/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

import java.io.NotSerializableException;

import org.apache.commons.logging.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * This class encapsulates a pool of threads that submit the tasks to a driver
 * and listen for the results.
 */
public class AsynchronousResultProcessor implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AsynchronousResultProcessor.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Client connection owning this results processor.
	 */
	private final JPPFClientConnectionImpl connection;
	/**
	 * The execution processed by this task.
	 */
	private ClientExecution execution = null;

	/**
	 * Initialize this result processor with a specified list of tasks, data provider and result listener.
	 * @param connection the client connection owning this results processor.
	 * @param execution the execution processed by this task.
	 */
	public AsynchronousResultProcessor(JPPFClientConnectionImpl connection, ClientExecution execution)
	{
		this.connection = connection;
		this.execution = execution;
	}

	/**
	 * This method executes until all partial results have been received.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		boolean error = false;
		try
		{
			if (!execution.isBlocking) connection.getLock().lock();
			connection.currentExecution = execution;
			int count = 0;
			for (JPPFTask task : execution.tasks) task.setPosition(count++);
			count = 0;
			boolean completed = false;
			//while (!completed)
			{
				try
				{
					/*
					connection.sendTasks(execution.tasks, execution.dataProvider, execution.policy);
					while (count < execution.tasks.size())
					{
						Pair<List<JPPFTask>, Integer> p = connection.receiveResults();
						count += p.first().size();
						if (execution.listener != null)
						{
							execution.listener.resultsReceived(new TaskResultEvent(p.first(), p.second()));
						}
					}
					completed = true;
					*/
					JPPFClient.getLoadBalancer().execute(execution, connection);
				}
				catch(NotSerializableException e)
				{
					throw e;
				}
				catch(InterruptedException e)
				{
					throw e;
				}
				catch(Exception e)
				{
					log.error("["+connection.getName()+"] "+e.getMessage(), e);
					connection.initConnection();
				}
			}
		}
		catch(Exception e)
		{
			error = true;
			log.error("["+connection.getName()+"] "+e.getMessage(), e);
		}
		finally
		{
			if (!execution.isBlocking) connection.getLock().unlock();
			if (!error) connection.currentExecution = null;
		}
	}

	/**
	 * Get all the data pertaining to the execution for this task.
	 * @return a <code>ClientExecution</code> instance.
	 */
	public ClientExecution getExecution()
	{
		return execution;
	}
}
