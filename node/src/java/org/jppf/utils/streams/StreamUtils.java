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

import org.jppf.utils.ExceptionUtils;
import org.slf4j.Logger;

/**
 * Collection of utility methods for manipulating streams.
 * @author Laurent Cohen
 */
public final class StreamUtils
{
  /**
   * Instantiating this class is not permitted.
   */
  private StreamUtils()
  {
  }

  /**
   * Attempt to close the specified closeable without logging an eventual error.
   * @param closeable the closeable to close.
   * @throws IOException if any error occurs while closing the closeable.
   */
  public static void close(final Closeable closeable) throws IOException
  {
    closeable.close();
  }

  /**
   * Attempt to silently close (no exception logging) the specified closeable.
   * @param closeable the closeable to close.
   */
  public static void closeSilent(final Closeable closeable)
  {
    close(closeable, null);
  }

  /**
   * Attempt to close the specified closeable and log any eventual error.
   * @param closeable the closeable to close.
   * @param log the logger to use; if null no logging occurs.
   */
  public static void close(final Closeable closeable, final Logger log)
  {
    if (closeable != null)
    {
      try
      {
        closeable.close();
      }
      catch (IOException e)
      {
        if (log != null)
        {
          String s = "unable to close stream/reader/writer: " + ExceptionUtils.getMessage(e);
          if (log.isDebugEnabled()) log.debug(s, e);
          else log.warn(s);
        }
      }
    }
  }

  /**
   * Get the content of an input stream as an array of bytes.
   * This method closes the input stream before terminating.
   * @param is the input stream to read from.
   * @return a byte array.
   * @throws IOException if an IO error occurs.
   */
  public static byte[] getInputStreamAsByte(final InputStream is) throws IOException
  {
    byte[] buffer = new byte[StreamConstants.TEMP_BUFFER_SIZE];
    byte[] result = null;
    ByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
    boolean end = false;
    try
    {
      while (!end)
      {
        int n = is.read(buffer, 0, buffer.length);
        if (n < 0) end = true;
        else baos.write(buffer, 0, n);
      }
      baos.flush();
      result = baos.toByteArray();
    }
    finally
    {
      try
      {
        is.close();
      }
      finally
      {
        baos.close();
      }
    }
    return result;
  }

  /**
   * Copy the data read from the specified input stream to the specified output stream.
   * This method closes both streams before terminating.
   * @param is the input stream to read from.
   * @param os the output stream to write to.
   * @throws IOException if an I/O error occurs.
   */
  public static void copyStream(final InputStream is, final OutputStream os) throws IOException
  {
    try
    {
      byte[] bytes = new byte[StreamConstants.TEMP_BUFFER_SIZE];
      while(true)
      {
        int n = is.read(bytes);
        if (n <= 0) break;
        os.write(bytes, 0, n);
      }
      os.flush();
    }
    finally
    {
      try
      {
        is.close();
      }
      finally
      {
        os.close();
      }
    }
  }
}
