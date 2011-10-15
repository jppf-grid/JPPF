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

package org.jppf.node.protocol;

import org.jppf.scheduling.JPPFSchedule;
import org.jppf.task.storage.DataProvider;

/**
 * 
 * @author Laurent Cohen
 */
public interface Task
{

	/**
	 * Get the result of the task execution.
	 * @return the result as an array of bytes.
	 */
	Object getResult();

	/**
	 * Get the exception that was raised by this task's execution. If the task raised a
	 * {@link Throwable}, the exception is embedded into a {@link org.jppf.JPPFException}.
	 * @return a <code>Exception</code> instance, or null if no exception was raised.
	 */
	Exception getException();

	/**
	 * Get the provider of shared data for this task.
	 * @return a <code>DataProvider</code> instance. 
	 */
	DataProvider getDataProvider();

	/**
	 * Get the timeout for this task.
	 * @return the timeout in milliseconds.
	 * @deprecated use the {@link JPPFSchedule} object from {@link #getTimeoutSchedule() getTimeoutSchedule()} instead.
	 */
	long getTimeout();

	/**
	 * Get the timeout date for this task.
	 * @return the date in string format.
	 * @deprecated use the {@link JPPFSchedule} object from {@link #getTimeoutSchedule() getTimeoutSchedule()} instead.
	 */
	String getTimeoutDate();

	/**
	 * Get the format of timeout date for this task.
	 * @return the timeout date format as a string pattern, as decribed in the specification for {@link SimpleDateFormat}.
	 * @deprecated use the {@link JPPFSchedule} object from {@link #getTimeoutSchedule() getTimeoutSchedule()} instead.
	 */
	String getTimeoutFormat();

	/**
	 * Get the user-assigned id for this task.
	 * @return the id as a string.
	 */
	String getId();

	/**
	 * Get the <code>JPPFRunnable</code>-annotated object or POJO wrapped by this task.
	 * @return an objet or class that is JPPF-annotated.
	 */
	Object getTaskObject();

	/**
	 * Get the task timeout schedule configuration.
	 * @return a <code>JPPFScheduleConfiguration</code> instance.
	 */
	JPPFSchedule getTimeoutSchedule();
}