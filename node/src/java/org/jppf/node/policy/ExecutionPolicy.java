/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.io.Serializable;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.utils.TypedProperties;

/**
 * Interface for all execution policy implementations.
 * @author Laurent Cohen
 */
public abstract class ExecutionPolicy implements Serializable
{
	/**
	 * Level of indentation used in the toString() method.
	 */
	protected static int toStringIndent = 0;
	/**
	 * Stores the XML representation of this object.
	 * Used to avoid doing it more than once.
	 */
	protected String computedToString = null;
	/**
	 * Determines whether this policy accepts the specified node.
	 * @param info system information for the node on which the tasks will run if accepted.
	 * @return true if the node is accepted, false otherwise.
	 */
	public abstract boolean accepts(JPPFSystemInformation info);

	/**
	 * Create an execution policy that is a logical "AND" combination of this policy and the one specified as operand.
	 * @param rules rules to combine this one with.
	 * @return an execution policy that combines the this policy with the operand in an "AND" operation.
	 */
	public ExecutionPolicy and(ExecutionPolicy...rules)
	{
		return new AndRule(makeRuleArray(this, rules));
	}

	/**
	 * Create an execution policy that is a logical "OR" combination of this policy and those specified as operands.
	 * @param rules rules to combine this one with.
	 * @return an execution policy that combines the this policy with the operand in an "OR" operation.
	 */
	public ExecutionPolicy or(ExecutionPolicy...rules)
	{
		return new OrRule(makeRuleArray(this, rules));
	}

	/**
	 * Create an execution policy that is a logical "XOR" combination of the 2 policies specified as operands.
	 * @param rules rules to combine this one with.
	 * @return an execution policy that combines the this policy with the operand in an "XOR" operation.
	 */
	public ExecutionPolicy xor(ExecutionPolicy...rules)
	{
		return new XorRule(makeRuleArray(this, rules));
	}

	/**
	 * Create an execution policy that is a negation of this policy.
	 * @return an execution policy that negates this policy.
	 */
	public ExecutionPolicy not()
	{
		return new NotRule(this);
	}

	/**
	 * Generate  new array with size +1 and the specified rule as first element. 
	 * @param rule the rule to set as first element.
	 * @param ruleArray the array of other rules.
	 * @return an array of <code>ExceutionPolicy</code> instances.
	 */
	private ExecutionPolicy[] makeRuleArray(ExecutionPolicy rule, ExecutionPolicy[] ruleArray)
	{
		ExecutionPolicy[] result = new ExecutionPolicy[ruleArray.length + 1];
		int count = 0;
		result[count++] = rule;
		for (ExecutionPolicy r: ruleArray) result[count++] = r;
		return result;
	}

	/**
	 * Get the value of the specified property in the specified set of system information.
	 * @param info the system information in which to lookup the property.
	 * @param name the name of the proeprty to look for.
	 * @return the value of the property, or null if it could not be found.
	 */
	public String getProperty(JPPFSystemInformation info, String name)
	{
		TypedProperties[] propsArray = { info.getJppf(), info.getSystem(), info.getEnv(), info.getNetwork(), info.getRuntime()};
		for (TypedProperties props: propsArray)
		{
			String value = props.getString(name);
			if (value != null) return value;
		}
		return null;
	}

	/**
	 * Get an indented string.
	 * @return an indented string depending on the value of <code>toStringIndent</code>.
	 */
	protected static String indent()
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<toStringIndent; i++) sb.append("  ");
		return sb.toString();
	}

	/**
	 * An execution policy that realizes a binary logical combination of the policies specified as operands.
	 */
	public abstract static class LogicalRule extends ExecutionPolicy
	{
		/**
		 * First operand.
		 */
		protected ExecutionPolicy[] rules = null;

		/**
		 * Initialize this binary logical operator with the specified operands.
		 * @param rules the first operand.
		 */
		public LogicalRule(ExecutionPolicy...rules)
		{
			this.rules = rules;
		}

		/**
		 * Print this object to a string.
		 * @return an XML string representation of this object
		 * @see java.lang.Object#toString()
		 */
		public String toString()
		{
			synchronized(ExecutionPolicy.class)
			{
				StringBuilder sb = new StringBuilder();
				toStringIndent++;
				if (rules == null) sb.append(indent()).append("null\n");
				else
				{
					for (ExecutionPolicy ep: rules) sb.append(ep.toString());
				}
				toStringIndent--;
				return sb.toString();
			}
		}
	}

	/**
	 * An execution policy that realizes a logical "AND" combination of multiple policies specified as operands.
	 */
	public static class AndRule extends LogicalRule
	{
		/**
		 * Initialize this AND operator with the specified operands.
		 * @param rules the rules to combine.
		 */
		public AndRule(ExecutionPolicy...rules)
		{
			super(rules);
		}

		/**
		 * Determine if a node is acceptable for this policy.
		 * @param info system information for the node on which the tasks will run if accepted.
		 * @return true if and only if the 2 operands' accepts() method return true or an empty or null operand list was specified.
		 * @see org.jppf.node.policy.ExecutionPolicy#accepts(org.jppf.management.JPPFSystemInformation)
		 */
		public boolean accepts(JPPFSystemInformation info)
		{
			if ((rules == null) || (rules.length <= 0)) return true;
			boolean b = true;
			for (ExecutionPolicy ep: rules) b = b && ep.accepts(info);
			return b;
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
					computedToString = new StringBuilder().append(indent()).append("<AND>\n").append(super.toString()).append(indent()).append("</AND>\n").toString();
				}
			}
			return computedToString;
		}
	}

	/**
	 * An execution policy that realizes a logical "OR" combination of multiple policies specified as operands.
	 */
	public static class OrRule extends LogicalRule
	{
		/**
		 * Initialize this OR operator with the specified operands.
		 * @param rules the rules to combine.
		 */
		public OrRule(ExecutionPolicy...rules)
		{
			super(rules);
		}

		/**
		 * Determine if a node is acceptable for this policy.
		 * @param info system information for the node on which the tasks will run if accepted.
		 * @return true if at least one of the operands' accepts() method returns true.
		 * @see org.jppf.node.policy.ExecutionPolicy#accepts(org.jppf.management.JPPFSystemInformation)
		 */
		public boolean accepts(JPPFSystemInformation info)
		{
			if ((rules == null) || (rules.length <= 0)) return true;
			boolean b = false;
			for (ExecutionPolicy ep: rules) b = b || ep.accepts(info);
			return b;
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
					computedToString = new StringBuilder().append(indent()).append("<OR>\n").append(super.toString()).append(indent()).append("</OR>\n").toString();
				}
			}
			return computedToString;
		}
	}

	/**
	 * An execution policy that realizes a logical "XOR" combination of multiple policies specified as operands.
	 */
	public static class XorRule extends LogicalRule
	{
		/**
		 * Initialize this OR operator with the specified operands.
		 * @param rules the rules to combine.
		 */
		public XorRule(ExecutionPolicy...rules)
		{
			super(rules);
		}

		/**
		 * Determine if a node is acceptable for this policy.
		 * @param info system information for the node on which the tasks will run if accepted.
		 * @return true if and only if the operands' accepts() method return different values.
		 * @see org.jppf.node.policy.ExecutionPolicy#accepts(org.jppf.management.JPPFSystemInformation)
		 */
		public boolean accepts(JPPFSystemInformation info)
		{
			if ((rules == null) || (rules.length <= 0)) return true;
			boolean b = rules[0].accepts(info);
			if (rules.length >= 1) for (int i=1; i<rules.length; i++) b = (b == rules[i].accepts(info));
			return b;
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
					computedToString = new StringBuilder().append(indent()).append("<XOR>\n").append(super.toString()).append(indent()).append("</XOR>\n").toString();
				}
			}
			return computedToString;
		}
	}

	/**
	 * An execution policy that realizes the negation of a policy specified as operand.
	 */
	public static class NotRule extends ExecutionPolicy
	{
		/**
		 * The operand.
		 */
		private ExecutionPolicy rule = null;

		/**
		 * Initialize this binary logical operator with the specified operands.
		 * @param rule the operand.
		 */
		public NotRule(ExecutionPolicy rule)
		{
			this.rule = rule;
		}

		/**
		 * Determine if a node is acceptable for this policy.
		 * @param info system information for the node on which the tasks will run if accepted.
		 * @return true if and only if the 2 operands' accepts() method return true.
		 * @see org.jppf.node.policy.ExecutionPolicy#accepts(org.jppf.management.JPPFSystemInformation)
		 */
		public boolean accepts(JPPFSystemInformation info)
		{
			return !rule.accepts(info);
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
					sb.append(indent()).append("<NOT>\n");
					toStringIndent++;
					if (rule == null) sb.append(indent()).append("null\n");
					else sb.append(rule.toString());
					toStringIndent--;
					sb.append(indent()).append("</NOT>\n");
					computedToString = sb.toString();
				}
			}
			return computedToString;
		}
	}
}
