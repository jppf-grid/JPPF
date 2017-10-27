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

package org.jppf.jmxremote.nio;

import java.nio.channels.SocketChannel;

import org.jppf.nio.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXChannelWrapper extends AbstractChannelWrapper<Void> {
  /**
   * The associated context.
   */
  private final JMXContext context;
  /**
   * The associated socket channel.
   */
  private final SocketChannel socketChannel;

  /**
   * 
   * @param channel the channel.
   * @param socketChannel the associated socket channel.
   */
  public JMXChannelWrapper(final JMXContext context, final SocketChannel socketChannel) {
    super(null);
    this.context = context;
    this.socketChannel = socketChannel;    
  }

  @Override
  public int getReadyOps() {
    return 0;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public NioContext<?> getContext() {
    return context;
  }

  @Override
  public SocketChannel getSocketChannel() {
    return socketChannel;
  }
}
