/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.server.protocol;

import java.io.Serializable;
import org.jppf.task.storage.DataProvider;

/**
 * Abstract superclass for all tasks submitted to the execution server.
 * This class provides the basic facilities to handle data shared among tasks, handling of task execution exception,
 * and handling of the execution results.
 * @author Laurent Cohen
 */
public abstract class JPPFTask implements Runnable, Serializable
{
	/**
	 * The result of the task execution.
	 */
	private Object result = null;
	/**
	 * The exception that was raised by this task's execution.
	 */
	private Exception exception = null;
	/**
	 * The provider of shared data for this task.
	 */
	private transient DataProvider dataProvider = null;

	/**
	 * Get the result of the task execution.
	 * @return the result as an array of bytes.
	 */
	public Object getResult()
	{
		return result;
	}

	/**
	 * Set the result of the task execution.
	 * @param  result the result of this task's execution.
	 */
	public void setResult(Object  result)
	{
		this.result = result;
	}

	/**
	 * Get the exception that was raised by this task's execution.
	 * @return a <code>Exception</code> instance, or null if no exception was raised.
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 * Get the exception that was raised by this task's execution.
	 * @param exception a <code>ClientApplicationException</code> instance.
	 */
	public void setException(Exception exception)
	{
		this.exception = exception;
	}

	/**
	 * Get the provider of shared data for this task.
	 * @return a <code>DataProvider</code> instance. 
	 */
	public DataProvider getDataProvider()
	{
		return dataProvider;
	}

	/**
	 * Set the provider of shared data for this task.
	 * @param dataProvider a <code>DataProvider</code> instance.
	 */
	public void setDataProvider(DataProvider dataProvider)
	{
		this.dataProvider = dataProvider;
	}
}
