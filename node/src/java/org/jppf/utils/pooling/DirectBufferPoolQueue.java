/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
 * A ByteBuffer pool backed by a {@link ConcurrentLinkedQueue}.
 * @author Laurent Cohen
 */
public class DirectBufferPoolQueue extends AbstractObjectPoolQueue<ByteBuffer>
{
  /**
   * Create a new object for the pool.
   * @return an object that can be returned to the pool.
   */
  @Override
  protected ByteBuffer create()
  {
    return ByteBuffer.allocateDirect(StreamConstants.TEMP_BUFFER_SIZE);
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
}
