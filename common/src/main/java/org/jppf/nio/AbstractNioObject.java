/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.nio;

import org.jppf.io.DataLocation;

/**
 * Abstract implementation of the {@link NioObject} interface, providing the means to keep stateful information
 * across multiple calls to the <code>read()</code> or <code>write()</code> methods.
 * @author Laurent Cohen
 */
public abstract class AbstractNioObject implements NioObject {
  /**
   * The size of the data ot send or receive.
   */
  protected final int size;
  /**
   * What has currently been read from or written to the message.
   */
  protected int count;
  /**
   * Location of the data to read or write.
   */
  protected final DataLocation location;
  /**
   * Actual bytes sent to or received from the underlying channel (in encrypted form for secure channels).
   */
  protected long channelCount;

  /**
   * Initialize this NioObject with the specified data location and size.
   * @param location the object which holds the data to read or write.
   * @param size the size of the data.
   */
  protected AbstractNioObject(final DataLocation location, final int size) {
    this.location = location;
    this.size = size;
  }

  @Override
  public DataLocation getData() {
    return location;
  }

  @Override
  public int getSize() {
    return size;
  }

  @Override
  public long getChannelCount() {
    return channelCount;
  }

  @Override
  public NioObject reset() {
    count = 0;
    channelCount = 0;
    return this;
  }
}
