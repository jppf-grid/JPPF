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

/**
 * Wrapper around a <code>ByteBuffer</code> that retains the order in which it was received.
 * @author Laurent Cohen
 */
public class ByteBufferWrapper
{
	/**
	 * The actual buffer.
	 */
	public ByteBuffer buffer = null;
	/**
	 * The creation order of this buffer wrapper instance.
	 */
	public int order = -1;

	/**
	 * Create a byte buffer wrapper with the specified buffer and count.
	 * @param buffer the actual buffer.
	 * @param order the creation order of this buffer wrapper instance.
	 */
	public ByteBufferWrapper(final ByteBuffer buffer, final int order)
	{
		this.buffer = buffer;
		this.order = order;
	}
}
