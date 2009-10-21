/*
 * JPPF.
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

package sample.dist.matrix;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.CustomPolicy;

/**
 * 
 * @author Laurent Cohen
 */
public class MyCustomPolicy extends CustomPolicy
{
	/**
	 * Determines whether this policy accepts the specified node.
	 * @param info system information for the node on which the tasks will run if accepted.
	 * @return true if the node is accepted, false otherwise.
	 * @see org.jppf.node.policy.ExecutionPolicy#accepts(org.jppf.management.JPPFSystemInformation)
	 */
	public boolean accepts(JPPFSystemInformation info)
	{
		String s = getProperty(info, "processing.thread");
		int n = 1;
		try
		{
			n = Integer.valueOf(s);
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		return n > 1;
	}
}
