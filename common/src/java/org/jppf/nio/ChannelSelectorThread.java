/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.nio;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.slf4j.*;

/**
 * Instances of this class perform the selection loop for a local (in-VM) channel.
 * @author Laurent Cohen
 */
public class ChannelSelectorThread extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ChannelSelectorThread.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The channel selector associated with this thread.
   */
  private final ChannelSelector selector;
  /**
   * The nio server that own this thread.
   */
  private final NioServer<?, ?> server;
  /**
   * The maximum time a select() operation can block.
   */
  private final long timeout;

  /**
   * Initialize this thread with the specified name, selector and NIO server.
   * @param selector the channel selector associated with this thread.
   * @param server the nio server that own this thread.
   */
  public ChannelSelectorThread(final ChannelSelector selector, final NioServer<?, ?> server)
  {
    this(selector, server, 0L);
  }

  /**
   * Initialize this thread with the specified name, selector and NIO server.
   * @param selector the channel selector associated with this thread.
   * @param server the nio server that own this thread.
   * @param timeout the maximum time a select() operation can block.
   */
  public ChannelSelectorThread(final ChannelSelector selector, final NioServer<?, ?> server, final long timeout)
  {
    if (timeout < 0L) throw new IllegalArgumentException("timeout must be >= 0");
    this.selector = selector;
    this.server = server;
    this.timeout = timeout;
  }

  /**
   * Perform the selection loop.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    while (!isStopped())
    {
      if (selector.select(timeout))
      {
        ChannelWrapper<?> channel = selector.getChannel();
        synchronized(channel)
        {
          if (debugEnabled) log.debug("selected channel " + channel);
          server.getTransitionManager().submitTransition(channel);
        }
      }
    }
  }

  /**
   * Closes this channel selector. If <code>close()</code> was already called, then this method has no effect.
   */
  public void close()
  {
    setStopped(true);
    selector.wakeUp();
  }
}
