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

package org.jppf.server.nio.client;

import org.jppf.server.nio.*;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * Completion listener that is used to notify that results were received from a node,
 * and they should be sent back to the client.
 * @author Laurent Cohen
 */
public class CompletionListener implements TaskCompletionListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CompletionListener.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The transition manager for the server to client channels.
   */
  private final StateTransitionManager transitionManager;
  /**
   * The client channel.
   */
  private final ChannelWrapper<?> channel;

  /**
   * Initialize this completion listener with the specified channel.
   * @param channel the client channel.
   */
  public CompletionListener(final ChannelWrapper<?> channel, final StateTransitionManager transitionManager)
  {
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (transitionManager == null) throw new IllegalArgumentException("transitionManager is null");

    this.channel = channel;
    this.transitionManager = transitionManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public void taskCompleted(final ServerJob result)
  {
    ClientContext context = (ClientContext) channel.getContext();
    synchronized(channel)
    {
      context.offerCompletedBundle(result);
      if (ClientState.IDLE.equals(context.getState()))
      {
        try
        {
          transitionManager.transitionChannel(channel, ClientTransition.TO_SENDING_RESULTS);
        }
        catch(Exception e)
        {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.info(e.getClass().getName() + " : " + e.getMessage());
        }
      }
    }
  }
}
