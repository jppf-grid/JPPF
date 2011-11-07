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

import org.jppf.utils.streams.StreamConstants;

/**
 * 
 * @author Laurent Cohen
 */
public class DirectBufferPoolImpl implements ObjectPool<ByteBuffer>
{
	/**
	 * The pool of {@link ByteBuffer}.
	 */
	private LinkedData<ByteBuffer> data = new LinkedData<ByteBuffer>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer get()
	{
		ByteBuffer bb = data.get();
		return bb == null ? ByteBuffer.allocateDirect(StreamConstants.TEMP_BUFFER_SIZE) : bb;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(final ByteBuffer buffer)
	{
		buffer.clear();
		data.put(buffer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty()
	{
		synchronized(data)
		{
			return data.head == null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size()
	{
		synchronized(data)
		{
			return data.size;
		}
	}


	/**
	 * @param <E>
	 */
	public static class LinkedData<E>
	{
		/**
		 * 
		 */
		private LinkedNode<E> head = null;
		/**
		 * 
		 */
		private LinkedNode<E> tail = null;
		/**
		 * 
		 */
		int size = 0;

		/**
		 * Add an object to the tail.
		 * @param content the object to add.
		 */
		public void put(final E content)
		{
			LinkedNode node = new LinkedNode(content);
			synchronized(this)
			{
				if (tail != null)
				{
					node.next = tail;
					tail.prev = node;
				}
				else head = node;
				tail = node;
				size++;
			}
		}

		/**
		 * 
		 * @return the head obect or null.
		 */
		public synchronized E get()
		{
			if (head == null) return null;
			LinkedNode<E> res = head;
			if (res.prev == null)
			{
				tail = null;
				head = null;
			}
			else
			{
				head = res.prev;
				head.next = null;
			}
			size--;
			return res.content;
		}

		/**
		 * Get the size of this queue.
		 * @return the size of this queue.
		 */
		private synchronized int size()
		{
			return size;
		}
	}

	/**
	 * @param <E>
	 */
	static class LinkedNode<E>
	{
		/**
		 * 
		 */
		final E content;
		/**
		 * 
		 */
		LinkedNode<E> prev = null;
		/**
		 * 
		 */
		LinkedNode<E> next = null;

		/**
		 * Initialize this node with the psecified content.
		 * @param content the node'sx content.
		 */
		LinkedNode(final E content)
		{
			this.content = content;
		}
	}
}
