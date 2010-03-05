/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.client.taskwrapper;

import static org.jppf.client.taskwrapper.TaskObjectWrapper.MethodType.*;

import java.lang.reflect.*;
import java.security.AccessController;

import org.jppf.utils.ReflectionUtils;

/**
 * Task wrapper for POJO classes.
 * @author Laurent Cohen
 */
public class PojoTaskWrapper extends AbstractTaskObjectWrapper
{
	/**
	 * The runnable object to execute.
	 */
	private Object taskObject = null;
	/**
	 * The name of the method to execute on the task object.
	 * Can be any method, including static method and constructor.
	 */
	private String method = null;
	/**
	 * The arguments of the method to execute.
	 */
	private Object[] args = null;
	/**
	 * The name of the object's class if the method is static or a constructor.
	 */
	private String className = null;

	/**
	 * Initialize this wrapper with the specified <code>Runnable</code> object.
	 * @param method the name of the method to execute on the task object.
	 * @param taskObject the runnable object to execute.
	 * @param args the arguments of the method to execute.
	 */
	public PojoTaskWrapper(String method, Object taskObject, Object...args)
	{
		this.method = method;
		if (taskObject instanceof Class)
		{
			Class clazz = (Class) taskObject;
			if (method.equals(clazz.getSimpleName())) methodType = CONSTRUCTOR;
			else methodType = STATIC;
			this.className = clazz.getName();
		}
		else
		{
			this.taskObject = taskObject;
			methodType = INSTANCE;
		}
		this.args = args;
	}

	/**
	 * Execute the run method of this runnable task.
	 * @return the result of the execution.
	 * @throws Exception if an error occurs during the execution.
	 * @see org.jppf.client.taskwrapper.TaskObjectWrapper#execute()
	 */
	public Object execute() throws Exception
	{
		Class clazz = INSTANCE.equals(methodType) ? taskObject.getClass() : Class.forName(className);
		Object result = null;
		AbstractPrivilegedAction action = null; 
		switch(methodType)
		{
			case INSTANCE:
			case STATIC:
				Method m = ReflectionUtils.getMatchingMethod(clazz, method, args);
				action = new PrivilegedMethodAction(m, taskObject, args);
				break;

			case CONSTRUCTOR:
				Constructor c = ReflectionUtils.getMatchingConstructor(clazz, args);
				action = new PrivilegedConstructorAction(c, args);
				break;
		}
		result = AccessController.doPrivileged(action);
		if (action.getException() != null) throw action.getException();
		return result;
	}

	/**
	 * Return the object on which a method or constructor is called.
	 * @return an object or null if the invoked method is static. 
	 * @see org.jppf.client.taskwrapper.TaskObjectWrapper#getTaskObject()
	 */
	public Object getTaskObject()
	{
		return taskObject;
	}
}
