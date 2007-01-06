/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jppf.server.nio;

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
	 * The set of IO operations the corresponding channel isinterested in after the transition.
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
	public NioTransition(S state, int interestOps)
	{
		this.state = state;
		this.interestOps = interestOps;
	}

	/**
	 * Get the set of IO operations the corresponding channel isinterested in after the transition.
	 * @return the set of interests as an int value.
	 */
	public int getInterestOps()
	{
		return interestOps;
	}

	/**
	 * Set the set of IO operations the corresponding channel isinterested in after the transition.
	 * @param interestOps the set of interests as an int value.
	 */
	public void setInterestOps(int interestOps)
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
	public void setState(S state)
	{
		this.state = state;
	}
}
