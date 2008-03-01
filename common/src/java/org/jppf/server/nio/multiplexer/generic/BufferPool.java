/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.server.nio.multiplexer.generic;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.apache.commons.logging.*;
import org.jppf.utils.JPPFConfiguration;

/**
 * Utility class implemented as a singleton, manages a pool of IO buffers. 
 * @author Laurent Cohen
 */
public final class BufferPool
{
	/**
	 * Maximum number of bytes that can be written or read in one shot.
	 */
	private static final int MAX_BUFFER_SIZE =
		1024 * JPPFConfiguration.getProperties().getInt("io.buffer.size", 128);
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(BufferPool.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Pool of IO buffers to pick from.
	 */
	private static LinkedList<ByteBuffer> bufferPool = new LinkedList<ByteBuffer>();
	/**
	 * Total number of allocated buffers.
	 */
	private static int nbAllocatedBuffers = 0;
	/**
	 * Current number of buffers in the pool.
	 */
	private static int nbBuffersInPool = 0;

	/**
	 * Instantiation of this class is not allowed.
	 */
	private BufferPool()
	{
	}

	/**
	 * Get a buffer from the pool. If the pool is emopty, a new buffer is created.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	public static ByteBuffer pickBuffer()
	{
		ByteBuffer result = null;
		synchronized(bufferPool)
		{
			if (bufferPool.isEmpty())
			{
				result = ByteBuffer.wrap(new byte[MAX_BUFFER_SIZE]);
				nbAllocatedBuffers++;
				if (debugEnabled) log.debug("allocated buffers: " + nbAllocatedBuffers);
			}
			else
			{
				result = bufferPool.remove();
				nbBuffersInPool--;
				if (debugEnabled) log.debug("buffers in pool: " + nbBuffersInPool);
			}
		}
		return result;
	}

	/**
	 * Release a buffer into the pool.
	 * @param buffer the buffer to release.
	 */
	public static void releaseBuffer(ByteBuffer buffer)
	{
		buffer.clear();
		synchronized(bufferPool)
		{
			bufferPool.add(buffer);
			nbBuffersInPool++;
			if (debugEnabled) log.debug("buffers in pool: " + nbBuffersInPool);
		}
	}

	/**
	 * Log the buffer pool statistics. This method is intended for debugging purposes only.
	 */
	private static void logStats()
	{
		log.debug("allocated buffers: " + nbAllocatedBuffers + ", buffers in pool: " + nbBuffersInPool);
	}
}
