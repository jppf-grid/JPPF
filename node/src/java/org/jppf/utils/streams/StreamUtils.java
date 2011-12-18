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

package org.jppf.utils.streams;

import java.io.*;

import org.jppf.utils.ExceptionUtils;
import org.slf4j.Logger;

/**
 * Collection of utility methods for manipulating streams.
 * @author Laurent Cohen
 */
public final class StreamUtils
{
  /**
   * Instantiating this class is not permitted.
   */
  private StreamUtils()
  {
  }

  /**
   * Attempt to close the specified input stream without logging an eventual error.
   * @param is the input stream to close.
   * @throws IOException if any error occurs while closing the stream.
   */
  public static void close(final InputStream is) throws IOException
  {
    is.close();
  }

  /**
   * Attempt to close the specified input stream and log any eventual error.
   * @param is the input stream to close.
   * @param log the logger to use; if null no logging occurs.
   */
  public static void close(final InputStream is, final Logger log)
  {
    if (is != null)
    {
      try
      {
        is.close();
      }
      catch (Exception e)
      {
        if (log != null)
        {
          String s = "unable to close input stream: " + ExceptionUtils.getMessage(e);
          if (log.isDebugEnabled()) log.debug(s, e);
          else log.warn(s);
        }
      }
    }
  }

  /**
   * Attempt to close the specified output stream without logging an eventual error.
   * @param os the output stream to close.
   * @throws IOException if any error occurs while closing the stream.
   */
  public static void close(final OutputStream os) throws IOException
  {
    os.close();
  }

  /**
   * Attempt to close the specified output stream and log any eventual error.
   * @param os the output stream to close.
   * @param log the logger to use; if null no logging occurs.
   */
  public static void close(final OutputStream os, final Logger log)
  {
    if (os != null)
    {
      try
      {
        os.close();
      }
      catch (Exception e)
      {
        if (log != null)
        {
          if (log.isDebugEnabled()) log.debug("unable to close output stream", e);
          else log.warn("unable to close output stream: " + e.getClass().getName() + ": " + e.getMessage());
        }
      }
    }
  }
}
