/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;



/**
 * State associated with a socket channel.
 * @param <T> the type of transitions for this state.
 * @author Laurent Cohen
 */
public abstract class NioState<T extends Enum<T>> {
  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   */
  public abstract T performTransition(ChannelWrapper<?> channel) throws Exception;

  /**
   * Extract the remote host name from the specified channel.
   * @param channel the channel that carries the host information.
   * @return the remote host name as a string.
   * @throws Exception if any error occurs.
   */
  public static String getChannelHost(final ChannelWrapper<?> channel) throws Exception {
    SocketChannel ch = getSocketChannel(channel);
    return  (ch == null) ? null : ((InetSocketAddress) (ch.getRemoteAddress())).getHostString();
  }

  /**
   * Extract the remote host name from the specified channel.
   * @param channel the channel that carries the host information.
   * @return the remote host name as a string.
   * @throws Exception if any error occurs.
   */
  public static String getSocketChannelAsString(final ChannelWrapper<?> channel) throws Exception {
    SocketChannel ch = getSocketChannel(channel);
    if (ch == null) return "null";
    return ch.socket().toString();
  }

  /**
   * Extract the socket channel from the specified channel wrapper.
   * @param channel the channel that carries the host information.
   * @return a {@link SocketChannel} instance, or {@code null} if the underlying channel is not a {@code SocketChannel}.
   * @throws Exception if any error occurs.
   */
  public static SocketChannel getSocketChannel(final ChannelWrapper<?> channel) throws Exception {
    if (channel instanceof SelectionKeyWrapper) {
      SelectionKeyWrapper skw = (SelectionKeyWrapper) channel;
      return  (SocketChannel) skw.getChannel().channel();
    }
    return null;
  }
}
