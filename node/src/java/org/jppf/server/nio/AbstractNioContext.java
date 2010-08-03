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
 * Context associated with an open communication channel.
 * @param <S> the type of states associated with this context.
 * @author Laurent Cohen
 */
public abstract class AbstractNioContext<S extends Enum<S>> implements NioContext<S>
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractNioContext.class);
	/**
	 * Determines whther DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The current state of the channel this context is associated with.
	 */
	protected S state = null;
	/**
	 * Uuid for this node context.
	 */
	protected String uuid = null;
	/**
	 * Container for the current message data.
	 */
	protected NioMessage message = null;
	/**
	 * Count of bytes read.
	 */
	public int readByteCount = -1;
	/**
	 * Count of bytes written.
	 */
	public int writeByteCount = -1;

	/**
	 * {@inheritDoc}
	 */
	public S getState()
	{
		return state;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setState(S state)
	{
		this.state = state;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * {@inheritDoc}
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
	 * Get the container for the current message data.
	 * @return an <code>NioMessage</code> instance.
	 */
	public NioMessage getMessage()
	{
		return message;
	}

	/**
	 * Set the container for the current message data.
	 * @param message an <code>NioMessage</code> instance.
	 */
	public void setMessage(NioMessage message)
	{
		this.message = message;
	}
}
