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
import java.util.*;

import org.jppf.io.IO;

/**
 * A ByteBuffer pool backed by a {@link java.util.LinkedList LinkedList}.
 * @author Laurent Cohen
 * @exclude
 */
public class SynchronizedDirectBufferPool extends AbstractObjectPoolQueue<ByteBuffer> {
  @Override
  protected ByteBuffer create() {
    return ByteBuffer.allocateDirect(IO.TEMP_BUFFER_SIZE);
  }

  @Override
  public ByteBuffer get() {
    final ByteBuffer buf;
    synchronized(queue) {
      buf = queue.poll();
    }
    return (buf == null) ? create() : buf;
  }

  @Override
  public void put(final ByteBuffer buffer) {
    buffer.clear();
    synchronized(queue) {
      queue.offer(buffer);
    }
  }

  @Override
  protected Queue<ByteBuffer> createQueue() {
    return new LinkedList<>();
  }
}
