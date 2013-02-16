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

package org.jppf.classloader.resource;

import java.io.*;

import org.jppf.utils.streams.*;

/**
 * Instances of this class represent the location of an artifact, generally a file or the data found at a url.
 * @param <T> the type of this location.
 * @author Laurent Cohen
 */
public abstract class AbstractResource<T> implements Serializable, Resource<T>
{
  /**
   * The path for this location.
   */
  protected T path = null;

  /**
   * Initialize this location with the specified type and path.
   * @param path the path for this location.
   */
  public AbstractResource(final T path)
  {
    this.path = path;
  }

  @Override
  public T getPath()
  {
    return path;
  }

  @Override
  public void copyTo(final Resource location) throws Exception
  {
    InputStream is = getInputStream();
    OutputStream os = location.getOutputStream();
    copyStream(is, os);
    is.close();
    os.flush();
    os.close();
  }

  @Override
  public byte[] toByteArray() throws Exception
  {
    InputStream is = getInputStream();
    JPPFByteArrayOutputStream os = new JPPFByteArrayOutputStream();
    copyStream(is, os);
    is.close();
    os.flush();
    os.close();
    return os.toByteArray();
  }

  @Override
  public String toString()
  {
    return String.valueOf(getPath());
  }

  /**
   * Copy the data read from the specified input stream to the specified output stream.
   * @param is the input stream to read from.
   * @param os the output stream to write to.
   * @throws IOException if an I/O error occurs.
   */
  private void copyStream(final InputStream is, final OutputStream os) throws IOException
  {
    byte[] bytes = new byte[StreamConstants.TEMP_BUFFER_SIZE];
    while(true)
    {
      int n = is.read(bytes);
      if (n <= 0) break;
      os.write(bytes, 0, n);
    }
  }
}
