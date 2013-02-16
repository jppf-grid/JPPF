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
import org.slf4j.*;

/**
 * Abstract implementation of the {@link NioObject} interface, providing the means to keep stateful information
 * across multiple calls to the <code>read()</code> or <code>write()</code> methods.
 * @author Laurent Cohen
 */
public abstract class AbstractNioObject implements NioObject
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNioObject.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The message length.
   */
  protected int size = 0;
  /**
   * What has currently been read from the message.
   */
  protected int count = 0;
  /**
   * Location of the data to read or write.
   */
  protected DataLocation location = null;
  /**
   * Determines whether we are in plain or SSL mode.
   */
  protected boolean isSSL = false;

  /**
   * Location of the data to read or write.
   * @return a <code>DataLocation</code> instance.
   */
  @Override
  public DataLocation getData()
  {
    return location;
  }

  /**
   * Number of bytes read from or written to the message.
   * @return  the number of bytes as an int.
   */
  public int getCount()
  {
    return count;
  }
}
