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

package org.jppf.comm.interceptor;

import java.nio.channels.SocketChannel;

/**
 * An abstract interceptor implementation which creates or obtains streams from the Socket or SocketChannel
 * passed on to its methods.
 * @author Laurent Cohen
 */
public abstract class AbstractNetworkConnectionInterceptor implements NetworkConnectionInterceptor {
  @Override
  public boolean onAccept(final SocketChannel acceptedChannel) {
    return onAccept(acceptedChannel.socket());
  }

  @Override
  public boolean onConnect(final SocketChannel connectedChannel) {
    return onConnect(connectedChannel.socket());
  }
}
