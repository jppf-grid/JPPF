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

import java.util.*;

import org.jppf.JPPFException;
import org.jppf.client.event.TaskResultListener;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.ReflectionUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFJob
{
	/**
	 * The list of tasks to execute.
	 */
	private List<JPPFTask> tasks = null;
	/**
	 * The container for data shared between tasks.
	 */
	private DataProvider dataProvider = null;
	/**
	 * The tasks execution policy.
	 */
	private ExecutionPolicy executionPolicy = null;
	/**
	 * The listener that receives notifications of completed tasks.
	 */
	private TaskResultListener resultsListener = null;
	/**
	 * Determines whether the execution of this job is blocking on the client side.
	 */
	private boolean blocking = true;

	/**
	 * Default constructor.
	 */
	public JPPFJob()
	{
	}

	/**
	 * Initialize a blocking job with the specified parameters.
	 * @param tasks the list of tasks to execute.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public JPPFJob(List<Object> tasks) throws JPPFException
	{
		this(tasks, null, (ExecutionPolicy) null);
	}

	/**
	 * Initialize a blocking job with the specified parameters.
	 * @param tasks the list of tasks to execute.
	 * @param dataProvider the container for data shared between tasks.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public JPPFJob(List<Object> tasks, DataProvider dataProvider) throws JPPFException
	{
		this(tasks, dataProvider, (ExecutionPolicy) null);
	}

	/**
	 * Initialize a blocking job with the specified parameters.
	 * @param tasks the list of tasks to execute.
	 * @param dataProvider the container for data shared between tasks.
	 * @param executionPolicy the tasks execution policy.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public JPPFJob(List<Object> tasks, DataProvider dataProvider, ExecutionPolicy executionPolicy)
		throws JPPFException
	{
		this(tasks, dataProvider, executionPolicy, null);
		blocking = true;
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param tasks the list of tasks to execute.
	 * @param resultsListener the listener that receives notifications of completed tasks.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public JPPFJob(List<Object> tasks, TaskResultListener resultsListener) throws JPPFException
	{
		this(tasks, null, null, resultsListener);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param tasks the list of tasks to execute.
	 * @param dataProvider the container for data shared between tasks.
	 * @param resultsListener the listener that receives notifications of completed tasks.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public JPPFJob(List<Object> tasks, DataProvider dataProvider, TaskResultListener resultsListener)
		throws JPPFException
	{
		this(tasks, dataProvider, null, resultsListener);
	}

	/**
	 * Initialize a non-blocking job with the specified parameters.
	 * @param tasks the list of tasks to execute.
	 * @param dataProvider the container for data shared between tasks.
	 * @param executionPolicy the tasks execution policy.
	 * @param resultsListener the listener that receives notifications of completed tasks.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public JPPFJob(List<Object> tasks, DataProvider dataProvider, ExecutionPolicy executionPolicy, TaskResultListener resultsListener)
		throws JPPFException
	{
		if (tasks != null)
		{
			for (int i=0; i<tasks.size(); i++)
			{
				Object o = tasks.get(i);
				if (o instanceof JPPFTask) continue;
				if ((o == null) || !ReflectionUtils.isJPPFAnnotated(o.getClass()))
					throw new JPPFException("object '" + o + "' at index " + i + " is not a JPPFTask nor JPPF-annotated");
			}
			setTasks(tasks);
		}
		this.dataProvider = dataProvider;
		this.executionPolicy = executionPolicy;
		this.resultsListener = resultsListener;
		blocking = false;
	}

	/**
	 * Get the list of takss to execute.
	 * @return a list of objects.
	 */
	public List<JPPFTask> getTasks()
	{
		return tasks;
	}

	/**
	 * Set the list of tasks to execute.
	 * @param tasks a list of objects.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public void setTasks(List<Object> tasks) throws JPPFException
	{
		if (this.tasks == null) this.tasks = new ArrayList<JPPFTask>();
		for (Object o: tasks)
		{
			if (o instanceof JPPFTask) this.tasks.add((JPPFTask) o);
			else this.tasks.add(new JPPFAnnotatedTask(o));
		}
	}

	/**
	 * Add a task to this job.
	 * @param taskObject the task to add to this job.
	 * @param args arguments to use with a JPPF-annotated class.
	 * @throws JPPFException if one of the tasks is neither a <code>JPPFTask</code> or a JPPF-annotated class.
	 */
	public void addTask(Object taskObject, Object...args) throws JPPFException
	{
		if (taskObject == null) throw new JPPFException("null tasks are not accepted");
		if (tasks == null) tasks = new ArrayList();
		if (ReflectionUtils.isJPPFAnnotated(taskObject.getClass())) tasks.add(new JPPFAnnotatedTask(taskObject, args));
		else if (taskObject instanceof JPPFTask) tasks.add((JPPFTask) taskObject);
		else throw new JPPFException("object '" + taskObject + "' is not a JPPFTask nor JPPF-annotated");
	}

	/**
	 * Get the container for data shared between tasks.
	 * @return a <code>DataProvider</code> instance.
	 */
	public DataProvider getDataProvider()
	{
		return dataProvider;
	}

	/**
	 * Set the container for data shared between tasks.
	 * @param dataProvider a <code>DataProvider</code> instance.
	 */
	public void setDataProvider(DataProvider dataProvider)
	{
		this.dataProvider = dataProvider;
	}

	/**
	 * Get the tasks execution policy.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	public ExecutionPolicy getExecutionPolicy()
	{
		return executionPolicy;
	}

	/**
	 * Set the tasks execution policy.
	 * @param executionPolicy an <code>ExecutionPolicy</code> instance.
	 */
	public void setExecutionPolicy(ExecutionPolicy executionPolicy)
	{
		this.executionPolicy = executionPolicy;
	}

	/**
	 * Get the listener that receives notifications of completed tasks.
	 * @return a <code>TaskCompletionListener</code> instance.
	 */
	public TaskResultListener getResultListener()
	{
		return resultsListener;
	}

	/**
	 * Set the listener that receives notifications of completed tasks.
	 * @param resultsListener a <code>TaskCompletionListener</code> instance.
	 */
	public void setResultListener(TaskResultListener resultsListener)
	{
		this.resultsListener = resultsListener;
		blocking = false;
	}

	/**
	 * Determine whether the execution of this job is blocking on the client side.
	 * @return true if the execution is blocking, false otherwise.
	 */
	public boolean isBlocking()
	{
		return blocking;
	}

	/**
	 * Specify whether the execution of this job is blocking on the client side.
	 * @param blocking true if the execution is blocking, false otherwise.
	 */
	public void setBlocking(boolean blocking)
	{
		this.blocking = blocking;
	}
}
