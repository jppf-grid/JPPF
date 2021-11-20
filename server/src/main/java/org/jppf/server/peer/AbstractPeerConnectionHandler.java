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

package org.jppf.server.peer;

import java.net.*;

import org.jppf.JPPFException;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Common super class for the class loader and job data channels of a peer driver connection.
 * @author Laurent Cohen
 */
abstract class AbstractPeerConnectionHandler implements AutoCloseable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractPeerConnectionHandler.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether ssl is enabled for peer-to-peer cpmmunication between servers.
   */
  final boolean secure;
  /**
   * Peer connection information.
   */
  final JPPFConnectionInformation connectionInfo;
  /**
   * Wrapper around the underlying socket connection.
   */
  SocketChannelClient socketClient = null;
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  SocketInitializer socketInitializer = SocketInitializer.Factory.newInstance();
  /**
   * Name of this remote peer connection.
   */
  final String name;
  /**
   * An optional action to perform upon closing the associated channel.
   */
  Runnable onCloseAction;
  /**
   * Wether to print the start and end of connection messages.
   */
  boolean printConnectionMessage;
  /**
   * The connection uuid, common to client class server and job server connections.
   */
  final String connectionUuid;
  /**
   * The channel identifier.
   */
  final int channelIdentifier;

  /**
   * Initialize this peer provider with the specified configuration name.
   * @param peerNameBase the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param secure {@code true} if the connection is established over SSL, {@code false} otherwise.
   * @param connectionUuid the connection uuid, common to client class server and job server connections.
   * @param channelIdentifier the channel identifier value which announces the type of channel to the remote peer.
   */
  public AbstractPeerConnectionHandler(final String peerNameBase, final JPPFConnectionInformation connectionInfo, final boolean secure, final String connectionUuid, final int channelIdentifier) {
    if (peerNameBase == null || peerNameBase.isEmpty()) throw new IllegalArgumentException("peerName is blank");
    if (connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");
    this.connectionInfo = connectionInfo;
    this.secure = secure;
    this.connectionUuid = connectionUuid;
    this.name = peerNameBase;
    this.channelIdentifier = channelIdentifier;
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  public synchronized void init() throws Exception {
    if (socketClient == null) socketClient = initSocketChannel();
    final String cname = String.format("%s@%s:%d", name, socketClient.getHost(), socketClient.getPort());
    if (printConnectionMessage) {
      final String msg = "Attempting connection to remote peer " + cname;
      log.info(msg);
      System.out.println(msg);
    }
    if (!socketInitializer.initialize(socketClient)) throw new ConnectException("could not connect to peer " + cname);
    if (!InterceptorHandler.invokeOnConnect(socketClient, JPPFIdentifiers.descriptorFor(channelIdentifier))) throw new JPPFException("peer connection denied by interceptor");
    if (debugEnabled) log.debug("Connected to peer {}, sending channel identifier", cname);
    socketClient.writeInt(channelIdentifier);
    if (printConnectionMessage) {
      final String msg = "Reconnected to remote peer " + cname;
      log.info(msg);
      System.out.println(msg);
    }
    postInit();
  }

  @Override
  public abstract void close();

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  abstract void postInit() throws Exception;

  /**
   * Initialize the socket channel client.
   * @return a non-connected <code>SocketChannelClient</code> instance.
   * @throws Exception if an error is raised during initialization.
   */
  private SocketChannelClient initSocketChannel() throws Exception {
    String host = connectionInfo.host == null || connectionInfo.host.isEmpty() ? "localhost" : connectionInfo.host;
    host = InetAddress.getByName(host).getHostName();
    final int port = secure ? connectionInfo.sslServerPorts[0] : connectionInfo.serverPorts[0];
    return new SocketChannelClient(host, port, false);
  }
}
