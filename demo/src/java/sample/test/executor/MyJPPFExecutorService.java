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

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.concurrent.JPPFExecutorService;

/**
 * An executor that allows setting a timeout on pure callable tasks.
 * @author Laurent Cohen
 */
public class MyJPPFExecutorService extends JPPFExecutorService
{
	/**
	 * 
	 */
	private Map<Class<? extends Callable>, ExecutionProperties> execPropertiesMap = new HashMap<Class<? extends Callable>, ExecutionProperties>();

	/**
	 * Initialize this executor form the specified JPPF client.
	 * @param client the JPPF Client.
	 */
	public MyJPPFExecutorService(JPPFClient client)
	{
		super(client);
	}

	/**
	 * Register the specified properties for the specified class of tasks.
	 * @param clazz the class of tasks to which the properties apply.
	 * @param execProperties the properties to set for tasks of this class.
	 * @return the execution properties set on the class.
	 */
	public ExecutionProperties registerClass(Class<? extends Callable> clazz, ExecutionProperties execProperties)
	{
		execPropertiesMap.put(clazz, execProperties);
		return execProperties;
	}

	/**
	 * Un-register the specified properties for the specified class of tasks.
	 * @param clazz the class of tasks to which the properties apply.
	 * @return the execution properties set on the class.
	 */
	public ExecutionProperties unregisterClass(Class<? extends Callable> clazz)
	{
		return execPropertiesMap.remove(clazz);
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> Future<T> submit(Callable<T> task)
	{
		ExecutionProperties props = execPropertiesMap.get(task.getClass());
		if (props != null)
		{
			CallableWrapper<T> wrapper = new CallableWrapper(task, props);
			return super.submit((Callable<T>) wrapper);
		}
		return super.submit(task);
	}
}
