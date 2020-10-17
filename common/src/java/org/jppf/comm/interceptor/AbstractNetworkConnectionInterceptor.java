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

import java.nio.channels.SocketChannel;

import org.jppf.utils.JPPFChannelDescriptor;
import org.slf4j.*;

/**
 * An abstract interceptor implementation which creates or obtains streams from the Socket or SocketChannel
 * passed on to its methods.
 * @author Laurent Cohen
 */
public abstract class AbstractNetworkConnectionInterceptor implements NetworkConnectionInterceptor {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractNetworkConnectionInterceptor.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  @Override
  public boolean onAccept(final SocketChannel acceptedChannel, final JPPFChannelDescriptor descriptor) {
    if (debugEnabled) log.debug("channel = {}, descriptor = {}", acceptedChannel, descriptor);
    return onAccept(acceptedChannel.socket(), descriptor);
  }

  @Override
  public boolean onConnect(final SocketChannel connectedChannel, final JPPFChannelDescriptor descriptor) {
    if (debugEnabled) log.debug("channel = {}, descriptor = {}", connectedChannel, descriptor);
    return onConnect(connectedChannel.socket(), descriptor);
  }
}
