/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.io.IO;

/**
 * Static factory for pool of direct byte buffers.
 * @author Laurent Cohen
 * @exclude
 */
public class DirectBufferPool {
  /**
   * 
   */
  private static final ThreadLocal<ByteBuffer> threadLocalBuffer = new ThreadLocal<>();
  /**
   * 
   */
  //private static final ObjectPool<ByteBuffer> pool = new DirectBufferPoolQueue().init(100);

  /**
   * Get a buffer from the pool, or a new buffer if the pool is empty.
   * @return a {@link ByteBuffer} instance.
   */
  public static ByteBuffer provideBuffer() {
    ByteBuffer bb = threadLocalBuffer.get();
    if (bb == null) {
      threadLocalBuffer.set(bb = ByteBuffer.allocateDirect(IO.TEMP_BUFFER_SIZE));
      //threadLocalBuffer.set(bb = pool.get());
    }
    return bb;
  }

  /**
   * Release a buffer into the pool and make it available.
   * @param buffer the buffer to release.
   */
  public static void releaseBuffer(final ByteBuffer buffer) {
    buffer.clear();
  }

  /**
   * Release a buffer into the pool and make it available.
   */
  /*
  public static void removeBuffer() {
    ByteBuffer bb = threadLocalBuffer.get();
    if (bb != null) {
      threadLocalBuffer.remove();
      pool.put(bb);
    }
  }
  */
}
