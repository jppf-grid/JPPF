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

package org.jppf.test.setup;

import java.io.Serializable;

/**
 * Instances of this class represent the result of a task execution.
 */
public class Result implements Serializable
{
	/**
	 * The result of executing this task.
	 */
	public String message = null;
	/**
	 * The position of this result.
	 */
	public int position = -1;

	/**
	 * Get a string representation of this object.
	 * @return a string representing this object.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "Result[\"" + message + "\", " + position + "]";
	}
}