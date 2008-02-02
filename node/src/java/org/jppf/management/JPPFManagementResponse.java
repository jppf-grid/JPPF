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

package org.jppf.management;

import java.io.Serializable;

/**
 * Instances of this class encapsulate the response to an admin request.
 * @author Laurent Cohen
 */
public class JPPFManagementResponse implements Serializable
{
	/**
	 * An eventual exception thrown while executing the admin command.
	 */
	private Exception exception = null;

	/**
	 * The result of the admin command, if any.
	 */
	private Object result = null;

	/**
	 * Initialize this response with the specified result and exception.
	 * @param result the result as an object.
	 * @param exception an instance of <code>Exception</code>.
	 */
	public JPPFManagementResponse(Object result, Exception exception)
	{
		this.result = result;
		this.exception = exception;
	}

	/**
	 * Initialize this response with the specified exception.
	 * @param exception an instance of <code>Exception</code>.
	 */
	public JPPFManagementResponse(Exception exception)
	{
		this.exception = exception;
	}

	/**
	 * Get an eventual exception thrown while executing the admin command.
	 * @return an instance of <code>Exception</code>.
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 * Set an eventual exception thrown while executing the admin command.
	 * @param exception an instance of <code>Exception</code>.
	 */
	public void setException(Exception exception)
	{
		this.exception = exception;
	}

	/**
	 * Get the result of the command's execution.
	 * @return the result as an object, or null if no result is generated.
	 */
	public Object getResult()
	{
		return result;
	}

	/**
	 * Get the result of the command's execution.
	 * @param result the result as an object, or null if no result is generated.
	 */
	public void setResult(Object result)
	{
		this.result = result;
	}

	/**
	 * Get a string representation of this request.
	 * @return a string that describes this request instance.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return super.toString() + " : reuslt=" + result + ", exception="+exception;
	}
}
