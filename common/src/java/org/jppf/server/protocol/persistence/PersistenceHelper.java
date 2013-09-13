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

package org.jppf.server.protocol.persistence;

import org.jppf.io.*;
import org.jppf.node.protocol.*;

/**
 * Utility methods to help persisting/retrieving server jobs and converting them to persistable objects.
 * @author Laurent Cohen
 * @exclude
 */
public class PersistenceHelper
{
  /**
   * Convert the specified {@link DataLocation} object to a {@link Location} that can be used by users.
   * @param dataLocation the data location object to convert.
   * @return a {@link Location} instance pointing to the same data as the original data location.
   */
  public static Location toLocation(final DataLocation dataLocation)
  {
    Location loc = null;
    if (dataLocation instanceof FileDataLocation) loc = new FileLocation(((FileDataLocation) dataLocation).getFilePath());
    else if (dataLocation instanceof MultipleBuffersLocation) loc = new BufferListLocation(((MultipleBuffersLocation) dataLocation).getBufferList());
    return loc;
  }

  /**
   * Convert the specified {@link Location} object to a {@link DataLocation} that can be used by users.
   * @param location the location object to convert.
   * @return a {@link DataLocation} instance pointing to the same data as the original data location.
   */
  public static DataLocation toDataLocation(final Location location)
  {
    DataLocation dl = null;
    if (location instanceof FileLocation) dl = new FileDataLocation(((FileLocation) location).getPath());
    else if (location instanceof BufferListLocation) dl = new MultipleBuffersLocation(((BufferListLocation) location).getPath());
    else if (location instanceof MemoryLocation) dl = new MultipleBuffersLocation(((MemoryLocation) location).toByteArray());
    return dl;
  }
}
