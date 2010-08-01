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

import org.jppf.utils.StringUtils;

/**
 * Channel wrapper implementation for a {@link SelectionKey}.
 * @author Laurent Cohen
 */
public class SelectionKeyWrapper extends AbstractChannelWrapper<SelectionKey>
{
	/**
	 * Initialize this channel wrapper with the specified channel.
	 * @param channel the channel to wrap.
	 */
	public SelectionKeyWrapper(SelectionKey channel)
	{
		super(channel);
	}

	/**
	 * Get the {@link AbstractNioContext} attached to the channel.
	 * @return a {@link AbstractNioContext} instance.
	 * @see org.jppf.server.nio.AbstractChannelWrapper#getContext()
	 */
	public NioContext getContext()
	{
		return (NioContext) getChannel().attachment();
	}

	/**
	 * Close the channel.
	 * @throws Exception if any error occurs while closing the channel.
	 */
	public void close() throws Exception
	{
		getChannel().channel().close();
	}

	/**
	 * Determine whether the channel is opened.
	 * @return true if the channel is opened, false otherwise.
	 */
	public boolean isOpen()
	{
		return getChannel().channel().isOpen();
	}

	/**
	 * Generate a string that represents this channel wrapper.
	 * @return a string that represents this channel wrapper.
	 * @see java.lang.Object#toString()
	 */
	/*
	public String toString()
	{
		SelectableChannel ch = ((SelectionKey) getChannel()).channel();
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(StringUtils.getRemoteHost(ch));
		sb.append(",").append(" readyOps=").append(getReadyOps());
		sb.append(",").append(" keyOps=").append(getKeyOps());
		sb.append(",").append(" state=").append(getContext().getState());
		sb.append("]");
		return sb.toString();
	}
	*/

	/**
	 * {@inheritDoc}
	 */
	public String getId()
	{
		return StringUtils.getRemoteHost(((SelectionKey) getChannel()).channel());
	}

	/**
	 * Get the operations enabled for this channel.
	 * @return the operations as an int value.
	 * @see org.jppf.server.nio.AbstractChannelWrapper#getKeyOps()
	 */
	public int getKeyOps()
	{
		return getChannel().interestOps();
	}

	/**
	 * Get the operations enabled for this channel.
	 * @param keyOps the operations as an int value.
	 * @see org.jppf.server.nio.AbstractChannelWrapper#setKeyOps(int)
	 */
	public void setKeyOps(int keyOps)
	{
		SelectionKey key = (SelectionKey) channel;
		key.selector().wakeup();
		channel.interestOps(keyOps);
	}

	/**
	 * Get the operations available for this channel.
	 * @return the operations as an int value.
	 * @see org.jppf.server.nio.AbstractChannelWrapper#getReadyOps()
	 */
	public int getReadyOps()
	{
		return getChannel().readyOps();
	}
}
