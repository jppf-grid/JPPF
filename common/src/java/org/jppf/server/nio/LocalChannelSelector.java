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

/**
 * 
 * @author Laurent Cohen
 */
public class LocalChannelSelector implements ChannelSelector
{
	/**
	 * The channel polled by this selector.
	 */
	private ChannelWrapper<?> channel = null;
	/**
	 * The server that handles the channel.
	 */
	private NioServer server = null;

	/**
	 * Initialize this selector with the specified channel.
	 * @param channel the channel polled by this selector.
	 * @param server the server that handles the channel.
	 */
	public LocalChannelSelector(ChannelWrapper<?> channel, NioServer server)
	{
		this.channel = channel;
		this.server = server;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean select()
	{
		return select(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean select(long timeout)
	{
		if (timeout < 0L) throw new IllegalArgumentException("timeout must be >= 0");
		long start = System.currentTimeMillis();
		long elapsed = 0;
		while (((timeout == 0L) || (elapsed < timeout)) && ((channel.getKeyOps() & channel.getReadyOps()) == 0))
		{
			synchronized(this)
			{
				try
				{
					if (timeout == 0) wait();
					else wait(timeout - elapsed);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			elapsed = System.currentTimeMillis() - start;
		}
		boolean b = (channel.getKeyOps() & channel.getReadyOps()) != 0;
		if (b)
		{
			server.getTransitionManager().submitTransition(channel);
			server.postSelect();
		}
		return b;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean selectNow()
	{
		return (channel.getKeyOps() & channel.getReadyOps()) != 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void wakeup()
	{
		notifyAll();
	}

	/**
	 * {@inheritDoc}
	 */
	public ChannelWrapper<?> getChannel()
	{
		return channel;
	}
}
