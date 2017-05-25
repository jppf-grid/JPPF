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

import org.jppf.io.IO;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.Logger;

/**
 * Collection of utility methods for manipulating streams.
 * @author Laurent Cohen
 */
public final class StreamUtils {
  /**
   * Instantiating this class is not permitted.
   */
  private StreamUtils() {
  }

  /**
   * Attempt to close the specified closeable without logging an eventual error.
   * @param closeable the closeable to close.
   * @throws Exception if any error occurs while closing the closeable.
   */
  public static void close(final AutoCloseable closeable) throws Exception {
    closeable.close();
  }

  /**
   * Attempt to silently close (no exception logging) the specified closeable.
   * @param closeable the closeable to close.
   */
  public static void closeSilent(final AutoCloseable closeable) {
    close(closeable, null);
  }

  /**
   * Attempt to close the specified closeable and log any eventual error.
   * @param closeable the closeable to close.
   * @param log the logger to use; if null no logging occurs.
   */
  public static void close(final AutoCloseable closeable, final Logger log) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception e) {
        if (log != null) {
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
  public static byte[] getInputStreamAsByte(final InputStream is) throws IOException {
    ByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
    copyStream(is, baos, true);
    return baos.toByteArray();
  }

  /**
   * Copy the data read from the specified input stream to the specified output stream.
   * This method closes both streams before terminating.
   * @param src the input stream to read from.
   * @param dest the output stream to write to.
   * @return the number of bytes copied.
   * @throws IOException if an I/O error occurs.
   */
  public static long copyFile(final File src, final File dest) throws IOException {
    try (InputStream is = new BufferedInputStream(new FileInputStream(src)); OutputStream os = new BufferedOutputStream(new FileOutputStream(dest))) {
      return copyStream(is, os);
    }
  }

  /**
   * Copy the data read from the specified input stream to the specified output stream.
   * This method closes both streams before terminating.
   * @param is the input stream to read from.
   * @param os the output stream to write to.
   * @return the number of bytes copied.
   * @throws IOException if an I/O error occurs.
   */
  public static long copyStream(final InputStream is, final OutputStream os) throws IOException {
    return copyStream(is, os, true);
  }

  /**
   * Copy the data read from the specified input stream to the specified output stream.
   * This method closes both streams before terminating.
   * @param is the input stream to read from.
   * @param os the output stream to write to.
   * @param closeStreams <code>true</code> to cause the streams to be closed at the end of the copy, <code>false</code> otherwise.
   * @return the number of bytes copied.
   * @throws IOException if an I/O error occurs.
   */
  public static long copyStream(final InputStream is, final OutputStream os, final boolean closeStreams) throws IOException {
    byte[] bytes =  IO.TEMP_BUFFER_POOL.get();
    long count = 0L;
    try {
      while(true) {
        int n = is.read(bytes);
        if (n <= 0) break;
        os.write(bytes, 0, n);
        count += n;
      }
      os.flush();
    } finally {
      IO.TEMP_BUFFER_POOL.put(bytes);
      if (closeStreams) {
        try {
          is.close();
        } finally {
          os.close();
        }
      }
    }
    return count;
  }

  /**
   * Display a message and wait until a key is pressed.
   * @throws Exception if any I/O error occurs.
   */
  public static void waitKeyPressed() throws Exception {
    waitKeyPressed("press <Enter> to continue ...");
  }

  /**
   * Display a message and wait until a key is pressed.
   * @param message the message to dispplay
   * @throws Exception if any I/O error occurs.
   */
  public static void waitKeyPressed(final String message) throws Exception {
    System.out.println(message);
    System.in.read();
  }

  /**
   * Display a message and wait until the specified key is pressed.
   * @param c the character to wait for.
   * @param message the message to dispplay.
   * @throws Exception if any I/O error occurs.
   */
  public static void waitKeyPressed(final char c, final String message) throws Exception {
    System.out.println(message);
    while ((char) System.in.read() != c);
  }

  /**
   * Convenience method to print a formatted mssage to System.out and a specified logger.
   * @param logger the logger to print to, may be {@code null}. If specified, the message is logged at INFO level.
   * @param format the message format as specified in {@link java.util.Formatter}.
   * @param params the formatted message parameters.
   */
  public static void printf(final Logger logger, final String format, final Object...params) {
    String message = String.format(format, params);
    System.out.println(message);
    if (logger != null) logger.info(message);
  }
}
