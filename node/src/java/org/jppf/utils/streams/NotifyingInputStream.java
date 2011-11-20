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

package org.jppf.utils.streams;

import java.io.*;

/**
 * Input stream that delegates read and skip operations to an underlying stream,
 * and sends notifications of how many bytes were read or skipped via a callback.
 * @see org.jppf.utils.streams.NotifyingStreamCallback
 * @see org.jppf.utils.streams.NotifyingOutputStream
 * @author Laurent Cohen
 */
public class NotifyingInputStream extends InputStream
{
  /**
   * The input stream to which operations are delegated.
   */
  private InputStream delegate;
  /**
   * The callback to notify of stream operations.
   */
  private NotifyingStreamCallback callback;

  /**
   * Initialize this stream with the specified input stream.
   * @param delegate the input stream to which operations are delegated.
   * @param callback the callback to notify of stream operations.
   */
  public NotifyingInputStream(final InputStream delegate, final NotifyingStreamCallback callback)
  {
    if (delegate == null) throw new IllegalArgumentException("input stream cannot be null");
    if (callback == null) throw new IllegalArgumentException("the callback cannot be null");
    this.delegate = delegate;
    this.callback = callback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int read() throws IOException
  {
    int n = delegate.read();
    if (n >= 0) callback.bytesNotification(1);
    return n;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int read(final byte[] b) throws IOException
  {
    return read(b, 0, b.length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException
  {
    int n = delegate.read(b, off, len);
    if (n >= 0) callback.bytesNotification(n);
    return n;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long skip(final long n) throws IOException
  {
    long l = delegate.skip(n);
    if (l >= 0) callback.bytesNotification(l);
    return l;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException
  {
    delegate.close();
  }
}
