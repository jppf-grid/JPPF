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

package org.jppf.classloader.resource;

import java.io.*;

/**
 * Abstraction of a block of data no matter how its actual location is referred to.
 * <p>The idea is to enable I/O operations with a very simple API between <code>Location</code> instances,
 * for instance between a URL and File locations to download a file from the internet and save it locally.
 * @param <T> the type of this location.
 * @author Laurent Cohen
 */
public interface Resource<T>
{
  /**
   * Get the path for this location.
   * @return the path.
   */
  T getPath();
  /**
   * Obtain an input stream to read from this resource.
   * @return an <code>InputStream</code> instance.
   * @throws Exception if an I/O error occurs.
   */
  InputStream getInputStream() throws Exception;
  /**
   * Obtain an output stream to write to this resource.
   * @return an <code>OutputStream</code> instance.
   * @throws Exception if an I/O error occurs.
   */
  OutputStream getOutputStream() throws Exception;
  /**
   * Copy the content of this resource into another resource.
   * @param location the location to copy to.
   * @throws Exception if an I/O error occurs.
   */
  void copyTo(Resource location) throws Exception;
  /**
   * Get the size of the data this resource points to.
   * @return the size as a long value, or -1 if the size is not available.
   */
  long size();
  /**
   * Get the content of this resource as an array of bytes.
   * @return a byte array.
   * @throws Exception if an I/O error occurs.
   */
  byte[] toByteArray() throws Exception;
}
