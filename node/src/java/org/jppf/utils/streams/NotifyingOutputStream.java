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

package org.jppf.utils.streams;

import java.io.*;

/**
 * Output stream that delegates write operations to an underlying stream,
 * and sends notifications of how many bytes were written via a callback.
 * @see org.jppf.utils.streams.NotifyingStreamCallback
 * @see org.jppf.utils.streams.NotifyingOutputStream
 * @author Laurent Cohen
 */
public class NotifyingOutputStream extends OutputStream
{
  /**
   * The output stream to which operations are delegated.
   */
  private OutputStream delegate;
  /**
   * The callback to notify of stream operations.
   */
  private NotifyingStreamCallback callback;

  /**
   * Initialize this stream with the specified input stream.
   * @param delegate the input stream to which operations are delegated.
   * @param callback the callback to notify of stream operations.
   */
  public NotifyingOutputStream(final OutputStream delegate, final NotifyingStreamCallback callback)
  {
    if (delegate == null) throw new IllegalArgumentException("output stream cannot be null");
    if (callback == null) throw new IllegalArgumentException("the callback cannot be null");
    this.delegate = delegate;
    this.callback = callback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final int n) throws IOException
  {
    delegate.write(n);
    callback.bytesNotification(1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final byte[] b) throws IOException
  {
    write(b, 0, b.length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException
  {
    delegate.write(b, off, len);
    callback.bytesNotification(len);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException
  {
    delegate.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws IOException
  {
    delegate.flush();
  }
}
