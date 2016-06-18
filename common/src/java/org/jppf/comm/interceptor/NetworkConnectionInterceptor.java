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

package org.jppf.comm.interceptor;

import java.net.*;
import java.nio.channels.*;

/**
 * Implementations of this interface enable user-defined code to use the underlying network connection
 * to perform network I/O, typically implementing an authentication protocol.
 * @author Laurent Cohen
 * @since 5.2
 */
public interface NetworkConnectionInterceptor {
  /**
   * Called when a {@link Socket} is accepted by a {@link ServerSocket} on the server side of a connection.
   * @param acceptedSocket the socket that was just accepted.
   * @return {@code true} to accept the connection {@code false} to deny it.
   */
  boolean onAccept(Socket acceptedSocket);

  /**
   * Called when a {@link SocketChannel} is accepted by a {@link ServerSocketChannel} on the server side of a connection.
   * @param acceptedChannel the socket that was just accepted.
   * @return {@code true} to accept the connection {@code false} to deny it.
   */
  boolean onAccept(SocketChannel acceptedChannel);

  /**
   * Called when a {@link Socket} is connected on the client side of a connection.
   * @param connectedSocket the socket that just connected.
   * @return {@code true} to accept the connection {@code false} to deny it.
   */
  boolean onConnect(Socket connectedSocket);

  /**
   * Called when a {@link SocketChannel} is connected on the client side of a connection.
   * @param connectedChannel the channel that just connected.
   * @return {@code true} to accept the connection {@code false} to deny it.
   */
  boolean onConnect(SocketChannel connectedChannel);
}
