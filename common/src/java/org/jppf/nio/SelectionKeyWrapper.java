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

import java.nio.channels.*;

/**
 * Channel wrapper implementation for a {@link SelectionKey}.
 * @author Laurent Cohen
 */
public class SelectionKeyWrapper extends AbstractChannelWrapper<SelectionKey> {
  /**
   * Initialize this channel wrapper with the specified channel.
   * @param channel the channel to wrap.
   */
  public SelectionKeyWrapper(final SelectionKey channel) {
    super(channel);
  }

  @Override
  public NioContext<?> getContext() {
    return (NioContext<?>) channel.attachment();
  }

  /**
   * Close the channel.
   * @throws Exception if any error occurs while closing the channel.
   */
  @Override
  public void close() throws Exception {
    final SelectableChannel ch = channel.channel();
    channel.cancel();
    ch.close();
  }

  /**
   * Determine whether the channel is opened.
   * @return true if the channel is opened, false otherwise.
   */
  @Override
  public boolean isOpen() {
    return channel.channel().isOpen();
  }

  /**
   * Generate a string that represents this channel wrapper.
   * @return a string that represents this channel wrapper.
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(1000);
    sb.append(getClass().getSimpleName());
    sb.append('[');
    sb.append("id=").append(getId());
    sb.append(", channel=");
    if (channel == null) sb.append("null");
    else {
      if (!channel.isValid() || !isOpen()) sb.append("invalid channel");
      else {
        sb.append(channel.channel());
        sb.append(", readyOps=").append(getReadyOps());
        sb.append(", interestOps=").append(getInterestOps());
      }
    }
    sb.append(", context=").append(getContext());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the operations enabled for this channel.
   * @return the operations as an int value.
   */
  @Override
  public int getInterestOps() {
    return channel.isValid() ? channel.interestOps() : -1;
  }

  /**
   * Get the operations enabled for this channel.
   * @param keyOps the operations as an int value.
   */
  @Override
  public void setInterestOps(final int keyOps) {
    if (channel.isValid()) channel.interestOps(keyOps);
  }

  /**
   * Get the operations available for this channel.
   * @return the operations as an int value.
   */
  @Override
  public int getReadyOps() {
    return channel.isValid() ? channel.readyOps() : -1;
  }

  /**
   * @return {@code false}.
   */
  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public SocketChannel getSocketChannel() {
    return (SocketChannel) channel.channel();
  }
}
