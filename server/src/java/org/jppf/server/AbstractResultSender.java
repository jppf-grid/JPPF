/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.server;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.*;
import org.jppf.server.protocol.TaskCompletionListener;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractResultSender implements TaskCompletionListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractResultSender.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The list of task bundles whose execution has been completed.
	 */
	private List<BundleWrapper> resultList = new ArrayList<BundleWrapper>();
	/**
	 * Number of tasks that haven't yet been executed.
	 */
	private int pendingTasksCount = 0;
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
		setResultList(new ArrayList<BundleWrapper>());
		setPendingTasksCount(count);
		waitForExecution();
	}

	/**
	 * Send the results of the tasks in a bundle back to the client who
	 * submitted the request.
	 * @param bundle the bundle to get the task results from.
	 * @throws Exception if an IO exception occurred while sending the results back.
	 */
	public abstract void sendPartialResults(BundleWrapper bundle) throws Exception;

	/**
	 * This method waits until all tasks of a request have been completed.
	 * @throws Exception if handing of the results fails.
	 */
	public synchronized void waitForExecution() throws Exception
	{
		while (getPendingTasksCount() > 0)
		{
			try
			{
				wait();
				if (debugEnabled) log.debug(""+getResultList().size()+" in result list");
				if (asynch)
				{
					for (BundleWrapper bundle : getResultList()) sendPartialResults(bundle);
				}
				else if (!getResultList().isEmpty())
				{
					BundleWrapper first = getResultList().remove(0);
					int count = first.getTasks().size();
					int size = getResultList().size();
					for (int i=0; i<size; i++)
					{
						BundleWrapper bundle = getResultList().remove(0);
						for (DataLocation task: bundle.getTasks())
						{
							first.addTask(task);
							count++;
						}
						bundle.getTasks().clear();
					}
					first.getBundle().setTaskCount(count);
					sendPartialResults(first);
				}
				getResultList().clear();
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
	public synchronized void taskCompleted(BundleWrapper result)
	{
		setPendingTasksCount(getPendingTasksCount() - result.getBundle().getTaskCount());
		if (debugEnabled)
		{
			log.debug("Received results for : " + result.getBundle().getTaskCount() + " [size=" + result.getTasks().size() + "] tasks");
			log.debug("Pending tasks: " + getPendingTasksCount());
		}
		getResultList().add(result);
		if (asynch || (getPendingTasksCount() <= 0)) notify();
	}

	/**
	 * Set the number of tasks that haven't yet been executed.
	 * @param pendingTasksCount the number of tasks as an int. 
	 */
	protected synchronized void setPendingTasksCount(int pendingTasksCount)
	{
		this.pendingTasksCount = pendingTasksCount;
	}

	/**
	 * Get the number of tasks that haven't yet been executed.
	 * @return the number of tasks as an int.
	 */
	protected synchronized int getPendingTasksCount()
	{
		return pendingTasksCount;
	}

	/**
	 * Set the list of task bundles whose execution has been completed.
	 * @param resultList a list of <code>BundleWrapper</code> instances.
	 */
	protected synchronized void setResultList(List<BundleWrapper> resultList)
	{
		this.resultList = resultList;
	}

	/**
	 * Get the list of task bundles whose execution has been completed.
	 * @return a list of <code>JPPFTaskBundle</code> instances.
	 */
	protected synchronized List<BundleWrapper> getResultList()
	{
		return resultList;
	}
}
