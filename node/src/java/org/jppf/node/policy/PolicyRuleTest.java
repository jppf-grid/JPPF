/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import org.apache.commons.logging.*;
import org.jppf.management.JPPFSystemInformation;

/**
 * Simple test of an execution policy.
 * @author Laurent Cohen
 */
public class PolicyRuleTest extends ExecutionPolicy
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(PolicyRuleTest.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * Determines whether this policy accepts the specified node.
	 * @param info system information for the node on which the tasks will run if accepted.
	 * @return true if the node is accepted, false otherwise.
	 * @see org.jppf.node.policy.ExecutionPolicy#accepts(org.jppf.management.JPPFSystemInformation)
	 */
	public boolean accepts(JPPFSystemInformation info)
	{
		boolean result = false;
		if (info != null)
		{
			String s = info.getJppf().getString("node.execution.policy");
			result = (s != null);
		}
		if (debugEnabled)
		{
			String s = info.getNetwork().getString("ipv4.addresses");
			log.debug("node [" + s + "] accepted = " + result);
		}
		return result;
	}
}
