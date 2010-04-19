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

import java.nio.channels.SelectionKey;

/**
 * Wraps a communication channel, no matter what the channel is.
 * @param <S> the type of wrapped channel.
 * @author Laurent Cohen
 */
public abstract class ChannelWrapper<S>
{
	/**
	 * The channel to wrap.
	 */
	protected S channel;
	
	/**
	 * Initialize this channel wrapper with the specified channel.
	 * @param channel the channel to wrap.
	 */
	public ChannelWrapper(S channel)
	{
		this.channel = channel;
	}

	/**
	 * Get the channel to wrap.
	 * @return the wrapped channel.
	 */
	public S getChannel()
	{
		return channel;
	}

	/**
	 * Close the channel.
	 * @throws Exception if any error occurs while closing the channel.
	 */
	public void close() throws Exception
	{
	}

	/**
	 * Get the {@link NioContext} attached to the channel.
	 * @return a {@link NioContext} instance.
	 */
	public abstract NioContext getContext();

	/**
	 * Determine whether the channel is opened.
	 * @return true if the channel is opened, false otherwise.
	 */
	public boolean isOpen()
	{
		return true;
	}

	/**
	 * Get the hashcode for this object.
	 * @return the hashcode of the wrapped channel.
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return ((channel == null) ? 0 : channel.hashCode());
	}

	/**
	 * Determine whether an other object is equal to this one.
	 * @param obj the object to compare with.
	 * @return true if this object is equal to the other one, false otherwise.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ChannelWrapper other = (ChannelWrapper) obj;
		if (channel == null) return (other.channel == null);
		return channel.equals(other.channel);
	}

	/**
	 * Generate a string that represents this channel wrapper.
	 * @return a string that represents this channel wrapper.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "" + channel;
	}

	/**
	 * Get the operations enabled for this channel.
	 * @return the operations as an int value.
	 */
	public int getKeyOps()
	{
		return 0;
	}

	/**
	 * Get the operations enabled for this channel.
	 * @param keyOps the operations as an int value.
	 */
	public void setKeyOps(int keyOps)
	{
	}

	/**
	 * Get the operations available for this channel.
	 * @return the operations as an int value.
	 */
	protected int readyOps()
	{
		return 0;
	}

	/**
	 * Determine whether the channel can be read from.
	 * @return true if the channel can be read, false otherwise.
	 */
	public boolean isReadable()
	{
		return (readyOps() & SelectionKey.OP_READ) > 0;
	}

	/**
	 * Determine whether the channel can be written to.
	 * @return true if the channel can be written to, false otherwise.
	 */
	public boolean isWritable()
	{
		return (readyOps() & SelectionKey.OP_WRITE) > 0;
	}

	/**
	 * Determine whether the channel can accept connections.
	 * @return true if the channel can accept connections, false otherwise.
	 */
	public boolean isAcceptable()
	{
		return (readyOps() & SelectionKey.OP_ACCEPT) > 0;
	}

	/**
	 * Determine whether the channel can be connected.
	 * @return true if the channel can be connected, false otherwise.
	 */
	public boolean isConnectable()
	{
		return (readyOps() & SelectionKey.OP_CONNECT) > 0;
	}
}
