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

package org.jppf.client.concurrent;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Callable wrapper around a Runnable.
 * @param <V> the type of result.
 */
class RunnableWrapper<V> implements Callable<V>, Serializable
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The runnable to execute.
	 */
	private Runnable runnable = null;
	/**
	 * The result to return.
	 */
	private V result = null;

	/**
	 * Initialize this callable with the specified parameters.
	 * @param runnable the runnable to execute.
	 * @param result he result to return.
	 */
	public RunnableWrapper(Runnable runnable, V result)
	{
		this.runnable = runnable;
		this.result = result;
	}

	/**
	 * Execute the task.
	 * @return the result specified in the constructor.
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
    public V call()
	{
		runnable.run();
		return result;
	}
}
