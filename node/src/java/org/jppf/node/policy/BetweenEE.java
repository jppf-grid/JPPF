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
 * An execution policy rule that encapsulates a test of type <i>a &lt; property_value &lt; b</i>.
 * The test applies to numeric values only.
 * @author Laurent Cohen
 */
public class BetweenEE extends ExecutionPolicy
{
	/**
	 * The name of the property to compare.
	 */
	private String propertyName = null;
	/**
	 * The interval's lower bound.
	 */
	private double a = 0d;
	/**
	 * The interval's upper bound.
	 */
	private double b = 0d;

	/**
	 * Define a comparison of type value between a and b with a excluded and b excluded.
	 * @param propertyName the name of the property to compare.
	 * @param a the lower bound.
	 * @param b the upper bound.
	 */
	public BetweenEE(String propertyName, double a, double b)
	{
		this.propertyName = propertyName;
		this.a = a;
		this.b = b;
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
			if (s != null)
			{
				double value = Double.valueOf(s);
				return (value > a) && (value < b);
			}
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
				sb.append(indent()).append("<BetweenEE>\n");
				toStringIndent++;
				sb.append(indent()).append("<Property>").append(propertyName).append("</Property>\n");
				sb.append(indent()).append("<Value>").append(a).append("</Value>\n");
				sb.append(indent()).append("<Value>").append(b).append("</Value>\n");
				toStringIndent--;
				sb.append(indent()).append("</BetweenEE>\n");
				computedToString = sb.toString();
			}
		}
		return computedToString;
	}
}
