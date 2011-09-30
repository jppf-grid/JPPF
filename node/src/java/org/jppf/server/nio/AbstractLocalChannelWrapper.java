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

package org.jppf.server.nio;

import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.utils.SimpleObjectLock;
import org.slf4j.*;

/**
 * Channel wrapper and I/O implementation for the class loader of an in-VM node.
 * @param <S> The type of message handled by this channel wrapper.
 * @param <T> The type of context used by the channel on the server side of the communication.
 * @author Laurent Cohen
 */
public class AbstractLocalChannelWrapper<S, T extends AbstractNioContext> extends AbstractChannelWrapper<T>
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(AbstractLocalChannelWrapper.class);
	/**
	 * Determines whether trace-level logging is enabled.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * This channel's key ops.
	 */
	protected AtomicInteger keyOps = new AtomicInteger(0);
	/**
	 * This channel's ready ops.
	 */
	protected AtomicInteger readyOps = new AtomicInteger(0);
	/**
	 * The resource passed to the node.
	 */
	protected S nodeResource = null;
	/**
	 * The resource passed to the server.
	 */
	protected S serverResource = null;
	/**
	 * Object used to synchronize threads when reading/writing the node message.
	 */
	protected final SimpleObjectLock nodeLock = new SimpleObjectLock();
	/**
	 * Object used to synchronize threads when reading/writing the server message.
	 */
	protected final SimpleObjectLock serverLock = new SimpleObjectLock();

	/**
	 * Initialize this I/O handler with the specified context.
	 * @param context the context used as communication channel.
	 */
	public AbstractLocalChannelWrapper(T context)
	{
		super(context);
		if (traceEnabled) log.trace("created " + this); 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public NioContext getContext()
	{
		return getChannel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int getKeyOps()
	{
		return keyOps.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void setKeyOps(int keyOps)
	{
		this.keyOps.set(keyOps);
		if (traceEnabled) log.debug("id=" + id + ", readyOps=" + readyOps + ", keyOps=" + keyOps);
		if (getSelector() != null) getSelector().wakeUp();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int getReadyOps()
	{
		return readyOps.get();
	}

	/**
	 * Set the operations for which this channel is ready.
	 * @param readyOps the bitwise operations as an int value.
	 */
	public void setReadyOps(int readyOps)
	{
		this.readyOps.set(readyOps);
		if (traceEnabled) log.debug("id=" + id + ", readyOps=" + readyOps + ", keyOps=" + keyOps);
		if (getSelector() != null) getSelector().wakeUp();
	}

	/**
	 * Get the resource passed to the node.
	 * @return an instance of the resource type used by this channel.
	 */
	public S getNodeResource()
	{
		synchronized(nodeLock)
		{
			return nodeResource;
		}
	}

	/**
	 * Set the resource passed to the node.
	 * @param resource an instance of the resource type used by this channel.
	 */
	public void setNodeResource(S resource)
	{
		synchronized(nodeLock)
		{
			this.nodeResource = resource;
		}
		nodeLock.wakeUp();
	}

	/**
	 * Get the resource passed to the server.
	 * @return an instance of the resource type used by this channel.
	 */
	public S getServerResource()
	{
		synchronized(serverLock)
		{
			return serverResource;
		}
	}

	/**
	 * Set the resource passed to the server.
	 * @param serverResource an instance of the resource type used by this channel.
	 */
	public void setServerResource(S serverResource)
	{
		synchronized(serverLock)
		{
			this.serverResource = serverResource;
		}
		serverLock.wakeUp();
	}


	/**
	 * Get the object used to synchronize threads when reading/writing the node resource.
	 * @return a {@link SimpleObjectLock} instance.
	 */
	public SimpleObjectLock getNodeLock()
	{
		return nodeLock;
	}

	/**
	 * Get the object used to synchronize threads when reading/writing the server resource.
	 * @return a {@link SimpleObjectLock} instance.
	 */
	public SimpleObjectLock getServerLock()
	{
		return serverLock;
	}
}
