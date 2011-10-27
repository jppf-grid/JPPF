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

package org.jppf.client.taskwrapper;

import java.util.concurrent.Callable;

/**
 * Task wrapper for classes implementing {@link java.util.concurrent.Callable Callable}.
 * @author Laurent Cohen
 */
public class CallableTaskWrapper extends AbstractTaskObjectWrapper
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The callable object to execute.
	 */
	private Callable callable = null;

	/**
	 * Initialize this wrapper with the specified <code>Runnable</code> object.
	 * @param callable the callable object to execute.
	 */
	public CallableTaskWrapper(final Callable callable)
	{
		this.callable = callable;
	}

	/**
	 * Execute the <code>call()</code> method of this callable task.
	 * @return the result of the execution.
	 * @throws Exception if an error occurs during the execution.
	 * @see org.jppf.client.taskwrapper.TaskObjectWrapper#execute()
	 */
	@Override
	public Object execute() throws Exception
	{
		return callable.call();
	}

	/**
	 * Return the object on which a method or constructor is called.
	 * @return an object or null if the invoked method is static.
	 * @see org.jppf.client.taskwrapper.TaskObjectWrapper#getTaskObject()
	 */
	@Override
	public Object getTaskObject()
	{
		return callable;
	}
}
