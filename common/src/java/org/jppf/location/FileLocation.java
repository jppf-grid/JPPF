/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * Wrapper for manipulating a file.
 * This implementation of the {@link Location} interface allows writing to and reading from a file.
 * @author Laurent Cohen
 */
public class FileLocation extends AbstractLocation<String>
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The file size.
   */
  private long size = -1L;

  /**
   * Initialize this location with the specified file path.
   * <p><b>Warning</b>: if this file location is intended for use on a remote machine,
   * then you should use {@link #FileLocation(java.lang.String) FileLocation(String)} instead,
   * since this constructor computes the path as the system-dependent {@link File#getCanonicalPath() canonical path} of the argument.
   * @param file an abstract file path.
   * @throws IOException if any I/O error occurs.
   */
  public FileLocation(final File file) throws IOException
  {
    super(file.getCanonicalPath());
  }

  /**
   * Initialize this location with the specified file path.
   * @param file a string representing the file path.
   */
  public FileLocation(final String file)
  {
    super(file);
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

  /**
   * Get the size of the file this location points to.
   * @return the size as a long value, or -1 if the file does not exist.
   */
  @Override
  public long size()
  {
    if (size < 0)
    {
      File file = new File(path);
      if (file.exists()) size = file.length();
    }
    return size;
  }
}
