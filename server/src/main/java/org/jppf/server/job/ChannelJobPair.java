/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.execute.ExecutorChannel;
import org.jppf.server.protocol.AbstractServerJob;
import org.jppf.utils.Pair;

/**
 * Instances of this class associate a node channel with a job that is being executed on the corresponding node.
 * @author Laurent Cohen
 */
public class ChannelJobPair extends Pair<ExecutorChannel<?>, AbstractServerJob>
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this object with the specified parameters.
   * @param first - the first object of this pair.
   * @param second - the second object of this pair.
   */
  public ChannelJobPair(final ExecutorChannel<?> first, final AbstractServerJob second)
  {
    super(first, second);
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * @param obj - the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the obj.
   * @see Object#equals(Object)
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (obj == null) return false;
    if (!obj.getClass().equals(this.getClass())) return false;
    final ChannelJobPair pair = (ChannelJobPair) obj;
    if (first() == null) return pair.first() == null;
    if (pair.first() == first()) return true;
    return first().equals(pair.first());
  }
}
