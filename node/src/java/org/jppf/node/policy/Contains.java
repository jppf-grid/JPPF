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

package org.jppf.node.policy;

import org.jppf.management.JPPFSystemInformation;

/**
 * An execution policy rule that encapsulates a test of type <i>property_value contains string</i>.
 * The test applies to string values only.
 * @author Laurent Cohen
 */
public class Contains extends ExecutionPolicy
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The name of the property to compare.
	 */
	private String propertyName = null;
	/**
	 * A string value to compare with.
	 */
	private String value = null;
	/**
	 * Determines if the comparison should ignore the string case.
	 */
	private boolean ignoreCase = false;

	/**
	 * Define an equality comparison between the string value of a property and another string value.
	 * @param propertyName the name of the property to compare.
	 * @param ignoreCase determines if the comparison should ignore the string case.
	 * @param a the value to compare with.
	 */
	public Contains(String propertyName, boolean ignoreCase, String a)
	{
		this.propertyName = propertyName;
		this.value = a;
		this.ignoreCase = ignoreCase;
	}

	/**
	 * Determines whether this policy accepts the specified node.
	 * @param info system information for the node on which the tasks will run if accepted.
	 * @return true if the node is accepted, false otherwise.
	 * @see org.jppf.node.policy.ExecutionPolicy#accepts(org.jppf.management.JPPFSystemInformation)
	 */
	@Override
    public boolean accepts(JPPFSystemInformation info)
	{
		if (value == null) return false;
		String s = getProperty(info, propertyName);
		if (s == null) return false;
		if (ignoreCase) return s.toLowerCase().contains(value.toLowerCase());
		return s.contains(value);
	}

	/**
	 * Print this object to a string.
	 * @return an XML string representation of this object
	 * @see java.lang.Object#toString()
	 */
	@Override
    public String toString()
	{
		if (computedToString == null)
		{
			synchronized(ExecutionPolicy.class)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(indent()).append("<Contains ignoreCase=\"").append(ignoreCase).append("\">\n");
				toStringIndent++;
				sb.append(indent()).append("<Property>").append(propertyName).append("</Property>\n");
				sb.append(indent()).append("<Value>").append(value).append("</Value>\n");
				toStringIndent--;
				sb.append(indent()).append("</Contains>\n");
				computedToString = sb.toString();
			}
		}
		return computedToString;
	}
}
