/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import java.nio.channels.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.*;
import org.jppf.utils.*;

/**
 * Instances of this class manage the state transitions of channels opened via a <code>NioServer</code>.
 * @param <S> type safe enum of the possible states for a channel.
 * @param <T> type safe enum of the possible state transitions for a channel.
 * @author Laurent Cohen
 */
public class StateTransitionManager<S extends Enum<S>, T extends Enum<T>>
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(StateTransitionManager.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The pool of threads used for submitting channel state transitions.
	 */
	protected ExecutorService executor = null;
	/**
	 * The server for which this transition manager is intended.
	 */
	private NioServer<S, T> server = null;
	/**
	 * Determines whether the submission of state transitions should be
	 * performed sequentially or through the executor thread pool.
	 */
	private boolean sequential = false;

	/**
	 * Initialize this transition manager with the specified server and sequential flag.
	 * @param server the server for which this transition manager is intended.
	 * @param sequential determines whether the submission of state transitions should be
	 * performed sequentially or through the executor thread pool.
	 */
	public StateTransitionManager(NioServer<S, T> server, boolean sequential)
	{
		this.server = server;
		this.sequential = sequential;
		int size = JPPFConfiguration.getProperties().getInt("transition.thread.pool.size", -1);
		if (size <= 0) size = Runtime.getRuntime().availableProcessors();
		if (!sequential) executor = Executors.newFixedThreadPool(size, new JPPFThreadFactory(server.getName()));
	}

	/**
	 * Submit the next state transition for a specified channel.
	 * @param key the selection key that references the channel.
	 */
	protected void submitTransition(ChannelWrapper key)
	{
		setKeyOps(key, 0);
		StateTransitionTask<S, T> transition = new StateTransitionTask<S, T>(key, server.getFactory());
		if (sequential) transition.run();
		else executor.submit(transition);
	}

	/**
	 * Set the interest ops of a specified selection key, ensuring no blocking occurs while doing so.
	 * This method is proposed as a convenience, to encapsulate the inner locking mechanism. 
	 * @param key the key on which to set the interest operations.
	 * @param ops the operations to set on the key.
	 */
	private void setKeyOps(ChannelWrapper key, int ops)
	{
		Lock lock = server.getLock();
		lock.lock();
		try
		{
			server.getSelector().wakeup();
			key.setKeyOps(ops);
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Transition the specified channel to the specified state.
	 * @param key the key holding the channel and associated context. 
	 * @param transition holds the new state of the channel and associated key ops.
	 */
	public void transitionChannel(ChannelWrapper key, T transition)
	{
		Lock lock = server.getLock();
		lock.lock();
		try
		{
			server.getSelector().wakeup();
			NioContext<S> context = (NioContext<S>) key.getContext();
			S s1 = context.getState();
			NioTransition<S> t = server.getFactory().getTransition(transition);
			S s2 = t.getState();
			context.setState(s2);
			key.setKeyOps(t.getInterestOps());
			if (debugEnabled) log.debug("transitionned " + key + " from " + s1 + " to " + s2);
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Register a channel not opened through this server.
	 * @param channel the channel to register.
	 * @param ops the operations the channel is initially interested in.
	 * @param context the context attached to the channel.
	 * @param action an action to perform upon registration of the channel.
	 * @return a {@link ChannelWrapper} instance.
	 */
	public ChannelWrapper registerChannel(SocketChannel channel, int ops, NioContext context,	ChannelRegistrationAction action)
	{
		ChannelWrapper wrapper = null;
		SelectionKey key = null;
		try
		{
			server.getLock().lock();
			try
			{
				server.getSelector().wakeup();
				key = channel.register(server.getSelector(), ops, context);
				wrapper = new SelectionKeyWrapper(key);
				if (action != null)
				{
					action.key = wrapper;
					action.run();
				}
			}
			finally
			{
				server.getLock().unlock();
			}
		}
		catch (ClosedChannelException e)
		{
			log.error(e.getMessage(), e);
		}
		return wrapper;
	}

	/**
	 * Submit the specified task for execution.
	 * @param r the task to run.
	 */
	public void submit(Runnable r)
	{
		/*
		if (sequential) r.run();
		else executor.submit(r);
		*/
		r.run();
	}

	/**
	 * Abstract super class for an action to perform upon registration of a channel.
	 */
	public abstract static class ChannelRegistrationAction implements Runnable
	{
		/**
		 * The key resulting form the channel registration.
		 */
		public ChannelWrapper key = null;
	}

	/**
	 * Determine whether the submission of state transitions should be performed sequentially.
	 * @return true if state transitions are sequential or false if they are in parallel.
	 */
	public boolean isSequential()
	{
		return sequential;
	}
}
