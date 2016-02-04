/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.classloader;

import static org.jppf.utils.StringUtils.build;

import java.nio.channels.SelectionKey;

import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Encapsulates a remote resource request submitted asynchronously
 * via the single-thread executor.
 */
class LocalResourceRequest extends AbstractResourceRequest
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LocalResourceRequest.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The channel used by the local node's class loader.
   */
  private final LocalClassLoaderChannel channel;

  /**
   * Initialize.
   * @param channel the channel used by the local node's class loader.
   */
  public LocalResourceRequest(final LocalClassLoaderChannel channel)
  {
    if (channel == null) throw new IllegalArgumentException("channel is null");
    
    this.channel = channel;
  }

  @Override
  public void run()
  {
    try
    {
      throwable = null;
      if (debugEnabled) log.debug(build("channel ", channel, " sending request ", request));
      synchronized(channel.getServerLock())
      {
        channel.setServerResource(request);
        channel.setReadyOps(SelectionKey.OP_READ);
        while (channel.getServerResource() != null) channel.getServerLock().goToSleep();
      }
      synchronized(channel.getNodeLock())
      {
        channel.setReadyOps(SelectionKey.OP_WRITE);
        while ((response = channel.getNodeResource()) == null) channel.getNodeLock().goToSleep();
        channel.setNodeResource(null);
      }
      if (debugEnabled) log.debug(build("channel ", channel, " got response ", response));
    }
    catch (Throwable t)
    {
      throwable = t;
    }
  }
}
