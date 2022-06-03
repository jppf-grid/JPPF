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

package org.jppf.comm.discovery;

import java.io.Closeable;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this class broadcast JPPF driver information at regular intervals,
 * to a configured UDP multicast group, to enable automatic discovery by clients,
 * nodes and peer drivers.
 * @author Laurent Cohen
 */
public class JPPFBroadcaster extends ThreadSynchronization implements Runnable, Closeable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFBroadcaster.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The UDP sockets to broadcast to, each bound to a different network interface.
   */
  private List<Pair<MulticastSocket, DatagramPacket>> socketsInfo;
  /**
   * Frequency of the broadcast in milliseconds.
   */
  private long broadcastInterval = 1000L;
  /**
   * Holds the driver connection information to broadcast.
   */
  private JPPFConnectionInformation info = null;
  /**
   * Used to keep track of sockets for which an error was detected.
   */
  private Set<Pair<MulticastSocket, DatagramPacket>> socketsInError = new HashSet<>();

  /**
   * Initialize this broadcaster using the server configuration information.
   * @param info holds the driver connection information to broadcast.
   */
  public JPPFBroadcaster(final JPPFConnectionInformation info) {
    this.info = info;
  }

  /**
   * Initialize the broadcast socket and data.
   * @throws Exception if an error occurs while initializing the datagram packet or socket.
   */
  private void init() throws Exception {
    final TypedProperties props = JPPFConfiguration.getProperties();
    broadcastInterval = props.get(JPPFProperties.DISCOVERY_BROADCAST_INTERVAL);
    final String group = props.get(JPPFProperties.DISCOVERY_GROUP);
    final int port = props.get(JPPFProperties.DISCOVERY_PORT);

    final List<InetAddress> addresses = NetworkUtils.getNonLocalIPV4Addresses();
    addresses.addAll(NetworkUtils.getNonLocalIPV6Addresses());
    if (addresses.isEmpty()) addresses.add(InetAddress.getByName("127.0.0.1"));
    final IPFilter filter = new IPFilter(props, true);
    final List<InetAddress> filteredAddresses = new LinkedList<>();
    for (final InetAddress addr: addresses) {
      if (filter.isAddressAccepted(addr)) filteredAddresses.add(addr);
    }
    if (debugEnabled) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Found ").append(filteredAddresses.size()).append(" address");
      if (filteredAddresses.size() > 1) sb.append("es");
      sb.append(':');
      for (InetAddress addr: filteredAddresses) sb.append(' ').append(addr.getHostAddress());
      log.debug(sb.toString());
    }
    socketsInfo = new ArrayList<>(filteredAddresses.size());
    for (final InetAddress addr: addresses) {
      try {
        final JPPFConnectionInformation ci = (JPPFConnectionInformation) info.clone();
        ci.host = addr.getHostAddress();
        final byte[] infoBytes = JPPFConnectionInformation.toBytes(ci);
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[512]);
        buffer.putInt(infoBytes.length);
        buffer.put(infoBytes);
        final DatagramPacket packet = new DatagramPacket(buffer.array(), 512, InetAddress.getByName(group), port);
        final MulticastSocket socket = new MulticastSocket(port);
        socket.setInterface(addr);
        socketsInfo.add(new Pair<>(socket, packet));
      } catch(final Exception e) {
        log.error("Unable to bind to interface " + addr.getHostAddress() + " on port " + port, e);
      }
    }
  }

  @Override
  public void run() {
    try {
      init();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      close();
      return;
    }
    if (!socketsInfo.isEmpty()) {
      while (!isStopped()) {
        for (Pair<MulticastSocket, DatagramPacket> si: socketsInfo) {
          try {
            if (isStopped()) break;
            si.first().send(si.second());
            if (socketsInError.contains(si)) socketsInError.remove(si);
          } catch(final Exception e) {
            if (!isStopped() && !socketsInError.contains(si)) {
              socketsInError.add(si);
              log.error(e.getMessage(), e);
            }
          }
        }
        if (!isStopped()) goToSleep(broadcastInterval);
      }
    }
    close();
  }

  @Override
  public void close() {
    setStopped(true);
    for (Pair<MulticastSocket, DatagramPacket> si: socketsInfo) si.first().close();
    socketsInfo.clear();
  }
}
