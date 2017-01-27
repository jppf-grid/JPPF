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

package org.jppf.io;

import org.jppf.utils.pooling.AbstractBoundedObjectPoolQueue;

/**
 * A bounded pool which holds byte arrays with a fixed size.
 * @author Laurent Cohen
 */
public class BoundedByteArrayPool extends AbstractBoundedObjectPoolQueue<byte[]> {
  /**
   * The size of byte arrays managed by this pool.
   */
  private final int bufferSize;

  /**
   * Initiialize this pool with the specified maximum capacity and buffer size.
   * @param capacity the max capacity of this pool.
   * @param bufferSize the size of byte arrays managed by this pool.
   */
  public BoundedByteArrayPool(final int capacity, final int bufferSize) {
    super(capacity);
    this.bufferSize = bufferSize;

  }

  @Override
  protected byte[] create() {
    return new byte[bufferSize];
  }
}
