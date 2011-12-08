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

package org.jppf.server.nio;

import org.slf4j.*;

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
	private static Logger log = LoggerFactory.getLogger(StateTransitionTask.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether TRACE logging level is enabled.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * The channel whose state is changing.
	 */
	private ChannelWrapper<?> channel = null;
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
	 * @param channel the channel whose state is changing.
	 * @param factory the factory for the server that runs this task.
	 */
	public StateTransitionTask(ChannelWrapper<?> channel, NioServerFactory<S, T> factory)
	{
		this.channel = channel;
		this.factory = factory;
	}

	/**
	 * Perform the state transition.
	 * @see java.lang.Runnable#run()
	 */
	@SuppressWarnings("unchecked")
	public void run()
	{
		synchronized(channel)
		{
			StateTransitionManager<S, T> transitionManager = factory.getServer().getTransitionManager();
			this.ctx = (NioContext<S>) channel.getContext();
			if (traceEnabled) log.trace("performing transition to state " + ctx.getState() + " for " + channel);
			try
			{
				NioState<T> state = factory.getState(ctx.getState());
				transitionManager.transitionChannel(channel, state.performTransition(channel));
			}
			catch(Exception e)
			{
			  String msg = "error on channel " + channel + " : " + e.getClass().getName() + ": ";
				if (debugEnabled) log.debug(msg + e.getMessage(), e);
				else log.warn(msg + e.getMessage());
				ctx.handleException(channel);
			}
		}
	}
}
