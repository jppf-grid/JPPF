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

package org.jppf.utils.streams;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.jppf.utils.pooling.DirectBufferPool;

/**
 * An {@link OutputStream} implementation that writes to an underlying {@link WritableByteChannel} which is assumed to be in <b>blocking mode</b>.
 * @author Laurent Cohen
 */
public class ChannelOutputStream extends OutputStream {
  /**
   * The backing {@link WritableByteChannel}.
   */
  private final WritableByteChannel channel;

  /**
   * Initialize this output stream with the specified writeable channel.
   * @param channel the channel to write to.
   */
  public ChannelOutputStream(final WritableByteChannel channel) {
    this.channel = channel;
  }

  @Override
  public void write(final int b) throws IOException {
    write(new byte[] { (byte) b }, 0, 1);
  }

  @Override
  public void write(final byte[] data) throws IOException {
    write(data, 0, data.length);
  }

  @Override
  public void write(final byte[] data, final int offset, final int len) throws IOException {
    ByteBuffer tmpBuffer = null;
    try {
      tmpBuffer = DirectBufferPool.provideBuffer();
      final int cap = tmpBuffer.capacity();
      int count = 0;
      while (count < len) {
        tmpBuffer.clear();
        final int size = Math.min(cap, len - count);
        tmpBuffer.put(data, offset + count, size);
        tmpBuffer.flip();
        final int n = channel.write(tmpBuffer);
        if (n < 0) break;
        count += n;
      }
    } finally {
      if (tmpBuffer != null) DirectBufferPool.releaseBuffer(tmpBuffer);
    }
  }
}
