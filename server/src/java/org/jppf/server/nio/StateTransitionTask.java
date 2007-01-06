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

import java.nio.channels.*;

import org.apache.log4j.Logger;

/**
 * Instances of this class perform the transition of a channel from one state to another.
 * They extend the Runnable interface so they can be executed concurrently by a pool of threads.
 * @param <S> the type of the states to use.
 * @param <T> the type of the transitions to use.
 * @param <U> the type of server running this task.
 * @author Laurent Cohen
 */
public class StateTransitionTask<S extends Enum<S>, T extends Enum<T>, U extends NioServer> implements Runnable
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(StateTransitionTask.class);
	/**
	 * The selection key corresponding to the channel whose state is changing.
	 */
	private SelectionKey key = null;
	/**
	 * The context attached to the key.
	 */
	private NioContext<S> ctx = null;
	/**
	 * The factory ofr the server that runs this task.
	 */
	private NioServerFactory<S, T, U> factory = null;

	/**
	 * Initialize this task with the specified key and factory.
	 * @param key the selection key corresponding to the channel whose state is changing.
	 * @param factory the factory for the server that runs this task.
	 */
	@SuppressWarnings("unchecked")
	public StateTransitionTask(SelectionKey key, NioServerFactory<S, T, U> factory)
	{
		this.key = key;
		this.factory = factory;
		ctx = (NioContext<S>) key.attachment();
	}

	/**
	 * Perform the state transition.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			NioState<T> state = factory.getState(ctx.getState());
			NioTransition<S> transition = factory.getTransition(state.performTransition(key));
			ctx.setState(transition.getState());
			key.interestOps(transition.getInterestOps());
			key.selector().wakeup();
		}
		catch(Exception e)
		{
			ctx.handleException((SocketChannel) key.channel());
			log.error(e.getMessage(), e);
		}
	}
}
