/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.server.nio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

import org.jppf.io.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class SSLNioObject extends AbstractNioObject
{
  /**
   * The source from which the data is read.
   */
  private InputStream is;
  /**
   * The destination to which the data is written.
   */
  private OutputStream os;
  /**
   * 
   */
  private SSLEngineManager engineManager;

  /**
   * Construct this SSLMessage.
   * @param channel the channel wrapper for the network connection.
   * @param size the size of the internal buffer.
   * @throws Exception if any error occurs.
   */
  public SSLNioObject(final ChannelWrapper<?> channel, final int size) throws Exception
  {
    this(channel, new MultipleBuffersLocation(size));
  }

  /**
   * Construct this SSLMessage.
   * @param channel the channel wrapper for the network connection.
   * @param location the location of the data to read from or write to.
   * @throws Exception if any error occurs.
   */
  public SSLNioObject(final ChannelWrapper<?> channel, final DataLocation location) throws Exception
  {
    SocketChannel socketChannel = (SocketChannel) ((SelectionKeyWrapper) channel).getChannel().channel();
    SSLEngine sslEngine = channel.getContext().getSSLEngine();
    this.engineManager = new SSLEngineManager(socketChannel, sslEngine);
    this.location = location;
    this.size = location.getSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean read() throws Exception
  {
    ByteBuffer buf = engineManager.getAppRecvBuffer();
    if (os == null) os = location.getOutputStream();
    int sum = 0;
    int n = 0;
    while (((n = engineManager.read()) > 0) && (count < size))
    {
      sum += n;
      count += n;
      os.write(buf.array(), 0, buf.position());
      buf.clear();
    }
    boolean b = count >= size;
    if (b) StreamUtils.closeSilent(os);
    return b;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean write() throws Exception
  {
    ByteBuffer buf = engineManager.getAppSendBuffer();
    byte[] tmp = buf.array();
    if (is == null)
    {
      is = location.getInputStream();
      buf.clear();
    }
    int sum = 0;
    int n = 0;
    do
    {
      int tmpCount = is.read(tmp, buf.position(), Math.min(size-count, buf.remaining()));
      buf.position(tmpCount-1);
      n = engineManager.write();
      if (n > 0)
      {
        sum += n;
        count += n;
      }
      if (n < tmpCount)
      {
        byte[] bytes = new byte[tmpCount - n];
        System.arraycopy(tmp, n, bytes, 0, bytes.length);
        buf.clear();
        buf.put(bytes);
      }
      else buf.clear();
    }
    while ((n > 0) && (count < size));
    boolean b = count >= size;
    if (b) StreamUtils.closeSilent(is);
    return b;
  }
}
