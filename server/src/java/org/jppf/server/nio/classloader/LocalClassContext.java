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

package org.jppf.server.nio.classloader;

import org.jppf.classloader.*;
import org.jppf.server.nio.ChannelWrapper;
import org.slf4j.*;

/**
 * Context object associated with a socket channel used by the class server of the JPPF driver.
 * @author Laurent Cohen
 */
public class LocalClassContext extends ClassContext
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LocalClassContext.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();

  @Override
  public void serializeResource() throws Exception
  {
  }

  @Override
  public JPPFResourceWrapper deserializeResource() throws Exception
  {
    return resource;
  }

  /**
   * Read data from a channel.
   * @param wrapper the channel to read the data from.
   * @return true if all the data has been read, false otherwise.
   * @throws Exception if an error occurs while reading the data.
   */
  @Override
  public boolean readMessage(final ChannelWrapper<?> wrapper) throws Exception
  {
    LocalClassLoaderChannel channel = (LocalClassLoaderChannel) wrapper;
    if (traceEnabled) log.trace("reading message for " + wrapper + ", message = " + message);
    JPPFResourceWrapper res;
    synchronized(channel.getServerLock())
    {
      while ((res = channel.getServerResource()) == null) channel.getServerLock().goToSleep();
      channel.setServerResource(null);
    }
    resource = res;
    if (traceEnabled) log.trace("message read for " + wrapper);
    return true;
  }

  /**
   * Write data to a channel.
   * @param wrapper the channel to write the data to.
   * @return true if all the data has been written, false otherwise.
   * @throws Exception if an error occurs while writing the data.
   */
  @Override
  public boolean writeMessage(final ChannelWrapper<?> wrapper) throws Exception
  {
    if (traceEnabled) log.trace("writing message for " + wrapper + ", resource=" + resource);
    LocalClassLoaderChannel channel = (LocalClassLoaderChannel) wrapper;
    channel.setNodeResource(resource);
    if (traceEnabled) log.trace("message written for " + wrapper + ", resource=" + resource);
    return true;
  }
}
