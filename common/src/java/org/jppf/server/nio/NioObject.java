/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import org.jppf.io.DataLocation;

/**
 * Abstraction of a sequence of data read from, or written to a data channel.
 * The size of the data is known before the read or write operation.
 * <p>Furthermore, multiple calls to the {@link #read()} or {@link #write()} method may be required
 * for the read or write operation to complete. This implies that an implementation of this interface
 * should keep state information between calls. 
 * @author Laurent Cohen
 */
public interface NioObject
{
  /**
   * Attempt to read an object from an inbound channel.
   * @return <code>true</code> if an object was fully read, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  boolean read() throws Exception;

  /**
   * Attempt to write an object to an outbound channel.
   * @return <code>true</code> if an object was fully written, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  boolean write() throws Exception;

  /**
   * Location of the data to read or write.
   * @return a <code>DataLocation</code> instance.
   */
  DataLocation getData();

  /**
   * Get the size of the data ot send or receive.
   * @return the size as an int.
   */
  int getSize();

  /**
   * Get the actual bytes sent to or received from the underlying channel.
   * @return the number of bytes as a long value.
   */
  long getChannelCount();
}
