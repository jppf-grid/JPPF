/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.client;

import java.io.NotSerializableException;
import java.util.List;

import org.apache.log4j.Logger;
import org.jppf.client.event.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.Pair;

/**
 * This class encapsulates a pool of threads that submit the tasks to a driver
 * and listen for the results.
 */
class AsynchronousResultProcessor implements Runnable
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(AsynchronousResultProcessor.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Client connection owning this results processor.
	 */
	private final JPPFClientConnection connection;
	/**
	 * The execution processed by this task.
	 */
	private ClientExecution execution = null;

	/**
	 * Initialize this result processor with a specified list of tasks, data provider and result listener.
	 * @param connection the client connection owning this results processor.
	 * @param execution the execution processed by this task.
	 */
	public AsynchronousResultProcessor(JPPFClientConnection connection, ClientExecution execution)
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
		if (!execution.isBlocking) connection.lock.lock();
		try
		{
			connection.currentExecution = execution;
			int count = 0;
			for (JPPFTask task : execution.tasks) task.setPosition(count++);
			count = 0;
			boolean completed = false;
			while (!completed)
			{
				try
				{
					connection.sendTasks(execution.tasks, execution.dataProvider);
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
			if (!error) connection.currentExecution = null;
			if (!execution.isBlocking) connection.lock.unlock();
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