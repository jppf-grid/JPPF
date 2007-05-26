/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server.protocol;

import java.io.Serializable;

import org.jppf.task.storage.DataProvider;

/**
 * Abstract superclass for all tasks submitted to the execution server.
 * This class provides the basic facilities to handle data shared among tasks, handling of task execution exception,
 * and handling of the execution results.<p>
 * JPPF clients have to extend this class and must implement the <code>run</code> method. In the
 * <code>run</code> method the task calculations are performed, and the result of the calculations
 * is set with the {@link #setResult(Object)} method:
 * <pre>
 * class MyTask extends JPPFTask {
 *     	public void run() {
 *          // do the calculation ...
 *          setResult(myResult);
 *      }
 * }
 * </pre>
 * @author Laurent Cohen
 */
public abstract class JPPFTask implements Runnable, Serializable
{
	/**
	 * The position of this task at the submission time.
	 */
	private int position;
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
	 * Get the exception that was raised by this task's execution. If the task raised a
	 * {@link Throwable}, the exception is embedded into a {@link org.jppf.JPPFException}.
	 * @return a <code>Exception</code> instance, or null if no exception was raised.
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 * Sets the exception that was raised by this task's execution in the <code>run</code> method.
	 * The exception is set by the JPPF framework.
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

	/**
	 * Returns the position of this task at the submission.
	 * @return Returns the position of this task at the submission.
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Sets the position of this task into the submission.
	 * @param position The position of this task into the submission.
	 */
	public void setPosition(int position) {
		this.position = position;
	}
}
