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

package org.jppf.location;

import java.io.*;

/**
 * Abstraction of a block of data no matter how its actual location is referred to.
 * <p>The idea is to enable I/O operations with a very simple API between <code>Location</code> instances,
 * for instance between a URL and File locations to download a file from the internet and save it locally.
 * @param <T> the type of this location.
 * @author Laurent Cohen
 */
public interface Location<T> extends Serializable {
  /**
   * Get the path for this location.
   * @return the path as a string.
   */
  T getPath();

  /**
   * Obtain an input stream to read from this location.
   * @return an <code>InputStream</code> instance.
   * @throws Exception if an I/O error occurs.
   */
  InputStream getInputStream() throws Exception;

  /**
   * Obtain an output stream to write to this location.
   * @return an <code>OutputStream</code> instance.
   * @throws Exception if an I/O error occurs.
   */
  OutputStream getOutputStream() throws Exception;

  /**
   * Copy the content at this location to another location.
   * @param <V> the type of path for the destination location.
   * @param location the destination location to copy to.
   * @return the destination location.
   * @throws Exception if an I/O error occurs.
   */
  <V> Location<V> copyTo(Location<V> location) throws Exception;

  /**
   * Get the size of the data this location points to.
   * @return the size as a long value, or -1 if the size is not available.
   */
  long size();

  /**
   * Get the content at this location as an array of bytes.
   * @return a byte array.
   * @throws Exception if an I/O error occurs.
   */
  byte[] toByteArray() throws Exception;

  /**
   * Add a listener to the list of location event listeners for this location.
   * @param listener the listener to add to the list.
   * @throws IllegalArgumentException if the listener object is null.
   */
  void addLocationEventListener(LocationEventListener listener);

  /**
   * Remove a listener from the list of location event listeners for this location.
   * @param listener the listener to remove from the list.
   * @throws IllegalArgumentException if the listener object is null.
   */
  void removeLocationEventListener(LocationEventListener listener);
}
