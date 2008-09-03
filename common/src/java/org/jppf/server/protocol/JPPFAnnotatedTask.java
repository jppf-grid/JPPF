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

package org.jppf.server.protocol;

import java.lang.reflect.*;
import java.security.*;

import org.jppf.utils.ReflectionUtils;


/**
 * JPPF task wrapper for an object whose class is annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable}.
 * @author Laurent Cohen
 */
public class JPPFAnnotatedTask extends JPPFTask
{
	/**
	 * A <code>JPPFRunnable</code>-annotated object.
	 */
	private Object taskObject = null;
	/**
	 * The methods arguments to pass on when it is invoked.
	 */
	private Object[] args = null;
	/**
	 * Initialize this task with an object whose class is annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable}.
	 * @param taskObject a <code>JPPFRunnable</code>-annotated object.
	 * @param args a <code>JPPFRunnable</code>-annotated object.
	 */
	public JPPFAnnotatedTask(Object taskObject, Object...args)
	{
		this.taskObject = taskObject;
		this.args = args;
	}

	/**
	 * Run the <code>JPPFRunnable</code>-annotated method of the task object.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			if (taskObject == null) return;
			Class clazz = taskObject.getClass();
			Method[] methods = clazz.getDeclaredMethods();
			for (Method m: methods)
			{
				if (ReflectionUtils.isJPPFAnnotated(m))
				{
					Object o = executeMethod(m);
					setResult(o);
					break;
				}
			}
		}
		catch(Exception e)
		{
			setException(e);
		}
	}

	/**
	 * Execute a JPPF-annotated method.
	 * @param m the method to execute.
	 * @return the result of the method invocation.
	 * @throws Exception if an error is raised while invoking the method.
	 */
	private Object executeMethod(final Method m) throws Exception
	{
		int mod = m.getModifiers();
		final Object invoker = Modifier.isStatic(mod) ? null : taskObject;
		int n = m.getParameterTypes().length;
		final Object[] params = new Object[n];
		for (int i=0; i<n; i++)
		{
			if ((args == null) || (i > args.length)) params[i] = null;
			else params[i] = args[i];
		}
		Object result = AccessController.doPrivileged(new PrivilegedAction<Object>()
		{
			public Object run()
			{
				Object o = null;
				try
				{
					o = m.invoke(invoker, params);
				}
				catch(Exception e)
				{
					setException(e);
				}
				return o;
			}
		});
		return result;
	}
}
