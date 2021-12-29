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

package org.jppf.example.interceptor.auth;

import java.io.IOException;
import java.net.Socket;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * A callback handler used to transport a socket conneciton between login context and login module.
 */
public class InterceptorCallbackHandler implements CallbackHandler {
  /**
   * The socket holding the connection to the jppf peer.
   */
  private final Socket socket;

  /**
   * Initialize this callback handler with the specified socket connection.
   * @param socket the socket holding the connection to the jppf peer.
   */
  public InterceptorCallbackHandler(final Socket socket) {
    this.socket = socket;
  }

  @Override
  public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    for (final Callback callback: callbacks) {
      if (callback instanceof SocketCallback)
        ((SocketCallback) callback).setSocket(socket);
    }
  }

  /**
   * Get the specified socket connection to which authentication is delegated.
   * @return the socket holding the connection to the jppf peer.
   */
  public Socket getSocket() {
    return socket;
  }
}
