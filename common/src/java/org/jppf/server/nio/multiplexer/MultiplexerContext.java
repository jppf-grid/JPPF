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
	 * The application port to which the channel may be bound.
	 */
	private int boundPort = -1;
	/**
	 * The multiplexer port to which the channel may be bound.
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
			if (linkedKey != null)
			{
				if (linkedKey.channel() != null)
				{
					try
					{
						linkedKey.channel().close();
					}
					catch(Exception e)
					{
						log.error(e.getMessage(), e);
					}
				}
			}
			channel.close();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
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

	/**
	 * Get the application port to which the channel may be bound.
	 * @return the port as an int value, or a negative value if the channel is not bound to an application port.
	 */
	public int getBoundPort()
	{
		return boundPort;
	}

	/**
	 * Set the application port to which the channel may be bound.
	 * @param boundPort the port as an int value, or a negative value if the channel is not bound to an application port.
	 */
	public void setBoundPort(int boundPort)
	{
		this.boundPort = boundPort;
	}

	/**
	 * Get the multiplexer port to which the channel may be bound.
	 * @return the port as an int value, or a negative value if the channel is not bound to a multiplexer port.
	 */
	public int getMultiplexerPort()
	{
		return multiplexerPort;
	}

	/**
	 * Set the multiplexer port to which the channel may be bound.
	 * @param multiplexerPort the port as an int value, or a negative value if the channel is not bound to a multiplexer port.
	 */
	public void setMultiplexerPort(int multiplexerPort)
	{
		this.multiplexerPort = multiplexerPort;
	}

	/**
	 * Determine whether the associated channel is connected to an application port.
	 * @return true if the channel is bound to an application port, false otherwise.
	 */
	public boolean isApplicationPort()
	{
		return boundPort > 0;
	}

	/**
	 * Determine whether the associated channel is connected to a multiplexer port.
	 * @return true if the channel is bound to a multiplexer port, false otherwise.
	 */
	public boolean isMultiplexerPort()
	{
		return multiplexerPort > 0;
	}

	/**
	 * Get the port outbound port number for this channel, sent as the initial message.
	 * @return the port number as an int, or -1 if it could not be read.
	 */
	public int readOutBoundPort()
	{
		if (message == null) return -1;
		message.buffer.flip();
		return message.buffer.getInt();
	}
}
