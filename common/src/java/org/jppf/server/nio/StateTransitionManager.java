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

import java.nio.channels.*;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.*;
import org.jppf.utils.*;

/**
 * 
 * @param <S>
 * @param <T>
 * @author Laurent Cohen
 */
public class StateTransitionManager<S extends Enum<S>, T extends Enum<T>>
{
	/**
	 * Size of the pool of threads for the state transition executor.
	 * It is defined as the value of the configuration property 
	 * &quot;transition.thread.pool.size&quot;, with a default value of 1.
	 */
	private static final int THREAD_POOL_SIZE =
		JPPFConfiguration.getProperties().getInt("transition.thread.pool.size", 1);
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NioServer.class);
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
	 * The list of key ops to set. 
	 */
	private LinkedList<KeyOpsSetting> keyOpsList = new LinkedList<KeyOpsSetting>();
	/**
	 * The list of channel registrations to perform.
	 */
	private LinkedList<ChannelRegistration> registrationList = new LinkedList<ChannelRegistration>();
	/**
	 * A reentrant lock on the nio server.
	 */
	private Lock lock = null;

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
		this.lock = server.getLock();
		if (!sequential) executor = Executors.newFixedThreadPool(threadPoolSize(), new JPPFThreadFactory(server.getName()));
	}

	/**
	 * Submit the next state transition for a specified channel.
	 * @param key the selection key that references the channel.
	 */
	protected void submitTransition(SelectionKey key)
	{
		setKeyOps(key, 0);
		//key.interestOps(0);
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
	public void setKeyOps(SelectionKey key, int ops)
	{
		lock.lock();
		try
		{
			server.getSelector().wakeup();
			key.interestOps(ops);
		}
		finally
		{
			lock.unlock();
		}
		/*
		synchronized(keyOpsList)
		{
			keyOpsList.add(new KeyOpsSetting(key, ops));
		}
		*/
	}

	/**
	 * Transition the specified channel to the specified state.
	 * @param key the key holding the channel and associated context. 
	 * @param transition holds the new state of the channel and associated key ops.
	 */
	public void transitionChannel(SelectionKey key, T transition)
	{
		NioContext<S> context = (NioContext<S>) key.attachment();
		NioTransition<S> t = server.getFactory().getTransition(transition);
		context.setState(t.getState());
		setKeyOps(key, t.getInterestOps());
	}

	/**
	 * Register a channel not opened through this server.
	 * @param channel the channel to register.
	 * @param ops the operations the channel is initially interested in.
	 * @param context the context attached to the channel.
	 */
	public void registerChannel(SocketChannel channel, int ops, NioContext context)
	{
		registerChannel(channel, ops, context, null);
	}

	/**
	 * Register a channel not opened through this server.
	 * @param channel the channel to register.
	 * @param ops the operations the channel is initially interested in.
	 * @param context the context attached to the channel.
	 * @param action an action to perform upon registration of the channel.
	 */
	public void registerChannel(SocketChannel channel, int ops, NioContext context,
		ChannelRegistrationAction action)
	{
		SelectionKey key = null;
		try
		{
			server.getLock().lock();
			try
			{
				server.getSelector().wakeup();
				key = channel.register(server.getSelector(), ops, context);
				if (action != null)
				{
					action.key = key;
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
		/*
		synchronized(registrationList)
		{
			registrationList.add(new ChannelRegistration(channel, ops, context, action));
		}
		*/
	}

	/**
	 * Perfom all the previously registered key ops setting actions.
	 */
	public void performKeyOpsSettings()
	{
		synchronized(keyOpsList)
		{
			while (keyOpsList.peek() != null)
			{
				KeyOpsSetting setting = keyOpsList.poll();
				setting.key().interestOps(setting.ops());
			}
		}
	}

	/**
	 * Perfom all the previously registered key ops setting actions.
	 */
	public void performChannelRegistrations()
	{
		synchronized(keyOpsList)
		{
			while (registrationList.peek() != null)
			{
				ChannelRegistration reg = registrationList.poll();
				SelectionKey key = null;
				try
				{
					key = reg.channel.register(server.getSelector(), reg.ops, reg.context);
					if (reg.action != null)
					{
						reg.action.key = key;
						reg.action.run();
					}
				}
				catch (ClosedChannelException e)
				{
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Submit the specified task for execution.
	 * @param r the task to run.
	 */
	public void submit(Runnable r)
	{
		if (sequential) r.run();
		else executor.submit(r);
	}

	/**
	 * Get the size of the state transition's thread pool.
	 * @return the value of the constant {@link #THREAD_POOL_SIZE THREAD_POOL_SIZE}.
	 */
	protected int threadPoolSize()
	{
		return THREAD_POOL_SIZE;
	}

	/**
	 * Encapsulation of a (SelectionKey, interest ops) pair.
	 */
	private class KeyOpsSetting extends Pair<SelectionKey, Integer>
	{
		/**
		 * Initialize this KeyOpsSetting.
		 * @param key the selection key for which to set the interest ops.
		 * @param ops the interest ops to set.
		 */
		public KeyOpsSetting(SelectionKey key, Integer ops)
		{
			super(key, ops);
		}

		/**
		 * Get the selection key for which to set the interest ops.
		 * @return a <code>SelectionKey</code> instance.
		 */
		public SelectionKey key()
		{
			return first();
		}

		/**
		 * Get the interest ops to set.
		 * @return the ops as an int value.
		 */
		public int ops()
		{
			return second();
		}
	}

	/**
	 * Encapsulation of the information required to regster a socket channel.
	 */
	private class ChannelRegistration
	{
		/**
		 * The channel to register.
		 */
		public SocketChannel channel = null;
		/**
		 * The initial interest ops to set.
		 */
		public int ops = 0;
		/**
		 * The context associated with the channel, to set as attachment.
		 */
		public NioContext context = null;
		/**
		 * An action to perform upon registration of the channel.
		 */
		public ChannelRegistrationAction action = null;

		/**
		 * Initialise this channel registration witht he specified parameters.
		 * @param channel the channel to register.
		 * @param ops the initial interest ops to set.
		 * @param context the context associated with the channel, to set as attachment.
		 * @param action an action to perform upon registration of the channel.
		 */
		public ChannelRegistration(SocketChannel channel, int ops, NioContext context, ChannelRegistrationAction action)
		{
			this.channel =channel;
			this.ops = ops;
			this.context = context;
			this.action = action;
		}
	}

	/**
	 * Abstract super class for an action to perform upon registration of a channel.
	 */
	public abstract static class ChannelRegistrationAction implements Runnable
	{
		/**
		 * The key resulting form the channel registration.
		 */
		public SelectionKey key = null;
	}
}
