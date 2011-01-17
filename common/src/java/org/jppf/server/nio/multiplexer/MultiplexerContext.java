/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.server.nio.*;
import org.jppf.utils.SerializationUtils;
import org.slf4j.*;

/**
 * Context obect associated with a socket channel used by the multiplexer. 
 * @author Laurent Cohen
 */
public class MultiplexerContext extends SimpleNioContext<MultiplexerState>
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(MultiplexerContext.class);
	/**
	 * The request currently processed.
	 */
	private ChannelWrapper linkedKey = null;
	/**
	 * The application port to which the channel may be bound.
	 */
	private int boundPort = -1;
	/**
	 * The multiplexer port to which the channel may be bound.
	 */
	private int multiplexerPort = -1;
	/**
	 * Encapsulates the data sent or received.
	 */
	private MultiplexerMessage multiplexerMessage = null;
	/**
	 * Contains pending messages to send.
	 */
	private Queue<MultiplexerMessage> messageQueue = new ConcurrentLinkedQueue<MultiplexerMessage>(); 

	/**
	 * Default constructor.
	 */
	public MultiplexerContext()
	{
		int breakpoint = 1;
	}

	/**
	 * Read data from a channel.
	 * @param wrapper the channel to read the data from.
	 * @return true if all the data has been read, false otherwise.
	 * @throws Exception if an error occurs while reading the data.
	 */
	public boolean readMessage(ChannelWrapper<?> wrapper) throws Exception
	{
		if (multiplexerMessage == null) multiplexerMessage = new MultiplexerMessage();
		return multiplexerMessage.read(wrapper);
	}

	/**
	 * Write data to a channel.
	 * @param wrapper the channel to write the data to.
	 * @return true if all the data has been written, false otherwise.
	 * @throws Exception if an error occurs while writing the data.
	 */
	public boolean writeMessage(ChannelWrapper<?> wrapper) throws Exception
	{
		return multiplexerMessage.write(wrapper);
	}

	/**
	 * {@inheritDoc}
	 */
	public void handleException(ChannelWrapper<?> channel)
	{
		try
		{
			if (linkedKey != null)
			{
				try
				{
					linkedKey.close();
				}
				catch(Exception e)
				{
					log.error(e.getMessage(), e);
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
	 * @return a <code>ChannelWrapper</code> instance.
	 */
	public synchronized ChannelWrapper<?> getLinkedKey()
	{
		return linkedKey;
	}

	/**
	 * Set the request currently processed.
	 * @param key a <code>ChannelWrapper</code> instance. 
	 */
	public synchronized void setLinkedKey(ChannelWrapper<?> key)
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
	 * Get the outbound port number for this channel, sent as the initial message.
	 * @return the port number as an int, or -1 if it could not be read.
	 */
	public int readOutBoundPort()
	{
		if (multiplexerMessage == null) return -1;
		try
		{
			return SerializationUtils.readInt(multiplexerMessage.location.getInputStream());
		}
		catch (Exception e)
		{
			log.warn(e.getClass().getSimpleName() + " : " + e.getMessage());
			return -1;
		}
	}

	/**
	 * Get the message that encapsulates the data sent or received.
	 * @return a {@link MultiplexerMessage} instance.
	 */
	public MultiplexerMessage getMultiplexerMessage()
	{
		return multiplexerMessage;
	}

	/**
	 * Set the message that encapsulates the data sent or received.
	 * @param multiplexerMessage a {@link MultiplexerMessage} instance.
	 */
	public void setMultiplexerMessage(MultiplexerMessage multiplexerMessage)
	{
		this.multiplexerMessage = multiplexerMessage;
	}

	/**
	 * Add a message to the message queue.
	 * @param message the message to add.
	 */
	public void queueMessage(MultiplexerMessage message)
	{
		messageQueue.offer(message);
	}

	/**
	 * Get the next queued mmessage and remove it from the queue.
	 * @return a {@link MultiplexerMessage} instance or null if the queue is empty.
	 */
	public MultiplexerMessage nextQueuedMessage()
	{
		return messageQueue.isEmpty() ? null : messageQueue.poll();
	}

	/**
	 * Determine whether there is at least one queue message.
	 * @return true if there is a least one message in the queue, false otherwise.
	 */
	public boolean hasQueuedMessage()
	{
		return !messageQueue.isEmpty();
	}
}
