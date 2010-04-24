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

import org.apache.commons.logging.*;

/**
 * Context associated with an open socket channel.
 * @param <S> the type of states associated with this context.
 * @author Laurent Cohen
 */
public abstract class NioContext<S extends Enum>
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(NioContext.class);
	/**
	 * Determines whther DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The current state of the channel this context is associated with.
	 */
	protected S state = null;
	/**
	 * Uuid for this node context.
	 */
	protected String uuid = null;

	/**
	 * Get the current state of the channel this context is associated with.
	 * @return a state enum value.
	 */
	public S getState()
	{
		return state;
	}

	/**
	 * Set the current state of the channel this context is associated with.
	 * @param state a state enum value.
	 */
	public void setState(S state)
	{
		this.state = state;
	}

	/**
	 * Read data from a channel.
	 * @param wrapper the channel to read the data from.
	 * @return true if all the data has been read, false otherwise.
	 * @throws Exception if an error occurs while reading the data.
	 */
	public abstract boolean readMessage(ChannelWrapper<?> wrapper) throws Exception;

	/**
	 * Write data to a channel.
	 * @param wrapper the channel to write the data to.
	 * @return true if all the data has been written, false otherwise.
	 * @throws Exception if an error occurs while writing the data.
	 */
	public abstract boolean writeMessage(ChannelWrapper<?> wrapper) throws Exception;

	/**
	 * Get the uuid for this node context.
	 * @return the uuid as a string.
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * Set the uuid for this node context.
	 * @param uuid the uuid as a string.
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * Give the non qualified name of the class of this instance.
	 * @return a class name as a string.
	 */
	protected String getShortClassName()
	{
		String fqn = getClass().getName();
		int idx = fqn.lastIndexOf(".");
		return fqn.substring(idx + 1);
	}

	/**
	 * Handle the cleanup when an exception occurs on the channel.
	 * @param channel the channel that threw the exception.
	 */
	public abstract void handleException(ChannelWrapper channel);
}
