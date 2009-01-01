/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.utils;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.apache.commons.logging.*;

/**
 * Utility class implemented as a singleton, manages a pool of IO buffers.<br>
 * All buffers created by this pool are backed by an array of bytes.<br>
 * The pool is implemented as a soft cache, meaning that all buffers in it are softly-referenced
 * and are guaranteed to be reclaimed by the garbage collector before an OutOfMemoryError is thrown.
 * @author Laurent Cohen
 */
public final class BufferPool
{
	/**
	 * Maximum number of bytes that can be written or read in one shot.
	 */
	private static final int MAX_BUFFER_SIZE =
		1024 * JPPFConfiguration.getProperties().getInt("io.buffer.size", 32);
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
	private static LinkedList<BufferReference> bufferPool = new LinkedList<BufferReference>();
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
	 * Get a buffer from the pool. If the pool is empty, a new buffer is created.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	public static ByteBuffer pickBuffer()
	{
		ByteBuffer result = null;
		synchronized(bufferPool)
		{
			while (result == null)
			{
				if (bufferPool.isEmpty())
				{
					result = ByteBuffer.wrap(new byte[MAX_BUFFER_SIZE]);
					nbAllocatedBuffers++;
					if (debugEnabled) log.debug("allocated buffers: " + nbAllocatedBuffers);
				}
				else
				{
					BufferReference ref = bufferPool.remove();
					ref.setRemovedFromPool(true);
					result = ref.get();
					//ref.clear();
					nbBuffersInPool--;
					if (debugEnabled) log.debug("buffers in pool: " + nbBuffersInPool);
				}
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
		if (buffer == null) return;
		buffer.clear();
		synchronized(bufferPool)
		{
			bufferPool.add(new BufferReference(buffer));
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

	/**
	 * Implementation of a soft reference wrapping a byte buffer from the pool.
	 */
	private static class BufferReference extends SoftReference<ByteBuffer>
	{
		/**
		 * Determines whether this reference was already removed from the pool.
		 */
		boolean removedFromPool = false; 

		/**
     * Creates a new soft reference that refers to the given object. The new
     * reference is not registered with any queue.
     * @param referent object the new soft reference will refer to
		 */
		public BufferReference(ByteBuffer referent)
		{
			super(referent);
		}

		/**
		 * Clear this reference and remove it from the buffer pool.
		 * @see java.lang.ref.Reference#clear()
		 */
		public void clear()
		{
			if (!isRemovedFromPool())
			{
				setRemovedFromPool(true);
				synchronized(bufferPool)
				{
					bufferPool.remove(get());
					nbBuffersInPool--;
					if (debugEnabled) log.debug("buffers in pool: " + nbBuffersInPool);
				}
			}
			super.clear();
		}

		/**
		 * Determine whether this reference was removed from the pool.
		 * @return true if the reference was removed from the pool, false otherwise.
		 */
		public synchronized boolean isRemovedFromPool()
		{
			return removedFromPool;
		}

		/**
		 * Specify whether this reference is removed from the pool.
		 * @param removedFromPool true if the reference is removed from the pool, false otherwise.
		 */
		public synchronized void setRemovedFromPool(boolean removedFromPool)
		{
			this.removedFromPool = removedFromPool;
		}
	}
}
