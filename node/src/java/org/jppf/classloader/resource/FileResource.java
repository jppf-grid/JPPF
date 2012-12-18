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
 * Abstraction for a resource stored into a file.
 * This implementation of the {@link Resource} interface allows writing to and reading from a file.
 * @author Laurent Cohen
 */
public class FileResource extends AbstractResource<File>
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this location with the specified file path.
   * @param file an abstract file path.
   */
  public FileResource(final File file)
  {
    super(file);
  }

  /**
   * Initialize this location with the specified file path.
   * @param file an abstract file path.
   */
  public FileResource(final String file)
  {
    super(new File(file));
  }

  @Override
  public InputStream getInputStream() throws Exception
  {
    return new BufferedInputStream(new FileInputStream(path));
  }

  @Override
  public OutputStream getOutputStream() throws Exception
  {
    return new BufferedOutputStream(new FileOutputStream(path));
  }

  @Override
  public long size()
  {
    if ((path != null) && path.exists()) return path.length();
    return -1;
  }
}
