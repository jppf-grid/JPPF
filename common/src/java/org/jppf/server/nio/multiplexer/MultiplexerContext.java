/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.nio.multiplexer;

import java.nio.channels.*;

import org.jppf.server.nio.NioContext;

/**
 * Context obect associated with a socket channel used by the multiplexer. 
 * @author Laurent Cohen
 */
public class MultiplexerContext extends NioContext<MultiplexerState>
{
	/**
	 * The request currently processed.
	 */
	private SelectionKey linkedKey = null;
	/**
	 * Port on which the connection was initially established.
	 */
	private int boundPort = -1;
	/**
	 * Port on which the connection was initially established.
	 */
	private int multiplexerPort = -1;

	/**
	 * Handle the cleanup when an exception occurs on the channel.
	 * @param channel the channel that threw the exception.
	 * @see org.jppf.server.nio.NioContext#handleException(java.nio.channels.SocketChannel)
	 */
	public void handleException(SocketChannel channel)
	{
		try
		{
			channel.close();
		}
		catch(Exception ignored)
		{
			LOG.error(ignored.getMessage(), ignored);
		}
	}

	/**
	 * Get the request currently processed.
	 * @return a <code>SelectionKey</code> instance.
	 */
	public synchronized SelectionKey getLinkedKey()
	{
		return linkedKey;
	}

	/**
	 * Set the request currently processed.
	 * @param key a <code>SelectionKey</code> instance. 
	 */
	public synchronized void setLinkedKey(SelectionKey key)
	{
		this.linkedKey = key;
	}
}
