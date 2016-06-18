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

import java.io.*;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.jppf.utils.streams.*;

/**
 * An abstract interceptor implementation which creates or obtains streams from the Socket or SocketChannel
 * passed on to its methods.
 * @author Laurent Cohen
 */
public abstract class AbstractNetworkConnectionInterceptor implements NetworkConnectionInterceptor {
  /**
   * Perform the interceptor's job on the specified accepted {@link Socket} or {@link SocketChannel} streams.
   * @param is the channel's input stream to read from.
   * @param os the channel's output stream to write to.
   * @return {@code true} to accept the connection {@code false} to deny it.
   */
  protected abstract boolean onAccept(final InputStream is, final OutputStream os);

  /**
   * Perform the interceptor's job on the specified connected {@link Socket} or {@link SocketChannel} streams.
   * @param is the channel's input stream to read from.
   * @param os the channel's output stream to write to.
   * @return {@code true} to accept the connection {@code false} to deny it.
   */
  protected abstract boolean onConnect(final InputStream is, final OutputStream os);

  @Override
  public boolean onAccept(final Socket acceptedSocket) {
    try {
      return onAccept(acceptedSocket.getInputStream(), acceptedSocket.getOutputStream());
    } catch(IOException e) {
      return false;
    }
  }

  @Override
  public boolean onAccept(final SocketChannel acceptedChannel) {
    return onAccept(new ChannelInputStream(acceptedChannel), new ChannelOutputStream(acceptedChannel));
  }

  @Override
  public boolean onConnect(final Socket connectedSocket) {
    try {
      return onConnect(connectedSocket.getInputStream(), connectedSocket.getOutputStream());
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public boolean onConnect(final SocketChannel connectedChannel) {
    return onConnect(new ChannelInputStream(connectedChannel), new ChannelOutputStream(connectedChannel));
  }
}
