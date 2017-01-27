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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.*;

/**
 * Data location backed by a file.
 * @author Laurent Cohen
 * @author Juan Manuel Rodriguez
 */
public class FileDataLocation extends AbstractDataLocation {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(FileDataLocation.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The current count of bytes read from/written to the underlying file.
   */
  private int count = 0;
  /**
   * The file channel used to write to or read from the underlying file.
   */
  private FileChannel fileChannel = null;
  /**
   * The path to the underlying file.
   */
  private String filePath = null;
  /**
   * Temporary buffer used for data transfers.
   */
  private ByteBuffer buffer = null;
  /**
   * The size of the block of data currently being transferred.
   */
  private int blockSize = 0;
  /**
   * Count of instances of this class which refer to the same underlying file.
   * This is a fix for bug
   * <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-105">JPPF-105 Temporary files disappear when setting a large MemoryMapDataProvider as data provider in a JPPFJob</a>.
   */
  final AtomicLong copyCount;

  /**
   * Initialize this file location with the specified file path and an unknown size.
   * @param path the path to the underlying file.
   */
  public FileDataLocation(final String path) {
    this(new File(path));
  }

  /**
   * Initialize this file location with the specified file path and size.
   * @param path the path to the underlying file.
   * @param size the size of the data represented by this file location.
   */
  public FileDataLocation(final String path, final int size) {
    filePath = path;
    this.size = size;
    copyCount = new AtomicLong(1L);
  }

  /**
   * Copy constructor.
   * @param path the path to the underlying file.
   * @param size the size of the data represented by this file location.
   * @param copyCount .
   */
  private FileDataLocation(final String path, final int size, final AtomicLong copyCount) {
    filePath = path;
    this.size = size;
    this.copyCount = copyCount;
    copyCount.incrementAndGet();
  }

  /**
   * Initialize this file location with the specified file and unknown size.
   * @param file an abstract path to the underlying file.
   */
  public FileDataLocation(final File file) {
    this(file, (int) file.length());
  }

  /**
   * Initialize this file location with the specified file and size.
   * @param file an abstract path to the underlying file.
   * @param size the size of the data represented by this file location.
   */
  public FileDataLocation(final File file, final int size) {
    this(file.getPath(), size);
  }

  /**
   * Transfer the content of this data location from the specified input source.
   * @param source the input source to transfer from.
   * @param blocking if true, the method will block until the entire content has been transferred.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int transferFrom(final InputSource source, final boolean blocking) throws Exception {
    if (!transferring) {
      transferring = true;
      fileChannel = new FileOutputStream(filePath).getChannel();
      buffer = ByteBuffer.wrap(IO.TEMP_BUFFER_POOL.get());
      if (size < buffer.limit()) buffer.limit(size);
      count = 0;
    }
    try {
      int n = blocking ? blockingTransferFrom(source) : nonBlockingTransferFrom(source);
      if ((n < 0) || (count >= size)) transferring = false;
      return n;
    } catch(Exception e) {
      transferring = false;
      throw e;
    }
    finally {
      if (!transferring) {
        buffer = null;
        if (fileChannel != null) {
          fileChannel.force(false);
          fileChannel.close();
          fileChannel = null;
        }
      }
    }
  }

  /**
   * Perform a non-blocking transfer to this data location from the specified input source.
   * @param source the input source to transfer from.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  private int nonBlockingTransferFrom(final InputSource source) throws Exception {
    int remaining = size - count;
    if (remaining < buffer.remaining()) buffer.limit(buffer.position() + remaining);
    int n = source.read(buffer);
    if (n > 0) {
      count += n;
      buffer.flip();
      int tempCount = 0;
      while (tempCount < n) {
        int tmp = fileChannel.write(buffer);
        if (tmp < 0) {
          transferring = false;
          return -1;
        }
        tempCount += tmp;
        //if (traceEnabled) log.trace("written " + tmp + " bytes (total: " + tempCount + '/' + n + ')');
      }
      buffer.clear();
    }
    if ((n < 0) || (count >= size)) transferring = false;
    return n;
  }

  /**
   * Perform a blocking transfer to this data location from the specified input source.
   * @param source the input source to transfer from.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  private int blockingTransferFrom(final InputSource source) throws Exception {
    while (count < size) {
      int remaining = size - count;
      if ((remaining < buffer.limit()) && (remaining > 0))  buffer.limit(remaining);
      int n = source.read(buffer);
      if (n < 0) {
        transferring = false;
        return -1;
      } else if (n > 0) {
        count += n;
        buffer.flip();
        int tempCount = 0;
        while (tempCount < n) {
          int tmp = fileChannel.write(buffer);
          if (tmp < 0) {
            transferring = false;
            return -1;
          }
          tempCount += tmp;
        }
        buffer.clear();
      }
    }
    transferring = false;
    return count;
  }

  /**
   * Transfer the content of this data location to the specified output destination.
   * @param dest the output destination to transfer to.
   * @param blocking if true, the method will block until the entire content has been transferred.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int transferTo(final OutputDestination dest, final boolean blocking) throws Exception {
    if (!transferring) {
      transferring = true;
      fileChannel = new FileInputStream(filePath).getChannel();
      buffer = ByteBuffer.wrap(IO.TEMP_BUFFER_POOL.get());
      count = 0;
    }
    try {
      return blocking ? blockingTransferTo(dest) : nonBlockingTransferTo(dest);
    } catch(Exception e) {
      transferring = false;
      throw e;
    } finally {
      if (!transferring) {
        buffer = null;
        if (fileChannel != null) {
          fileChannel.close();
          fileChannel = null;
        }
      }
    }
  }

  /**
   * Perform a non-blocking from this data location to the specified output destination.
   * @param dest the output destination to transfer to.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  private int nonBlockingTransferTo(final OutputDestination dest) throws Exception {
    if (blockSize == 0) {
      blockSize = fileChannel.read(buffer);
      buffer.flip();
    }
    int n = dest.write(buffer);
    if (n < 0) {
      transferring = false;
      return -1;
    }
    if (!buffer.hasRemaining()) {
      count += blockSize;
      blockSize = 0;
      buffer.clear();
      if (count >= size) transferring = false;
    }
    return n;
  }

  /**
   * Perform a non-blocking from this data location to the specified output destination.
   * @param dest the output destination to transfer to.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  private int blockingTransferTo(final OutputDestination dest) throws Exception {
    while (count < size) {
      blockSize = fileChannel.read(buffer);
      buffer.flip();
      while(buffer.hasRemaining()) {
        int n = dest.write(buffer);
        if (n < 0) {
          transferring = false;
          return -1;
        }
        count += n;
      }
      if (count < size) buffer.clear();
    }
    transferring = false;
    return count;
  }

  /**
   * This method deletes the underlying file.
   * @throws Throwable if an error occurs.
   */
  @Override
  protected void finalize() throws Throwable {
    try {
      if (traceEnabled) log.trace("finalizing " + this);
      if (copyCount.decrementAndGet() <= 0) {
        File file = new File(filePath);
        if (file.exists()) file.delete();
      }
    } finally {
      super.finalize();
    }
  }

  /**
   * Get an input stream for this location.
   * @return an <code>InputStream</code> instance.
   * @throws Exception if an I/O error occurs.
   */
  @Override
  public InputStream getInputStream() throws Exception {
    return new BufferedInputStream(new FileInputStream(filePath));
  }

  /**
   * Get an output stream for this location.
   * @return an <code>OutputStream</code> instance.
   * @throws Exception if an I/O error occurs.
   */
  @Override
  public OutputStream getOutputStream() throws Exception {
    return new BufferedOutputStream(new FileOutputStream(filePath));
  }

  /**
   * Make a shallow copy of this data location.
   * The data it points to is not copied.
   * @return a new DataLocation instance pointing to the same data.
   */
  @Override
  public DataLocation copy() {
    if (traceEnabled) log.trace("copying " + this);
    return new FileDataLocation(filePath, size, copyCount);
  }

  /**
   * Get the path to the underlying file.
   * @return the path as a string.
   */
  public String getFilePath() {
    return filePath;
  }
}
