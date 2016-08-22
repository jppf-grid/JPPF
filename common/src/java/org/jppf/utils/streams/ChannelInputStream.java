/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
import java.nio.channels.*;

import org.jppf.utils.pooling.DirectBufferPool;

/**
 * An {@link InputStream} implementation that reads from an underlying {@link ReadableByteChannel} which is assumed to be in <b>blocking mode</b>.
 * @author Laurent Cohen
 */
public class ChannelInputStream extends InputStream {
  /**
   * The backing {@link ReadableByteChannel}.
   */
  private final ReadableByteChannel channel;

  /**
   * Initialize this output stream with the specified writeable channel.
   * @param channel the channel to write to.
   */
  public ChannelInputStream(final ReadableByteChannel channel) {
    this.channel = channel;
  }

  @Override
  public int read() throws IOException {
    byte[] buf = new byte[1];
    read(buf, 0, 1);
    return buf[0] & 0xff;
  }

  @Override
  public int read(final byte[] data) throws IOException {
    return read(data, 0, data.length);
  }

  @Override
  public int read(final byte[] buffer, final int offset, final int len) throws IOException {
    ByteBuffer data = ByteBuffer.wrap(buffer, offset, len);
    ByteBuffer tmpBuffer = null;
    try {
      tmpBuffer = DirectBufferPool.provideBuffer();
      int remaining = data.remaining();
      int count = 0;
      while (count < remaining) {
        if (data.remaining() < tmpBuffer.remaining()) tmpBuffer.limit(data.remaining());
        int n = channel.read(tmpBuffer);
        if (n < 0) throw new EOFException();
        else if (n > 0) {
          count += n;
          tmpBuffer.flip();
          data.put(tmpBuffer);
          tmpBuffer.clear();
        }
      }
      return count;
    } finally {
      if (tmpBuffer != null) {
        DirectBufferPool.releaseBuffer(tmpBuffer);
        tmpBuffer = null;
      }
    }
  }
}
