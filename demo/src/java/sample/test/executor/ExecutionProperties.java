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

import java.io.Serializable;

import org.jppf.scheduling.JPPFSchedule;

/**
 * This class encapsulates an external definiton of the timeout behavior for a task. 
 */
public class ExecutionProperties implements Serializable
{
	/**
	 * The timeout for the tasks.
	 */
	private JPPFSchedule timeout = null;
	/**
	 * The name of the method to invoke when the timeout expires.
	 */
	private String onTimeoutMethodName = null;

	/**
	 * Initialize these execution properties.
	 * Default constructor provided just in case ...
	 */
	public ExecutionProperties()
	{
	}

	/**
	 * Initialize with the specified timeout.
	 * @param timeout the timeout for the tasks.
	 */
	public ExecutionProperties(JPPFSchedule timeout)
	{
		this.timeout = timeout;
	}

	/**
	 * Initialize with the specified timeout schedule and method.
	 * @param timeout the timeout for the tasks.
	 * @param onTimeoutMethodName the name of the method to invoke when the timeout expires.
	 */
	public ExecutionProperties(JPPFSchedule timeout, String onTimeoutMethodName)
	{
		this.timeout = timeout;
		this.onTimeoutMethodName = onTimeoutMethodName;
	}

	/**
	 * Get the timeout for the tasks.
	 * @return the timeout as a <code>JPPFSchedule</code>.
	 */
	public JPPFSchedule getTimeout()
	{
		return timeout;
	}

	/**
	 * Set the timeout for the tasks.
	 * @param timeout the timeout as a <code>JPPFSchedule</code>.
	 */
	public void setTimeout(JPPFSchedule timeout)
	{
		this.timeout = timeout;
	}

	/**
	 * Get the name of the method to invoke when the timeout expires.
	 * @return the method name.
	 */
	public String getOnTimeoutMethodName()
	{
		return onTimeoutMethodName;
	}

	/**
	 * Set the name of the method to invoke when the timeout expires.
	 * @param onTimeoutMethodName the method name.
	 */
	public void setOnTimeoutMethodName(String onTimeoutMethodName)
	{
		this.onTimeoutMethodName = onTimeoutMethodName;
	}
}