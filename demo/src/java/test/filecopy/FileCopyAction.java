/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package test.filecopy;

import java.io.*;
import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * This action is executed on the client side and reads, then sends the next chunk of a file
 * back to a task executing on the node.
 * @author Laurent Cohen
 */
public class FileCopyAction implements JPPFCallable<CopyResult> {
  /**
   * Set to {@code true} to print debug messages in the client's output console.
   */
  private final static boolean DEBUG = false;
  /**
   * Mapping of action uuids to input streams.
   */
  private static Map<String, InputStream> streamMap = new HashMap<>();
  /**
   * The location of the file to copy in the client file system or classpath.
   */
  private final String clientLocation;
  /**
   * The maximum size of each chunk read fom the file.
   */
  private final int maxChunkSize;
  /**
   * UUID associated with this action.
   */
  private final String uuid;
  /**
   * The current position in the file.
   */
  private long pos = 0L;
  /**
   * Determines whether the file was read entirely or not.
   */
  private boolean eof = false;

  /**
   * Initialize this action.
   * @param clientLocation the location to copy the file from in the client file system.
   * @param maxChunkSize the maximum size of each chunk read fom the file.
   */
  public FileCopyAction(final String clientLocation, final int maxChunkSize) {
    this.clientLocation = clientLocation;
    this.maxChunkSize = maxChunkSize;
    this.uuid = JPPFUuid.normalUUID();
  }

  /**
   * Copy constructor.
   * @param action the action to copy information from.
   */
  public FileCopyAction(final FileCopyAction action) {
    this.clientLocation = action.clientLocation;
    this.maxChunkSize = action.maxChunkSize;
    this.eof = action.eof;
    this.pos = action.maxChunkSize;
    this.uuid = action.uuid;
  }

  @Override
  public CopyResult call() throws Exception {
    try {
      if (DEBUG) System.out.printf("start of call(), uuid=%s%n", uuid);
      if (eof) throw new EOFException("eof reached");
      InputStream is = null;
      if (pos == 0L) {
        if (DEBUG) System.out.printf("creating input stream for '%s'%n", clientLocation);
        is = FileUtils.getFileInputStream(clientLocation);
        synchronized(streamMap) {
          streamMap.put(uuid, is);
        }
      } else {
        if (DEBUG) System.out.printf("getting input stream from map for '%s'%n", clientLocation);
        synchronized(streamMap) {
          is = streamMap.get(uuid);
        }
      }
      if (is == null) throw new FileNotFoundException("file '" + clientLocation + "' not found");
      byte[] bytes = new byte[maxChunkSize];
      int n = is.read(bytes);
      if (DEBUG) System.out.printf("read %d bytes for '%s'%n", n, clientLocation);
      if (n < maxChunkSize) {
        eof = true;
        StreamUtils.closeSilent(is);
        synchronized(streamMap) {
          streamMap.remove(uuid);
        }
      }
      if (n <= 0) return new CopyResult(null, true);
      byte[] result = new byte[n];
      System.arraycopy(bytes, 0, result, 0, n);
      return new CopyResult(result, eof);
    } catch (Exception e) {
      synchronized(streamMap) {
        InputStream is = streamMap.get(uuid);
        if (is != null) {
          StreamUtils.closeSilent(is);
          streamMap.remove(uuid);
        }
      }
      throw e;
    }
  }

  /**
   * Get the current position in the file.
   * @return the position as an int.
   */
  public long getPos() {
    return pos;
  }

  /**
   * Set the current position in the file.
   * @param pos the position as an int.
   */
  public void setPos(final long pos) {
    this.pos = pos;
  }

  /**
   * Determine whether the file was read entirely.
   * @return {@code true} if the whole file was read, {@code false} otherwise.
   */
  public boolean isEof() {
    return eof;
  }

  /**
   * Specify whether the file was read entirely.
   * @param eof {@code true} if the whole file was read, {@code false} otherwise.
   */
  public void setEof(final boolean eof) {
    this.eof = eof;
  }

  /**
   * Get the location of the file to copy in the client file system or classpath.
   * @return the file path as a string.
   */
  public String getClientLocation() {
    return clientLocation;
  }

  /**
   * Get the maximum size of each chunk read fom the file.
   * @return the maximum chunk size as an int.
   */
  public int getMaxChunkSize() {
    return maxChunkSize;
  }

  /**
   * Get the UUID associated with this action.
   * @return the UUID as a string.
   */
  public String getUuid() {
    return uuid;
  }
}
