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

package org.jppf.node.policy;

/**
 * Instances of this class build an execution policy graph, based on a policy
 * descriptor parsed from an XML document.
 * @author Laurent Cohen
 */
public class PolicyBuilder
{
	/**
	 * Build an execution policy from a parsed policy descriptor.
	 * @param desc the descriptor parsed from an XML document.
	 * @return an <code>ExecutionPolicy</code> instance.
	 * @throws Exception if an error occurs while generating a policy object.
	 */
	public ExecutionPolicy buildPolicy(PolicyDescriptor desc) throws Exception
	{
		String type = desc.type;
		if ("NOT".equals(type)) return buildNotPolicy(desc);
		else if ("AND".equals(type)) return buildAndPolicy(desc);
		else if ("OR".equals(type)) return buildOrPolicy(desc);
		else if ("XOR".equals(type)) return buildXorPolicy(desc);
		else if ("LessThan".equals(type)) return buildLessThanPolicy(desc);
		else if ("MoreThan".equals(type)) return buildMoreThanPolicy(desc);
		else if ("AtMost".equals(type)) return buildAtMostPolicy(desc);
		else if ("AtLeast".equals(type)) return buildAtLeastPolicy(desc);
		else if ("BetweenII".equals(type)) return buildBetweenIIPolicy(desc);
		else if ("BetweenIE".equals(type)) return buildBetweenIEPolicy(desc);
		else if ("BetweenEI".equals(type)) return buildBetweenEIPolicy(desc);
		else if ("BetweenEE".equals(type)) return buildBetweenEEPolicy(desc);
		else if ("Equal".equals(type)) return buildEqualPolicy(desc);
		else if ("Contains".equals(type)) return buildContainsPolicy(desc);
		else if ("OneOf".equals(type)) return buildOneOfPolicy(desc);
		else if ("RegExp".equals(type)) return buildRegExpPolicy(desc);
		else if ("CustomRule".equals(type)) return buildCustomPolicy(desc);
		return null;
	}

	/**
	 * Build a NOT policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 * @throws Exception if an error occurs while generating the policy object.
	 */
	private ExecutionPolicy buildNotPolicy(PolicyDescriptor desc) throws Exception
	{
		return new ExecutionPolicy.NotRule(buildPolicy(desc.children.get(0)));
	}

	/**
	 * Build an AND policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 * @throws Exception if an error occurs while generating the policy object.
	 */
	private ExecutionPolicy buildAndPolicy(PolicyDescriptor desc) throws Exception
	{
		ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
		int count = 0;
		for (PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
		return new ExecutionPolicy.AndRule(rules);
	}

	/**
	 * Build an OR policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 * @throws Exception if an error occurs while generating the policy object.
	 */
	private ExecutionPolicy buildOrPolicy(PolicyDescriptor desc) throws Exception
	{
		ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
		int count = 0;
		for (PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
		return new ExecutionPolicy.OrRule(rules);
	}

	/**
	 * Build an XOR policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 * @throws Exception if an error occurs while generating the policy object.
	 */
	private ExecutionPolicy buildXorPolicy(PolicyDescriptor desc) throws Exception
	{
		ExecutionPolicy[] rules = new ExecutionPolicy[desc.children.size()];
		int count = 0;
		for (PolicyDescriptor child: desc.children) rules[count++] = buildPolicy(child);
		return new ExecutionPolicy.XorRule(rules);
	}

	/**
	 * Build a LessThan policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildLessThanPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		double value = 0d;
		try
		{
			value = Double.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("'"+s+"' is not a double value", e);
		}
		return new LessThan(desc.operands.get(0), value);
	}

	/**
	 * Build an AtMost policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildAtMostPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		double value = 0d;
		try
		{
			value = Double.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("'"+s+"' is not a double value", e);
		}
		return new AtMost(desc.operands.get(0), value);
	}

	/**
	 * Build a MoreThan policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildMoreThanPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		double value = 0d;
		try
		{
			value = Double.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("'"+s+"' is not a double value", e);
		}
		return new MoreThan(desc.operands.get(0), value);
	}

	/**
	 * Build an AtLeast policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildAtLeastPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		double value = 0d;
		try
		{
			value = Double.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("'"+s+"' is not a double value", e);
		}
		return new AtLeast(desc.operands.get(0), value);
	}

	/**
	 * Build a BetweenII policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildBetweenIIPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		double value1 = 0d;
		double value2 = 0d;
		try
		{
			value1 = Double.valueOf(s);
			s = desc.operands.get(2);
			value2 = Double.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("'"+s+"' is not a double value", e);
		}
		return new BetweenII(desc.operands.get(0), value1, value2);
	}

	/**
	 * Build a BetweenIE policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildBetweenIEPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		double value1 = 0d;
		double value2 = 0d;
		try
		{
			value1 = Double.valueOf(s);
			s = desc.operands.get(2);
			value2 = Double.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("'"+s+"' is not a double value", e);
		}
		return new BetweenIE(desc.operands.get(0), value1, value2);
	}

	/**
	 * Build a BetweenEI policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildBetweenEIPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		double value1 = 0d;
		double value2 = 0d;
		try
		{
			value1 = Double.valueOf(s);
			s = desc.operands.get(2);
			value2 = Double.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("'"+s+"' is not a double value", e);
		}
		return new BetweenEI(desc.operands.get(0), value1, value2);
	}

	/**
	 * Build a BetweenEE policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildBetweenEEPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		double value1 = 0d;
		double value2 = 0d;
		try
		{
			value1 = Double.valueOf(s);
			s = desc.operands.get(2);
			value2 = Double.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("'"+s+"' is not a double value", e);
		}
		return new BetweenEE(desc.operands.get(0), value1, value2);
	}

	/**
	 * Build a Equal policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildEqualPolicy(PolicyDescriptor desc)
	{
		String s = desc.operands.get(1);
		if ("string".equals(desc.valueType))
		{
			boolean ignoreCase = (desc.ignoreCase == null) ? false : Boolean.valueOf(desc.ignoreCase);
			return new Equal(desc.operands.get(0), ignoreCase, s);
		}
		if ("numeric".equals(desc.valueType))
		{
			double value = 0d;
			try
			{
				value = Double.valueOf(s);
				return new Equal(desc.operands.get(0), value);
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("'"+s+"' is not a double value", e);
			}
		}
		return new Equal(desc.operands.get(0), Boolean.valueOf(s));
	}

	/**
	 * Build a Contains policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildContainsPolicy(PolicyDescriptor desc)
	{
		boolean ignoreCase = (desc.ignoreCase == null) ? false : Boolean.valueOf(desc.ignoreCase);
		return new Contains(desc.operands.get(0), ignoreCase, desc.operands.get(1));
	}

	/**
	 * Build a OneOf policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildOneOfPolicy(PolicyDescriptor desc)
	{
		if ("numeric".equals(desc.valueType))
		{
			double[] values = new double[desc.operands.size() - 1];
			for (int i=1; i<desc.operands.size(); i++)
			{
				String s = desc.operands.get(i);
				try
				{
					values[i-1] = Double.valueOf(s);
				}
				catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("'"+s+"' is not a double value", e);
				}
			}
			return new OneOf(desc.operands.get(0), values);
		}
		String[] values = new String[desc.operands.size() - 1];
		for (int i=1; i<desc.operands.size(); i++) values[i-1] = desc.operands.get(i);
		boolean ignoreCase = Boolean.valueOf(desc.ignoreCase);
		return new OneOf(desc.operands.get(0), ignoreCase, values);
	}

	/**
	 * Build a RegExp policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 */
	private ExecutionPolicy buildRegExpPolicy(PolicyDescriptor desc)
	{
		return new RegExp(desc.operands.get(0), desc.operands.get(1));
	}

	/**
	 * Build a custom policy from a descriptor.
	 * @param desc the descriptor to use.
	 * @return an <code>ExecutionPolicy</code> instance.
	 * @throws Exception if an error occurs while generating the custom policy object.
	 */
	private ExecutionPolicy buildCustomPolicy(PolicyDescriptor desc) throws Exception
	{
		Class clazz = Class.forName(desc.className);
		CustomPolicy policy = (CustomPolicy) clazz.newInstance();
		policy.setArgs(desc.arguments.toArray(new String[0]));
		policy.initialize();
		return policy;
	}
}
