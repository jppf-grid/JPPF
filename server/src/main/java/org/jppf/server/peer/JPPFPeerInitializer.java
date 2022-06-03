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

import java.util.concurrent.atomic.*;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.concurrent.ThreadUtils;
import org.slf4j.*;


/**
 * Instances of this class are used to initialize the connections to a peer driver
 * in a separate thread.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class JPPFPeerInitializer implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFPeerInitializer.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Sequence number for connection uuids.
   */
  static final AtomicInteger SEQUENCE = new AtomicInteger(0);
  /**
   * Name of the peer in the configuration file.
   */
  private final String peerName;
  /**
   * Peer connection information.
   */
  private final JPPFConnectionInformation connectionInfo;
  /**
   * Determines whether communication with remote peer servers should be secure.
   */
  private final boolean secure;
  /**
   * Whether this connection and its pool were created by the discovery mechanism.
   */
  private final boolean fromDiscovery;
  /**
   * The class loader channel initializer.
   */
  private AbstractPeerConnectionHandler provider;
  /**
   * The job data channel initializer.
   */
  private AbstractPeerConnectionHandler node;
  /**
   * Whether this initializer is currently attempting to (re)connect to the peer.
   */
  private final AtomicBoolean connecting = new AtomicBoolean(false);
  /**
   * 
   */
  final String connectionUuid;
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;

  /**
   * Initialize this peer initializer from a specified peerName.
   * @param driver reference to the JPPF driver.
   * @param peerName the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public JPPFPeerInitializer(final JPPFDriver driver, final String peerName, final JPPFConnectionInformation connectionInfo, final boolean secure) {
    this(driver, peerName, connectionInfo, secure, false);
  }

  /**
   * Initialize this peer initializer from a specified peerName.
   * @param driver reference to the JPPF driver.
   * @param peerName the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param secure specifies whether the connection should be established over SSL/TLS.
   * @param fromDiscovery determines whether the connection info was obtained from the auto-discovery mechanism.
   */
  public JPPFPeerInitializer(final JPPFDriver driver, final String peerName, final JPPFConnectionInformation connectionInfo, final boolean secure, final boolean fromDiscovery) {
    if (peerName == null || peerName.isEmpty()) throw new IllegalArgumentException("peerName is blank");
    if (connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");
    this.driver         = driver;
    this.peerName       = peerName;
    this.connectionInfo = connectionInfo;
    this.secure         = secure;
    this.fromDiscovery  = fromDiscovery;
    this.connectionUuid = driver.getUuid() + '-' + SEQUENCE.incrementAndGet();
    log.debug("created new peer initializer {}", this);
  }

  @Override
  public synchronized void run() {
    if (debugEnabled) log.debug("start initialization of peer [{}]", peerName);
    try {
      if (connecting.compareAndSet(false, true)) {
        if (provider == null) {
          provider = new AsyncPeerResourceProvider(peerName, connectionInfo, driver.getAsyncClientClassServer(), secure, connectionUuid);
        }
        provider.init();
        if (node == null) node = new AsyncPeerNode(peerName, connectionInfo, driver.getAsyncClientNioServer(), secure, connectionUuid);
        node.onCloseAction = () -> start();
        node.init();
      }
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      if (provider != null) {
        provider.close();
        provider = null;
      }
      if (node != null) {
        node.close();
        node = null;
      }
      if (fromDiscovery) {
        final PeerDiscoveryThread pdt = driver.getInitializer().getPeerDiscoveryThread();
        if (pdt != null) {
          final boolean removed = pdt.removeConnectionInformation(connectionInfo);
          if (debugEnabled) log.debug((removed ? "successfully removed " : "failed to remove ") + "{}", connectionInfo);
        }
      }
    } finally {
      connecting.set(false);
      if (debugEnabled) log.debug("end initialization of peer [{}]", peerName);
    }
  }

  /**
   * Start a thread running this initializer.
   */
  public void start() {
    ThreadUtils.startThread(this, String.format("%s[%s]", getClass().getSimpleName(), peerName));
  }

  /**
   * 
   */
  public void close() {
    if (provider != null) {
      provider.close();
      provider = null;
    }
    if (node != null) {
      node.close();
      node = null;
    }
  }
}
