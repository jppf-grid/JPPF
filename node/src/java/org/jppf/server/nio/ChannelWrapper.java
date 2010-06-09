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
 * @param <S>
 * @author Laurent Cohen
 */
public interface ChannelWrapper<S>
{

	/**
	 * Get the channel to wrap.
	 * @return the wrapped channel.
	 */
	S getChannel();

	/**
	 * Close the channel.
	 * @throws Exception if any error occurs while closing the channel.
	 */
	void close() throws Exception;

	/**
	 * Get the {@link AbstractNioContext} attached to the channel.
	 * @return a {@link AbstractNioContext} instance.
	 */
	NioContext<?> getContext();

	/**
	 * Determine whether the channel is opened.
	 * @return true if the channel is opened, false otherwise.
	 */
	boolean isOpen();

	/**
	 * Get the operations enabled for this channel.
	 * @return the operations as an int value.
	 */
	int getKeyOps();

	/**
	 * Get the operations enabled for this channel.
	 * @param keyOps the operations as an int value.
	 */
	void setKeyOps(int keyOps);

	/**
	 * Get the operations available for this channel.
	 * @return the operations as an int value.
	 */
	int getReadyOps();

	/**
	 * Determine whether the channel can be read from.
	 * @return true if the channel can be read, false otherwise.
	 */
	boolean isReadable();

	/**
	 * Determine whether the channel can be written to.
	 * @return true if the channel can be written to, false otherwise.
	 */
	boolean isWritable();

	/**
	 * Determine whether the channel can accept connections.
	 * @return true if the channel can accept connections, false otherwise.
	 */
	boolean isAcceptable();

	/**
	 * Determine whether the channel can be connected.
	 * @return true if the channel can be connected, false otherwise.
	 */
	boolean isConnectable();

	/**
	 * Get the selector associated with this channel.
	 * @return a {@link LocalChannelSelector} instance.
	 */
	ChannelSelector getSelector();

	/**
	 * Set the selector associated with this channel.
	 * @param selector a {@link LocalChannelSelector} instance.
	 */
	void setSelector(ChannelSelector selector);
}