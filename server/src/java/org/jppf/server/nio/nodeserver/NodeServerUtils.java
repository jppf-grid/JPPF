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

package org.jppf.server.nio.nodeserver;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.nio.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * Collection of utility methods to help with job data service.
 * @author Laurent Cohen
 */
class NodeServerUtils {
  /**
   * Compute a repeatable unique identifier for a node, which can be reused over node restarts.
   * @param factory bundler (load-balancer) factory.
   * @param channel the channel that carries the host information.
   * @param info the system information for the node.
   * @return a pair of string representing the clear string (keft side) and resulting unique string identifier for the node (right side).
   * @throws Exception if any error occurs.
   */
  static Pair<String, String> getNodeIdentifier(final JPPFBundlerFactory factory, final ChannelWrapper<?> channel, final JPPFSystemInformation info) throws Exception {
    StringBuilder sb = new StringBuilder();
    String ip = NetworkUtils.getNonLocalHostAddress();
    sb.append('[').append(ip == null ? "localhost" : ip);
    if (channel instanceof SelectionKeyWrapper) {
      SelectionKeyWrapper skw = (SelectionKeyWrapper) channel;
      SocketChannel ch = (SocketChannel) skw.getChannel().channel();
      sb.append(':').append(ch.socket().getLocalPort()).append(']');
      InetSocketAddress isa = (InetSocketAddress) ch.getRemoteAddress();
      sb.append(isa.getAddress().getHostAddress());
    } else if (channel.isLocal()) {
      sb.append( "local_channel").append(']');
    }
    TypedProperties jppf = info.getJppf();
    boolean master = jppf.get(JPPFProperties.PROVISIONING_MASTER);
    boolean slave = jppf.get(JPPFProperties.PROVISIONING_SLAVE);
    if (master || slave) {
      sb.append(master ? "master" : "slave");
      sb.append(jppf.get(JPPFProperties.PROVISIONING_SLAVE_PATH_PREFIX));
      if (slave) sb.append(jppf.get(JPPFProperties.PROVISIONING_SLAVE_ID));
    }
    String s = sb.toString();
    return new Pair<>(s, CryptoUtils.computeHash(s, factory.getHashAlgorithm()));
  }

  /**
   * Extract the remote host name from the specified channel.
   * @param channel the channel that carries the host information.
   * @return the remote host name as a string.
   * @throws Exception if any error occurs.
   */
  static String getChannelHost(final ChannelWrapper<?> channel) throws Exception {
    if (channel instanceof SelectionKeyWrapper) {
      SelectionKeyWrapper skw = (SelectionKeyWrapper) channel;
      SocketChannel ch = (SocketChannel) skw.getChannel().channel();
      return  ((InetSocketAddress) (ch.getRemoteAddress())).getHostString();
    } else if (channel.isLocal()) {
      return "localhost";
    }
    return null;
  }
}
