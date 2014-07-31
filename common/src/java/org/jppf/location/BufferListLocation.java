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

package org.jppf.location;

import java.io.*;
import java.util.List;

import org.jppf.utils.JPPFBuffer;
import org.jppf.utils.streams.*;

/**
 * An in-memory location based on a list of buffers.
 * @author Laurent Cohen
 * @exclude
 */
public class BufferListLocation extends AbstractLocation<List<JPPFBuffer>>
{
  /**
   * The size of the data.
   */
  private long length = 0L;

  /**
   * Initialize this location.
   * @param path the lis of buffers containing the data.
   */
  public BufferListLocation(final List<JPPFBuffer> path)
  {
    super(path);
    for (JPPFBuffer buffer: path) length += buffer.length; 
  }

  @Override
  public InputStream getInputStream() throws Exception
  {
    return new MultipleBuffersInputStream(path);
  }

  @Override
  public OutputStream getOutputStream() throws Exception
  {
    return new MultipleBuffersOutputStream(path);
  }

  @Override
  public long size()
  {
    return length;
  }
}
