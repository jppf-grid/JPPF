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
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The actual value of the priority.
	 */
	private int value = 0;

	/**
	 * Initialize this priority witht he specified object.
	 * @param value the object used as priority.
	 */
	public JPPFPriority(int value)
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
		int v2 = o.getValue();
		return value > v2 ? -1 : (value < v2 ? 1 : 0);
	}

	/**
	 * Get the actual value of the priority.
	 * @return the priority as an integer value.
	 */
	public int getValue()
	{
		return value;
	}

	/**
	 * Indcates whether some object is "equal" to this one.
	 * @param obj the object ot compare with.
	 * @return true if the 2 objects are equal, false otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof JPPFPriority)) return false; 
		return value == ((JPPFPriority) obj).getValue();
	}

	/**
	 * Return a hashcode value for this object. 
	 * @return the priority value.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return value;
	}
}
