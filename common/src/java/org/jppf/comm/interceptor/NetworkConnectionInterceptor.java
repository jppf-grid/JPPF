/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.comm.interceptor;

import java.net.*;
import java.nio.channels.*;
import java.util.List;

import org.jppf.utils.JPPFChannelDescriptor;

/**
 * Implementations of this interface enable user-defined code to use the underlying network connection
 * to perform network I/O, typically implementing an authentication protocol.
 * @author Laurent Cohen
 * @since 5.2
 */
public interface NetworkConnectionInterceptor {
  /**
   * The list of interceptors loaded via SPI, possibly empty. This list is unmodifiable and any attempt
   * to modify it will raise an {@code UnsupportOperationException}.
   */
  List<NetworkConnectionInterceptor> INTERCEPTORS = InterceptorHandler.INTERCEPTORS;

  /**
   * Called when a {@link Socket} is accepted by a {@link ServerSocket} on the server side of a connection.
   * @param acceptedSocket the socket that was just accepted.
   * @return {@code true} to accept the connection {@code false} to deny it.
   * @deprecated use {@link #onAccept(Socket, JPPFChannelDescriptor)} instead.
   */
  default boolean onAccept(Socket acceptedSocket) {
    return onAccept(acceptedSocket, JPPFChannelDescriptor.UNKNOWN);
  }

  /**
   * Called when a {@link Socket} is accepted by a {@link ServerSocket} on the server side of a connection.
   * @param acceptedSocket the socket that was just accepted.
   * @param descriptor provides information on the accepted socket.
   * @return {@code true} to accept the connection {@code false} to deny it.
   * @since 6.3
   */
  boolean onAccept(Socket acceptedSocket, JPPFChannelDescriptor descriptor);

  /**
   * Called when a {@link SocketChannel} is accepted by a {@link ServerSocketChannel} on the server side of a connection.
   * @param acceptedChannel the socket that was just accepted.
   * @return {@code true} to accept the connection {@code false} to deny it.
   * @deprecated use {@link #onAccept(SocketChannel, JPPFChannelDescriptor)} instead.
   */
  default boolean onAccept(SocketChannel acceptedChannel) {
    return onAccept(acceptedChannel, JPPFChannelDescriptor.UNKNOWN);
  }

  /**
   * Called when a {@link SocketChannel} is accepted by a {@link ServerSocketChannel} on the server side of a connection.
   * @param acceptedChannel the socket that was just accepted.
   * @param descriptor provides information on the accepted socket channel.
   * @return {@code true} to accept the connection {@code false} to deny it.
   * @since 6.3
   */
  boolean onAccept(SocketChannel acceptedChannel, JPPFChannelDescriptor descriptor);

  /**
   * Called when a {@link Socket} is connected on the client side of a connection.
   * @param connectedSocket the socket that just connected.
   * @return {@code true} to accept the connection {@code false} to deny it.
   * @deprecated use {@link #onConnect(Socket, JPPFChannelDescriptor)} instead.
   */
  default boolean onConnect(Socket connectedSocket) {
    return onConnect(connectedSocket, JPPFChannelDescriptor.UNKNOWN);
  }

  /**
   * Called when a {@link Socket} is connected on the client side of a connection.
   * @param connectedSocket the socket that just connected.
   * @param descriptor provides information on the connected socket.
   * @return {@code true} to accept the connection {@code false} to deny it.
   * @since 6.3
   */
  boolean onConnect(Socket connectedSocket, JPPFChannelDescriptor descriptor);

  /**
   * Called when a {@link SocketChannel} is connected on the client side of a connection.
   * @param connectedChannel the channel that just connected.
   * @return {@code true} to accept the connection {@code false} to deny it.
   * @deprecated use {@link #onConnect(SocketChannel, JPPFChannelDescriptor)} instead.
   */
  default boolean onConnect(SocketChannel connectedChannel) {
    return onConnect(connectedChannel, JPPFChannelDescriptor.UNKNOWN);
  }

  /**
   * Called when a {@link SocketChannel} is connected on the client side of a connection.
   * @param connectedChannel the channel that just connected.
   * @param descriptor provides information on the connected socket channel.
   * @return {@code true} to accept the connection {@code false} to deny it.
   * @since 6.3
   */
  boolean onConnect(SocketChannel connectedChannel, JPPFChannelDescriptor descriptor);
}
