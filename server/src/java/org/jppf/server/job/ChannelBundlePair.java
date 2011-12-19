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

package org.jppf.server.job;

import org.jppf.server.nio.*;
import org.jppf.server.protocol.BundleWrapper;
import org.jppf.utils.Pair;

/**
 * Instances of this class associate a node channel with a job that is being executed on the corresponding node.
 * @author Laurent Cohen
 */
public class ChannelBundlePair extends Pair<ChannelWrapper<?>, BundleWrapper>
{
	/**
	 * Initialize this object with the specified parameters.
	 * @param first - the first object of this pair.
	 * @param second - the second object of this pair.
	 */
	public ChannelBundlePair(ChannelWrapper<?> first, BundleWrapper second)
	{
		super(first, second);
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 * @param obj - the reference object with which to compare.
	 * @return <code>true</code> if this object is the same as the obj.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (!obj.getClass().equals(this.getClass())) return false;
		ChannelBundlePair pair = (ChannelBundlePair) obj;
		if (first() == null) return pair.first() == null;
		if (pair.first() == first()) return true;
		return first().equals(pair.first());
	}
}
