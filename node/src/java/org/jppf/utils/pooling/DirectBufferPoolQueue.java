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

package org.jppf.utils.pooling;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.utils.streams.StreamConstants;

/**
 * A ByteBuffer pool backed by a {@link ConcurrentLinkedQueue}. 
 * @author Laurent Cohen
 */
public class DirectBufferPoolQueue implements ObjectPool<ByteBuffer>
{
	/**
	 * The pool of {@link ByteBuffer}.
	 */
	private static Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<ByteBuffer>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer get()
	{
		ByteBuffer bb = queue.poll();
		if (bb == null) bb = ByteBuffer.allocateDirect(StreamConstants.TEMP_BUFFER_SIZE);
		return bb;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(final ByteBuffer buffer)
	{
		buffer.clear();
		queue.offer(buffer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty()
	{
		return queue.isEmpty();
	}

	/**
	 * Use this method with precaution, as its performance is in O(n).<br/>
	 * {@inheritDoc}
	 */
	@Override
	public int size()
	{
		return queue.size();
	}
}
