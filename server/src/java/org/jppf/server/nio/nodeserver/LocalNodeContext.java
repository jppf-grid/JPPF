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

package org.jppf.server.nio.nodeserver;

import org.jppf.server.nio.*;
import org.slf4j.*;


/**
 * Context associated with a channel serving tasks to a local (in-VM) node.
 * @author Laurent Cohen
 */
public class LocalNodeContext extends AbstractNodeContext
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LocalNodeContext.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * {@inheritDoc}.
   */
  @Override
  public AbstractTaskBundleMessage newMessage()
  {
    return new LocalNodeMessage();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readMessage(final ChannelWrapper<?> channel) throws Exception
  {
    if (debugEnabled) log.debug("reading message from " + channel);
    LocalNodeChannel handler = (LocalNodeChannel) channel;
    while (handler.getServerResource() == null) handler.getServerLock().goToSleep();
    setMessage(handler.getServerResource());
    handler.setServerResource(null);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean writeMessage(final ChannelWrapper<?> channel) throws Exception
  {
    LocalNodeChannel handler = (LocalNodeChannel) channel;
    boolean b = super.writeMessage(channel);
    if (debugEnabled) log.debug("wrote " + message + " to " + channel);
    handler.setNodeResource((LocalNodeMessage) message);
    return b;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMessage(final NioMessage nodeMessage)
  {
    super.setMessage(nodeMessage);
    ((LocalNodeChannel) getChannel()).wakeUp();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setState(final NodeState state)
  {
    if (NodeState.SENDING_BUNDLE.equals(this.state) && NodeState.IDLE.equals(state))
    {
      log.debug("debug stack", new Exception());
      int breakpoint = 0;
    }
    super.setState(state);
  }
}
