/*
 * Java Parallel Processing Framework.
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

import java.security.PrivilegedAction;

/**
 * Abstract superclass for priviledged actions  used to invoke a method or constructor through reflection when a security manager is present. 
 */
public abstract class AbstractPrivilegedAction implements PrivilegedAction
{
	/**
	 * Used to capture an exception resulting from the method or constructore invocation.
	 */
	protected Exception exception = null;
	/**
	 * The parameters of the method or constructor to invoke.
	 */
	protected Object[] args = null;

	/**
	 * Get the exception resulting from the method or constructore invocation.
	 * @return an <code>Exception</code> instance.
	 */
	public Exception getException()
	{
		return exception;
	}
}
