/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import org.jppf.utils.*;
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
    TypedProperties props = JPPFConfiguration.getProperties();
    broadcastInterval = props.getLong("jppf.discovery.broadcast.interval", 1000L);
    String group = props.getString("jppf.discovery.group", "230.0.0.1");
    int port = props.getInt("jppf.discovery.port", 11111);

    List<InetAddress> addresses = NetworkUtils.getNonLocalIPV4Addresses();
    addresses.addAll(NetworkUtils.getNonLocalIPV6Addresses());
    if (addresses.isEmpty()) addresses.add(InetAddress.getByName("127.0.0.1"));
    IPFilter filter = new IPFilter(props, true);
    List<InetAddress> filteredAddresses = new LinkedList<>();
    for (InetAddress addr: addresses) {
      if (filter.isAddressAccepted(addr)) filteredAddresses.add(addr);
    }
    if (debugEnabled) {
      StringBuilder sb = new StringBuilder();
      sb.append("Found ").append(filteredAddresses.size()).append(" address");
      if (filteredAddresses.size() > 1) sb.append("es");
      sb.append(':');
      for (InetAddress addr: filteredAddresses) sb.append(' ').append(addr.getHostAddress());
      log.debug(sb.toString());
    }
    socketsInfo = new ArrayList<>(filteredAddresses.size());
    for (InetAddress addr: addresses) {
      try {
        JPPFConnectionInformation ci = (JPPFConnectionInformation) info.clone();
        ci.host = addr.getHostAddress();
        byte[] infoBytes = JPPFConnectionInformation.toBytes(ci);
        ByteBuffer buffer = ByteBuffer.wrap(new byte[512]);
        buffer.putInt(infoBytes.length);
        buffer.put(infoBytes);
        DatagramPacket packet = new DatagramPacket(buffer.array(), 512, InetAddress.getByName(group), port);
        MulticastSocket socket = new MulticastSocket(port);
        socket.setInterface(addr);
        socketsInfo.add(new Pair<>(socket, packet));
      } catch(Exception e) {
        log.error("Unable to bind to interface " + addr.getHostAddress() + " on port " + port, e);
      }
    }
  }

  @Override
  public void run() {
    try {
      init();
    } catch(Exception e) {
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
          } catch(Exception e) {
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
