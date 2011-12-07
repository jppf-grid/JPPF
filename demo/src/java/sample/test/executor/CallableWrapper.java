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

package sample.test.executor;

import java.lang.reflect.*;
import java.util.concurrent.Callable;

import org.jppf.server.protocol.JPPFTask;

/**
 * Wrapper that extends JPPFTask for a Callable.
 * We make this class a Callable too, so we can call JPPFExecutorService.submit(Callable) without ambiguity.
 * @param <V>
 * @author Laurent Cohen
 */
class CallableWrapper<V> extends JPPFTask implements Callable<V>
{
	/**
	 * The actual task to execute.
	 */
	private final Callable<V> task;
	/**
	 * The execution properties rto apply to the task.
	 */
	private final ExecutionProperties execProperties;

	/**
	 * Initialize this task with the specified Callable and execution properties.
	 * @param task the actual task to execute.
	 * @param execProperties encaapsulates the timeout specifications.
	 */
	CallableWrapper(Callable<V> task, ExecutionProperties execProperties)
	{
		if (task == null) throw new IllegalArgumentException("task cannot be null");
		this.task = task;
		this.execProperties = execProperties;
		if (execProperties != null) setTimeoutSchedule(execProperties.getTimeout());
	}

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		try
		{
			setResult(call());
		}
		catch (Exception e)
		{
			// InterruptedException is thrown when the task expires
			setException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public V call() throws Exception
	{
		return task.call();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTimeout()
	{
		try
		{
			Method m = findTimeoutMethod();
			if (m != null)
			{
				m.invoke(task);
			}
		}
		catch (Exception e)
		{
			setException(e);
		}
	}

	/**
	 * Find the method specified by <code>ExecutionProperties.getTimeoutMethodName()</code>.
	 * @return the method to invoke in lieu of the onTimeout().
	 * @throws Exception if any error occurs.
	 */
	private Method findTimeoutMethod() throws Exception
	{
		if ((execProperties == null) || (execProperties.getOnTimeoutMethodName() == null)) return null;
		String name = execProperties.getOnTimeoutMethodName();
		Class clazz = task.getClass();
		for (Method m: clazz.getMethods())
		{
			if (m.getName().equals(name))
			{
				// if no-args instance method
				if (!Modifier.isStatic(m.getModifiers()) && (m.getParameterTypes().length == 0))
				{
					return m;
				}
			}
		}
		return null;
	}
}
