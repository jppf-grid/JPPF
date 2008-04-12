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

package org.jppf.server.queue;

import java.io.Serializable;

/**
 * Encapsulation of an integer value as a priority.
 * This class also defines its natural order as a descending order of priority values.
 * @author Laurent Cohen
 */
public class JPPFPriority implements Comparable<JPPFPriority>, Serializable
{
	/**
	 * The actual value of the priority.
	 */
	private Integer value = null;

	/**
	 * Initialize this priority witht he specified object.
	 * @param value the object used as priority.
	 * @throws IllegalArgumentException if value is not an instance of <code>Comparable</code>.
	 */
	public JPPFPriority(int value) throws IllegalArgumentException
	{
		this.value = value;
	}

	/**
	 * Compare this priority with another. This method defines a descending order of priority values,
	 * meaning a higher priority will come before a lower one.
	 * @param o the priority to compare with.
	 * @return a positive value if this priority is greater, a negative value if it is less, otherwise 0.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(JPPFPriority o)
	{
		if (o == null) return 1;
		return o.getValue().compareTo(value);
	}

	/**
	 * Get the actual value of the priority.
	 * @return the priority as an integer value.
	 */
	public Integer getValue()
	{
		return value;
	}
}
