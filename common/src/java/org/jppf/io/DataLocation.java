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

package org.jppf.io;

import java.io.*;


/**
 * This interface represents an abstraction of a block of data, regardless of where it is stored.
 * @author Laurent Cohen
 */
public interface DataLocation
{
  /**
   * Constant for unknown data location size.
   */
  int UNKNOWN_SIZE = -1;
  /**
   * Get the size of the data referenced by this data location.
   * @return the data size as an int.
   */
  int getSize();
  /**
   * Transfer the content of this data location from the specified input source.
   * @param source - the input source to transfer to.
   * @param blocking - if true, the method will block until the entire content has been transferred.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  int transferFrom(InputSource source, boolean blocking) throws Exception;
  /**
   * Transfer the content of this data location to the specified output destination.
   * @param dest - the output destination to transfer to.
   * @param blocking - if true, the method will block until the entire content has been transferred.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  int transferTo(OutputDestination dest, boolean blocking) throws Exception;
  /**
   * Get an input stream for this location.
   * @return an <code>InputStream</code> instance.
   * @throws Exception if an I/O error occurs.
   */
  InputStream getInputStream() throws Exception;
  /**
   * Get an output stream for this location.
   * @return an <code>OutputStream</code> instance.
   * @throws Exception if an I/O error occurs.
   */
  OutputStream getOutputStream() throws Exception;
  /**
   * Make a shallow copy of this data location.
   * The data it points to is not copied.
   * @return a new DataLocation instance pointing to the same data.
   */
  DataLocation copy();
}
