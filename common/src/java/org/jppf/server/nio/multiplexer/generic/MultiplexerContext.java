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

package org.jppf.server.nio.multiplexer.generic;

import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.server.nio.*;
import org.jppf.utils.StringUtils;
import org.jppf.utils.streams.StreamConstants;
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
	private static Logger log = LoggerFactory.getLogger(ReceivingState.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
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
	 * A list of messages waiting to be sent.
	 */
	private Queue<ByteBufferWrapper> pendingMessages = new ConcurrentLinkedQueue<ByteBufferWrapper>();
	/**
	 * The message currently being sent.
	 */
	private ByteBuffer currentMessage = null;
	/**
	 * Determines whether end of stream was reached during the last read operation.
	 */
	public boolean eof = false;
	/**
	 * 
	 */
	private int readMessageCount = 0; 

	/**
	 * {@inheritDoc}
	 */
	public void handleException(ChannelWrapper channel)
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
	public synchronized ChannelWrapper getLinkedKey()
	{
		return linkedKey;
	}

	/**
	 * Set the request currently processed.
	 * @param key a <code>ChannelWrapper</code> instance. 
	 */
	public synchronized void setLinkedKey(ChannelWrapper key)
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

	/**
	 * Read data from a channel.
	 * @param wrapper the channel to read the data from.
	 * @return a ByteBuffer containing the data read from the channel, or null if no data was read.
	 * @throws Exception if an error occurs while reading the data.
	 */
	public ByteBuffer readMultiplexerMessage(ChannelWrapper<?> wrapper) throws Exception
	{
		ByteBuffer msg = ByteBuffer.wrap(new byte[StreamConstants.TEMP_BUFFER_SIZE]);
		int count = 0;
		int n = 0;
		ReadableByteChannel channel = (ReadableByteChannel) ((SelectionKeyWrapper) wrapper).getChannel();
		do
		{
			count = channel.read(msg);
			n += count;
		}
		while ((count > 0) && msg.hasRemaining());
		if (debugEnabled)
		{
			log.debug("[" + getShortClassName() + "] " + "read " + n + " bytes from " +
				StringUtils.getRemoteHost((SocketChannel) channel));
		}
		if (count < 0) setEof(true);
		if (msg.position() > 0)
		{
			msg.flip();
			return msg;
		}
		return null;
	}

	/**
	 * Write the current message to a channel.
	 * @param wrapper the channel to write the data to.
	 * @return true if the current message was completely written, false otherwise.
	 * @throws Exception if an error occurs while reading the data.
	 */
	public boolean writeMultiplexerMessage(ChannelWrapper<?> wrapper) throws Exception
	{
		ByteBuffer msg = getCurrentMessage();
		int count = 0;
		WritableByteChannel channel = (WritableByteChannel) ((SelectionKeyWrapper) wrapper).getChannel();
		do
		{
			count = channel.write(msg);
		}
		while ((count > 0) && msg.hasRemaining());
		if (debugEnabled)
		{
			log.debug("[" + getShortClassName() + "] " + "written " + count + " bytes to " +
				StringUtils.getRemoteHost((SocketChannel) channel));
		}
		return !msg.hasRemaining();
	}

	/**
	 * Add a message to the list of pending messages.
	 * @param message the message to add to the list.
	 */
	public synchronized void addPendingMessage(ByteBufferWrapper message)
	{
		pendingMessages.add(message);
	}

	/**
	 * Retrieve, and remove from the list, the next pending message.
	 * @return the next message, or null if there is no message.
	 */
	public synchronized ByteBufferWrapper nextPendingMessage()
	{
		return pendingMessages.poll();
	}

	/**
	 * Determine if this context has any pending message waiting to be sent.
	 * @return true if there is at least one pending message, false otherwise.
	 */
	public synchronized boolean hasPendingMessage()
	{
		return pendingMessages.peek() != null;
	}

	/**
	 * Get the message currently being sent.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	public synchronized ByteBuffer getCurrentMessage()
	{
		return currentMessage;
	}

	/**
	 * Set the message currently being sent.
	 * @param message a <code>ByteBuffer</code> instance.
	 */
	public synchronized void setCurrentMessage(ByteBuffer message)
	{
		currentMessage = message;
	}

	/**
	 * Determine whether end of stream was reached during the last read operation.
	 * @return true if EOF was reached, false otherwise.
	 */
	public boolean isEof()
	{
		return eof;
	}

	/**
	 * Specifiy whether end of stream was reached during the last read operation.
	 * @param eof true to specify that EOF was reached, false otherwise.
	 */
	public void setEof(boolean eof)
	{
		this.eof = eof;
	}

	/**
	 * Generate a new read meessage count value.
	 * @return the read message count incremented by one.
	 */
	public synchronized int newReadMessageCount()
	{
		return ++readMessageCount;
	}
}
