/*
 * Java Parallel Processing Framework.
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

package org.jppf.server.nio;

import static org.jppf.utils.StringUtils.getRemoteHost;

import java.nio.channels.*;

import org.apache.commons.logging.*;

/**
 * Instances of this class perform the transition of a channel from one state to another.
 * They extend the Runnable interface so they can be executed concurrently by a pool of threads.
 * @param <S> the type of the states to use.
 * @param <T> the type of the transitions to use.
 * @author Laurent Cohen
 */
public class StateTransitionTask<S extends Enum<S>, T extends Enum<T>> implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(StateTransitionTask.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
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
	private NioServerFactory<S, T> factory = null;

	/**
	 * Initialize this task with the specified key and factory.
	 * @param key the selection key corresponding to the channel whose state is changing.
	 * @param factory the factory for the server that runs this task.
	 */
	public StateTransitionTask(SelectionKey key, NioServerFactory<S, T> factory)
	{
		this.key = key;
		this.factory = factory;
	}

	/**
	 * Perform the state transition.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			this.ctx = (NioContext<S>) key.attachment();
			if (debugEnabled)
			{
				synchronized(factory)
				{
					log.debug(getRemoteHost(key.channel()) + " transition from " + ctx.getState());
				}
			}
			NioState<T> state = factory.getState(ctx.getState());
			NioTransition<S> transition = factory.getTransition(state.performTransition(key));
			ctx.setState(transition.getState());
			factory.getServer().getTransitionManager().setKeyOps(key, transition.getInterestOps());
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.warn(e);
			ctx.handleException((SocketChannel) key.channel());
		}
	}
}
