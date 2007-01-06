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
package org.jppf.server;

import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractResultSender implements TaskCompletionListener
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(AbstractResultSender.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The list of task bundles whose execution has been completed.
	 */
	protected List<JPPFTaskBundle> resultList = new ArrayList<JPPFTaskBundle>();
	/**
	 * Number of tasks that haven't yet been executed.
	 */
	protected int pendingTasksCount = 0;
	/**
	 * Used to serialize and deserialize the tasks data.
	 */
	protected SerializationHelper helper = new SerializationHelperImpl();
	/**
	 * The socket client used to communicate over a socket connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Determines whether results can be sent asynchronously,
	 * or if we must wait until all tasks have been completed.
	 */
	protected boolean asynch = true;

	/**
	 * Initialize this result sender with a specified socket client.
	 * @param socketClient the socket client used to send results back.
	 * @param asynch determines whether results can be sent asynchronously,
	 * or if it must wait until all tasks have been completed.
	 */
	public AbstractResultSender(SocketWrapper socketClient, boolean asynch)
	{
		this.socketClient = socketClient;
		this.asynch = asynch;
	}

	/**
	 * Wait for executed task bundles and send them back to the client.
	 * @param count the number of tasks that must be executed.
	 * @throws Exception if an error occurs while sending one or more task bundles.
	 */
	public void run(int count) throws Exception
	{
		if (debugEnabled) log.debug("Pending tasks: "+count);
		resultList = new ArrayList<JPPFTaskBundle>();
		pendingTasksCount = count;
		waitForExecution();
	}

	/**
	 * Send the results of the tasks in a bundle back to the client who
	 * submitted the request.
	 * @param bundle the bundle to get the task results from.
	 * @throws Exception if an IO exception occurred while sending the results back.
	 */
	public abstract void sendPartialResults(JPPFTaskBundle bundle) throws Exception;

	/**
	 * This method waits until all tasks of a request have been completed.
	 * @throws Exception if handing of the results fails.
	 */
	public synchronized void waitForExecution() throws Exception
	{
		while (pendingTasksCount > 0)
		{
			try
			{
				wait();
				synchronized (resultList)
				{
					if (debugEnabled) log.debug(""+resultList.size()+" in result list");
					if (asynch)
					{
						for (JPPFTaskBundle bundle : resultList) sendPartialResults(bundle);
					}
					else if (!resultList.isEmpty())
					{
						JPPFTaskBundle first = resultList.remove(0);
						List<byte[]> taskList = first.getTasks();
						int count = first.getTaskCount();
						for (JPPFTaskBundle bundle : resultList)
						{
							taskList.addAll(bundle.getTasks());
							count += bundle.getTaskCount();
						}
						first.setTaskCount(count);
						long elapsed = System.currentTimeMillis() - first.getNodeExecutionTime();
						first.setNodeExecutionTime(elapsed);
						sendPartialResults(first);
					}
					resultList.clear();
				}
			}
			catch (InterruptedException e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Callback method invoked when the execution of a task has completed. This
	 * method triggers a check of the request completion status. When all tasks
	 * have completed, this connection sends all results back.
	 * @param result the result of the task's execution.
	 */
	public synchronized void taskCompleted(JPPFTaskBundle result)
	{
		pendingTasksCount -= result.getTaskCount();
		if (debugEnabled) log.debug("Pending tasks: "+pendingTasksCount);
		resultList.add(result);
		if (asynch || (pendingTasksCount <= 0)) notify();
	}
}
