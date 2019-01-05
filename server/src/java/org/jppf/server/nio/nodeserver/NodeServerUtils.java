/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.io.MultipleBuffersLocation;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.nio.*;
import org.jppf.node.protocol.*;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.serialization.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.ServerJob;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Collection of utility methods to help with job data service.
 * @author Laurent Cohen
 */
public class NodeServerUtils {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeServerUtils.class);

  /**
   * Compute a repeatable unique identifier for a node, which can be reused over node restarts.
   * @param factory bundler (load-balancer) factory.
   * @param channel the channel that carries the host information.
   * @param info the system information for the node.
   * @return a pair of string representing the clear string (keft side) and resulting unique string identifier for the node (right side).
   * @throws Exception if any error occurs.
   */
  public static Pair<String, String> getNodeIdentifier(final JPPFBundlerFactory factory, final ChannelWrapper<?> channel, final JPPFSystemInformation info) throws Exception {
    final StringBuilder sb = new StringBuilder();
    final String ip = NetworkUtils.getNonLocalHostAddress();
    sb.append('[').append(ip == null ? "localhost" : ip);
    if (channel instanceof SelectionKeyWrapper) {
      final SelectionKeyWrapper skw = (SelectionKeyWrapper) channel;
      final SocketChannel ch = (SocketChannel) skw.getChannel().channel();
      sb.append(':').append(ch.socket().getLocalPort()).append(']');
      final InetSocketAddress isa = (InetSocketAddress) ch.getRemoteAddress();
      sb.append(isa.getAddress().getHostAddress());
    } else if (channel.isLocal()) {
      sb.append( "local_channel").append(']');
    }
    final TypedProperties jppf = info.getJppf();
    final boolean master = jppf.get(JPPFProperties.PROVISIONING_MASTER);
    final boolean slave = jppf.get(JPPFProperties.PROVISIONING_SLAVE);
    if (master || slave) {
      sb.append(master ? "master" : "slave");
      sb.append(jppf.get(JPPFProperties.PROVISIONING_SLAVE_PATH_PREFIX));
      if (slave) sb.append(jppf.get(JPPFProperties.PROVISIONING_SLAVE_ID));
    }
    final String s = sb.toString();
    return new Pair<>(s, CryptoUtils.computeHash(s, factory.getHashAlgorithm()));
  }

  /**
   * Compute a repeatable unique identifier for a node, which can be reused over node restarts.
   * @param factory bundler (load-balancer) factory.
   * @param channel the channel that carries the host information.
   * @param info the system information for the node.
   * @return a pair of string representing the clear string (keft side) and resulting unique string identifier for the node (right side).
   * @throws Exception if any error occurs.
   */
  public static Pair<String, String> getNodeIdentifier(final JPPFBundlerFactory factory, final BaseNodeContext<?> channel, final JPPFSystemInformation info) throws Exception {
    final StringBuilder sb = new StringBuilder();
    final String ip = NetworkUtils.getNonLocalHostAddress();
    sb.append('[').append(ip == null ? "localhost" : ip);
    if (channel.getSocketChannel() != null) {
      final SocketChannel ch = channel.getSocketChannel();
      sb.append(':').append(ch.socket().getLocalPort()).append(']');
      final InetSocketAddress isa = (InetSocketAddress) ch.getRemoteAddress();
      sb.append(isa.getAddress().getHostAddress());
    } else if (channel.isLocal()) {
      sb.append( "local_channel").append(']');
    }
    final TypedProperties jppf = info.getJppf();
    final boolean master = jppf.get(JPPFProperties.PROVISIONING_MASTER);
    final boolean slave = jppf.get(JPPFProperties.PROVISIONING_SLAVE);
    if (master || slave) {
      sb.append(master ? "master" : "slave");
      sb.append(jppf.get(JPPFProperties.PROVISIONING_SLAVE_PATH_PREFIX));
      if (slave) sb.append(jppf.get(JPPFProperties.PROVISIONING_SLAVE_ID));
    }
    final String s = sb.toString();
    return new Pair<>(s, CryptoUtils.computeHash(s, factory.getHashAlgorithm()));
  }

  /**
   * Extract the remote host name from the specified channel.
   * @param channel the channel that carries the host information.
   * @return the remote host name as a string.
   * @throws Exception if any error occurs.
   */
  public static String getChannelHost(final ChannelWrapper<?> channel) throws Exception {
    if (channel instanceof SelectionKeyWrapper) {
      final SelectionKeyWrapper skw = (SelectionKeyWrapper) channel;
      final SocketChannel ch = (SocketChannel) skw.getChannel().channel();
      return  ((InetSocketAddress) (ch.getRemoteAddress())).getHostString();
    } else if (channel.isLocal()) {
      return "localhost";
    }
    return null;
  }

  /**
   * Extract the remote host name from the specified channel.
   * @param context the channel.
   * @return the remote host name as a string.
   * @throws Exception if any error occurs.
   */
  public static String getChannelHost(final BaseNodeContext<?> context) throws Exception {
    if (!context.isLocal()) {
      final SocketChannel ch = context.getSocketChannel();
      return  ((InetSocketAddress) (ch.getRemoteAddress())).getHostString();
    }
    else  return "localhost";
  }

  /**
   * Create the base server job used to generate the initial bundle sent to each node.
   * @param driver the JPPF driver.
   * @return a {@link ServerJob} instance, with no task in it.
   */
  public static ServerJob createInitialServerJob(final JPPFDriver driver) {
    try {
      final SerializationHelper helper = new SerializationHelperImpl();
      // serializing a null data provider.
      final JPPFBuffer buf = helper.getSerializer().serialize(null);
      final byte[] lengthBytes = SerializationUtils.writeInt(buf.getLength());
      final TaskBundle bundle = new JPPFTaskBundle();
      bundle.setName("server handshake");
      bundle.setUuid(driver.getUuid());
      bundle.getUuidPath().add(driver.getUuid());
      bundle.setTaskCount(0);
      bundle.setHandshake(true);
      final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
      final TypedProperties config = driver.getConfiguration();
      final Map<String, TypedProperties> defMap = new HashMap<>();
      defMap.putAll(factory.extractDefinitions(config, JPPFDatasourceFactory.Scope.REMOTE));
      bundle.setParameter(BundleParameter.DATASOURCE_DEFINITIONS, defMap);
      return new ServerJob(new ReentrantLock(), null, bundle, new MultipleBuffersLocation(new JPPFBuffer(lengthBytes), buf));
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }
}
