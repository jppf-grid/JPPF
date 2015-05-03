/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.nio;

/**
 * Instances of this class define the transition of one NIO state to another.
 * @param <S> the type of states this transition goes to.
 * @author Laurent Cohen
 */
public class NioTransition<S extends Enum>
{
  /**
   * The new state after the transition.
   */
  private S state = null;
  /**
   * The set of IO operations the corresponding channel is interested in after the transition.
   */
  private int interestOps = 0;

  /**
   * Default instantiation of this class is not permitted.
   */
  private NioTransition()
  {
  }

  /**
   * Create a new transition with the specified state and set of interests.
   * @param state the state after the transition.
   * @param interestOps the new set of interests after the transition.
   */
  public NioTransition(final S state, final int interestOps)
  {
    this.state = state;
    this.interestOps = interestOps;
  }

  /**
   * Get the set of IO operations the corresponding channel is interested in after the transition.
   * @return the set of interests as an int value.
   */
  public int getInterestOps()
  {
    return interestOps;
  }

  /**
   * Set the set of IO operations the corresponding channel is interested in after the transition.
   * @param interestOps the set of interests as an int value.
   */
  public void setInterestOps(final int interestOps)
  {
    this.interestOps = interestOps;
  }

  /**
   * Get the new state after the transition.
   * @return an <code>NioState</code> instance.
   */
  public S getState()
  {
    return state;
  }

  /**
   * Set the new state after the transition.
   * @param state an <code>NioState</code> instance.
   */
  public void setState(final S state)
  {
    this.state = state;
  }
}
