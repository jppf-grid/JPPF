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
package org.jppf.server.peer;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;


/**
 * Instances of this class are used to initialize the connections to a peer driver
 * in a separate thread.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class JPPFPeerInitializer extends Thread {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFPeerInitializer.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Name of the peer in the configuration file.
   */
  private final String peerName;
  /**
   * Peer connection information.
   */
  private final JPPFConnectionInformation connectionInfo;
  /**
   * JPPF class server
   */
  private final ClientClassNioServer classServer;
  /**
   * Determines whether communication with remote peer servers should be secure.
   */
  private final boolean secure;
  /**
   * 
   */
  private final boolean fromDiscovery;

  /**
   * Initialize this peer initializer from a specified peerName.
   * @param peerName the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param classServer JPPF class server
   * @param secure specifies whether the connection should be established over SSL/TLS.
   */
  public JPPFPeerInitializer(final String peerName, final JPPFConnectionInformation connectionInfo, final ClientClassNioServer classServer, final boolean secure) {
    this(peerName, connectionInfo, classServer, secure, false);
  }

  /**
   * Initialize this peer initializer from a specified peerName.
   * @param peerName the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param classServer JPPF class server
   * @param secure specifies whether the connection should be established over SSL/TLS.
   * @param fromDiscovery determines whether the connection info was obtained from the auto-discovery mechanism.
   */
  public JPPFPeerInitializer(final String peerName, final JPPFConnectionInformation connectionInfo, final ClientClassNioServer classServer, final boolean secure, final boolean fromDiscovery) {
    if (peerName == null || peerName.isEmpty()) throw new IllegalArgumentException("peerName is blank");
    if (connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");
    this.peerName       = peerName;
    this.connectionInfo = connectionInfo;
    this.classServer    = classServer;
    this.secure         = secure;
    this.fromDiscovery = fromDiscovery;
    setName(String.format("%s[%s]", getClass().getSimpleName(), peerName));
    log.debug("created new peer initializer {}", this);
  }

  /**
   * Perform the peer initialization.
   */
  @Override
  public void run() {
    boolean end = false;
    while (!end) {
      log.info("start initialization of peer [{}]", peerName);
      PeerResourceProvider prp = null;
      try {
        prp = new PeerResourceProvider(peerName, connectionInfo, classServer, secure);
        prp.init();
        new PeerNode(peerName, connectionInfo, secure).run();
      } catch(Exception e) {
        log.error(e.getMessage(), e);
        if (prp != null) {
          prp.close();
          prp = null;
        }
        if (fromDiscovery) {
          PeerDiscoveryThread pdt = JPPFDriver.getInstance().getInitializer().getPeerDiscoveryThread();
          if (pdt != null) {
            boolean removed = pdt.removeConnectionInformation(connectionInfo);
            if (debugEnabled) log.debug((removed ? "successfully removed " : "failure to remove ") + "{}", connectionInfo);
          }
          end = true;
        }
      } finally {
        log.info("end initialization of peer [{}]", peerName);
      }
    }
  }
}
