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

import org.slf4j.*;

/**
 * Static factory for a bool of direct byte buffers.
 * @author Laurent Cohen
 */
public class DirectBufferPool
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(DirectBufferPool.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * 
   */
  //private static ObjectPool<ByteBuffer> pool = new DirectBufferPoolImpl();
  private static ObjectPool<ByteBuffer> pool = new DirectBufferPoolQueue();

  /**
   * Get a buffer from the pool, or a new buffer if the pool is empty.
   * @return a {@link ByteBuffer} instance.
   */
  public static ByteBuffer provideBuffer()
  {
    return pool.get();
  }

  /**
   * Release a buffer into the pool and make it available.
   * @param buffer the buffer to release.
   */
  public static void releaseBuffer(final ByteBuffer buffer)
  {
    pool.put(buffer);
  }
}
