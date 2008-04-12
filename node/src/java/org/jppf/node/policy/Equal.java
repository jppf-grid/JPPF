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

package org.jppf.node.policy;

import org.jppf.management.JPPFSystemInformation;

/**
 * An execution policy rule that encapsulates a test of type <i>property_value == value</i>.
 * The test applies to numeric, string and boolean values only.
 * @author Laurent Cohen
 */
public class Equal extends ExecutionPolicy
{
	/**
	 * The name of the property to compare.
	 */
	private String propertyName = null;
	/**
	 * A numeric value to compare with.
	 */
	private Number numberValue = null;
	/**
	 * A string value to compare with.
	 */
	private String stringValue = null;
	/**
	 * An object value to compare with.
	 */
	private Boolean booleanValue = null;
	/**
	 * Determines if the comparison should ignore the string case.
	 */
	private boolean ignoreCase = false;

	/**
	 * Define an equality comparison between the numeric value of a property and another numeric value.
	 * @param propertyName the name of the property to compare.
	 * @param a the value to compare with.
	 */
	public Equal(String propertyName, double a)
	{
		this.propertyName = propertyName;
		this.numberValue = a;
	}

	/**
	 * Define an equality comparison between the numeric value of a property and another numeric value.
	 * @param propertyName the name of the property to compare.
	 * @param ignoreCase determines if the comparison should ignore the string case.
	 * @param a the value to compare with.
	 */
	public Equal(String propertyName, boolean ignoreCase, String a)
	{
		this.propertyName = propertyName;
		this.stringValue = a;
		this.ignoreCase = ignoreCase;
	}

	/**
	 * Define an equality comparison between the numeric value of a property and another numeric value.
	 * @param propertyName the name of the property to compare.
	 * @param a the value to compare with.
	 */
	public Equal(String propertyName, boolean a)
	{
		this.propertyName = propertyName;
		this.booleanValue = a;
	}

	/**
	 * Determines whether this policy accepts the specified node.
	 * @param info system information for the node on which the tasks will run if accepted.
	 * @return true if the node is accepted, false otherwise.
	 * @see org.jppf.node.policy.ExecutionPolicy#accepts(org.jppf.management.JPPFSystemInformation)
	 */
	public boolean accepts(JPPFSystemInformation info)
	{
		try
		{
			String s = getProperty(info, propertyName);
			if (numberValue != null) return Double.valueOf(s).doubleValue() == numberValue.doubleValue();
			else if (stringValue != null)
			{
				return ignoreCase ? stringValue.equalsIgnoreCase(s) : stringValue.equals(s);
			}
			else if (booleanValue != null) return Boolean.valueOf(s).booleanValue() == booleanValue.booleanValue();
			else return s == null;
		}
		catch(Exception e)
		{
		}
		return false;
	}

	/**
	 * Print this object to a string.
	 * @return an XML string representation of this object
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		if (computedToString == null)
		{
			synchronized(ExecutionPolicy.class)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(indent()).append("<Equal valueType=\"");
				if (stringValue != null) sb.append("string");
				else if (numberValue != null) sb.append("numeric");
				else if (booleanValue != null) sb.append("boolean");
				sb.append("\" ignoreCase=\"").append(ignoreCase).append("\">\n");
				toStringIndent++;
				sb.append(indent()).append("<Property>").append(propertyName).append("</Property>\n");
				sb.append(indent()).append("<Value>");
				if (stringValue != null) sb.append(stringValue);
				else if (numberValue != null) sb.append(numberValue);
				else if (booleanValue != null) sb.append(booleanValue);
				sb.append("</Value>\n");
				toStringIndent--;
				sb.append(indent()).append("</Equal>\n");
				computedToString = sb.toString();
			}
		}
		return computedToString;
	}
}
