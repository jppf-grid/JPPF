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

import java.io.*;
import java.nio.ByteBuffer;

import org.jppf.io.*;
import org.jppf.utils.JPPFBuffer;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Implementation of {@link NioObject} for reading or writing data from or to an SSL channel.
 * The channel and the corresponding {@link javax.net.ssl.SSLEngine SSLEngine} are both
 * encapsulated within an instance of {@link SSLHandlerImpl}.
 * @author Laurent Cohen
 */
public class SSLNioObject extends AbstractNioObject {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SSLNioObject.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The source from which the data is read.
   */
  private InputStream is;
  /**
   * The destination to which the data is written.
   */
  private OutputStream os;
  /**
   * The associated sslHandler, if any.
   */
  private final SSLHandler sslHandler;
  /**
   * 
   */
  private int statefulCount;

  /**
   * Construct this SSLMessage.
   * @param size the size of the internal buffer.
   * @param sslHandler the <code>SSLHandler</code> to use with this nio object.
   * @throws Exception if any error occurs.
   */
  public SSLNioObject(final int size, final SSLHandler sslHandler) throws Exception {
    this(new MultipleBuffersLocation(size), sslHandler);
  }

  /**
   * Construct this SSLMessage.
   * @param buf the internal buffer.
   * @param sslHandler the <code>SSLHandler</code> to use with this nio object.
   * @throws Exception if any error occurs.
   */
  public SSLNioObject(final JPPFBuffer buf, final SSLHandler sslHandler) throws Exception {
    this(new MultipleBuffersLocation(buf), sslHandler);
  }

  /**
   * Construct this SSLMessage.
   * @param location the location of the data to read from or write to.
   * @param sslHandler the <code>SSLHandler</code> to use with this nio object.
   * @throws Exception if any error occurs.
   */
  public SSLNioObject(final DataLocation location, final SSLHandler sslHandler) throws Exception {
    super(location, location.getSize());
    this.sslHandler = sslHandler;
  }

  @Override
  public boolean read() throws Exception {
    if (count >= size) return true;
    final ByteBuffer buf = sslHandler.getAppReceiveBuffer();
    if (os == null) {
      os = location.getOutputStream();
    }
    int n = 0;
    while (count < size) {
      if (buf.position() <= 0) {
        n = sslHandler.read();
        if (n == 0) return false;
        else if (n < 0) throw new EOFException();
        channelCount += sslHandler.getChannelReadCount();
      }
      buf.flip();
      if (traceEnabled) log.trace("n1=" + n + ", count=" + count + ", size=" + size + ", buf=" + buf);
      n = Math.min(buf.remaining(), size - count);
      os.write(buf.array(), 0, n);
      count += n;
      buf.position(n);
      if (traceEnabled) log.trace("n2=" + n + " count=" + count + ", size=" + size + ", buf=" + buf);
      buf.compact();
      if (traceEnabled) log.trace("after compact(): buf=" + buf + ", netRcvBuf=" + sslHandler.getNetReceiveBuffer());
    }
    final boolean b = count >= size;
    if (b) {
      StreamUtils.close(os, log);
      os = null;
    }
    //channelCount = count;
    return b;
  }

  @Override
  public boolean write() throws Exception {
    if (count >= size) return true;
    final ByteBuffer buf = sslHandler.getAppSendBuffer();
    if (is == null) {
      is = location.getInputStream();
      statefulCount = 0;
    }
    if (traceEnabled) log.trace("statefulCount=" + statefulCount + ", count=" + count + ", size=" + size + ", buf=" + buf);
    if (buf.hasRemaining() && (statefulCount < size)) {
      final int min = Math.min(size - statefulCount, buf.remaining());
      final int read = is.read(buf.array(), buf.position(), min);
      if (read > 0) {
        statefulCount += read;
        buf.position(buf.position() + read);
      }
    }
    if (traceEnabled) log.trace("statefulCount=" + statefulCount + ", count=" + count + ", size=" + size + ", buf=" + buf);
    int n;
    do {
      n = sslHandler.write();
      if (n > 0) count += n;
      if (traceEnabled) log.trace("n=" + n + ", statefulCount=" + statefulCount + ", count=" + count + ", size=" + size + ", buf=" + buf);
    } while (n > 0);
    final boolean b = count >= size;
    if (b) {
      buf.clear();
      StreamUtils.close(is, log);
      is = null;
    }
    //channelCount += sslHandler.getChannelWriteCount();
    channelCount = count;
    return b;
  }

  @Override
  public NioObject reset() {
    is = null;
    os = null;
    statefulCount = 0;
    return super.reset();
  }
}
