/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.server.protocol;

import java.io.*;

import org.jppf.JPPFException;

/**
 * Instances of this class are used to signal that a task could not be sent back by the node to the server.
 * <p>This generally happens when a task cannot be serialized after its execution, or if a data transformation is
 * applied and fails with an exception. An instance of this class captures the context of the error, including the exception
 * that occurred, the object's <code>toString()</code> descriptor and its class name.
 * <p>When such an error occurs, an instance of this class will be sent instead of the initial JPPF task.
 * @author Laurent Cohen
 */
public final class JPPFExceptionResult extends JPPFTask
{
	/**
	 * This captures the result of ("" + object).
	 */
	private String objectDescriptor = null;
	/**
	 * The fully qualified class name of the object that triggered the error.
	 */
	private String className = null;

	/**
	 * Initialize this task with the spepcifed error context.
	 * @param throwable the throwable that is to be captured.
	 * @param object the object on which the throwable applies.
	 */
	public JPPFExceptionResult(Throwable throwable, Object object)
	{
		if (throwable instanceof Exception) setException((Exception) throwable);
		else setException(new JPPFException(throwable));
		objectDescriptor = "" + object;
		if (object != null) className = object.getClass().getName();
	}

	/**
	 * Display the error context captured in this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		System.out.println(toString());
	}

	/**
	 * Construct a string representation of this object.
	 * @return a string representing this JPPFExceptionResult.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Error occurred on object [").append(objectDescriptor).append("], class=").append(className);
		if (getException() != null)
		{
			sb.append(" :\n");
			StringWriter sw = new StringWriter();
			PrintWriter writer = new PrintWriter(sw);
			getException().printStackTrace(writer);
			sb.append(sw.toString());
		}
		return sb.toString();
	}
}
