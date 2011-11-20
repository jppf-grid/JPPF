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

package org.jppf.server.nio.multiplexer;

import org.jppf.server.nio.AbstractSocketChannelHandler;
import org.slf4j.*;

/**
 * Instances of this class act as a separate thread wrapper around a channel handler.
 * @author Laurent Cohen
 */
public class MultiplexerChannelInitializer implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MultiplexerChannelInitializer.class);
  /**
   * Wrapper for the new connection to establish.
   */
  private AbstractSocketChannelHandler channelHandler = null;

  /**
   * Instantiate this initializer with the specified parameters.
   * @param channelHandler wrapper for the new connection to establish.
   */
  public MultiplexerChannelInitializer(final AbstractSocketChannelHandler channelHandler)
  {
    this.channelHandler = channelHandler;
  }

  /**
   * Perform the channel initialization.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      channelHandler.init();
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }
}
